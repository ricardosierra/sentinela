---
phase: 07-ui-onboarding-e-home
plan: 09
subsystem: ui-settings
tags: [protecao, configuracoes, acessibilidade, confirmacao, modo-discador, honestidade]

requires:
  - "07-01 — as 269 chaves pt-BR, incluindo o plural real de settings_clear_history_confirm"
  - "07-03 — OptionCard/optionCardGroup, SettingSwitchRow, SentinelaTopBar"
  - "Fase 6 — HonestyCard, InfoBanner, DialerModeState e a tela de ativacao do discador"
  - "07-04 — SettingsUiState e o dono de estado sem funcao de gravacao diferida"
  - "TouchTargetAsserts.kt (07-02) — os tres asserts de dois eixos, importados"

provides:
  - "ui/settings/SettingsUiState — tipo em arquivo proprio, com contagem de registros e primeiro quadro"
  - "ui/settings/SettingsGroup — cartao de grupo com cabecalho semantico e tinta de atencao"
  - "ui/settings/SettingsNavRow — linha navegavel com desabilitado e motivo no NO DA LINHA"
  - "ui/settings/SettingsScreen — os 16 itens, efeito imediato, exatamente duas confirmacoes"
  - "ProtectionScreenTest — 36 casos, com o caso de completude da tabela de 16 itens"

affects:
  - "07-10 — o envelope de navegacao liga onOpenDialerActivation e onOpenAbout"
  - "07-11 — fecho da fase, cobertura e requisitos"

tech-stack:
  added: []
  patterns:
    - "Desabilitado e motivo no no do proprio controle, jamais no filho ou no ancestral"
    - "Cabecalho semantico por grupo: tela de 16 itens sem heading e intransitavel"
    - "Confirmacao SO em perda de dado; troca de politica nunca confirma"
    - "Frase de honestidade reusada por identificador de recurso, nunca reescrita"

key-files:
  created:
    - app/src/main/java/org/sentinela/app/ui/settings/SettingsUiState.kt
    - app/src/main/java/org/sentinela/app/ui/settings/SettingsGroup.kt
    - app/src/main/java/org/sentinela/app/ui/settings/SettingsScreen.kt
    - app/src/test/java/org/sentinela/app/ui/settings/ProtectionScreenTest.kt
  modified:
    - app/src/main/java/org/sentinela/app/ui/settings/SettingsViewModel.kt
    - app/src/test/java/org/sentinela/app/ui/settings/SettingsViewModelTest.kt

decisions:
  - "Os rotulos das politicas por origem COLIDEM entre grupos: teste que busca 'Bloquear' para trocar contatos acerta o cartao de desconhecidos — a busca correta e pela descricao"
  - "Botao de texto do Material desenha 40dp: piso EXIGIDO de 48dp, quarta medicao do eixo do desenho neste projeto"
  - "Cabecalho de grupo nunca repete o rotulo de um item: dois nos com o mesmo texto quebram a busca por texto e duplicam a leitura de tela"
  - "As cinco janelas de retencao sao as unicas opcoes sem descricao propria; a explicacao vive uma vez como nota do grupo"
  - "A consequencia destrutiva de 'nao guardar' vive SO no dialogo: dize-la tambem no cartao criaria duas copias da mesma frase"
  - "Nome totalmente qualificado em pre-visualizacao reprova no Bloco 2 — carrega o identificador do aplicativo"
  - "historyCount virou historyRecordCount e o tipo saiu do arquivo do dono de estado"

metrics:
  duration: ~65min
  tasks: 3
  files: 6
  tests_added: 36
  tests_total_jvm: 824
  coverage: 96.6157%
  completed: 2026-07-30
---

# Phase 7 Plano 09: Tela Proteção Summary

Os dezesseis itens da tela Proteção em nove grupos com cabeçalho semântico, cada um com explicação
curta e permanente sob ele, efeito imediato na triagem, **exatamente duas** confirmações — as duas por
perda de dado — e as quatro honestidades provadas reusadas por identificador de recurso, nunca
reescritas.

## O que foi entregue

**Task 1 — estado e agrupamento** (commit `b7d59b3`)

`SettingsUiState` saiu do arquivo do dono de estado e virou tipo próprio: quem consome o estado é a
composta, e assim cada pré-visualização e cada caso de teste monta o estado direto, sem repositório,
banco nem consulta ao sistema. `historyCount` virou `historyRecordCount` e ganhou `loading`.

