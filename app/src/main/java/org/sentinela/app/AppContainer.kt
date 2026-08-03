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
import org.sentinela.app.notifications.IncomingCallNotifier
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
import org.sentinela.app.telecom.call.CallSessionStore
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

    /**
     * Deixou de ser privado no plano 06-05 pelo mesmo motivo de [phoneUtil]: o formatador
     * progressivo da tela de discagem precisa de uma região, e travá-la em `BR` contrariaria a
     * cascata decidida na Fase 2 (aparelho → preferência do usuário → `BR`).
     */
    val regionProvider: RegionProvider by lazy {
        CascadingRegionProvider(
            device = AndroidRegionProvider(
                appContext.getSystemService(TelephonyManager::class.java),
            ),
            // Degrau 2 (preferência do usuário) só ganha persistência na Fase 3;
            // aqui o contrato existe com fallback em memória.
            userPreference = RegionProvider { null },
        )
    }

    /**
     * Deixou de ser privado no plano 06-05: a tela de discagem precisa do formatador progressivo
     * da própria biblioteca (`getAsYouTypeFormatter`) para mostrar `(11) 91234-5678` enquanto o
     * usuário digita. Construir um segundo util só para a tela recarregaria os metadados inteiros —
     * a instância continua sendo **uma** no processo, que é o invariante que importa aqui.
     */
    val phoneUtil by lazy { phoneNumberUtil(assetsPhoneMetadataLoader(appContext)) }

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

    init {
        // Pre-warm: carrega os metadados do libphonenumber em background assim que o
        // AppContainer nasce, evitando StrictMode violation (I/O) na main thread durante a triagem.
        appScope.launch { phoneUtil }
    }

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

    /**
     * Máscara única de exibição, com os metadados de telefone já resolvidos. Exposta porque a
     * camada de telefonia precisa mascarar a identidade **antes** de entregá-la ao aviso do
     * sistema: quem publica notificação nunca recebe número cru.
     */
    val maskNumber: (String) -> String by lazy {
        { numero -> PhoneMask.mask(phoneUtil, numero) }
    }

    /**
     * Aviso de chamada do modo discador (chamada recebida em tela cheia e chamada em curso).
     * Preguiçoso pelo mesmo motivo do aviso de chamada bloqueada: criar canal e tocar no serviço
     * de notificações na partida do processo pesaria no orçamento do caminho de resposta ao
     * sistema de telefonia. No modo filtro, que é o padrão, este objeto nunca nasce.
     */
    val incomingCallNotifier: IncomingCallNotifier by lazy { IncomingCallNotifier(appContext) }

    override val postScreeningWork: PostScreeningWork by lazy {
        PostScreeningWork(
            settings = settingsRepository,
            history = blockedCallRepository,
            notifier = blockedCallNotifier,
            mask = maskNumber,
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

    /**
     * Armazém da sessão de chamada do modo discador. Instância ÚNICA do processo: a Fase 5 mediu
     * que um segundo container derruba o processo, e aqui a exigência é ainda mais direta — o
     * serviço de chamada e a tela de chamada precisam olhar para o MESMO estado, senão a tela
     * mostra uma ligação e o usuário comanda outra.
     *
     * Recebe o escopo real do processo de propósito: é ele que liga o vigia do prazo de
     * apresentação do coordenador. Escopo ausente desliga o vigia e existe apenas para os testes
     * de lógica pura.
     *
     * Preguiçoso, como todo o resto: no modo filtro, que é o padrão, este objeto nunca nasce.
     */
    val callSessionStore: CallSessionStore by lazy {
        CallSessionStore(
            scope = appScope,
            // A troca do aviso de chamada recebida pelo aviso de chamada em curso é a transição
            // para o estado ativo, e o armazém é o único que a conhece. O notificador continua
            // preguiçoso: esta referência só o constrói quando a primeira chamada fica ativa.
            notifications = { identity -> incomingCallNotifier.notifyOngoing(identity) },
            maskNumber = maskNumber,
        )
    }

    private companion object {
        const val SETTINGS_DATASTORE_NAME = "sentinela_settings"
    }
}
