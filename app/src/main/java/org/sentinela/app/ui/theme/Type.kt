package org.sentinela.app.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

// Escala Material 3 padrao como base. As familias Inter/Geist do design system
// dependem dos arquivos de fonte empacotados em `res/font/`; enquanto eles nao
// existirem no repositorio, a familia de texto do sistema mantem a escala e os
// pesos equivalentes. Nenhuma fonte e resolvida pela rede: o app nao declara
// permissao de internet e nenhum provedor de fontes do sistema e usado.
val SentinelaTypography = Typography()

/**
 * Familia dos tres estilos numericos. O monoespacado do sistema ja entrega
 * figuras de largura fixa, o que cumpre o requisito funcional do cronometro
 * (sem largura fixa o numero "pula" a cada segundo). A troca pela Geist esta
 * registrada em `docs/backlog/fontes-inter-geist.md`.
 */
private val NumberFamily = FontFamily.Monospace

/** Numero digitado na tela de discagem (`number-xl` do contrato de design). */
val Typography.numberXl: TextStyle
    get() = TextStyle(
        fontFamily = NumberFamily,
        fontSize = 32.sp,
        fontWeight = FontWeight.Medium,
        lineHeight = 40.sp,
        letterSpacing = 0.5.sp,
    )

/** Numero na tela de chamada (`number-lg` do contrato de design). */
val Typography.numberLg: TextStyle
    get() = TextStyle(
        fontFamily = NumberFamily,
        fontSize = 24.sp,
        fontWeight = FontWeight.Medium,
        lineHeight = 32.sp,
        letterSpacing = 0.5.sp,
    )

/**
 * Cronometro da chamada ativa. `tnum` pede figuras tabulares (todas as figuras
 * com a mesma largura de avanco), o que impede o cronometro de deslocar o
 * layout a cada segundo.
 */
val Typography.timer: TextStyle
    get() = TextStyle(
        fontFamily = NumberFamily,
        fontSize = 16.sp,
        fontWeight = FontWeight.Medium,
        lineHeight = 24.sp,
        fontFeatureSettings = "tnum",
    )
