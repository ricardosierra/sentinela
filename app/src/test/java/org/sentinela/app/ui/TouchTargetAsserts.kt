package org.sentinela.app.ui

import androidx.compose.ui.test.SemanticsNodeInteraction
import androidx.compose.ui.unit.Dp
import org.junit.Assert.assertTrue

/*
 * Asserts de alvo de toque em DOIS EIXOS, em pacote neutro de teste.
 *
 * Este arquivo nasceu de uma extracao, e os dois motivos dela ficam registrados aqui porque os dois
 * continuam valendo:
 *
 * 1. Estes asserts nasceram dentro de uma classe de teste do pacote de chamada, e as telas do
 *    onboarding, da home e da Protecao vivem em outros pacotes. Duplicar os helpers deixaria o eixo
 *    com dentes divergir entre as copias — uma copia acertada e a outra esquecida medindo menos.
 *    Por isso: DUPLICAR ESTES HELPERS E PROIBIDO PARA SEMPRE. Quem precisar deles importa daqui.
 *
 * 2. Classe de teste nao enxerga membros de outra classe de teste entre sandboxes de SDK do
 *    Robolectric — registro da Fase 5. Arquivo de apoio neutro, com funcoes de nivel de arquivo, e
 *    a forma que ja resolveu isso antes neste projeto, e e a forma usada aqui.
 *
 * Nenhuma linha de comportamento mudou na extracao: as tres funcoes e o auxiliar privado sao os
 * mesmos que a Fase 6 escreveu e provou.
 */

/**
 * Altura do alvo de toque acima de um MINIMO.
 *
 * A biblioteca de teste do Compose so oferece a comparacao por igualdade, e igualdade e o assert
 * errado para este contrato: o minimo do projeto e 48dp, mas atender e recusar valem 72dp e as
 * teclas tambem. Um assert de igualdade quebraria a cada acerto de acabamento, e afrouxa-lo para o
 * valor exato de cada botao deixaria de medir o contrato de acessibilidade.
 *
 * Mede o alvo de TOQUE, nao a caixa visual: e o alvo de toque que o dedo alcanca, e ele pode ser
 * maior que o desenho.
 */
internal fun SemanticsNodeInteraction.assertTouchHeightIsAtLeast(
    minimo: Dp,
): SemanticsNodeInteraction = tambemMede(minimo, vertical = true)

/** Largura do alvo de toque acima de um minimo. Mesma justificativa de [assertTouchHeightIsAtLeast]. */
internal fun SemanticsNodeInteraction.assertTouchWidthIsAtLeast(
    minimo: Dp,
): SemanticsNodeInteraction = tambemMede(minimo, vertical = false)

/**
 * Altura DESENHADA do controle acima de um minimo — o segundo eixo, e o que tem dentes.
 *
 * Foi acrescentado depois de uma prova de vermelho falhar: reduzir um controle secundario de 56dp
 * para 40dp deixou os asserts de alvo de toque VERDES, porque o proprio Compose expande o alvo de
 * toque de qualquer componente interativo ate o minimo da plataforma. O alvo continuava correto para
 * o dedo, mas o desenho tinha encolhido, e nenhum assert percebia. Sem este eixo a suite media a
 * garantia do Compose em vez de medir o nosso layout.
 */
internal fun SemanticsNodeInteraction.assertLayoutHeightIsAtLeast(
    minimo: Dp,
): SemanticsNodeInteraction {
    val node = fetchSemanticsNode()
    val medida = with(node.layoutInfo.density) { node.size.height.toDp() }
    assertTrue("controle desenhado com altura de $medida, abaixo do minimo de $minimo", medida >= minimo)
    return this
}

private fun SemanticsNodeInteraction.tambemMede(
    minimo: Dp,
    vertical: Boolean,
): SemanticsNodeInteraction {
    val node = fetchSemanticsNode()
    val alvo = node.touchBoundsInRoot
    val medida = with(node.layoutInfo.density) {
        (if (vertical) alvo.height else alvo.width).toDp()
    }
    val eixo = if (vertical) "altura" else "largura"
    assertTrue(
        "alvo de toque com $eixo de $medida, abaixo do minimo de $minimo",
        medida >= minimo,
    )
    return this
}
