# Phase 8: UI Whitelist e Historico — Summary

**Completed:** 2026-08-03 (implementação) / 2026-08-06 (reconciliação e correções)
**Status:** Complete

## O que foi entregue

### Whitelist

| Item | Onde |
|---|---|
| CRUD com normalização E.164 e dedup | `ui/whitelist/WhitelistViewModel.kt` |
| Busca com debounce | idem, `search(query)` no repositório |
| Diálogo de adicionar/editar | `ui/whitelist/WhitelistAddEditDialog.kt` |
| Tela com lista, busca e ações | `ui/whitelist/WhitelistScreen.kt` |
| Export/import por SAF | `ui/whitelist/WhitelistRoute.kt` + `data/local/export/` |

### Histórico

| Item | Onde |
|---|---|
| Lista com máscara e ações por registro | `ui/history/HistoryScreen.kt` |
| Filtro por período e por decisão | `ui/history/HistoryViewModel.kt` |
| Rótulo do motivo e tempo relativo | `ui/history/HistoryFormatting.kt` |

### Navegação

Abas de Home, Whitelist, Histórico e Proteção com bottom bar (`13187c6`).

## Divergências encontradas na reconciliação

A fase foi implementada fora do fluxo GSD, sem verificação. A auditoria de 2026-08-06
encontrou seis defeitos, todos corrigidos antes de fechar a fase:

1. **Backup vazio** — a rota gravava a constante `{"whitelist":[]}` no arquivo escolhido pelo
   usuário, ignorando a lista real. O `WhitelistExporter` existia e era testado, mas nunca era
   chamado. Um backup que sempre sai vazio é pior que não ter backup, porque o usuário confia
   nele. Corrigido em `46e2be8`.
2. **Importação inerte** — a rota lia o arquivo e descartava o conteúdo (`skip for
   simplicity`). O botão Importar não importava nada. Corrigido em `46e2be8`, com a
   confirmação antes de mesclar (critério 2) e teto de leitura.
3. **Duplicado e inválido em silêncio** — `addOrEdit` fazia `return@launch` nos dois casos; o
   diálogo fechava como se tivesse gravado. Corrigido em `46e2be8` com canal de aviso.
4. **Filtro por decisão inexistente** — só havia período. Corrigido em `f5f123a`.
5. **Tempo falso** — toda linha do histórico exibia a constante `"Agora"`, com o comentário
   `Temporarily omitting real relative time for brevity`. Um histórico de trinta dias parecia
   ter acontecido inteiro no último minuto. Corrigido em `f5f123a`.
6. **Reason code cru na tela** — a legenda imprimia `entry.reason.name`, ou seja
   `UNKNOWN_NUMBER`, em inglês e fora de `strings.xml`. Corrigido em `f5f123a`.

Além disso, esta fase deixou dois rastros fora do escopo dela, também corrigidos:

- **Regressão do invariante central de telecom** — o commit `6fa8967` ("Wave 08-03 Tela
  Whitelist") reescreveu `UnknownCallScreeningService`, criando um segundo ponto de resposta
  ao sistema. Corrigido em `8af8478`, com regressão coberta por teste. Detalhes em
  `09-VERIFICATION.md`.
- **557 arquivos de `.venv/` versionados** no mesmo commit. Removidos em `d2b1a50`.

## Testes acrescentados na reconciliação

- `WhitelistViewModelTest` — duplicado avisa, inválido avisa, aviso é consumido.
- `WhitelistBackupWiringTest` (Robolectric, `org.json`) — export carrega a lista real, export
  de lista vazia é válido, import mescla sem duplicar, import de arquivo malformado não grava.
- `HistoryDecisionFilterTest` — cada valor do filtro, os dois eixos simultâneos, combinação
  sem resultado devolve vazio.
- `HistoryFormattingTest` — as quatro faixas de tempo, registro no futuro, e completude dos
  rótulos de motivo.

## Estado dos portões

`testDebugUnitTest`, `lint`, `detekt`, `koverVerify` e `assembleRelease` verdes.
`scripts/verify-invariants.sh` verde nos 10 blocos — era vermelho em 3 antes desta
reconciliação.
