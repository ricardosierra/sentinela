package org.sentinela.app

import android.content.Context
import android.telephony.TelephonyManager
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.preferencesDataStoreFile
import androidx.room.Room
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import org.sentinela.app.data.contacts.ContactKeyCache
import org.sentinela.app.data.contacts.ContactLookupRepository
import org.sentinela.app.data.contacts.ContactsContractLookupSource
import org.sentinela.app.data.contacts.DefaultContactLookupRepository
import org.sentinela.app.data.local.PersonalWhitelistRepository
import org.sentinela.app.data.local.RoomBlockedCallRepository
import org.sentinela.app.data.local.RoomWhitelistRepository
import org.sentinela.app.data.local.db.SENTINELA_MIGRATIONS
import org.sentinela.app.data.local.db.SentinelaDatabase
import org.sentinela.app.domain.CallDecisionEngine
import org.sentinela.app.notifications.AndroidBlockedCallNotifier
import org.sentinela.app.notifications.BlockedCallNotifier
import org.sentinela.app.settings.DataStoreSettingsRepository
import org.sentinela.app.phone.CascadingRegionProvider
import org.sentinela.app.phone.LibPhoneNumberNormalizer
import org.sentinela.app.phone.PhoneMask
import org.sentinela.app.phone.PhoneNumberNormalizer
import org.sentinela.app.phone.RegionProvider
import org.sentinela.app.phone.phoneNumberUtil
import org.sentinela.app.telecom.CallResponseFactory
import org.sentinela.app.telecom.PostScreeningWork
import org.sentinela.app.telecom.ScreenedCallFactory
import org.sentinela.app.telecom.ScreeningCoordinator
import org.sentinela.app.telecom.ScreeningDependencies
import org.sentinela.app.platform.AndroidRegionProvider
import org.sentinela.app.platform.assetsPhoneMetadataLoader

/**
 * Container de dependências manual, instância única do processo. Nada é construído na partida:
 * cada colaborador nasce preguiçoso, na primeira vez que alguém precisa dele, porque este
 * arquivo define o custo de partida a frio do caminho de resposta ao sistema de telefonia.
 *
 * Ele também cumpre o contrato de que o serviço de triagem precisa, para que nem o serviço nem
 * a interface instanciem qualquer coisa por conta própria.
 */
