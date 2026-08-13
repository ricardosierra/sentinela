package org.sentinela.app.settings

/**
 * Configurações de triagem com os padrões do MVP: proteção ativa, desconhecidos
 * bloqueados, privados bloqueados, contatos tocando, whitelist nunca silenciada,
 * pedido de não registrar no histórico do telefone ligado (pedido que o Android
 * só atende para apps de operadora), notificação própria desabilitada, fallback
 * seguro permitindo a chamada, histórico local ligado, retenção de 30 dias e a
 * exceção de chamada repetida ligada.
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
    /**
     * Se o mesmo número voltar a ligar logo depois de ser bloqueado, a segunda
     * chamada toca. Ligada por padrão (decisão do usuário, 2026-07-29).
     */
    val repeatedCallBypassEnabled: Boolean = true,
    /**
     * O que a notificação própria mostra quando está ligada. MASKED exibe o número no formato
     * escolhido em [maskNumbers]; ANONYMOUS não mostra dígito algum. Na tela bloqueada a versão
     * pública nunca carrega número, independentemente destas opções.
     */
    val notificationIdentification: NotificationIdentification = NotificationIdentification.MASKED,
    /**
     * Se exibe os números mascarados (com asteriscos) na interface, histórico e notificações.
     * Desligado por padrão: sem os dígitos completos o usuário não reconhece quem ligou, e o
     * número é dado do próprio dono do aparelho — a máscara continua obrigatória em log.
     */
    val maskNumbers: Boolean = false,
)

/**
 * Identificação exibida na notificação própria. MASKED usa a máscara única do app
 * (`+55 11 9****-1234`); ANONYMOUS não mostra dígito algum, para quem lê a tela
 * bloqueada em público. Nenhuma das duas carrega a sequência completa de dígitos.
 */
enum class NotificationIdentification { MASKED, ANONYMOUS }

/**
 * Política por origem da chamada (contato, whitelist ou desconhecido) —
 * espelha as opções dos mockups: Tocar / Bloquear / Silenciar / Nunca Silenciar.
 * NEVER_SILENCE = tocar mesmo em Não Perturbe (bypass de DND na camada telecom).
 */
enum class OriginPolicy { RING, BLOCK, SILENCE, NEVER_SILENCE }

enum class BlockMode { REJECT, SILENT_VOICEMAIL }

enum class FallbackPolicy { ALLOW, BLOCK }
