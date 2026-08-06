# Telas do MVP — mapeamento dos mockups Stitch

> Fonte: 8 telas em [`telas/`](telas/) (screen.png + code.html cada). Tokens e fundamentos em
> [`DESIGN.md`](DESIGN.md). Labels da bottom nav em inglês nos mockups viram:
> **Início / Permitidos / Histórico / Ajustes**.

## Fluxo geral

```
boas_vindas → papel (call screening) → desconhecidos → contatos (políticas) → whitelist → dashboard
                                                                                  ├── whitelist_pessoal
                                                                                  ├── hist_rico_de_bloqueios
                                                                                  ├── proteção (ajustes, inclui modo discador)
                                                                                  └── privacidade e sobre + apoio (fora dos mockups)

modo discador (opcional):  proteção → ativação/reversão → discagem
                           chamada recebida / de saída → ativa → teclado de tons  (§11)
```

As telas do modo discador ficam **fora do fluxo de navegação normal**: a chamada é aberta pelo
sistema, não pelo usuário, e a discagem pode ser aberta pelo próprio Android com um número
pré-preenchido.

Onboarding canônico: boas-vindas → papel → desconhecidos → contatos → whitelist →
verificação final (os mockups divergiam na contagem de passos; ver `DECISOES.md`).
O convite de avaliação/apoio **nunca** aparece durante o onboarding (ENG-04).

## 1. Boas-vindas — `ui/onboarding/WelcomeScreen.kt`

Landing pré-onboarding. Cabeçalho com escudo e a palavra **Sentinela**, hero tonal, título, três
cartões de característica, selo de código aberto, CTA pill e microcopy de duração.

**Componentes:** `SentinelaWatermark`, `HonestyCard` ×3, CTA pill de 56dp.
**Chaves:** `welcome_*`, `onboarding_start_cta`, `onboarding_duration_hint`, `about_open_source_badge`.

**Substituições de copy (decisão do usuário, 2026-07-30).** O mockup prometia capacidades que o MVP
não tem. O layout foi preservado; **só os textos** mudaram:

| Prometido no mockup | O que a tela diz | Por quê |
|---|---|---|
| base mundial com milhões de números | Bloqueio local | o app não declara acesso à internet |
| processamento local com cifragem | Sem internet, nada sai do aparelho | não há cifragem; há ausência de rede, que é verificável |
| filtros inteligentes | Regra clara e previsível | a decisão é determinística, não aprende |
| classificação de motivo da chamada | motivo **da decisão**, não da chamada | o app não classifica intenção |
| proteção contra remetentes já conhecidos | desconhecido não interrompe | mais forte: não depende de reconhecer o número |

As cinco capacidades estão registradas, com dependências, em
[`../backlog/capacidades-prometidas-nos-mockups.md`](../backlog/capacidades-prometidas-nos-mockups.md)
para versões posteriores ao MVP.

**Imagens remotas substituídas.** Duas telas dos mockups carregavam imagem de um domínio externo.
Impossível sem acesso à internet, e contraditório num app de privacidade. Ambas viraram superfície
tonal — o que o próprio mockup do passo 1 já fazia.

## 2. Passo 1 de 6: papel de triagem — `ui/onboarding/RoleStepScreen.kt`

Explicação do que o app faz, **o aviso obrigatório de escopo** (só chamadas telefônicas são
filtradas — não WhatsApp nem outras chamadas por internet) e o pedido do papel de triagem.

**Três ramos de estado:** papel disponível e não concedido, concedido, indisponível no aparelho.
Nenhum deles bloqueia o fluxo — negar leva à home com o estado real e o botão de correção.
**Componentes:** `StepHeader`, `HonestyCard`, CTA. **Chaves:** `role_step_*`, `onboarding_scope_warning`.

## 3. Passo 2 de 6: desconhecidos — `ui/onboarding/UnknownPolicyStepScreen.kt`

Quatro políticas em `OptionCard` agrupados por `Modifier.optionCardGroup()`, com descrição
permanente sob cada uma. **Padrão: Bloquear** — extraído do mockup e coincidente com
`ScreeningSettings`, então a tela reflete o repositório em vez de redefinir padrão.

## 4. Passo 3 de 6: contatos — `ui/onboarding/ContactsPolicyStepScreen.kt`

Política para quem está na agenda, com o pedido de leitura da agenda. **Padrão: Tocar.**

**Quatro ramos de permissão**, do estado puro de 4 estados que a Phase 4 entregou: nunca pedida,
negada uma vez, negada permanentemente (atalho para as configurações do sistema, sem insistir),
concedida. **As opções nunca são desabilitadas** — desabilitar seria pressão. Negar não impede
seguir: a consulta devolve indisponível e a política de reserva resolve.

