# Phase 6 — Evidência de execução

Coletada em 2026-07-29, ao fim do plano 06-08. Vale a mesma regra probatória das Phases 1, 4 e 5:
evidência só conta **depois de `clean` e com `--no-build-cache`** — `FROM-CACHE` tem exatamente o
mesmo defeito de `UP-TO-DATE`.

```bash
./gradlew clean
./gradlew --no-build-cache assembleDebug testDebugUnitTest koverVerify koverLog lint detekt
bash scripts/verify-invariants.sh
bash scripts/run-instrumented-tests.sh
bash scripts/verify-dialer-lifecycle.sh   # exige aparelho vivo + ./gradlew installDebug
```

## 1. Build pós-clean, sem cache

```
BUILD SUCCESSFUL in 23s
78 actionable tasks: 78 executed
```

**78 de 78 executadas, zero `from cache`.** As cinco linhas `UP-TO-DATE` da saída são tarefas de
ciclo de vida sem saída própria (`preBuild`, `preDebugBuild`, `generateDebugAssets`,
`preDebugUnitTestBuild`, `preReleaseBuild`) — nenhuma delas é actionable e nenhuma produz artefato
ou verde de teste. Toda tarefa que produz algo rodou de verdade.

APK debug gerado: `app/build/outputs/apk/debug/app-debug.apk`, 66,8 MB (debug, sem R8).

Testes JVM agregados dos XMLs de `app/build/test-results/testDebugUnitTest/`:

```
tests=603 failures=0 errors=0 skipped=0
```

A Phase 6 levou a suíte JVM de **417** (fim da Phase 5) para **603** casos — 186 novos.

## 2. Suíte instrumentada

```
bash scripts/run-instrumented-tests.sh

Starting 80 tests on Medium_Phone_API_35(AVD) - 15
Finished 80 tests on Medium_Phone_API_35(AVD) - 15
BUILD SUCCESSFUL in 48s
```

**80 de 80 verdes, zero falhas.** A Phase 6 levou a suíte instrumentada de **53** para **80**
casos. O script apaga os `TEST-*.xml` antes de rodar e prova o boot do emulador por
`sys.boot_completed`, nunca por `wait-for-device` — relatório antigo tem o mesmo defeito probatório
de `UP-TO-DATE`.

**Primeira execução deste plano falhou 1 de 80** e a falha era real, embora não fosse do produto:

```
DialerScreeningIntegrationTest.contatoComPoliticaSilenciarESilenciado
java.lang.AssertionError: numero na agenda com politica de contatos Silenciar nao foi silenciado
  expected:<Silence(reason=CONTACT)> but was:<Allow(reason=CONTACT)>
```

Causa medida: o repositório de configurações serve o retrato de um cache em memória mantido por um
coletor (decisão da Fase 3, para que o caminho quente da triagem não toque disco), e o coletor é
alimentado de forma assíncrona. O caso gravava a política e triava no instante seguinte, então
disputava com a própria gravação e às vezes decidia pelo valor anterior — o `Tocar` que a limpeza
do caso anterior acabara de restaurar. Corrigido no **teste**, com espera explícita até o
repositório passar a reportar a política gravada, e mensagem própria de falha. O produto está
certo: cache eventualmente consistente é desenho deliberado, e nenhuma triagem real disputa com uma
gravação feita no mesmo milissegundo. É a mesma classe de armadilha do `@Before` de contatos do
plano 06-07, agora no eixo das configurações.

## 3. Ciclo de vida do papel, dirigido de fora do processo

```
bash scripts/verify-dialer-lifecycle.sh   → exit 0

== 1: ponto de partida ==
ok:   telefone padrao inicial e o discador de fabrica (com.google.android.dialer)
== 2: concessao do papel de triagem e do papel de telefone padrao ==
ok:   papel de triagem concedido (codigo de saida 0)
ok:   papel de telefone padrao concedido pelo caminho que verifica elegibilidade
ok:   o aplicativo e o telefone padrao do aparelho
ok:   os dois papeis convivem no mesmo aplicativo (um unico vinculo de triagem, medido na pesquisa)
== 3: morte do processo no meio de uma chamada ==
ok:   chamada de saida em curso no sistema de telefonia
ok:   processo do aplicativo vivo durante a chamada (pid 2890)
ok:   o processo morreu de fato (era 2890, agora 'nenhum')
ok:   A CHAMADA SOBREVIVEU a morte do nosso processo
ok:   o sistema de telefonia religou no discador de fabrica sozinho
ok:   limpeza de chamadas presas devolveu o aparelho ao estado neutro
== 4: a chamada seguinte volta a ser nossa ==
ok:   o sistema voltou a vincular o servico de interface de chamada do aplicativo
== 5: reversao ==
ok:   devolucao do papel aceita (codigo de saida 0)
ok:   o discador de fabrica voltou a ser o telefone padrao — telefonia nunca fica sem aplicativo
ok:   o papel de triagem SOBREVIVEU a reversao — o modo filtro continua valendo sem reconfiguracao
ok:   a plataforma encerrou o aplicativo ao retirar o papel (era 3462, agora 'nenhum')
      (aviso: o motivo do encerramento nao apareceu no diagnostico desta janela)

TODOS os passos do ciclo de vida do modo discador OK
```

