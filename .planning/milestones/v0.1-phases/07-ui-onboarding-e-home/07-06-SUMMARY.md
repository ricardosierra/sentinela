---
phase: 07-ui-onboarding-e-home
plan: 06
subsystem: ui-onboarding
tags: [onboarding, permissao-em-runtime, politica-por-origem, acessibilidade, honestidade]
requires:
  - "os seis componentes compartilhados de 07-03 (OptionCard, StepHeader, SentinelaTopBar, SettingSwitchRow)"
  - "InfoBanner da Fase 6"
  - "a maquina pura de quatro estados da permissao da agenda (Fase 4, unificada em RuntimePermissionAsk na Fase 5)"
  - "os asserts de alvo de toque em dois eixos de 07-02"
provides:
  - "ContactsPolicyStepScreen: passo 3 de 6 com os quatro ramos da permissao da agenda, quatro politicas e o interruptor de privados"
  - "WhitelistPolicyStepScreen: passo 4 de 6 com Nunca Silenciar como padrao e hint permanente"
  - "23 casos de composicao travando os quatro ramos, os dois padroes e os dois eixos de toque"
affects:
  - "07-09 monta os passos 3 e 4 na rota e liga o launcher da permissao da agenda"
  - "a tela Protecao repete o interruptor de privados com a mesma explicacao"
tech-stack:
  added: []
  patterns:
    - "ramo de permissao por `when` exaustivo sobre o enum, reusando canRequest e shouldOfferSystemSettings"
    - "consequencia honesta como AVISO na tela, nunca como nota de rodape"
    - "opcao nunca desabilitada como forma de pressionar concessao de permissao"
    - "pre-visualizacao por PreviewParameter quando quatro funcoes anotadas estourariam o limite de funcoes do detekt"
key-files:
  created:
    - app/src/main/java/org/sentinela/app/ui/onboarding/ContactsPolicyStepScreen.kt
    - app/src/main/java/org/sentinela/app/ui/onboarding/WhitelistPolicyStepScreen.kt
    - app/src/test/java/org/sentinela/app/ui/onboarding/ContactsAndWhitelistStepTest.kt
  modified: []
decisions:
  - "negar a leitura da agenda NAO desabilita nenhuma das quatro opcoes: a escolha e preferencia persistida e desabilitar seria pressao, nao protecao"
  - "na negacao definitiva nao existe botao de pedir a permissao — a plataforma nao mostra mais o dialogo e o toque nao faria nada"
  - "o aviso temporizado do mockup da whitelist virou texto permanente; o quadro ilustrado remoto virou cartao tonal (o app nao tem rede)"
metrics:
  duration: ~55min
  tasks: 3
  files: 3
  completed: 2026-07-30
---

# Phase 7 Plan 06: Passos 3 e 4 do onboarding Summary

Os passos de contatos e de whitelist ficaram prontos com os quatro ramos da permissao da agenda
tratados um a um, a consequencia honesta de negar a agenda visivel NA TELA, e as quatro opcoes
sempre editaveis — provado por vermelho que desabilita-las quebra a suite.

## O que foi feito

### Task 1 — `ContactsPolicyStepScreen`, passo 3 de 6 (`f527335`)

Composta pura, sem dono de estado e sem plataforma: quem dispara o launcher da permissao e a rota,
por `onGrantContacts`. Barra superior com o contador de passo e a acao de pular, titulo e explicador
alinhados a esquerda, o cartao de permissao, as quatro politicas em grupo selecionavel (com "Tocar"
selado como padrao do Sentinela), o interruptor de bloquear privados com a explicacao permanente, e
o botao pilula de 56dp com "Proximo" FIXO fora da area rolavel.

Os quatro ramos do cartao de permissao, nenhum mudo:

| Estado | Tratamento na tela |
|---|---|
| nunca perguntado | cartao tonal com icone de contatos, a justificativa e o botao tonal de permitir |
| concedido | colapsa num chip pilula com a cor de aceitar a 20% e o texto de concedido |
| negado uma vez | aviso com a consequencia honesta e acao de permitir |
| negado definitivamente | aviso de bloqueio com atalho para as configuracoes, SEM botao de pedir |

O ramo sai de um `when` exaustivo sobre o enum — estado novo da permissao quebra a compilacao aqui
em vez de produzir um ramo mudo — e a oferta de acao passa por `canRequest` e
`shouldOfferSystemSettings` como elas ja existem. Nenhuma condicao propria foi reimplementada, e
nenhuma segunda maquina de estado nasceu: a que existe e a pura da Fase 4, ja unificada em
`RuntimePermissionAsk` na Fase 5.

A descricao de "Nunca Silenciar" e a do recurso, corrigida na Fase 1 — ela diz que o "Nao Perturbe"
do sistema continua valendo, e o `git diff` do `strings.xml` deste plano esta VAZIO: nenhuma chave
foi criada nem alterada, as 269 existentes bastaram.

