package org.sentinela.app.ui.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Shapes
import androidx.compose.ui.unit.dp

/** Raio de card, chip retangular e campo (8dp). */
val ShapeSmall = RoundedCornerShape(8.dp)

/** Raio de card de conteudo (16dp) — `HonestyCard`, banners. */
val ShapeMedium = RoundedCornerShape(16.dp)

/** Raio de painel ancorado ao rodape e folha inferior (24dp). */
val ShapeLarge = RoundedCornerShape(24.dp)

/** Pilula: chips de origem e CTA de largura total. */
val ShapePill = RoundedCornerShape(50)

/**
 * Formas centralizadas do tema — evita `RoundedCornerShape` espalhado pelos
 * pontos de uso. `extraSmall` e `extraLarge` reusam os extremos da escala.
 */
val SentinelaShapes = Shapes(
    extraSmall = ShapeSmall,
    small = ShapeSmall,
    medium = ShapeMedium,
    large = ShapeLarge,
    extraLarge = ShapeLarge,
)