## 5. Passo 4 de 6: whitelist — `ui/onboarding/WhitelistPolicyStepScreen.kt`

Tratamento da whitelist pessoal. **Padrão: Nunca Silenciar.** Mesmo padrão de `OptionCard` do passo 2.

## 6. Passos 5–6 e home

### Passo 5: notificação — `ui/onboarding/NotificationStepScreen.kt`
Opt-in **sem pressão**: nasce desligada, porque o valor do produto é não interromper. A permissão de
notificação é pedida em runtime só quando o usuário liga a opção.

### Passo 6: verificação final — `ui/onboarding/SummaryStepScreen.kt`
Resumo do estado real. **Nunca falsamente positivo:** se o papel foi negado ou uma permissão falta, a
tela diz isso em vez de mostrar visto verde. `CheckRow` com o botão de correção em nó próprio,
alcançável pelo leitor de tela.

### Home — `ui/home/HomeScreen.kt`
`StatusHeroCard` com o interruptor principal, `StatCard`, `LastBlockedCard`, `QuickActionRow`,
`InfoBanner` para os avisos.

- **O interruptor liga/desliga a preferência de proteção**, não o papel do sistema. O papel é estado
  somente-leitura num banner com botão de correção — revogar papel **mata o processo** (medido na
  Phase 6), e o usuário veria o app fechar sozinho ao mexer num interruptor.
- **Status consultado vivo a cada retomada, nunca cacheado.** Medido: p50 29,9 µs — três ordens de
  grandeza abaixo de um frame, então cachear só compraria a mentira.
- **"Última bloqueada" usa número mascarado.** A home pode estar visível a terceiros. Número completo
  aparece **somente** nas telas de chamada e discagem (§11).
- **Contagem: zero mentiroso é impossível por tipo.** `StatCard` recebe `StatValue`
  (`Loaded` / `Unavailable` / `Loading`), nunca um número cru. Com o histórico desligado a tela mostra
  o estado, não `0`.
- Oito estados degradados cobertos, incluindo proteção ligada sem leitura da agenda (avisa que
  contatos podem cair como desconhecidos, com atalho para conceder).


## 7. `telas/whitelist_pessoal/` — Permitidos

TopAppBar com back + busca; campo "Buscar…" (placeholder adaptado para descrições/números);
section header **"Números Permitidos (N)"**; lista com avatar de iniciais, nome/descrição,
número formatado, ações editar/excluir; info card "Sobre a Whitelist"; FAB **"+"** (Adicionar
Número). **Adições fora do mockup (exigidas pelo escopo):** menu com **Exportar/Importar
backup** (com confirmação), toggle ativar/desativar por entrada e acesso ao **tratamento da
whitelist** (mesmas 4 opções do passo 4). O card "Sobre a Whitelist" descreve o tratamento
conforme a política configurada — o texto fixo do mockup ("nunca serão bloqueados") só era
verdadeiro para o padrão.

## 8. `telas/hist_rico_de_bloqueios/` — Histórico

> PNG deste mockup veio corrompido no zip; layout extraído do `code.html`.

TopAppBar **"Histórico do Sentinela"** + ação "Limpar tudo" (`delete_sweep` com confirmação);
chips de período **Hoje / 7 dias / 30 dias** + **filtro por decisão** (exigido pelo escopo,
fora do mockup); lista com ícone por motivo (block/visibility_off), número mascarado,
timestamp relativo; ações por item (swipe/menu): **Permitir** (→ whitelist), **Marcar
indesejado**, **Excluir**; empty state "Fim do histórico recente"/"Nenhuma chamada bloqueada
ainda.". Badge "ALTO RISCO" do mockup **não entra** (MVP não classifica risco).

## 9. Privacidade e sobre + Apoie o Sentinela — **sem mockup**

Seguir `DESIGN.md`: lista de dados armazenados, permissões usadas (link para matriz),
retenção configurada, **Limpar todos os dados** (confirmação dupla), versão, limitações,
links para configurações do app e do canal de notificação.

Seção **"Apoie o Sentinela"** (UIX-13/ENG-03), destaque visual do card:
- Pitch: *"O Sentinela é open source: sem propaganda, sem telemetria, sem envio de dados
  para a nuvem — 100% offline, rodando no seu próprio celular."*
- Ações: **Deixar um comentário de apoio** (avaliação) e dois botões de copiar endereço de
  doação — **Bitcoin** (on-chain) e **Liquid (L-BTC)** — cada um com toast
  "Endereço copiado!"; QR opcional.
