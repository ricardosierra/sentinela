# Backlog — empacotar as fontes Inter e Geist

**Origem:** Fase 6, plano 06-02 (fundação visual das telas de chamada e discagem).
**Status:** pendente, sem impacto funcional.

## Situação

O contrato de design (`.planning/phases/06-modo-discador-opcional/06-UI-SPEC.md`, seção
Tipografia) especifica **Inter** para texto e **Geist** para número de telefone, labels e
cronômetro. Os arquivos de fonte **não estão no repositório** — `app/src/main/res/font/` não
existe.

## O que foi feito no lugar

A regra de reserva do próprio plano foi aplicada:

- Texto segue a família de texto do sistema, mantendo a escala e os pesos do Material 3.
- `numberXl`, `numberLg` e `timer` usam a família monoespaçada do sistema. O monoespaçado já
  entrega figuras de largura fixa, então o requisito **funcional** do cronômetro (não deslocar o
  layout a cada segundo) está cumprido — só o desenho da letra difere do mockup.
- `timer` declara `fontFeatureSettings = "tnum"`, que continua valendo quando a Geist entrar.

Nenhuma fonte é resolvida em tempo de execução. O app não declara permissão de internet e
**nenhum provedor de fontes do sistema pode ser usado** — fonte baixável está proibida aqui.

## O que falta

1. Obter Inter e Geist (ambas SIL OFL 1.1) e commitar os `.ttf` em `app/src/main/res/font/`.
2. Registrar a licença: cópia do OFL em `app/src/main/res/raw/` ou em `docs/`, com atribuição na
   tela "Privacidade e sobre".
3. Trocar a família de texto e a família numérica em
   `app/src/main/java/org/sentinela/app/ui/theme/Type.kt` (ponto único: a constante
   `NumberFamily` e o `SentinelaTypography`).
4. Reconferir visualmente as telas de chamada e discagem: a Geist é mais estreita que o
   monoespaçado, então o número cabe com folga — a troca não deve piorar nenhum layout.

## Risco de não fazer

Cosmético. O número e o cronômetro ficam com desenho monoespaçado em vez do desenho do mockup.
Nenhum critério de aceite funcional da Fase 6 depende disso.
