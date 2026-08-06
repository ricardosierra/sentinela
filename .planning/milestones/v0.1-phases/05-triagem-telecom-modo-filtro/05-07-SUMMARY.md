---
phase: 05-triagem-telecom-modo-filtro
plan: 07
subsystem: documentacao-e-qualidade
tags: [honestidade, limitacoes, validacao-fisica, kover, evidencia]
requires: ["05-06"]
provides:
  - "docs/LIMITACOES.md corrigido com a fonte do Android"
  - "cenarios 40-51 do roteiro fisico da Phase 9"
  - "camada de triagem no denominador do Kover"
  - "05-EVIDENCE.md pos-limpeza"
affects:
  - "docs/ARQUITETURA.md"
  - "docs/design/TELAS.md"
  - "CLAUDE.md / AGENTS.md"
  - ".planning/STATE.md"
tech-stack:
  added: []
  patterns:
    - "filtro do Kover: includes por pacote, excludes SEMPRE por nome de classe"
    - "criterio de aceite por grep nao distingue include de exclude — verificar a intencao"
key-files:
  created:
    - .planning/phases/05-triagem-telecom-modo-filtro/05-EVIDENCE.md
  modified:
    - docs/LIMITACOES.md
    - docs/TESTE-FISICO-SAMSUNG.md
    - docs/ARQUITETURA.md
    - docs/design/TELAS.md
    - app/src/main/res/values/strings.xml
    - app/src/main/java/org/sentinela/app/domain/ScreenedCall.kt
    - app/src/main/java/org/sentinela/app/settings/ScreeningSettings.kt
    - app/build.gradle.kts
    - CLAUDE.md
    - AGENTS.md
    - .planning/STATE.md
decisions:
  - "Ocultar do historico do telefone e inatingivel: o Android so honra o pedido para app de operadora, e o modo discador da Fase 6 nao destrava"
  - "Nunca Silenciar descreve o que o Sentinela faz, nunca o que o sistema faz: o nao perturbar nao e contornavel"
  - "Contatos chegam a triagem enquanto a leitura da agenda estiver concedida — revogar muda o comportamento em silencio"
  - "Camada de triagem entrou no Kover sem nenhum exclude novo: cobertura SUBIU de 96,68% para 97,64%"
metrics:
  tasks: 3
  jvm_tests: 417
  instrumented_tests: 53
  coverage: "97.6351%"
  completed: 2026-07-29
---

# Phase 5 Plano 07: Honestidade, Validacao Fisica e Evidencia — Summary

Fechamento da fase pela honestidade: tres afirmacoes que o projeto mantinha escritas foram
derrubadas pela pesquisa na fonte do Android e corrigidas em codigo e documentacao, os doze
cenarios que so podem ser provados em aparelho foram numerados e diferidos, e a camada de
triagem entrou no denominador da cobertura sem nenhum exclude novo.

## O que foi feito

### Task 1 — Correcao das afirmacoes falsas (`5aeabc0`)

Quatro correcoes, cada uma citando a origem (codigo do Telecom do Android) e apontando o
cenario correspondente do roteiro fisico:

| Afirmacao antiga | Verdade medida |
|---|---|
| Pular o registro no historico do telefone e "best-effort", varia por fabricante | E **no-op** para app que nao seja de operadora. Nao e variacao de OEM: e decisao do proprio Android. A Fase 6 **nao** destrava |
| No modo filtro contatos nem chegam ao app | Chegam, enquanto a leitura da agenda estiver concedida. A decisao e nossa desde a Fase 4 |
| "Nunca Silenciar" ignora o modo nao perturbar | Nao ignora. O nao perturbar e avaliado por um filtro paralelo que nenhum campo da resposta de triagem alcanca |
| (ausente) | Numero oculto nao e entregue a triagem no modo filtro; a opcao so tem efeito no modo discador |

O que o bloqueio **de fato** entrega passou a estar escrito junto: nao toca, nao vibra, nao
mostra tela de chamada e nao gera aviso de chamada perdida. O registro no historico permanece.

Textos ajustados: as duas descricoes de "Nunca Silenciar" em `strings.xml` e em `TELAS.md`, o
rotulo e a nova descricao da opcao de nao registrar no historico, o KDoc de `ContactLookup`, o
KDoc de `ScreeningSettings`, a abertura de `ARQUITETURA.md` e o diagrama, e a nota do card de
contatos no onboarding.

Nenhuma string promete ausencia de registro, toque durante o nao perturbar, filtragem de
aplicativos de mensagem ou bloqueio garantido.

### Task 2 — Cenarios 40 a 51 (`a1d331a`)

Secao nova em `docs/TESTE-FISICO-SAMSUNG.md`, continuando do 39, com os doze cenarios
transcritos da pesquisa: bloqueio real, onde a chamada bloqueada aparece no historico da One UI,
contato tocando, leitura de agenda revogada, caixa postal, nao perturbar, silenciar, percentis em
hardware, inicio a frio por chamada, perda do papel, notificacao na tela bloqueada e dual SIM.

