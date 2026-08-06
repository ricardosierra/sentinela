---
phase: 09-apoio-privacidade-release-e-validacao-fisica
verified: 2026-08-06T00:00:00Z
status: human_needed
score: 6/6 success criteria verified em código; critério 5 (validação em Samsung físico) exige o mantenedor e um aparelho
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
| 2 | Seção "Apoie o Sentinela" com open source / sem propaganda / sem telemetria / sem nuvem / 100% offline, comentário de apoio e doação em Bitcoin (endereço real do mantenedor) | ✓ VERIFIED (após correção) | A seção existe em `AboutScreen` com todos os destaques e o botão de comentário. A doação saiu da tela enquanto o endereço era o placeholder da v0.1.0 e voltou com os endereços **reais** do mantenedor — Bitcoin on-chain e Liquid (L-BTC) —, cada um com botão de copiar. Endereços vivem só em `strings.xml`; a tela nunca monta nem edita endereço. Travado por `SupportAddressTest`. |
| 3 | Tela "Privacidade e sobre" lista dados, permissões, retenção, versão e limitações reais, com limpar-tudo funcional | ✓ VERIFIED | `AboutScreen.kt` + `AboutViewModel.kt`; limpar-tudo com duas confirmações (`about_clear_warning_*`, `about_clear_final_*`) chamando `onClearAllData`. Commit `40410d6`. |
| 4 | `assembleRelease` gera APK minificado assinado; logs sensíveis ausentes do release; cobertura Kover ≥ 80% | ✓ VERIFIED | `app/build.gradle.kts`: `isMinifyEnabled = true`, `isShrinkResources = true`, `signingConfig` de release. `proguard-rules.pro` com `-assumenosideeffects class android.util.Log`. `./gradlew assembleRelease` verde em 2026-08-06; `koverVerify` verde. |
| 5 | Roteiro `docs/TESTE-FISICO-SAMSUNG.md` executado em Samsung com resultados registrados ou pendências documentadas | ⚠ HUMAN NEEDED | O roteiro existe com 51 cenários, incluindo os do modo discador (69-72, commit `2515456`), e declara explicitamente que cada cenário é veredito pendente até rodar no aparelho. A alternativa "pendências documentadas" do critério está cumprida; a execução real **não**, e nenhum agente pode cumpri-la. |
| 6 | Critérios da seção 16 do prompt verificados ou justificados, com relatório final (QLT-05) | ✓ VERIFIED | Este relatório mais `08-VERIFICATION.md` e o `.planning/PHASE_09_REPORT.md` original. Os itens que dependem de aparelho estão nomeados no critério 5 em vez de declarados verdes. |

**Score:** 6/6 verificáveis em código. O critério 5 depende do mantenedor e de um aparelho.

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

**Resolução (2026-08-06), em duas etapas:**

1. O botão de doação saiu da tela e a string foi retirada (commit `a5c1363`). Gerar o endereço
   dentro da conversa não era opção: gerar endereço é gerar chave privada, e uma chave que passa
   por transcrição e logs não serve para custodiar doação.
2. O mantenedor gerou os endereços em carteira própria e forneceu só a parte pública. A doação
   voltou à tela com **dois** destinos — Bitcoin on-chain e Liquid (L-BTC) —, cada um com botão
   de copiar.

**A trava contra a repetição** é o que faltava na v0.1.0: nenhum teste olhava para essa string.
Agora `SupportAddressTest` decodifica bech32 (BIP-173) e blech32 e confere, sobre o valor que
vai para o APK, o alfabeto, a ausência de maiúscula/minúscula misturadas, o **checksum**, o
prefixo de rede (`bc` / `lq`) e o tamanho do payload (20 bytes de P2WPKH; 33+20 no confidencial
da Liquid). Há ainda um caso que reprova o placeholder da v0.1.0 nominalmente e outro que
corrompe um caractere de propósito para provar que o teste morde. Um erro ao colar passa a
quebrar o build antes de virar dinheiro na carteira de um estranho.

**Estrutura que sustenta a trava:** os endereços existem em um único lugar, `strings.xml`, e a
tela os recebe prontos por parâmetro — nunca monta, concatena ou edita endereço.

## Human Verification

**1 item exige o mantenedor:**

1. **Executar `docs/TESTE-FISICO-SAMSUNG.md` em Samsung físico** — 51 cenários, incluindo os
   4 do modo discador. Cobre o que só o aparelho responde: se a chamada bloqueada realmente
   não toca, onde ela aparece na One UI, o p95 do caminho de decisão (cenário 35) e o
   comportamento do `InCallService` em chamada real. Registrar os resultados no próprio
   roteiro.


## Required Artifacts

| Artifact | Status |
|---|---|
| `09-CONTEXT.md` | ✓ (retroativo) |
| `09-SUMMARY.md` | ✓ (retroativo) |
| `09-VERIFICATION.md` | ✓ |
| PLANs | ✗ — a fase não foi planejada pelo GSD; os commits `5b1b577`..`7ddcad6` são o registro real |
