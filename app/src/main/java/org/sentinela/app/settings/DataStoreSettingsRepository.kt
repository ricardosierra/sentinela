package org.sentinela.app.settings

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.MutablePreferences
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch
import java.io.IOException

/**
 * Configurações de triagem persistidas em DataStore Preferences.
 *
 * A instância de [DataStore] é recebida pronta, nunca criada aqui por delegate de
 * Context: o runtime lança se existirem duas instâncias sobre o mesmo arquivo, então
 * a instância única do `AppContainer` é contrato de execução, não estilo.
 *
 * Nenhuma chamada de rede e nenhum valor de configuração é escrito em log.
 */
class DataStoreSettingsRepository(
    private val dataStore: DataStore<Preferences>,
    scope: CoroutineScope,
) : SettingsRepository {

    /**
     * Caminho quente do CallScreeningService (Fase 5). Medido na pesquisa da Fase 3:
     * a PRIMEIRA leitura do DataStore custa 10,9 ms; as seguintes p95 3,87 ms.
     * Cabe nos 200 ms, mas somado a Room + normalização come metade do orçamento —
     * por isso o snapshot é servido de memória.
     */
    @Volatile
    private var cached: ScreeningSettings? = null

    override val settings: Flow<ScreeningSettings> =
        dataStore.data
            // Arquivo corrompido NÃO pode derrubar o Flow: por tabela, derrubaria a
            // decisão. emptyPreferences() cai nos defaults SEGUROS do MVP.
            .catch { e -> if (e is IOException) emit(emptyPreferences()) else throw e }
            .map { it.toScreeningSettings() }
            .onEach { cached = it }

    /** ENG-01: contador local de aberturas, base do convite de avaliação (Fase 9). */
    val appOpenCount: Flow<Int> =
        dataStore.data
            .catch { e -> if (e is IOException) emit(emptyPreferences()) else throw e }
            .map { it[Keys.APP_OPEN_COUNT] ?: 0 }

    /**
     * CTT-01: distingue "nunca pedimos" de "negada permanentemente" — a plataforma devolve
     * `shouldShowRequestPermissionRationale = false` nos dois casos. Ver
     * [org.sentinela.app.data.contacts.contactsPermissionState].
     *
     * Deliberadamente fora de [ScreeningSettings]: não é configuração de triagem e não pode
     * entrar no snapshot servido no caminho quente do Service.
     */
    val contactsPermissionAsked: Flow<Boolean> =
        dataStore.data
            .catch { e -> if (e is IOException) emit(emptyPreferences()) else throw e }
            .map { it[Keys.CONTACTS_PERMISSION_ASKED] ?: false }

    /**
     * Chamar no momento em que o launcher da permissão é disparado, NUNCA no callback: o
     * usuário pode matar o app com o diálogo do sistema aberto. Idempotente.
     */
    suspend fun markContactsPermissionAsked() {
        dataStore.edit { it[Keys.CONTACTS_PERMISSION_ASKED] = true }
    }

    init {
        // Aquece e mantém o cache enquanto o processo viver.
        scope.launch { settings.collect() }
    }

    override suspend fun snapshot(): ScreeningSettings =
        cached ?: settings.first().also { cached = it }

    override suspend fun update(transform: (ScreeningSettings) -> ScreeningSettings) {
        dataStore.edit { prefs -> transform(prefs.toScreeningSettings()).writeInto(prefs) }
    }

    /** Devolve o novo valor. Atômico: o edit() do DataStore é transacional. */
    suspend fun incrementAppOpenCount(): Int {
        var updated = 0
        dataStore.edit { prefs ->
            updated = (prefs[Keys.APP_OPEN_COUNT] ?: 0) + 1
            prefs[Keys.APP_OPEN_COUNT] = updated
        }
        return updated
    }

    /**
     * Chaves persistidas. O nome textual é o contrato com o disco: renomear uma chave
     * descarta silenciosamente a configuração já gravada do usuário.
     */
    private object Keys {
        val PROTECTION_ENABLED = booleanPreferencesKey("protection_enabled")
        val UNKNOWN_POLICY = stringPreferencesKey("unknown_policy")
        val CONTACTS_POLICY = stringPreferencesKey("contacts_policy")
        val WHITELIST_POLICY = stringPreferencesKey("whitelist_policy")
        val BLOCK_PRIVATE_NUMBERS = booleanPreferencesKey("block_private_numbers")
        val BLOCK_MODE = stringPreferencesKey("block_mode")
        val HIDE_FROM_NATIVE_CALL_LOG = booleanPreferencesKey("hide_from_native_call_log")
        val SHOW_OWN_NOTIFICATION = booleanPreferencesKey("show_own_notification")
        val FALLBACK_POLICY = stringPreferencesKey("fallback_policy")
        val HISTORY_ENABLED = booleanPreferencesKey("history_enabled")
        val RETENTION_POLICY = stringPreferencesKey("retention_policy")
        val APP_OPEN_COUNT = intPreferencesKey("app_open_count")
        val CONTACTS_PERMISSION_ASKED = booleanPreferencesKey("contacts_permission_asked")
    }

    private fun Preferences.toScreeningSettings(): ScreeningSettings {
        val padrao = ScreeningSettings()
        return ScreeningSettings(
            protectionEnabled = this[Keys.PROTECTION_ENABLED] ?: padrao.protectionEnabled,
            unknownPolicy = origin(this[Keys.UNKNOWN_POLICY], padrao.unknownPolicy),
            contactsPolicy = origin(this[Keys.CONTACTS_POLICY], padrao.contactsPolicy),
            whitelistPolicy = origin(this[Keys.WHITELIST_POLICY], padrao.whitelistPolicy),
            blockPrivateNumbers = this[Keys.BLOCK_PRIVATE_NUMBERS] ?: padrao.blockPrivateNumbers,
            blockMode = BlockMode.entries.firstOrNull { it.name == this[Keys.BLOCK_MODE] }
                ?: padrao.blockMode,
            hideFromNativeCallLog =
                this[Keys.HIDE_FROM_NATIVE_CALL_LOG] ?: padrao.hideFromNativeCallLog,
            showOwnNotification = this[Keys.SHOW_OWN_NOTIFICATION] ?: padrao.showOwnNotification,
            fallbackPolicy = FallbackPolicy.entries.firstOrNull { it.name == this[Keys.FALLBACK_POLICY] }
                ?: padrao.fallbackPolicy,
            historyEnabled = this[Keys.HISTORY_ENABLED] ?: padrao.historyEnabled,
            // fromId já é tolerante: id desconhecido volta ao padrão do MVP.
            retentionPolicy = RetentionPolicy.fromId(this[Keys.RETENTION_POLICY]),
        )
    }

    /**
     * Leitura tolerante de enum: valor desconhecido (config escrita por versão mais
     * nova, ou arquivo adulterado) cai no padrão do campo em vez de lançar. A posição
     * da constante nunca é persistida — só o nome.
     */
    private fun origin(raw: String?, padrao: OriginPolicy): OriginPolicy =
        OriginPolicy.entries.firstOrNull { it.name == raw } ?: padrao

    private fun ScreeningSettings.writeInto(prefs: MutablePreferences) {
        prefs[Keys.PROTECTION_ENABLED] = protectionEnabled
        prefs[Keys.UNKNOWN_POLICY] = unknownPolicy.name
        prefs[Keys.CONTACTS_POLICY] = contactsPolicy.name
        prefs[Keys.WHITELIST_POLICY] = whitelistPolicy.name
        prefs[Keys.BLOCK_PRIVATE_NUMBERS] = blockPrivateNumbers
        prefs[Keys.BLOCK_MODE] = blockMode.name
        prefs[Keys.HIDE_FROM_NATIVE_CALL_LOG] = hideFromNativeCallLog
        prefs[Keys.SHOW_OWN_NOTIFICATION] = showOwnNotification
        prefs[Keys.FALLBACK_POLICY] = fallbackPolicy.name
        prefs[Keys.HISTORY_ENABLED] = historyEnabled
        prefs[Keys.RETENTION_POLICY] = retentionPolicy.id
    }
}