O KDoc registra a alternativa considerada e rejeitada (desabilitar os cartoes) com os dois motivos:
desabilitar sem explicar e pior que nao desabilitar, e o estado desabilitado e justamente onde a
semantica mesclada perde informacao.

### Task 2 — `WhitelistPolicyStepScreen`, passo 4 de 6 (`c0e8441`)

Passo 4 com "Nunca Silenciar" PRIMEIRO e selado como padrao, cartao explicativo tonal com circulo de
80dp e escudo, pergunta em rotulo, quatro politicas, botao "Proximo", botao de texto "Voltar" e o
texto de rodape fixo. As tres adaptacoes do mockup estao registradas em KDoc: a imagem remota virou
cartao tonal porque o aplicativo nao declara acesso a internet; o aviso que desliza depois de um
segundo virou TEXTO PERMANENTE, porque informacao que desaparece sozinha e informacao perdida e aviso
temporizado e hostil a quem usa leitor de tela; e o botao e "Proximo", nao "Finalizar", porque o
contador unico de seis passos resolveu a contradicao dos mockups e finalizar so existe no passo 6.
A quarta correcao, de texto, tambem esta registrada: o padrao e do Sentinela, nao do sistema.

### Task 3 — `ContactsAndWhitelistStepTest` (`3bf0d89`)

Robolectric com `sdk = [35]` e os qualificadores de tela reais, `createComposeRule`. **23 casos**
(minimo do plano: 14), 0 falhas. Um caso por estado da permissao nos ramos e outro por estado na
garantia de que as quatro opcoes continuam habilitadas — quatro casos e nao um laco, porque a regra
de composicao hospeda UMA arvore por caso. Sete usos de `assertLayoutHeightIsAtLeast`, cada um no
mesmo caso do par de asserts de alvo de toque. Os tres asserts sao IMPORTADOS de
`org.sentinela.app.ui`; nenhum foi redeclarado. Todo texto de assert vem de `context.getString`.

O caso do interruptor de privados cobre o ponto de risco (b) da semantica mesclada de novo: papel de
interruptor no proprio no do controle, descricao de estado distinta entre ligado e desligado, e a
explicacao permanente visivel como no separado.

## Provas de vermelho

Todas sobre codigo JA COMMITADO, todas restauradas por edicao manual — nenhum `git checkout`, pelo
precedente da Fase 6 (o comando reverteria trabalho concorrente ainda fora do indice). Depois das
tres restauracoes o `git diff` dos dois arquivos de producao ficou VAZIO.

### Prova 1 — a negacao definitiva oferecendo o pedido em vez do atalho

```
negado definitivamente exibe o bloqueio com atalho para as configuracoes FAILED
    java.lang.AssertionError at ContactsAndWhitelistStepTest.kt:172
negado definitivamente nao oferece nenhum pedido do sistema FAILED
    java.lang.AssertionError at ContactsAndWhitelistStepTest.kt:181
23 tests completed, 2 failed
```

Dois casos vermelhos, e os dois importam: o primeiro perde o atalho que e a UNICA saida real nesse
estado, e o segundo pega o botao que a plataforma ja ignora — o toque que nao faz nada.

### Prova 2 — as quatro opcoes desabilitadas sem a permissao

```
nunca perguntado mantem as quatro opcoes habilitadas FAILED
negado uma vez mantem as quatro opcoes habilitadas FAILED
negado definitivamente mantem as quatro opcoes habilitadas FAILED
23 tests completed, 3 failed
```

Exatamente os tres estados nao concedidos, e o caso do estado concedido seguiu verde — o vermelho
aponta a regra certa, e nao um efeito colateral. Confirma que a decisao de manter as opcoes
editaveis esta travada por teste, e nao apenas escrita em KDoc.

### Prova 3 — o padrao do passo 4 trocado para tocar

```
o passo 4 vem com nunca silenciar pre-selecionado FAILED
    java.lang.AssertionError at ContactsAndWhitelistStepTest.kt:255
23 tests completed, 1 failed
```

## Deviations from Plan

### Auto-fixed Issues

**1. [Rule 3 - Blocking] `LongMethod` e `TooManyFunctions` do detekt no passo 3**

- **Found during:** Task 1
- **Issue:** a composta principal fechou em 62 linhas (limite compartilhado 60), e a correcao natural
  — extrair o conteudo rolavel — levou o arquivo a 11 funcoes, exatamente o limite de funcoes por
  arquivo.
- **Fix:** conteudo rolavel extraido para `ConteudoDoPasso` privado, e as quatro pre-visualizacoes
  (uma por estado da permissao, como o plano pede) reunidas numa unica funcao alimentada por um
  provedor de parametro. As quatro pre-visualizacoes continuam existindo. Nenhum `@Suppress` novo e
  nenhuma folga no `detekt.yml` compartilhado. O mesmo padrao foi aplicado desde o inicio no passo 4.