`SettingsGroup` é o cartão de grupo, com o rótulo declarado como **cabeçalho semântico**. Isso não é
enfeite: o leitor de tela navega por cabeçalho, e sem ele uma tela de dezesseis itens é uma lista
linear intransitável. `tinted` pinta o grupo com a cor de atenção a 15% — literal do arquivo de cores,
não do esquema, porque do nível 31 em diante o papel de parede passaria a decidir a diferença entre
protegido e desprotegido.

`SettingsNavRow` põe `enabled` e o motivo no **nó da própria linha**. A linha inteira é o alvo, então é
ela quem responde às buscas; estado declarado num filho fica onde ninguém consulta.

**Task 2 — os dezesseis itens** (commit `cc37e22`)

Composta pura, uma função de retorno por item. Zero menção a container de injeção ou a dono de estado
(`grep` devolve 0), zero passo intermediário de gravação, e o cartão de limitações com as quatro
frases originais por identificador de recurso.

Item 9 navega para a tela de ativação que já existe — ela é **destino**, não filha desta tela
(`grep` do nome dela devolve 0). Em indisponível a linha fica desabilitada com o motivo; em bloqueado
pela agenda fica **habilitada de propósito**, porque é a tela de destino que explica o pré-requisito, e
barrar a entrada esconderia justamente a explicação de que o usuário precisa.

**Task 3 — ProtectionScreenTest** (commit `0c4787c`)

36 casos (mínimo pedido: 22), incluindo o caso de **completude** que percorre as dezesseis chaves de
rótulo lidas do recurso, o caso de **ausência de confirmação** que troca as quatro políticas e exige
zero diálogo com uma única emissão cada, e quatro casos de alvo de toque nos **dois eixos**.

## Provas de vermelho executadas

Todas sabotaram produção **já commitada** e foram restauradas por edição manual, nunca por
`git checkout` (lição de 06-02). O `git diff` voltou vazio depois de cada uma.

| Prova | Sabotagem | Resultado |
|---|---|---|
| 1 — item esquecido | remover o interruptor de chamada repetida da tela | **6 casos vermelhos**, incluindo o de completude e o de explicação permanente |
| 2 — confirmação excessiva | fazer a política de desconhecidos abrir diálogo | **exatamente 1** vermelho: o de ausência de confirmação |
| 3 — perda de dado sem confirmar | limpar histórico chamando a ação direto, sem diálogo | **4 vermelhos**, incluindo o que exige zero apagamento antes de confirmar |
| 4 — desabilitado no filho | mover o desabilitado da linha do discador para o ícone filho | **exatamente 1** vermelho: o do anúncio de desabilitado |

A prova 1 rendeu um achado de desenho: além do caso de completude, os cinco casos que encontram
interruptor **por posição na travessia** ficaram vermelhos. Isso é a rigidez pretendida — mover um
interruptor sem atualizar a lista de ordem avisa em vez de passar em silêncio.

## Deviations from Plan

### Auto-fixed Issues

**1. [Rule 1 - Bug] Botão destrutivo desenhado em 40dp, abaixo do piso de 48dp**

- **Encontrado em:** Task 3, pelo eixo do DESENHO
- **Problema:** o botão de texto do Material desenha 40dp de altura. Os dois asserts de alvo de toque
  ficavam verdes, porque o Compose expande sozinho o alvo até o mínimo da plataforma — só o eixo do
  desenho pegou. É a **quarta** vez que este eixo pega o mesmo tipo de defeito neste projeto (Fase 6,
  07-03 e aqui).
- **Correção:** piso `requiredHeightIn(min = 48.dp)`. `heightIn` negociaria com o pai e voltaria a
  40dp em tela apertada, repetindo o defeito que 07-03 mediu na ordem dos modificadores.
- **Commit:** `0c4787c`

**2. [Rule 1 - Bug] Os rótulos das políticas por origem colidem entre os três grupos**

- **Encontrado em:** Task 3
- **Problema:** "Bloquear", "Silenciar", "Tocar" e "Nunca Silenciar" são rótulos que se repetem nos
  grupos de desconhecidos, contatos e lista pessoal. O caso que troca a política de **contatos**
  buscando o texto "Bloquear" acertava o cartão do grupo de **desconhecidos** — e passava a afirmar a
  coisa errada, com o veredito medido: `[SILENCE, BLOCK]` em vez de `[SILENCE]`.
- **Correção:** as políticas são clicadas pela **descrição**, que é única por grupo. O cartão mescla os
  descendentes, então a busca pela descrição encontra exatamente o cartão pretendido.
- **Consequência de desenho:** nenhum cabeçalho de grupo repete o rótulo de um item seu. Dois nós com
  o mesmo texto quebrariam a busca por texto e duplicariam a leitura de tela.
- **Commit:** `0c4787c`

