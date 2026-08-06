---
phase: 08-ui-whitelist-e-historico
verified: 2026-08-06T00:00:00Z
status: passed
score: 5/5 success criteria verified (após correção de 6 defeitos encontrados na auditoria)
---

# Phase 8: UI Whitelist e Historico — Verification Report

**Phase Goal:** Usuário gerencia exceções e audita bloqueios sem sair do app.

**Verified:** 2026-08-06
**Status:** passed
**Re-verification:** Sim — verificação retroativa. A fase foi implementada fora do fluxo GSD e
nunca tinha sido verificada. A primeira passagem reprovou 3 dos 5 critérios.

## Goal Achievement

### Observable Truths / Success Criteria

| # | Truth (from ROADMAP) | Status | Evidence |
|---|---|---|---|
| 1 | Cadastro de número BR normaliza para E.164, detecta duplicado e aparece na busca | ✓ VERIFIED (após correção) | `WhitelistViewModel.addOrEdit` normaliza via `PhoneNumberNormalizer` antes do repositório e checa `repository.contains(key)`. Duplicado e inválido agora produzem `WhitelistFeedback` exibido em snackbar — antes eram `return@launch` silenciosos. Testes: `numero duplicado nao grava e avisa o usuario`, `numero invalido nao grava e avisa o usuario`. Busca coberta por `search query triggers repository search`. |
| 2 | Export gera arquivo local; import valida formato/tamanho, pede confirmação e mescla sem duplicar | ✓ VERIFIED (após correção) | Era o defeito mais grave: a rota gravava `{"whitelist":[]}` fixo e descartava o arquivo importado. Agora `WhitelistRoute` chama `viewModel.exportJson()` (que serializa a lista real via `WhitelistExporter`) e `viewModel.import(json)` (que valida e mescla via `WhitelistImporter`). Confirmação antes de mesclar em `ImportConfirmDialog`; teto de leitura em `MAX_IMPORT_CHARS`. Testes: `WhitelistBackupWiringTest` (4 casos). |
| 3 | Histórico filtra por período/decisão e cada registro permite: permitir (→ whitelist), marcar indesejado, excluir | ✓ VERIFIED (após correção) | Período já existia (`HistoryFilter`); o eixo decisão foi acrescentado (`HistoryDecisionFilter`, sobre a classificação do usuário — justificativa em `08-CONTEXT.md`). As três ações estão em `HistoryScreen` e `HistoryViewModel.markAsLegitimateAndAllow` / `markAsUnwanted` / `deleteCall`. Testes: `HistoryDecisionFilterTest` (5 casos, incluindo os dois eixos simultâneos). |
| 4 | Tratamento da whitelist (Nunca Silenciar/Tocar/Bloquear/Silenciar) é configurável na UI | ✓ VERIFIED | `SettingsScreen` expõe `settings_whitelist_policy` com as quatro opções; onboarding tem `WhitelistPolicyStepScreen` com default "Nunca Silenciar". Efeito imediato herdado da Fase 7 (sem botão salvar). |
| 5 | Números aparecem sempre mascarados nas listas e notificações | ✓ VERIFIED | Bloco 10 de `scripts/verify-invariants.sh` proíbe `Text(...numberE164...)` em `ui/history` e `ui/whitelist` — verde. Notificação usa `PhoneMask` desde a Fase 5 (Bloco 9). |

**Score:** 5/5. Três critérios só passaram após correção; nenhum passou na primeira leitura do
código entregue.

## Defeitos encontrados e corrigidos

| # | Defeito | Severidade | Commit |
|---|---|---|---|
| 1 | Export gravava constante vazia, ignorando a lista | Alta — perda de dado silenciosa | `46e2be8` |
| 2 | Import lia o arquivo e descartava | Alta — funcionalidade inexistente | `46e2be8` |
| 3 | Duplicado/inválido descartados sem aviso | Média — critério 1 não cumprido | `46e2be8` |
| 4 | Filtro por decisão ausente | Média — critério 3 não cumprido | `f5f123a` |
| 5 | `"Agora"` fixo em toda linha do histórico | Média — informação falsa na tela | `f5f123a` |
| 6 | `reason.name` cru exibido ao usuário | Baixa — vazamento de código interno | `f5f123a` |

## Fora do escopo desta fase, causado por ela

| Item | Severidade | Commit |
|---|---|---|
| `6fa8967` quebrou a resposta única do `CallScreeningService` | **Crítica** — invariante central do produto | `8af8478` |
| `6fa8967` versionou 557 arquivos de `.venv/` | Média — higiene do repositório | `d2b1a50` |

## Required Artifacts

| Artifact | Status |
|---|---|
| `08-CONTEXT.md` | ✓ (retroativo) |
| `08-SUMMARY.md` | ✓ (retroativo) |
| `08-VERIFICATION.md` | ✓ |
| PLANs | ✗ — a fase não foi planejada pelo GSD; os commits `6a6d087`..`6631ef9` são o registro real |

## Human Verification

Nenhum item desta fase exige aparelho físico para fechar. O exercício de import/export e das
ações do histórico em Samsung está no roteiro da Fase 9 (`docs/TESTE-FISICO-SAMSUNG.md`),
conforme a política de validação física registrada no ROADMAP.
