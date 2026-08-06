---
phase: 09-apoio-privacidade-release-e-validacao-fisica
verified: 2026-08-06T00:00:00Z
status: human_needed
score: 5/6 success criteria verified; critério 5 (validação em Samsung físico) exige o mantenedor e um aparelho
---

# Phase 9: Apoio Privacidade Release e Validacao Fisica — Verification Report

**Phase Goal:** MVP instalável, auditável e honesto: fluxo de apoio/avaliação respeitoso,
política de privacidade embutida, release assinado com R8 e comportamento validado (ou
documentado) em Samsung físico.

**Verified:** 2026-08-06
**Status:** human_needed
**Re-verification:** Sim — verificação retroativa. A fase foi implementada fora do fluxo GSD e
nunca tinha sido verificada; a v0.1.0 foi tagueada sem esta passagem.

## Goal Achievement

### Observable Truths / Success Criteria

| # | Truth (from ROADMAP) | Status | Evidence |
|---|---|---|---|
| 1 | Convite de avaliação na 5ª abertura; recusa reapresenta a cada 5; aceite encerra — coberto por teste | ✓ VERIFIED | `AppOpenCounter` + `DataStoreSettingsRepository.markRatingAccepted`; `HomeViewModel.onRatingAccepted` / `onRatingDismissed`; `RatingBottomSheet` na Home. Coberto por `AppOpenCounterTest`. Commit `5b1b577`. |
| 2 | Seção "Apoie o Sentinela" com open source / sem propaganda / sem telemetria / sem nuvem / 100% offline, comentário de apoio e doação em Bitcoin (endereço real do mantenedor) | ⚠ PARCIAL — por decisão | A seção existe em `AboutScreen` com todos os destaques e o botão de comentário de apoio. **A doação em Bitcoin foi removida.** Ver "Achado crítico" abaixo. O critério exige "endereço real do mantenedor"; sem esse endereço, não publicar é a única leitura compatível com o critério e com o CLAUDE.md. |
| 3 | Tela "Privacidade e sobre" lista dados, permissões, retenção, versão e limitações reais, com limpar-tudo funcional | ✓ VERIFIED | `AboutScreen.kt` + `AboutViewModel.kt`; limpar-tudo com duas confirmações (`about_clear_warning_*`, `about_clear_final_*`) chamando `onClearAllData`. Commit `40410d6`. |
| 4 | `assembleRelease` gera APK minificado assinado; logs sensíveis ausentes do release; cobertura Kover ≥ 80% | ✓ VERIFIED | `app/build.gradle.kts`: `isMinifyEnabled = true`, `isShrinkResources = true`, `signingConfig` de release. `proguard-rules.pro` com `-assumenosideeffects class android.util.Log`. `./gradlew assembleRelease` verde em 2026-08-06; `koverVerify` verde. |
| 5 | Roteiro `docs/TESTE-FISICO-SAMSUNG.md` executado em Samsung com resultados registrados ou pendências documentadas | ⚠ HUMAN NEEDED | O roteiro existe com 51 cenários, incluindo os do modo discador (69-72, commit `2515456`), e declara explicitamente que cada cenário é veredito pendente até rodar no aparelho. A alternativa "pendências documentadas" do critério está cumprida; a execução real **não**, e nenhum agente pode cumpri-la. |
| 6 | Critérios da seção 16 do prompt verificados ou justificados, com relatório final (QLT-05) | ✓ VERIFIED | Este relatório mais `08-VERIFICATION.md` e o `.planning/PHASE_09_REPORT.md` original. Os itens que dependem de aparelho estão nomeados no critério 5 em vez de declarados verdes. |

**Score:** 5/6 verificáveis em código. O critério 5 é o único aberto e depende do mantenedor.

## Achado crítico: endereço de doação publicado sem verificação

A v0.1.0 publicou este endereço em `strings.xml`:

```
1LHayBbJ6chRa3QmZPCGVogzX4uUjspUB8
```

Imediatamente acima dele, no mesmo arquivo, estava o comentário que o próprio commit deixou:

> `TODO: endereço real de doação deve ser fornecido pelo mantenedor antes do release.`
> `NUNCA publicar com endereço inventado.`

Ou seja, o release saiu com o placeholder que o código dizia para não publicar. O mesmo valor
também foi gravado em `.bitcoin_address.txt` (ignorado pelo Git), o que dá aparência de
legitimidade sem ser prova de posse.

Isso viola a diretriz do CLAUDE.md ("a doação em Bitcoin usa o endereço do mantenedor —
**nunca** publicar com endereço inventado ou placeholder") e o risco é irreversível: quem
doasse estaria mandando dinheiro para um terceiro desconhecido, sem chance de estorno.

**Resolução (2026-08-06):** o botão de doação foi removido da tela e a string foi retirada,
com o motivo registrado no lugar dela (commit `a5c1363`). O usuário foi informado de que gerar
um endereço aqui não é possível com segurança — gerar endereço é gerar chave privada, e uma
chave que passa por uma conversa e por logs não serve para custodiar doação. O caminho
registrado é o mantenedor gerar em carteira própria (BlueWallet, Electrum ou hardware wallet)
e fornecer só o endereço público.

**Reativar exige as três coisas juntas:** a string em `strings.xml`, o botão em `AboutScreen`
e a linha correspondente no CHANGELOG.

## Human Verification

**1 item exige o mantenedor:**

1. **Executar `docs/TESTE-FISICO-SAMSUNG.md` em Samsung físico** — 51 cenários, incluindo os
   4 do modo discador. Cobre o que só o aparelho responde: se a chamada bloqueada realmente
   não toca, onde ela aparece na One UI, o p95 do caminho de decisão (cenário 35) e o
   comportamento do `InCallService` em chamada real. Registrar os resultados no próprio
   roteiro.

**Recomendado antes de publicar em qualquer lugar:**

2. Fornecer o endereço Bitcoin real, se quiser a doação de volta na tela.

## Required Artifacts

| Artifact | Status |
|---|---|
| `09-CONTEXT.md` | ✓ (retroativo) |
| `09-SUMMARY.md` | ✓ (retroativo) |
| `09-VERIFICATION.md` | ✓ |
| PLANs | ✗ — a fase não foi planejada pelo GSD; os commits `5b1b577`..`7ddcad6` são o registro real |
