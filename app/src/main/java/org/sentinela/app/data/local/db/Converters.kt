package org.sentinela.app.data.local.db

import androidx.room.TypeConverter
import org.sentinela.app.data.local.CallClassification
import org.sentinela.app.domain.DecisionReason

/**
 * Conversao entre enums de dominio e as colunas de texto do banco.
 *
 * `DecisionReason` e persistido pelo `code` estavel e `CallClassification` pelo `name`.
 * O indice da constante (a posicao dela na declaracao do enum) NUNCA e persistido:
 * reordenar ou inserir uma entrada reescreveria o historico ja gravado do usuario.
 *
 * Leitura e TOLERANTE: um valor gravado por uma versao mais nova do app (ou corrompido)
 * cai num fallback documentado em vez de lancar. Explodir aqui derrubaria a tela de
 * historico inteira por causa de uma linha — perda de acesso a dado que existe.
 */
class Converters {

    @TypeConverter
    fun fromDecisionReason(reason: DecisionReason): String = reason.code

    /** Fallback: `UNKNOWN_NUMBER` — o motivo mais generico e sem dado pessoal. */
    @TypeConverter
    fun toDecisionReason(code: String?): DecisionReason =
        DecisionReason.entries.firstOrNull { it.code == code } ?: DecisionReason.UNKNOWN_NUMBER

    @TypeConverter
    fun fromCallClassification(classification: CallClassification): String = classification.name

    /** Fallback: `UNCLASSIFIED` — o estado inicial, que o usuario pode reclassificar. */
    @TypeConverter
    fun toCallClassification(name: String?): CallClassification =
        CallClassification.entries.firstOrNull { it.name == name } ?: CallClassification.UNCLASSIFIED
}
