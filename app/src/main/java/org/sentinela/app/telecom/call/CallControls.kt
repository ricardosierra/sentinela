package org.sentinela.app.telecom.call

/**
 * Costura entre os comandos da interface e a telefonia.
 *
 * Existe por um motivo medido, não por gosto de abstração: a pesquisa desta fase constatou
 * que pedir mudo ao serviço da plataforma quando não há telefone vinculado **não lança nada
 * e não faz nada**. Um teste que chamasse o serviço direto e apenas verificasse que nada
 * estourou ficaria verde sem provar comportamento algum. Aqui, cada comando é um evento
 * observável, verificável em máquina virtual pura.
 *
 * Nenhuma implementação desta costura captura exceção: falha no caminho da chamada precisa
 * subir. A pesquisa mediu que o processo morto é detectado pelo sistema, que assume a chamada
 * com o discador do aparelho; interface viva e congelada não é detectada por ninguém.
 */
interface CallControls {
    fun answer()
    fun reject()
    fun hangUp()
    fun setMuted(muted: Boolean)
    fun setSpeakerOn(on: Boolean)
    fun playDtmf(digit: Char)
    fun stopDtmf()
}