Reprodução independente da execução do plano 06-07, com identificadores de processo diferentes: o
encerramento ao perder o papel não foi acidente de uma execução. O aviso da última linha é honesto
e esperado — o motivo textual do sistema (`Permission or app op changed`) já havia rolado fora da
janela de diagnóstico consultada; o **efeito** afirmado é o processo ter deixado de existir, e esse
foi medido.

**Ordem de execução importa e foi respeitada:** a suíte instrumentada deixa o aparelho virtual com
o Sentinela como telefone padrão (nenhum teste devolve o próprio papel, porque devolvê-lo mataria o
processo do próprio teste); o script de ciclo de vida devolve o papel na largada e na saída. Rodar
a suíte antes e o script depois deixa o aparelho no estado de fábrica — conferido ao final:

```
adb shell cmd role get-role-holders android.app.role.DIALER
com.google.android.dialer
```

## 4. Cobertura e o gate

```
./gradlew koverLog
application line coverage: 96.648%
koverVerify → BUILD SUCCESSFUL (minBound 80)
```

Percentual **antes** do exclude deste plano: `95.4741%`. **Depois:** `96.648%`.

**Filtro em vigor** (`app/build.gradle.kts`, bloco `kover { }`):

Incluídos no denominador — `org.sentinela.app.` seguido de `domain.*`, `phone.*`, `data.*`,
`settings.*`, `telecom.*`, `notifications.*` e `permissions.*`.

Excluídos, **sempre por nome de classe, jamais por pacote**:

| Exclude | Por quê |
|---|---|
| `data.local.db.*` | gerado pelo Room (KSP); só executa em teste instrumentado |
| `data.contacts.ContactsContractLookupSource` | fonte do provider de contatos; só executa instrumentada (Fase 4) |
| `telecom.call.TelecomCallControls` (+ aninhadas) | **novo nesta fase:** só faz efeito com um objeto de chamada montado pela própria plataforma, que nenhum teste em JVM pode construir |
| `telecom.SentinelaInCallService` (+ aninhadas) | **novo nesta fase:** só roda quando o sistema o vincula |
| `*_Impl`, `*_Impl$*`, `@Dao`, `@Database` | gerado pelo Room |

As duas classes novas estão cobertas por `InCallServiceBindTest`, `InCallServiceDeathTest` e por
`scripts/verify-dialer-lifecycle.sh`. **Nenhuma classe pura do modo discador aparece em exclude** —
estado e retrato da chamada, tradutor dos códigos da plataforma, a costura abstrata dos controles,
o coordenador da sessão e o armazém da sessão seguem todos no denominador do gate. Classe pura com
cobertura baixa se resolve escrevendo teste, nunca excluindo.

**Gate demonstrado falhando** (piso levantado para 99 sobre a mesma medição, com o exclude já no
lugar):

```
> Task :app:koverVerify FAILED
FAILURE: Build failed with an exception.
```

Piso restaurado em 80 e `koverVerify` verde em seguida. Um gate que nunca foi visto vermelho não é
gate.

## 5. Invariantes

```
bash scripts/verify-invariants.sh   → exit 0
== Bloco 1: permissoes no manifest mergeado ==
ok:   sem android.permission.INTERNET (PRV-01)
ok:   permissao autorizada: android.permission.CALL_PHONE
ok:   permissao autorizada: android.permission.POST_NOTIFICATIONS
ok:   permissao autorizada: android.permission.READ_CONTACTS
ok:   permissao autorizada: android.permission.USE_FULL_SCREEN_INTENT
ok:   permissao autorizada: org.sentinela.app.DYNAMIC_RECEIVER_NOT_EXPORTED_PERMISSION
ok:   nenhuma permissao fora da allowlist
ok:   nenhuma permissao de fase futura antecipada
ok:   servico protegido por BIND_SCREENING_SERVICE
ok:   action android.telecom.CallScreeningService registrada
== Bloco 2: rebranding centralizado ==
ok:   nenhum applicationId literal em Kotlin
ok:   nenhuma string hardcoded (text = "...") em Kotlin
ok:   nenhuma cor literal fora de ui/theme
ok:   sentinelaApplicationId usado 3x em app/build.gradle.kts
ok:   app_name definido em strings.xml
== Bloco 3: dominio e normalizacao puros ==
ok:   domain sem import de android.*
ok:   phone sem import de android.*
== Bloco 4: relatorios de qualidade ==
ok:   detekt sem issues
ok:   lint sem issues
== Bloco 5: integridade do dado local ==
ok:   sem fallbackToDestructiveMigration (migracao explicita obrigatoria)
ok:   sem allowMainThreadQueries
ok:   schema Room v1 exportado (app/schemas/*/1.json)
ok:   nenhuma coluna de nome de contato na camada de dados
== Bloco 6: dado de contato apenas em memoria ==
ok:   nenhuma coluna de identidade de contato no schema exportado
ok:   provider de contatos so e citado em data/contacts
ok:   nenhuma coluna de identidade do contato projetada em app/src/main/java
ok:   data/contacts sem nenhum mecanismo de persistencia
== Bloco 7: regra de decisao concentrada no motor ==
ok:   camada de telefonia nao cita politica por origem nem modo de bloqueio
ok:   decisao de bloqueio so e construida no dominio
ok:   coordenador da triagem sem tipo da plataforma
ok:   resposta ao sistema nao e emitida fora do servico de triagem
ok:   servico de triagem responde ao sistema em um unico ponto
== Bloco 8: elegibilidade ao papel de telefone padrao ==
ok:   manifest mergeado declara os dois filtros da acao de discagem
ok:   um dos filtros de discagem usa o esquema de telefone
ok:   nenhum arquivo desabilita componente do proprio aplicativo
ok:   chamada originada apenas pelo gerenciador de telecomunicacoes
ok:   camada da sessao de chamada nao importa a camada de interface
ok:   coordenador da sessao de chamada sem tipo da plataforma
== todos os invariantes OK ==
```

