// O arquivo agrupa o enum de consulta e a constante da janela, que só fazem
// sentido juntos; por isso o nome do arquivo é o do conceito, não o do enum.
@file:Suppress("MatchingDeclarationName")

package org.sentinela.app.domain

/**
 * Resultado da consulta ao histórico local: este mesmo número já foi bloqueado
 * há pouco tempo?
 *
 * `LOOKUP_FAILED` existe para que uma falha na consulta jamais vire bloqueio: o
 * motor trata a falha exatamente como `MISS` e segue a política normal.
 */
enum class RepeatedCallLookup { HIT, MISS, LOOKUP_FAILED }

/**
 * Janela da exceção de chamada repetida (5 minutos).
 *
 * Racional do usuário (2026-07-29): quem tem uma emergência de verdade insiste e
 * liga de novo em seguida; discagem automatizada de propaganda normalmente não
 * repete tão rápido. Se o mesmo número volta a ligar dentro desta janela depois
 * de ter sido bloqueado, a segunda chamada toca.
 *
 * O valor é uma constante nomeada por exigência explícita do contexto da fase:
 * literal solto espalhado pelo código não pode ser testado nem ajustado com
 * segurança, e o detekt cobra número mágico.
 */
const val REPEATED_CALL_WINDOW_MILLIS: Long = 5L * 60L * 1000L