class AppContainer(
    private val appContext: Context,
) : ScreeningDependencies {

    val decisionEngine: CallDecisionEngine by lazy { CallDecisionEngine() }

    private val regionProvider: RegionProvider by lazy {
        CascadingRegionProvider(
            device = AndroidRegionProvider(
                appContext.getSystemService(TelephonyManager::class.java),
            ),
            // Degrau 2 (preferência do usuário) só ganha persistência na Fase 3;
            // aqui o contrato existe com fallback em memória.
            userPreference = RegionProvider { null },
        )
    }

    private val phoneUtil by lazy { phoneNumberUtil(assetsPhoneMetadataLoader(appContext)) }

    /**
     * Instância única. `createInstance` desserializa metadados (dezenas de ms): construir aqui,
     * NUNCA dentro de `onScreenCall` — a Fase 5 tem orçamento p95 < 200 ms.
     */
    val phoneNumberNormalizer: PhoneNumberNormalizer by lazy {
        LibPhoneNumberNormalizer(
            util = phoneUtil,
            regionProvider = regionProvider,
        )
    }

    /**
     * Escopo de processo para o cache do DataStore e para o trabalho de abertura.
     * `SupervisorJob`: uma falha na poda não pode cancelar o collector das
     * configurações, que alimenta o caminho quente do Service.
     */
    private val appScope: CoroutineScope =
        CoroutineScope(SupervisorJob() + Dispatchers.IO)

    /**
     * Instância ÚNICA do banco. Construir mais de uma vez custaria abertura de
     * SQLite no caminho quente do Service (Fase 5).
     * A migração destrutiva do Room é PROIBIDA aqui: apagaria a whitelist do
     * usuário numa atualização (recusada por scripts/verify-invariants.sh).
     *
     * O spread copia a cadeia de migrações uma única vez, na criação do banco, e é a
     * assinatura que o Room oferece — o custo é irrelevante fora do caminho quente.
     * Suprimido aqui, no ponto de uso, em vez de afrouxar a regra no detekt.yml
     * compartilhado, que pagaria uma dívida global por um caso local.
     */
    @Suppress("SpreadOperator")
    private val database: SentinelaDatabase by lazy {
        Room.databaseBuilder(appContext, SentinelaDatabase::class.java, SentinelaDatabase.NAME)
            .addMigrations(*SENTINELA_MIGRATIONS)
            .build()
    }

    /**
     * Instância ÚNICA do DataStore. O runtime derruba o processo se existirem duas
     * instâncias sobre o mesmo arquivo — reproduzido na pesquisa da Fase 3. Singleton
     * aqui é contrato de execução, não estilo; por isso também não se usa o delegate
     * de Context, que esconde a instância no ponto de uso.
     * Caminho real: files/datastore/sentinela_settings.preferences_pb, excluído do
     * backup pela exclusão recursiva do diretório `datastore`.
     */
    private val settingsDataStore: DataStore<Preferences> by lazy {
        PreferenceDataStoreFactory.create(scope = appScope) {
            appContext.preferencesDataStoreFile(SETTINGS_DATASTORE_NAME)
        }
    }

    val settingsRepository: DataStoreSettingsRepository by lazy {
        DataStoreSettingsRepository(settingsDataStore, appScope)
    }

    val whitelistRepository: PersonalWhitelistRepository by lazy {
        RoomWhitelistRepository(database.whitelistDao())
    }

    val blockedCallRepository: RoomBlockedCallRepository by lazy {
        RoomBlockedCallRepository(database.blockedCallDao(), settingsRepository)
    }

    /**
     * Chamado uma vez na abertura do app: incrementa o contador (ENG-01, base do
     * convite de avaliação da Fase 9) e aplica a retenção (HST-02).
     * Nenhum agendador em segundo plano: podar uma tabela local pequena não paga a
     * dependência nem o custo de cold start.
     */
    fun onAppOpened() {
        appScope.launch {
            settingsRepository.incrementAppOpenCount()
            blockedCallRepository.pruneNow()
        }
    }

    /**
     * Instância ÚNICA do processo. Nada aqui é construído em `Application.onCreate`: a fonte, o
     * cache e o registro do observador são PREGUIÇOSOS, só na primeira consulta. Construir o
     * conjunto de chaves foi medido em 2,57 s com 5.000 contatos (plano 04-04) — 12× o orçamento
     * inteiro da decisão —, por isso ele nunca é aguardado: cache frio responde pela sonda direta.
     * O registro do observador morre com o processo; `close()` existe para os testes
     * instrumentados e em produção nunca é chamado.
     */
    val contactLookupRepository: ContactLookupRepository by lazy {
        val source = ContactsContractLookupSource(appContext)
        DefaultContactLookupRepository(
            source = source,
            cache = ContactKeyCache(
                source = source,
                normalizer = phoneNumberNormalizer,
                scope = appScope,
            ),
            normalizer = phoneNumberNormalizer,
        )
    }

    override val screenedCallFactory: ScreenedCallFactory by lazy {
        ScreenedCallFactory(phoneNumberNormalizer)
    }

    override val callResponseFactory: CallResponseFactory by lazy { CallResponseFactory() }

    /**
     * Canal silencioso e opcional. Preguicoso de proposito: criar canal e tocar no servico de
     * notificacoes na partida do processo pesaria no orcamento do caminho de resposta.
     */
    val blockedCallNotifier: BlockedCallNotifier by lazy {
        AndroidBlockedCallNotifier(appContext) { settingsRepository.cachedSnapshot() }
    }

    override val postScreeningWork: PostScreeningWork by lazy {
        PostScreeningWork(
            settings = settingsRepository,
            history = blockedCallRepository,
            notifier = blockedCallNotifier,
            mask = { numero -> PhoneMask.mask(phoneUtil, numero) },
        )
    }

    override val screeningCoordinator: ScreeningCoordinator by lazy {
        ScreeningCoordinator(
            settings = settingsRepository,
            contacts = contactLookupRepository,
            whitelist = whitelistRepository,
            blockedCalls = blockedCallRepository,
            engine = decisionEngine,
        )
    }

    override fun launchAfterResponse(block: suspend () -> Unit) {
        appScope.launch { runCatching { block() } }
    }

    // TODO(Fase 6): componentes do modo discador (InCallService/ROLE_DIALER).

    private companion object {
        const val SETTINGS_DATASTORE_NAME = "sentinela_settings"
    }
}