- Acima dos botões, uma linha honesta: doação é opcional e o usuário deve conferir o endereço
  antes de enviar. Nada de contagem, meta ou insistência — apoio não é dark pattern.
- Os endereços vêm de `support_bitcoin_address` e `support_liquid_address`, que são a **única**
  fonte deles no projeto: nenhum endereço literal em Kotlin, doc ou README. `SupportAddressTest`
  recalcula o checksum dos dois (bech32 em `bc`, blech32 confidencial em `lq`) — endereço colado
  errado reprova o build, porque doação para endereço errado não tem volta.

## 10. Proteção (Ajustes) — `ui/settings/SettingsScreen.kt` — implementada na Fase 7

Sem mockup de origem; derivada do sistema visual. **16 itens** em `SettingsGroup` sobre
`surface-container` (padding 16dp), cada opção com explicação **permanente** de uma linha em
`on-surface-variant` — nada de dica escondida, porque o objetivo declarado da fase é o usuário
entender o que cada política significa.

**Contrato de comportamento, travado por teste:**
- **Efeito imediato, sem botão salvar.** O `snapshot()` do DataStore com cache volátil já alimenta a
  triagem; a mudança vale na próxima chamada. Teste afirma a ausência de qualquer ação de salvar.
- **Exatamente dois diálogos de confirmação**, e só para o que perde dado. Trocar política **não**
  confirma — é reversível, e confirmação excessiva treina o usuário a aceitar sem ler. Um teste
  afirma que limpar histórico não apaga nada antes da confirmação.
- **Completude:** um teste percorre os 16 itens e falha se algum desaparecer.
- O modo discador reaproveita `ui/dialer/DialerActivationScreen.kt`, entregue na Fase 6 e ligado à
  navegação nesta fase.

**Armadilha real encontrada aqui:** os rótulos das políticas ("Bloquear", "Silenciar", "Tocar",
"Nunca Silenciar") **colidem entre os três grupos**. Clicar por rótulo atingia o grupo errado. As
políticas são acionadas pela descrição, única por grupo, e nenhum título de grupo pode repetir o
rótulo de um membro.

Itens, na ordem:

1. **Ativar proteção** — toggle master (mesmo estado do hero do dashboard); OFF pinta o card
   com `error-container` a 15% e texto "Proteção desativada".
2. **Números desconhecidos** — seleção única: Bloquear / Silenciar / Permitir (`unknownPolicy`).
3. **Contatos da agenda** — seleção única: Tocar / Bloquear / Silenciar / Nunca Silenciar
   (`contactsPolicy`), com nota "vale enquanto a leitura da agenda estiver concedida".
4. **Whitelist pessoal** — seleção única: Nunca Silenciar / Tocar / Bloquear / Silenciar
   (`whitelistPolicy`).
5. **Bloquear números privados** — switch (`blockPrivateNumbers`).
6. **Modo de bloqueio** — "Rejeitar imediatamente" × "Encaminhar silenciosamente"
   (`BlockMode`), com explicação do efeito para quem liga.
7. **Pedir para não registrar no histórico do telefone** — switch (`hideFromNativeCallLog`) +
   nota obrigatória de que o Android só atende o pedido para apps de operadora: a chamada
   bloqueada continua no histórico, marcada como bloqueada (item 3 de LIMITACOES.md). O switch
   existe para a intenção do usuário e para o modo operadora; a tela nunca promete ausência de
   registro.
8. **Exibir notificação silenciosa** — switch (`showOwnNotification`); ao ligar pela primeira
   vez dispara o pedido de `POST_NOTIFICATIONS`; sub-opção número mascarado × anônimo.
9. **Modo discador (avançado)** — switch/fluxo (`ROLE_DIALER`): explica o que muda (políticas
   valem para contatos; UI de chamada própria), exige READ_CONTACTS e oferece reversão.
10. **Retenção do histórico** — seletor (nunca/7/30/90 dias/manual).
11. **Em caso de erro** — política de fallback: "Permitir chamada (recomendado)" ×
    "Bloquear chamada" (`FallbackPolicy`), com explicação honesta do trade-off.

## 11. UI de chamada e discagem (modo discador, Fase 6) — contrato fechado, fora dos mockups

Derivada do mesmo design system, com acabamento equivalente aos mockups. Contrato completo (com
tabelas de cor, tipografia e chaves de string) no plano da fase; o que segue é o resumo canônico.

### 11.1 As seis telas

