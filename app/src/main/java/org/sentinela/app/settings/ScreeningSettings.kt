package org.sentinela.app.settings

/**
 * Configurações de triagem com os padrões do MVP: proteção ativa, desconhecidos
 * e privados bloqueados, sem rastro no histórico nativo, notificação própria
 * desabilitada e fallback seguro permitindo a chamada.
 */
data class ScreeningSettings(
    val protectionEnabled: Boolean = true,
    val blockUnknownNumbers: Boolean = true,
    val blockPrivateNumbers: Boolean = true,
    val blockMode: BlockMode = BlockMode.REJECT,
    val hideFromNativeCallLog: Boolean = true,
    val showOwnNotification: Boolean = false,
    val fallbackPolicy: FallbackPolicy = FallbackPolicy.ALLOW,
)

enum class BlockMode { REJECT, SILENT_VOICEMAIL }

enum class FallbackPolicy { ALLOW, BLOCK }