**8 blocos, 38 checagens, todas verdes.** O Bloco 8 é da Phase 6 e trava a elegibilidade ao papel
de telefone padrão: os dois filtros da ação de discagem no manifest mergeado, a proibição
permanente de desabilitar componente próprio, a origem da chamada apenas pelo gerenciador de
telecomunicações e a pureza da camada da sessão.

## 6. Provas de vermelho da fase, com quem as executou

| Sabotagem | Resultado medido | Plano |
|---|---|---|
| Ramo final da tradução devolvendo `Ended` em vez de `Unsupported` | 5 vermelhos | 06-01 |
| Novo dígito de tom sem encerrar o tom pendente | 1 vermelho no pareamento | 06-01 |
| `answer()` embrulhado em captura de exceção | 1 vermelho na matriz de falhas | 06-01 |
| Uma só das duas defesas do vigia de apresentação | **tudo verde** — as duas são redundantes de propósito | 06-01 |
| Um dígito de cada cor funcional alterado | 2 e 4 vermelhos, incluindo o apelido deixando de casar com o token destrutivo | 06-02 |
| String com promessa desonesta acrescentada | varredura de honestidade da copy vermelha | 06-02 |
| Segundo filtro de discagem removido do manifest, com re-merge real | 2 vermelhos do Bloco 8 | 06-03 |
| Chamada ao método que desabilita componente próprio | 1 vermelho (8.2) | 06-03 |
| Intenção construída pela ação direta de ligar | 1 vermelho (8.3) | 06-03 |
| Import da camada de interface no armazém e da plataforma no coordenador | 2 vermelhos (8.4) | 06-03 |
| Controle de chamada reduzido de 56dp para 40dp | verde no eixo do alvo de toque, **vermelho** só no eixo do tamanho desenhado | 06-04 |
| `requiredSize` trocado por `size` no botão de atender | círculo comprimido de 72dp para 23dp em tela curta — vermelho | 06-04 |
| Originador ignorando o estado da permissão | 2 vermelhos | 06-05 |
| Importância do canal de chamada baixada | 1 vermelho | 06-06 |
| Número sem máscara no texto da notificação | 2 vermelhos | 06-06 |
| Degradação publicando aviso sem ações | 3 vermelhos | 06-06 |
| Ação apontando para componente inexistente | 2 vermelhos | 06-06 |
| Consulta à agenda anulada no coordenador de triagem | **5 vermelhos** nas quatro políticas de contato e no padrão de fábrica | 06-07 |
| Declaração do serviço de chamada removida do manifest, app reinstalado | concessão do papel **rc=255**, `RuntimeException: Failed` | 06-07 |
| Piso do gate de cobertura levantado para 99 | `koverVerify FAILED` | 06-08 |

Toda sabotagem incidiu em código **já commitado**. A restauração foi por cópia de arquivo salva
antes da sabotagem em todo plano posterior ao 06-02, em que um `git checkout` reverteu 74 strings
ainda fora do índice; nos casos em que o `git checkout` foi usado (06-03 e 06-05), foi porque o
trabalho novo já estava no índice, e isso está registrado ali.

## 7. O que esta fase deliberadamente NÃO prova

- **Roteamento de áudio real** (viva-voz, fone, Bluetooth): o aparelho virtual expõe somente a rota
  de alto-falante. Cenário 52 do roteiro físico. É o único ponto de DIA-02 fora do alcance daqui.
- **Chamada de entrada simulada:** exige o console do aparelho virtual, cujo segredo de acesso vive
  no diretório pessoal de quem executa. Substituída pelo exercício do coordenador de triagem real
  com a agenda preparada.
- **Número privado no modo discador:** não medido, e por isso `docs/LIMITACOES.md` item 8 voltou a
  dizer **não verificado**. Veredito no cenário 59.
- **Comportamento da One UI:** diálogo de troca de telefone padrão, papel após atualização do
  sistema e otimização de bateria agressiva — cenários 23, 57 e 58, com as três questões abertas
  registradas no roteiro.