Ficou registrado explicitamente que os criterios de aceite 1, 2 e 6 desta fase no ROADMAP e o
veredito do percentil de cauda sao **diferidos para a Phase 9 por desenho** — a fase entregou o
comportamento e a prova em JVM e emulador, nao a prova em hardware.

O bloco de registro de comportamento de OEM foi corrigido: a entrada bloqueada no historico
nativo deixou de ser exemplo de "desvio", porque agora se sabe que e o comportamento normal.

### Task 3 — Kover e evidencia (`2210309`)

O filtro foi alargado com os pacotes de triagem, notificacao e permissoes. **Nenhum exclude
novo foi necessario:** a cobertura subiu de 96,68% para **97,6351%**. Isso e consequencia
direta do desenho do plano 05-03 — o coordenador e puro e a traducao da resposta roda sob
Robolectric, que e teste em JVM e portanto e medido. O coordenador esta no denominador e nao
aparece em nenhum exclude.

Gate demonstrado falhando antes de ser aceito: com o limite em 99, `koverVerify FAILED` com o
percentual real; restaurado para 80, verde.

`05-EVIDENCE.md` criado no formato do `04-EVIDENCE.md`, com o comando pos-limpeza, as
`71 actionable tasks: 71 executed`, as duas contagens de teste, o filtro em vigor, os percentis
do 05-06, a tabela de todas as provas de vermelho da fase plano a plano e as pendencias
diferidas para as Phases 7 e 9.

## Verificacao

```
./gradlew clean && ./gradlew --no-build-cache assembleDebug testDebugUnitTest \
  koverVerify lint detekt   -> BUILD SUCCESSFUL, 71 actionable tasks: 71 executed
tests=417 failures=0 errors=0 skipped=0
application line coverage: 97.6351%   (gate minBound 80)
bash scripts/verify-invariants.sh     -> == todos os invariantes OK ==
bash scripts/run-instrumented-tests.sh -> 53/53 completed, 0 skipped, 0 failed
```

## Deviations from Plan

### Auto-fixed Issues

**1. [Rule 2 - Correcao] Afirmacoes falsas em arquivos fora da lista do plano**

- **Found during:** Task 1
- **Issue:** o plano listou `LIMITACOES.md`, `TELAS.md`, `strings.xml` e dois KDocs, mas a mesma
  afirmacao falsa sobre contatos nao chegarem a triagem estava tambem em `docs/ARQUITETURA.md`
  (prosa de abertura e diagrama) e no `CLAUDE.md`/`AGENTS.md`, que sao o contexto obrigatorio de
  todo agente. Deixar so os arquivos da lista corrigidos faria a fonte mais lida do projeto
  continuar ensinando a premissa errada.
- **Fix:** as tres correcoes tambem em `ARQUITETURA.md` e no par `CLAUDE.md`/`AGENTS.md`,
  replicados no mesmo commit como manda a regra do projeto.
- **Commit:** `5aeabc0`

**2. [Rule 1 - Criterio insatisfazivel] O criterio do Kover colidia com a propria tarefa**

- **Found during:** Task 3
- **Issue:** o criterio `[ "$(grep -c 'org.sentinela.app.telecom.\*' app/build.gradle.kts)" -eq 0 ]`
  existe para proibir **exclude por pacote**, mas o grep e cego a qual bloco a linha pertence — e
  a propria tarefa mandava acrescentar exatamente esse pacote aos **includes**. Satisfazer o grep
  ao pe da letra exigiria nao alargar o filtro, que era o objetivo do plano.
- **Fix:** a intencao foi verificada diretamente sobre o bloco de excludes, que ficou **intacto**:
  segue com o gerado pelo Room e a unica classe nomeada da Fase 4, sem nenhum pacote novo e sem o
  coordenador. Os criterios `minBound(80)` = 1 e `ScreeningCoordinator` = 0 passam como escritos.
- **Licao:** e a terceira vez nesta fase que um criterio por grep se autossabota (KDoc no 05-04,
  literal do shell no 05-06, include contra exclude aqui). Criterio por grep prova ausencia de
  texto, nunca ausencia de comportamento.

## Notas para as proximas fases

- **Fase 6 (discador):** nao planejar nada apoiado em ocultar a chamada do historico do telefone.
  O papel de discador padrao nao concede a isencao — ela e exclusiva de aplicativo de operadora.
  O que a Fase 6 realmente destrava e o numero oculto e a interface propria de chamada.
- **Fase 7 (telas):** a opcao de nao registrar no historico precisa nascer com a nota honesta ao
  lado (`settings_hide_native_log_desc`), nunca sozinha.
- **Fase 9:** cenarios 40 a 51 sao o fechamento real dos criterios 1, 2 e 6 desta fase.

## Self-Check: PASSED

- `05-EVIDENCE.md` — FOUND
- `05-07-SUMMARY.md` — FOUND
- commit `5aeabc0` — FOUND
- commit `a1d331a` — FOUND
- commit `2210309` — FOUND
