---
phase: 06-modo-discador-opcional
plan: 08
subsystem: docs
tags: [honestidade, limitacoes, telas, validacao-fisica, kover, changelog, evidencia, dia-05, qlt-06]
requires:
  - "06-03 (matriz de permissoes corrigida e Bloco 8 dos invariantes)"
  - "06-04/06-05/06-06 (as telas que a secao 11 passa a descrever)"
  - "06-07 (a medicao de que perder papel do sistema encerra o processo, e a suite instrumentada de 80 casos)"
  - "05 (formato do arquivo de evidencia e o precedente de exclude por nome de classe)"
provides:
  - "docs/LIMITACOES.md sem afirmacao nao medida: numero privado no modo discador volta a NAO VERIFICADO"
  - "docs/LIMITACOES.md item 9: perder papel do sistema encerra o processo do app"
  - "docs/LIMITACOES.md com o escopo do modo discador (uma chamada por vez, sem video, emergencia sempre nativa)"
  - "docs/design/TELAS.md secao 11 como contrato de 120 linhas em vez de esboco de 6"
  - "docs/TESTE-FISICO-SAMSUNG.md com 60 cenarios: 23-30 revisados no lugar e 52-60 novos"
  - "gate de cobertura ampliado com dois excludes por nome de classe, 96,648% sobre piso 80"
  - "CHANGELOG.md com o bloco tecnico da Phase 6 no formato Release Notes"
  - ".planning/phases/06-modo-discador-opcional/06-EVIDENCE.md — suite pos-clean sem cache"
affects:
  - "Phase 7 (a tela de Protecao herda o contrato da secao 11 e o texto honesto do modo discador)"
  - "Phase 9 (nove cenarios fisicos novos; o cenario 59 decide o texto final do item 8 das limitacoes)"
  - "fechamento de versao (o changelog ainda nao tem os blocos das Phases 2 a 5 — deferred-items.md)"
tech-stack:
  added: []
  patterns:
    - "afirmacao de documento sem medicao correspondente e defeito, e a correcao e escrever NAO VERIFICADO com o cenario que resolve"
    - "cenario fisico ja escrito e revisado no lugar, nunca duplicado com numero novo"
    - "exclude de cobertura sempre por nome de classe, e o gate so vale depois de visto vermelho com o mesmo denominador"
key-files:
  created:
    - .planning/phases/06-modo-discador-opcional/06-EVIDENCE.md
    - .planning/phases/06-modo-discador-opcional/deferred-items.md
  modified:
    - docs/LIMITACOES.md
    - docs/design/TELAS.md
    - docs/TESTE-FISICO-SAMSUNG.md
    - app/build.gradle.kts
    - CHANGELOG.md
    - app/src/androidTest/java/org/sentinela/app/telecom/DialerScreeningIntegrationTest.kt
decisions:
  - "o item de numero privado volta a NAO VERIFICADO: a fase provou papel, vinculo, politica e reversao, e nunca mediu entrega de chamada sem identificacao"
  - "excludes novos do Kover sao exatamente dois nomes de classe (costura da telefonia e servico de interface de chamada); nenhuma classe pura do modo discador saiu do denominador"
  - "a corrida entre gravar configuracao e triar era defeito do TESTE: o cache eventualmente consistente do repositorio e desenho deliberado da Fase 3"
  - "os cinco UP-TO-DATE pos-clean foram registrados na evidencia em vez de silenciados — sao tarefas de ciclo de vida sem saida, e omitir a ressalva enfraqueceria a propria evidencia"
metrics:
  duration_minutes: 34
  tasks: 3
  files_created: 2
  files_modified: 6
  tests_added: 0
  completed: 2026-07-29
---

# Phase 6 Plan 08: Fechamento Honesto e Prova da Fase Summary

A fase de maior risco do MVP termina sem documento mentindo: a única afirmação da base documental
que nunca havia sido medida — a de que o modo discador destrava número privado — foi **corrigida
para não verificada**, a seção de telas deixou de ser um esboço de seis linhas e virou contrato, o
roteiro físico passou a cobrir o modo discador uma vez só, e o gate de cobertura voltou a ser
cobrado com o denominador honesto (96,648% sobre piso 80), demonstrado vermelho antes de aceito.

## O que foi feito

**Task 1 — honestidade e a seção 11** (`2f3e49b`)

O item 8 de `docs/LIMITACOES.md` afirmava que a opção de bloquear números privados "só tem efeito
real no modo discador". Isso **nunca foi medido**. A fase provou elegibilidade ao papel, vínculo do
serviço, política por contato e reversão; não provou entrega de chamada sem identificação, porque
simular chamada de entrada com identificação bloqueada está fora do alcance do processo de teste. O
item agora separa o que se sabe (no modo filtro a chamada sem handle nunca chega à triagem, por
leitura da fonte do Telecom) do que não se sabe (se o papel de telefone padrão faz essas chamadas
passarem pela triagem, onde o bloqueio ainda é possível, ou se elas só aparecem na interface de
chamada, quando já é tarde), e aponta o cenário 59 como veredito. Nenhum texto da interface promete
o recurso: número privado não aparece nem na lista "o que muda" nem na "o que não muda".