1. **Chamada recebida** (`IncomingCallScreen`) — tela cheia, exibida por pedido de tela cheia no
   canal de chamada, **nunca** por overlay de sistema; aparece também sobre a tela bloqueada.
   Coluna: marca d'água → rótulo de estado → avatar 96dp → identidade → chip de origem →
   barra Recusar/Atender empurrada para o rodapé.
2. **Chamada de saída** (`OutgoingCallScreen`) — mesmo esqueleto; estado "Chamando…"/"Tocando…",
   três pontos de 6dp em fade sequencial (nunca indicador circular girando), ação única
   **Encerrar**, e mudo/viva-voz já habilitados antes do atendimento.
3. **Chamada ativa** (`ActiveCallScreen`) — identidade + cronômetro, fileira de três controles
   (Mudo / Teclado / Viva-voz) e **Encerrar** centralizado.
4. **Teclado de tons** (`DtmfKeypadSheet`) — painel ancorado ao rodapé, ~70% da altura, cantos
   superiores 24dp; o cronômetro e o Encerrar continuam visíveis e clicáveis acima dele. Grade
   3×4 (`1 2 3 / 4 5 6 / 7 8 9 / * 0 #`), linha de dígitos enviados no topo, sem placeholder.
   O tom em si é responsabilidade do Telecom, não da UI.
5. **Discagem** (`DialpadScreen`) — alvo do pedido de discagem do sistema e entrada manual.
   Campo **somente saída** (não abre teclado do sistema), formatação progressiva pt-BR,
   sugestão de nome quando o número casa com contato/whitelist (nunca inventada), grade de
   teclas idêntica à do teclado de tons, **Chamar** e **Apagar** (toque = 1 dígito, toque longo
   = limpa tudo). Falha ao originar vira barra de aviso com "Tentar de novo" e **mantém** o
   número digitado.
6. **Ativação / reversão do modo discador** (`DialerActivationScreen`) — alcançada por Proteção.
   Dois cards de **estilo idêntico e deliberado**, "O que muda" e "O que não muda", com o mesmo
   peso visual; card de pré-requisito quando a leitura da agenda está negada (o CTA fica
   desabilitado); microcopy "Quem decide é o Android. Você pode voltar quando quiser.".
   Com o modo ativo a mesma tela vira painel de reversão: chip "Ativo", CTA **tonal** (reverter
   não é destruir dado — nunca cor de erro) que abre o **seletor do sistema**. O app nunca força
   a troca. Se o papel foi perdido em silêncio, banner informativo, sem vermelho e sem alarme:
   "O modo filtro continua funcionando."

### 11.2 As quatro variantes de identidade (todas obrigatórias)

| Caso | Avatar | Primária | Secundária | Chip |
|------|--------|----------|-----------|------|
| Contato da agenda | foto da agenda, ou monograma com anel 2dp `primary` | nome do contato | número completo formatado | "Contato" |
| Número na whitelist | ícone `verified_user` sobre `primary` a 15% | descrição da entrada, senão o número | número completo | "Permitido" |
| Desconhecido com número | ícone `person` | **o número completo** promovido a linha primária | região/operadora **não** é exibida (o MVP não a conhece — nunca inventar) | "Desconhecido" |
| Privado / sem identificação | ícone `visibility_off` | "Privado" | "ID Oculto" | "Número privado" |

A foto do contato é lida **em memória** no instante da chamada e nunca cacheada em disco. Com a
leitura da agenda revogada, a tela degrada para "Desconhecido com número" — sem erro e **sem
nenhum aviso durante a chamada**.

A tela de chamada recebida **não expõe controle de política**: o chip é passivo. Mudar política
com o telefone tocando é decisão sob pressão, e qualquer toque perto de atender/recusar aumenta a
chance de erro irreversível. Ajuste de política vive em Proteção e no item do histórico.

### 11.3 Tamanhos travados

| Elemento | Tamanho |
|----------|---------|
| Atender e Recusar | **72dp** de diâmetro |
| Encerrar | **64dp** |
| Tecla do teclado (discagem e tons) | **72dp**, gap 8dp |
| Controle secundário (mudo/viva-voz/teclado) e Apagar | **56dp** |
| Fechar o painel de teclado | 48dp |
| Avatar / monograma | **96dp** |

São mínimos contratuais, não sugestões: o tamanho **desenhado** é o que vale, porque o Compose
expande sozinho o alvo de toque de qualquer componente interativo e um controle encolhido passaria
verde num teste que só olhasse o alvo. Nenhuma ação destrutiva fica a menos de 24dp de outra ação.

### 11.4 Cores funcionais fixas, fora da cor dinâmica

