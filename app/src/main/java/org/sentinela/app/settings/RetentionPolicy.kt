package org.sentinela.app.settings

/**
 * Janelas de retencao em dias, nomeadas pelo que significam para o usuario.
 * Ficam fora do enum porque uma entrada nao pode ler o companion antes de existir.
 */
private const val UMA_SEMANA = 7
private const val UM_MES = 30
private const val TRES_MESES = 90

/**
 * Retencao do historico local (HST-02). O `id` e o valor PERSISTIDO — nunca a posicao
 * da constante na declaracao do enum, que mudaria de significado ao reordenar as
 * entradas e corromperia silenciosamente a configuracao do usuario.
 *
 * Regra pura e sem Android: e assim que ela entra no gate de cobertura.
 */
enum class RetentionPolicy(val id: String, val days: Int?) {
    /** Nao guardar nada: record() retorna cedo. */
    NEVER_STORE("never", 0),
    DAYS_7("7d", UMA_SEMANA),
    DAYS_30("30d", UM_MES),
    DAYS_90("90d", TRES_MESES),

    /** Guarda ate o usuario excluir: nunca poda automaticamente. */
    MANUAL("manual", null),
    ;

    /** O historico deve gravar novos registros com esta politica? */
    val shouldStore: Boolean get() = this != NEVER_STORE

    /**
     * Instante-limite: registros com timestamp ANTERIOR a este valor sao podados.
     * `null` = nao podar (MANUAL) ou nada a podar (NEVER_STORE nem grava).
     */
    fun cutoffUtcMillis(nowUtcMillis: Long): Long? =
        days?.takeIf { it > 0 }?.let { nowUtcMillis - it * MILLIS_PER_DAY }

    companion object {
        const val MILLIS_PER_DAY: Long = 86_400_000L

        /** Leitura tolerante: valor desconhecido ou ausente cai no padrao do MVP. */
        fun fromId(id: String?): RetentionPolicy =
            entries.firstOrNull { it.id == id } ?: DAYS_30
    }
}