Duas correções no mesmo arquivo. O item 3 registra que a Fase 6 **reconfirmou em execução**, com o
papel de telefone padrão ativo, que a chamada bloqueada continua entrando no histórico do telefone —
bloquear com as configurações de fábrica produz justamente a variante que *pede* para não registrar,
e o registro entra igual. E entrou um item 9: **perder um papel do sistema encerra o processo do
app**, com as consequências visíveis (tela aberta desaparece; o app sempre volta em processo novo) e
a razão de engenharia que isso sustenta (estado do modo derivado de consultas ao sistema, nunca de
valor gravado; proibição permanente de desligar o modo desabilitando componente próprio).

O escopo do modo discador foi escrito onde se procura por ele: **uma chamada por vez** — sem chamada
em espera, segunda chamada, conferência ou transferência —, sem videochamada, sem mensagem rápida ao
recusar (exigiria permissão fora da lista), sem gesto de arrastar para atender (decisão, não falta) e
**chamada de emergência sempre atendida pelo discador do aparelho**, mesmo com o papel nas nossas
mãos.

A seção 11 de `docs/design/TELAS.md` foi substituída por inteiro: as seis telas, as quatro variantes
de identidade em tabela, os tamanhos travados com a nota de que o tamanho **desenhado** é o que vale
(a lição do plano 06-04, em que um controle encolhido passava verde num assert que só olhava o alvo
de toque), as quatro cores funcionais fixas com o motivo, a fronteira número completo na tela contra
mascarado em log/notificação/histórico, e os dez requisitos de acessibilidade como critério. As telas
do modo discador entraram no diagrama de fluxo com a ressalva de que ficam fora da navegação normal:
quem abre a chamada é o sistema.

**Task 2 — roteiro físico** (`4b20836`)

Os cenários **23 a 30 foram revisados no lugar**, sem renumerar e sem duplicar. Cada um passou a
dizer o que a automação já provou e o que sobra para o aparelho: no 23, a elegibilidade está provada
e o que resta é o diálogo do fabricante; nos 25 e 26, a decisão está provada e o que resta é a
percepção real (não tocar, não vibrar, não acender); no 30, a reversão está provada e o que resta é
a interface da One UI — incluindo a sugestão que o plano 06-07 deixou, de conferir que o
encerramento do app ao devolver o papel não deixa notificação de chamada órfã.

Os **nove cenários novos, 52 a 60**, entraram em seção própria, sem repetir assunto de 23–30: rota
de áudio real (o único ponto de DIA-02 genuinamente impossível no aparelho virtual, que só expõe
alto-falante), tela cheia sobre a tela bloqueada do fabricante, permissão de tela cheia revogada,
morte no meio da chamada em aparelho real, dois chips sem conta padrão, papel tomado por atualização
do sistema, otimização de bateria agressiva, número privado e histórico do fabricante. Total do
documento: **60 cenários, cada número exatamente uma vez**.

As três questões abertas de fabricante foram registradas na seção de comportamento OEM, com a regra
explícita: **nenhum ajuste preventivo de fabricante entra no código** antes de um item falhar
comprovadamente ali. E os critérios de aceite ganharam o mapa DIA-01 a DIA-05 → cenários, com
SCR-04 marcado como parcial e não verificado e SCR-07 como decisão do Android.

**Task 3 — gate, changelog e evidência** (`73dcc81`)

`koverLog` antes de qualquer mudança: **95,4741%**. O relatório por classe mostrou onde o
denominador estava desonesto: a costura que traduz comando de interface para a telefonia (23,1% de
linhas) e o serviço de interface de chamada, os dois inalcançáveis em JVM — a primeira só faz efeito
com um objeto de chamada montado pela própria plataforma, o segundo só roda quando o sistema o
vincula. Entraram como **dois excludes por nome de classe** (mais as aninhadas), com o comentário do
bloco registrando os percentuais em prosa. Depois: **96,648%**.

Nenhuma classe pura do modo discador saiu do denominador: estado e retrato da chamada, tradutor dos
códigos da plataforma, a costura abstrata dos controles, o coordenador e o armazém da sessão seguem
todos sendo cobrados pelo gate. Classe pura com cobertura baixa se resolve escrevendo teste.

O gate foi **demonstrado falhando** com o piso levantado para 99 sobre a mesma medição, com o
exclude já no lugar — a sabotagem incide sobre o denominador novo, não sobre o antigo — e restaurado
em 80 com `koverVerify` verde em seguida.

## Verificação

```
./gradlew clean && ./gradlew --no-build-cache assembleDebug testDebugUnitTest \
  koverVerify koverLog koverXmlReport lint detekt   -> BUILD SUCCESSFUL, 78 de 78 executadas
testes JVM agregados dos XMLs                       -> tests=603 failures=0 errors=0 skipped=0
./gradlew koverLog                                  -> 96,648% (gate 80)
koverVerify com piso 99                             -> FAILED (restaurado para 80)
bash scripts/verify-invariants.sh                   -> exit 0, 8 blocos, 38 checagens ok
bash scripts/run-instrumented-tests.sh              -> 80 de 80 verdes
bash scripts/verify-dialer-lifecycle.sh             -> exit 0, TODOS os passos OK
adb shell cmd role get-role-holders DIALER (ao fim) -> com.google.android.dialer
```

