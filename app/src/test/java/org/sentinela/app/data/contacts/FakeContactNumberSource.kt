package org.sentinela.app.data.contacts

/**
 * Fonte de contatos em memoria para testes JVM puros.
 *
 * Os contadores existem para a prova ESTRUTURAL de que o cache e usado: "o provider nao foi
 * consultado" e uma contagem, nunca um cronometro. Cronometro mediria o agendador do host e daria
 * verde ate com o cache desligado — a licao da Fase 3.
 */
class FakeContactNumberSource : ContactNumberSource {

    var granted: Boolean = true

    /** Numeros crus, como a agenda os guarda (nacional, formatado, E.164, lixo). */
    var rawNumbers: List<String> = emptyList()

    /** Numeros que a sonda direta encontra, ja como o chamador os passa. */
    var probeHits: Set<String> = emptySet()

    var failProbe: Boolean = false
    var failRawNumbers: Boolean = false

    var probeCount = 0
        private set
    var rawNumbersCount = 0
        private set
    var observerCount = 0
        private set

    /** Ultimos argumentos da sonda — prova que a segunda sonda foi de fato oferecida. */
    var lastProbeArgs: Pair<String, String?>? = null
        private set

    private var onChange: (() -> Unit)? = null

    override fun hasPermission(): Boolean = granted

    override fun probe(e164: String, nationalDigits: String?): Boolean {
        probeCount++
        lastProbeArgs = e164 to nationalDigits
        if (failProbe) error("falha simulada do provider")
        return e164 in probeHits || (nationalDigits != null && nationalDigits in probeHits)
    }

    override fun allRawNumbers(): List<String> {
        rawNumbersCount++
        if (failRawNumbers) error("falha simulada na leitura em lote")
        return rawNumbers
    }

    override fun observeChanges(onChange: () -> Unit) {
        observerCount++
        this.onChange = onChange
    }

    override fun close() {
        onChange = null
    }

    /** Simula uma notificacao do provider. */
    fun notifyChange() {
        onChange?.invoke()
    }
}
