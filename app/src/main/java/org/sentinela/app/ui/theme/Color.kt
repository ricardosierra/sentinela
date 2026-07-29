package org.sentinela.app.ui.theme

import androidx.compose.ui.graphics.Color

// Tokens "Silent Guardian" — dark first (docs/design/DESIGN.md)
val Surface = Color(0xFF081425)
val SurfaceContainerLowest = Color(0xFF040E1F)
val SurfaceContainerLow = Color(0xFF111C2D)
val SurfaceContainer = Color(0xFF152031)
val SurfaceContainerHigh = Color(0xFF1F2A3C)
val SurfaceContainerHighest = Color(0xFF2A3548)
val OnSurface = Color(0xFFD8E3FB)
val OnSurfaceVariant = Color(0xFFC2C6D6)
val Outline = Color(0xFF8C909F)
val OutlineVariant = Color(0xFF424754)

val Primary = Color(0xFFADC6FF)
val OnPrimary = Color(0xFF002E6A)
val PrimaryContainer = Color(0xFF4D8EFF)
val OnPrimaryContainer = Color(0xFF00285D)

val Secondary = Color(0xFFB7C8E1)
val OnSecondary = Color(0xFF213145)
val SecondaryContainer = Color(0xFF3A4A5F)
val OnSecondaryContainer = Color(0xFFA9BAD3)

val Tertiary = Color(0xFFBEC6E0)
val OnTertiary = Color(0xFF283044)
val TertiaryContainer = Color(0xFF8990A8)
val OnTertiaryContainer = Color(0xFF22293D)

val Error = Color(0xFFFFB4AB)
val OnError = Color(0xFF690005)
val ErrorContainer = Color(0xFF93000A)
val OnErrorContainer = Color(0xFFFFDAD6)

// ---------------------------------------------------------------------------
// Cores funcionais da chamada (atender / recusar / encerrar)
// ---------------------------------------------------------------------------
//
// Estes QUATRO tokens sao literais deste arquivo e chegam aos componentes por
// PARAMETRO. Nenhum deles pode ser lido de um papel do esquema de cor do tema.
//
// Por que o vermelho tambem ganha apelido proprio, em vez de reaproveitar o
// papel destrutivo do esquema: `SentinelaTheme` monta o esquema INTEIRO a
// partir da cor dinamica do papel de parede quando o aparelho e do nivel 31 ou
// maior (o padrao). Isso substitui todos os papeis do esquema, inclusive os
// papeis destrutivos. Naquele caminho, quem responde pelo tom de recusar
// passaria a ser o papel de parede — exatamente junto com o verde de atender.
// Um papel de parede poderia entao aproximar os dois botoes da tela de chamada
// recebida e produzir uma recusa acidental de uma chamada real, que e um erro
// irreversivel.
//
// Nao ha cor nova aqui: `CallReject` e `OnCallReject` valem, digito por digito,
// o mesmo que os dois tokens destrutivos declarados acima. O contrato de design
// segue honrado; o que muda e a FONTE — literal de arquivo, nunca papel do
// esquema. Contraste do par >= 7:1 nos dois casos.
val CallAccept = Color(0xFF1E6E42)
val OnCallAccept = Color(0xFFD9F2E3)
val CallReject = Color(0xFF93000A)
val OnCallReject = Color(0xFFFFDAD6)
