package org.sentinela.app.telecom

import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.flow.flowOf
import org.sentinela.app.data.contacts.ContactLookupRepository
import org.sentinela.app.data.local.BlockedCallEntry
import org.sentinela.app.data.local.BlockedCallRepository
import org.sentinela.app.data.local.CallClassification
import org.sentinela.app.data.local.PersonalWhitelistRepository
import org.sentinela.app.data.local.WhitelistEntry
import org.sentinela.app.domain.CallDecision
import org.sentinela.app.domain.CallDecisionEngine
import org.sentinela.app.domain.ContactLookup
import org.sentinela.app.domain.RepeatedCallLookup
import org.sentinela.app.domain.ScreenedCall
import org.sentinela.app.domain.WhitelistLookup
import org.sentinela.app.settings.ScreeningSettings
import org.sentinela.app.notifications.BlockedCallNotifier
import org.sentinela.app.phone.CascadingRegionProvider
import org.sentinela.app.phone.LibPhoneNumberNormalizer
import org.sentinela.app.phone.RegionProvider
import org.sentinela.app.phone.TestMetadata
import org.sentinela.app.settings.SettingsRepository

/** Mascara fixa: o teste do serviço não mede normalização, mede ligação. */
const val MASCARA_DE_TESTE = "+55 11 9****-8888"

const val AGORA_DE_TESTE = 1_700_000_000_000L

/**
 * Dublês controláveis dos quatro colaboradores do [ScreeningCoordinator].
 *
 * Cada um tem dois interruptores: `falha` (lança na próxima consulta) e
 * `atrasoMillis` (suspende antes de responder, para exercitar o prazo interno).
 * São eles que permitem injetar exceção em CADA ponto do caminho de triagem,
 * que é o objeto do plano 05-03 — um caminho que não responde nunca aparece em
 * teste de caminho feliz.
 */
class FakeSettingsRepository(
    var valor: ScreeningSettings = ScreeningSettings(),
) : SettingsRepository {
    var falha: Boolean = false
    var atrasoMillis: Long = 0
    var chamadas: Int = 0

    override val settings: Flow<ScreeningSettings> get() = flowOf(valor)

    override suspend fun snapshot(): ScreeningSettings {
        chamadas++
        if (atrasoMillis > 0) delay(atrasoMillis)
        if (falha) error("falha injetada nas configurações")
        return valor
    }

    override suspend fun update(transform: (ScreeningSettings) -> ScreeningSettings) {
        valor = transform(valor)
    }
}

class FakeContactLookupRepository(
    var resultado: ContactLookup = ContactLookup.MISS,
) : ContactLookupRepository {
    var falha: Boolean = false
    var atrasoMillis: Long = 0
    var chamadas: Int = 0

    override suspend fun lookup(numberE164: String): ContactLookup {
        chamadas++
        if (atrasoMillis > 0) delay(atrasoMillis)
        if (falha) error("falha injetada na agenda")
        return resultado
    }
}

class FakePersonalWhitelistRepository(
    var presente: Boolean = false,
) : PersonalWhitelistRepository {
    var falha: Boolean = false
    var atrasoMillis: Long = 0
    var chamadas: Int = 0

    override suspend fun contains(numberE164: String): Boolean {
        chamadas++
        if (atrasoMillis > 0) delay(atrasoMillis)
        if (falha) error("falha injetada na whitelist")
        return presente
    }

    override fun observeAll(): Flow<List<WhitelistEntry>> = emptyFlow()

    override fun search(query: String): Flow<List<WhitelistEntry>> = emptyFlow()

    override suspend fun upsert(entry: WhitelistEntry) = Unit

    override suspend fun delete(id: Long) = Unit
}