- **Commit:** `f527335`

**2. [Rule 1 - Bug] icone de volume depreciado no passo 4**

- **Found during:** Task 2
- **Issue:** o icone de volume que o contrato de design pede para "Tocar" na whitelist produziu aviso
  de depreciacao no proprio arquivo novo, com a plataforma pedindo a variante espelhada.
- **Fix:** trocado pela variante espelhada. O arquivo novo entrou sem aviso proprio; os avisos que
  restam no build sao todos anteriores a este plano, em arquivos de outros pacotes.
- **Commit:** `c0e8441`

**3. [Rule 3 - Blocking] o criterio de contagem por texto nao distingue KDoc de codigo**

- **Found during:** Task 1
- **Issue:** o criterio exige exatamente uma ocorrencia da chave do aviso de negacao simples, e o
  KDoc citava a chave pelo nome para explicar a consequencia honesta — a contagem deu 2.
- **Fix:** o KDoc passou a DESCREVER o texto ("o texto do ramo de negacao simples") em vez de nomear
  o recurso, com o motivo registrado no proprio comentario. E a quarta encarnacao da armadilha
  registrada nas Fases 3 e 5 neste repositorio.
- **Commit:** `f527335`

### Fora de escopo, nao corrigido

Nada foi tocado fora dos tres arquivos do plano. Durante a Task 1 o `detekt` acusou
`UnusedPrivateMember` em `ui/onboarding/WelcomeScreen.kt` e, na verificacao final, o
`compileDebugKotlin` falhou por referencia nao resolvida em `ui/onboarding/RoleStepScreen.kt` com
`ui/components/CheckRow.kt` modificado — as duas coisas eram provas de vermelho EM CURSO de planos
concorrentes desta onda, sobre arquivos de outro dono. Fronteira respeitada: nada editado, verificacao
repetida ate a onda liberar (verde na terceira tentativa), no molde de ambiente registrado em 07-01,
07-02 e 07-03.

## Verification

```
./gradlew --rerun-tasks assembleDebug testDebugUnitTest lint detekt   BUILD SUCCESSFUL
suite JVM completa: 759 casos, 0 falhas (era 685 ao fim de 07-03; inclui casos de planos concorrentes)
ContactsAndWhitelistStepTest: 23 casos, 0 falhas
bash scripts/verify-invariants.sh                                    todos os invariantes OK (8 blocos)
./gradlew koverLog                                                   96,6157% (inalterado: ui.* fora do filtro)
git diff app/src/main/res/values/strings.xml                         VAZIO (nenhuma chave nova)
grep -c 'contacts_permission_denied'      ContactsPolicyStepScreen    1
grep -c 'contacts_option_never_silence_desc'                          1
grep -c 'enabled = false'                 ContactsPolicyStepScreen    0
as quatro constantes de ContactsPermissionState no arquivo            1 cada
grep -c 'onboarding_next' / 'onboarding_finish'  Whitelist...Screen   1 / 0
grep -ciE 'googleusercontent|https?://'   Whitelist...Screen          0
grep -ciE 'Snackbar'                      Whitelist...Screen          0
grep -c 'whitelist_setup_hint'            Whitelist...Screen          1
grep -c 'assertLayoutHeightIsAtLeast'     ContactsAndWhitelistStepTest 7
```

`app/build.gradle.kts` nao foi tocado e o filtro do Kover nao foi alargado — os tres arquivos vivem
em `ui.*`, fora dos pacotes medidos, e nenhum exclude novo foi criado.

## Notas para os planos seguintes

- A rota do passo 3 e quem grava o flag persistido da permissao e dispara o launcher, e ela deve
  gravar o flag NO MOMENTO do disparo, nunca no retorno (contrato da Fase 4). A tela nao sabe disso e
  nao deve saber.
- `onOpenAppSettings` so e exercido no ramo de negacao definitiva; a rota pode ligar os dois retornos
  ao mesmo lugar sem quebrar teste, mas perderia a distincao que a tela faz.
- O interruptor de bloquear privados repete na tela Protecao com a MESMA explicacao permanente: o
  texto ja existe no recurso e nao deve ser reescrito lá.
- Quem precisar de quatro pre-visualizacoes num arquivo que ja tenha varias compostas privadas usa o
  provedor de parametro: quatro funcoes anotadas estouram o limite de funcoes por arquivo, e afrouxar
  a regra compartilhada por pre-visualizacao seria o preco errado.

## Self-Check: PASSED

Os tres arquivos criados existem no disco e os tres commits (`f527335`, `c0e8441`, `3bf0d89`) estao
no historico. Os dois arquivos de producao no disco sao identicos ao ultimo commit: as tres
restauracoes das provas de vermelho fecharam sem diferenca residual.