| Ação | Fundo | Conteúdo |
|------|-------|----------|
| Atender | `#1E6E42` | `#D9F2E3` |
| Recusar / Encerrar | `#93000A` (`error-container`) | `#FFDAD6` (`on-error-container`) |

A cor dinâmica continua ligada no resto do app, mas **estas quatro cores saem por literal** e
chegam por parâmetro. Razão: a partir do Android 12 o tema troca o esquema **inteiro** por um
derivado do papel de parede — ler o vermelho de recusar pelo esquema deixaria um papel de parede
aproximá-lo do verde de atender e produzir o único erro irreversível do app (recusar por engano
uma chamada real). Os dois botões diferem também por **ícone** (`call` × `call_end`) e por
**rótulo textual** sob o botão: estado nunca é comunicado só por cor. Contraste medido ≥ 7:1.

### 11.5 Número completo na tela, mascarado em todo o resto

Nas telas de chamada, de tons e de discagem o número aparece **completo e formatado**: é a chamada
do próprio usuário, acontecendo agora, e ele precisa do número inteiro para decidir se atende — um
discador que mascara o número que está tocando é inútil. A proibição do projeto é sobre **log,
notificação, histórico e relatório de falha**, e ali a máscara é obrigatória, sempre. A fronteira é
testável e testada. Origem privada nunca ganha rótulo inventado: "Privado" / "ID Oculto", nunca
"Desconhecido".

### 11.6 Os dez requisitos de acessibilidade (critério de aceite, não enfeite)

1. Todo alvo de toque ≥ 48dp — os tamanhos de 11.3 são todos folgados sobre esse piso.
2. Todo ícone acionável com descrição de conteúdo **em recurso**, incluindo o estado quando o
   controle alterna.
3. Nunca só cor: atender/recusar por ícone e rótulo; mudo/viva-voz/teclado por ícone e/ou
   descrição de estado.
4. Contraste ≥ 4,5:1 no corpo e ≥ 3:1 em texto grande e ícones; ≥ 7:1 nos pares funcionais.
   Sob cor dinâmica, par que caia abaixo do mínimo cai para os tokens fixos.
5. Ordem de foco **declarada** na chamada recebida: marca → estado → identidade → chip →
   recusar → atender. Elemento decorativo não é focável.
6. Número lido **dígito a dígito**, nunca como valor numérico — na discagem e na chamada.
7. Escala de fonte a 200%: nenhum controle sai da tela nem se sobrepõe; o número reduz por
   autoajuste, os botões **não** reduzem.
8. Com "reduzir animações" ligado, pulsação, fade dos três pontos e escala de toque são
   suprimidos. Nenhuma informação depende de animação.
9. Na tela bloqueada a chamada recebida é legível e operável sem desbloquear, e nada além do que
   a tela já mostra vaza para a tela de bloqueio.
10. Em paisagem, chamada recebida e ativa viram duas colunas (identidade à esquerda, ações à
    direita) — os botões de ação **nunca** são cortados.

### 11.7 Escopo e honestidade

Uma chamada por vez: chamada em espera, segunda chamada, conferência, transferência, vídeo,
gravação, mensagem rápida ao recusar, arrastar para atender, histórico do discador e busca de
contatos na discagem estão **fora desta versão** e registrados em
[`../LIMITACOES.md`](../LIMITACOES.md) — são decisões de escopo, não lacunas. Nenhum texto destas
telas afirma que o bloqueio é garantido ou total, que a chamada bloqueada deixa de entrar no
histórico do telefone, ou que chamadas de aplicativos de internet são filtradas. Nenhum texto
pressiona a ativação: sem "recomendado", sem urgência, sem contador.

## 12. Convite de avaliação (5ª abertura) — **sem mockup**

Bottom sheet (radius 24dp) disparado na 5ª abertura (depois 10ª, 15ª… até aceitar — ENG-02):
título **"Está gostando do Sentinela?"**, corpo com o pitch open source, ações
**"Avaliar agora"** (In-App Review quando disponível; senão abre a seção Apoie) e
**"Agora não"**. Nunca sobre onboarding, chamada ou diálogo do sistema.

## Componentes recorrentes (design system aplicado)

- Header fixo h-64dp com escudo + título; backdrop blur ao rolar.
- Option-cards single-select com `check_circle` preenchido no selecionado.
- CTA pill full-width h-56dp fixo no rodapé com gradiente para transparente.
- Cards glass: `surface-container` a 60% + blur 12; radius 12–16dp; pills full.
- Bottom nav com aba ativa em pill `secondary-container`.
- Números sempre em Geist (monoespaçada de dados) e sempre mascarados fora do fluxo de cadastro.