**3. [Rule 3 - Blocking] Nome totalmente qualificado em pré-visualização reprova o Bloco 2**

- **Encontrado em:** verificação final
- **Problema:** `org.sentinela.app.settings.ScreeningSettings(...)` numa pré-visualização carrega o
  identificador do aplicativo como literal em Kotlin, que o invariante de rebranding reprova fora de
  `package`/`import` — inclusive em pré-visualização.
- **Correção:** import mais nome curto. Mesmo achado que 07-04 registrou pelo lado oposto (lá o nome
  qualificado era a tentativa de fugir da contagem de linhas do `grep`).
- **Commit:** `099a795`

**4. [Rule 3 - Blocking] `SettingsUiState` mudou de arquivo e de nome de campo**

- **Encontrado em:** Task 1
- **Problema:** o plano manda criar `SettingsUiState.kt` com `historyRecordCount`, mas o tipo já
  existia dentro de `SettingsViewModel.kt` (07-04) com `historyCount`.
- **Correção:** tipo movido para o arquivo próprio e campo renomeado, com o dono de estado e o único
  caso de teste que o lia ajustados no mesmo commit. Os dois arquivos de 07-04 não estão em execução
  paralela nesta onda.
- **Commit:** `b7d59b3`

### Decisões de desenho tomadas dentro do contrato

- **Ordem por agrupamento temático, não pela numeração da tabela.** Os itens 7 e 10 (registro no
  telefone e chamada repetida) ficam no grupo de proteção, e os itens 5 e 6 (privados e modo de
  bloqueio) no grupo de desconhecidos. A §9.3 exige ordem de travessia igual à ordem visual, o que
  continua valendo; a tabela numera itens, não impõe nove grupos de um item.
- **As cinco janelas de retenção são as únicas opções sem descrição própria.** A duração inteira já
  está dita no rótulo, e a explicação do item vive **uma vez**, como nota do grupo. Cinco parágrafos
  idênticos seriam enchimento; cinco parágrafos diferentes seriam pior.
- **A consequência destrutiva de "não guardar" vive só no diálogo**, onde a §9.2 a coloca. Dizê-la
  também no cartão criaria duas cópias da mesma frase, e a cópia esquecida é sempre a que fica errada.
- **As duas sub-opções de identificação da notificação reusam chaves existentes** como descrição (o
  texto do canal e o texto anônimo que a notificação realmente mostra). Não existe chave dedicada para
  elas, e `strings.xml` não é arquivo deste plano nesta onda de quatro agentes em paralelo.

### Itens fora de escopo, registrados e não corrigidos

Durante a verificação, `ui/home/StatusHeroCard.kt` e `ui/home/HomeScreen.kt` apareceram vermelhos
(detekt e compilação). São arquivos do plano 07-08, em execução paralela na mesma onda. **Não
corrigidos**, e nenhum deles é falha deste plano: as tentativas seguintes ficaram verdes sozinhas.

Duas falhas de ambiente reconhecidas e superadas por repetição, ambas já registradas em 07-01: erros
de referência não resolvida em arquivo de outro agente no meio da edição dele, e `java.io.EOFException`
no arquivo binário de resultados quando dois planos rodam a mesma tarefa de teste ao mesmo tempo.

## Verificação final

```
./gradlew --rerun-tasks assembleDebug testDebugUnitTest lint detekt   BUILD SUCCESSFUL
bash scripts/verify-invariants.sh                                     todos os invariantes OK
./gradlew koverLog                                                    96,6157%
./gradlew koverVerify                                                 BUILD SUCCESSFUL (piso 80)
```

- **824 testes JVM** no total; **36 novos** neste plano.
- `app/build.gradle.kts` e o filtro do Kover **não tocados** (07-11 é o dono).
- Nenhuma permissão nova, nenhuma biblioteca nova, nenhuma chave nova em `strings.xml`, nenhuma linha
  do `CallDecisionEngine`.
- `settings_hide_native_log_desc` aparece **uma vez** e o valor da chave está intacto.

## Requisitos

`UIX-03` (tela Proteção) e `UIX-09` fecham aqui: a tela existe, cada configuração é alterável, tem
explicação permanente e efeito imediato. **`UIX-10` e `UIX-11` seguem PENDENTES de propósito** —
estados de carregamento e erro em *todas* as telas e o fecho de acessibilidade só têm veredito com a
navegação ligada (07-10) e o fecho da fase (07-11). Marcar antes é o estado falsamente positivo que o
item 11 da §10.3 proíbe, e o precedente é de 07-01, 07-04 e 07-07.

## Self-Check: PASSED

Os seis arquivos declarados existem no disco e os quatro commits de tarefa existem no historico.