Critérios de aceite por grep:

| Critério | Medido | Esperado |
|---|---|---|
| "uma chamada por vez" em `LIMITACOES.md` | 1 | >= 1 |
| "nao verificado" em `LIMITACOES.md` | 1 | >= 1 |
| linhas da seção 11 de `TELAS.md` | 121 | >= 40 |
| "sem mockup" em `TELAS.md` | 3 | <= 3 |
| cenários 52 a 60 | 9 | 9 |
| cenários 23 a 29 | 7 | 7 |
| cenário 30 | 1 | 1 |
| total de cenários | 60 | 60 |
| exclude de pacote no bloco de cobertura | 0 | 0 |

**Ordem de execução das duas suítes importa e foi respeitada.** A suíte instrumentada deixa o
aparelho virtual com o Sentinela como telefone padrão (nenhum teste devolve o próprio papel, porque
devolvê-lo mataria o processo do próprio teste); o script de ciclo de vida devolve o papel na
largada e na saída. Instrumentada primeiro, script depois, aparelho conferido de fábrica ao final.

## Deviations from Plan

### Auto-fixed Issues

**1. [Rule 1 - Bug] Corrida entre gravar configuração e triar deixou 1 de 80 vermelho**

- **Encontrado em:** Task 3, na primeira execução da suíte instrumentada
- **Sintoma:** `DialerScreeningIntegrationTest.contatoComPoliticaSilenciarESilenciado`
  `expected:<Silence(reason=CONTACT)> but was:<Allow(reason=CONTACT)>`
- **Causa medida:** o repositório de configurações serve o retrato de um cache em memória mantido
  por um coletor — decisão da Fase 3, para que o caminho quente da triagem não toque disco — e o
  coletor é alimentado de forma assíncrona. O caso gravava a política e triava no instante seguinte,
  disputando com a própria gravação e às vezes decidindo pelo `Tocar` que a limpeza do caso anterior
  acabara de restaurar. Passou verde no plano 06-07 por sorte de escalonamento.
- **Correção:** no **teste**, espera explícita até o repositório passar a reportar a política
  gravada, com mensagem própria de falha; o caso do padrão de fábrica ganhou a mesma espera.
  **Zero linha de produção alterada:** cache eventualmente consistente é desenho deliberado, e
  nenhuma triagem real disputa com uma gravação feita no mesmo milissegundo. Mesma classe de
  armadilha do `@Before` de contatos do plano 06-07, agora no eixo das configurações.
- **Arquivo:** `app/src/androidTest/.../DialerScreeningIntegrationTest.kt` — commit `73dcc81`

### Deliberado, além da letra do plano

- **`docs/LIMITACOES.md` ganhou um item 9** que o plano não pediu (encerramento do processo ao
  perder papel). É a descoberta central do plano 06-07 e ela não tinha lugar em nenhum documento
  vivo; deixá-la só no resumo de plano seria perdê-la.
- **Cinco tarefas `UP-TO-DATE` após `clean` foram registradas na evidência, com o motivo**, em vez
  de silenciadas. São tarefas de ciclo de vida sem saída própria e nenhuma é actionable, mas as
  evidências anteriores afirmam "zero up-to-date" sem essa ressalva, e omitir enfraqueceria
  justamente o que a evidência serve para sustentar.
- **`deferred-items.md`** registra que o changelog não tem os blocos técnicos das Phases 2 a 5. Fora
  de escopo por desenho: escrever quatro fases de changelog de memória seria pior que a lacuna —
  cada bloco precisa sair do respectivo resumo, no fechamento da versão.

### Escopo intocado

Zero arquivo de produção Kotlin modificado. `CallDecisionEngine` intocado na fase inteira. Nenhuma
permissão nova, nenhuma biblioteca nova, nenhuma chamada de rede, nenhum injetor de dependência,
nenhum agendador. Manifest intocado. `.planning/STATE.md` e o roadmap não foram tocados pelas tasks
— são do fluxo de fechamento.

## Blockers e pendências

- **Cenário 59 é o único que decide o texto final do item 8** das limitações. Até ele, o documento
  fica dizendo não verificado, o que é a resposta correta.
- **Rota de áudio real** segue o único ponto de DIA-02 fora do alcance do aparelho virtual.
- **Changelog das Phases 2 a 5** (ver `deferred-items.md`) — fechamento de versão.

## Autenticação / checkpoints

Nenhum. Plano autônomo do começo ao fim.

## Self-Check: PASSED

Os 2 arquivos criados existem em disco, os 6 modificados estão no histórico, e os 3 commits
declarados (`2f3e49b`, `4b20836`, `73dcc81`) existem.
