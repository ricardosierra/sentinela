package org.sentinela.app.settings

/**
 * Configurações de triagem com os padrões do MVP: proteção ativa, desconhecidos
 * bloqueados, privados bloqueados, contatos tocando, whitelist nunca silenciada,
 * sem rastro no histórico nativo, notificação própria desabilitada, fallback
 * seguro permitindo a chamada, histórico local ligado e retenção de 30 dias.
 */
data class ScreeningSettings(
    val protectionEnabled: Boolean = true,
    /** O que fazer com número fora dos contatos e da whitelist. */
    val unknownPolicy: OriginPolicy = OriginPolicy.BLOCK,
    /** O que fazer com quem está na agenda (aplicável no modo discador). */
    val contactsPolicy: OriginPolicy = OriginPolicy.RING,
    /** O que fazer com a whitelist pessoal (padrão do mockup: nunca silenciar). */
    val whitelistPolicy: OriginPolicy = OriginPolicy.NEVER_SILENCE,
    val blockPrivateNumbers: Boolean = true,
    /** Como bloquear quando a política é BLOCK: rejeitar já ou caixa postal. */
    val blockMode: BlockMode = BlockMode.REJECT,
    val hideFromNativeCallLog: Boolean = true,
    val showOwnNotification: Boolean = false,
    val fallbackPolicy: FallbackPolicy = FallbackPolicy.ALLOW,
    /** Histórico ligado por padrão: é o que dá auditabilidade ao bloqueio. */
    val historyEnabled: Boolean = true,
    /** Retenção padrão 30 dias (decisão travada no CONTEXT da Fase 3). */
    val retentionPolicy: RetentionPolicy = RetentionPolicy.DAYS_30,
)

/**
 * Política por origem da chamada (contato, whitelist ou desconhecido) —
 * espelha as opções dos mockups: Tocar / Bloquear / Silenciar / Nunca Silenciar.
 * NEVER_SILENCE = tocar mesmo em Não Perturbe (bypass de DND na camada telecom).
 */
enum class OriginPolicy { RING, BLOCK, SILENCE, NEVER_SILENCE }

enum class BlockMode { REJECT, SILENT_VOICEMAIL }

enum class FallbackPolicy { ALLOW, BLOCK }