class FakeBlockedCallRepository(
    var resultado: RepeatedCallLookup = RepeatedCallLookup.MISS,
) : BlockedCallRepository {
    var falha: Boolean = false
    var atrasoMillis: Long = 0
    var chamadas: Int = 0
    var ultimoAgora: Long = -1

    val gravados = mutableListOf<BlockedCallEntry>()
    var idGravado: Long = 0

    override suspend fun record(entry: BlockedCallEntry): Long {
        gravados += entry
        return idGravado
    }

    override fun observeRecent(): Flow<List<BlockedCallEntry>> = flowOf(emptyList())

    override fun observeTotalCount(): Flow<Long> = flowOf(0L)

    override suspend fun deleteById(id: Long) = Unit

    override suspend fun clearAll() = Unit

    override suspend fun pruneOlderThan(utcMillis: Long) {
        val keep = gravados.filter { it.timestampUtcMillis >= utcMillis }
        gravados.clear()
        gravados.addAll(keep)
    }

    override suspend fun updateClassification(id: Long, classification: CallClassification) {
        val index = gravados.indexOfFirst { it.id == id }
        if (index != -1) {
            gravados[index] = gravados[index].copy(classification = classification)
        }
    }

    override suspend fun hasRecentBlock(numberE164: String?, nowUtcMillis: Long): RepeatedCallLookup {
        chamadas++
        ultimoAgora = nowUtcMillis
        if (atrasoMillis > 0) delay(atrasoMillis)
        if (falha) error("falha injetada no histórico")
        return resultado
    }
}

/** Motor que pode ser mandado explodir, para provar que nem o núcleo derruba a resposta. */
class ExplodingDecisionEngine : CallDecisionEngine() {
    var falha: Boolean = false

    override fun decide(
        call: ScreenedCall,
        settings: ScreeningSettings,
        contact: ContactLookup,
        whitelist: WhitelistLookup,
        repeatedCall: RepeatedCallLookup,
    ): CallDecision {
        if (falha) error("falha injetada no motor")
        return super.decide(call, settings, contact, whitelist, repeatedCall)
    }
}

/** Coletor de decisões emitidas: a contagem é a prova da resposta única. */
class RespostaGravada {
    val decisoes = mutableListOf<CallDecision>()
    var falharNaPrimeira: Boolean = false

    val total: Int get() = decisoes.size
    val unica: CallDecision get() = decisoes.single()

    val configuracoes = mutableListOf<ScreeningSettings>()

    fun costura(): (CallDecision, ScreeningSettings) -> Unit = { decisao, valores ->
        decisoes += decisao
        configuracoes += valores
        if (falharNaPrimeira && decisoes.size == 1) error("falha injetada na costura de resposta")
    }
}

/** Notificador de teste: só registra o que foi pedido, sem tocar na plataforma. */
class NotificadorGravado : BlockedCallNotifier {
    val enviadas = mutableListOf<BlockedCallEntry>()

    /**
     * Faz o trabalho posterior à resposta explodir. Existe para provar que uma falha DEPOIS de a
     * decisão já ter saído não vira uma segunda resposta ao sistema — a regressão da Fase 8.
     */
    var explode: Boolean = false

    override fun ensureChannel() = Unit
    override fun notifyBlocked(entry: BlockedCallEntry) {
        enviadas += entry
        if (explode) error("falha injetada no trabalho posterior à resposta")
    }
}

/**
 * Substitui o container do processo por dublês. Construir um container de verdade aqui
 * derrubaria o processo do teste: dois armazenamentos de configurações sobre o mesmo arquivo
 * não coexistem, e isso já foi medido.
 */
class AmbienteDeTriagem(motorComDefeito: Boolean = false) : ScreeningDependencies {
    val settings = FakeSettingsRepository()
    val contatos = FakeContactLookupRepository()
    val whitelist = FakePersonalWhitelistRepository()
    val historico = FakeBlockedCallRepository()
    val notificador = NotificadorGravado()

    private val motor = if (motorComDefeito) {
        ExplodingDecisionEngine().apply { falha = true }
    } else {
        CallDecisionEngine()
    }

    override val screenedCallFactory = ScreenedCallFactory(
        LibPhoneNumberNormalizer(
            TestMetadata.util(),
            RegionProvider { CascadingRegionProvider.DEFAULT_REGION },
        ),
    )

    override val callResponseFactory = CallResponseFactory()

    override val screeningCoordinator = ScreeningCoordinator(
        settings = settings,
        contacts = contatos,
        whitelist = whitelist,
        blockedCalls = historico,
        engine = motor,
        clock = { AGORA_DE_TESTE },
    )

    override val postScreeningWork = PostScreeningWork(
        settings = settings,
        history = historico,
        notifier = notificador,
        mask = { MASCARA_DE_TESTE },
        clock = { AGORA_DE_TESTE },
    )

    /**
     * Em produção isto vai para o escopo do processo. No teste roda de forma síncrona: a
     * ordem já é provada pelo coordenador, e aqui o que importa é o resultado observável.
     */
    override fun launchAfterResponse(block: suspend () -> Unit) {
        runBlocking { runCatching { block() } }
    }
}
