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

## 1. `telas/boas_vindas_ao_sentinela/` — Boas-vindas

Landing pré-onboarding. Header (escudo + "Sentinela"), hero flutuante com ícone de segurança,
H1 **"Sua primeira linha de defesa contra spam."**, bento grid com 3 feature-cards, imagem
com badge "Proteção Ativa", CTA pill **"Começar Configuração"** + microcopy
*"Leva menos de 2 minutos."*.

**Adaptações obrigatórias:**
- O card "Base Global — Milhões de números identificados" promete o que o MVP local não faz
  → usar os cards honestos (**Bloqueio Local / Silencioso / Sem Internet**).
- Incluir selo/linha **"100% open source — sem propaganda, sem telemetria"** (UIX-13/ENG-03).

## 2. `telas/onboarding/` — Papel de Call Screening (passo 1)

Hero com escudo, explicação de privacidade, bento grid (Bloqueio Local / Silencioso / Sem
Internet), CTA **"Configurar Agora"** com estado "Solicitando permissão…", disclaimer sobre a
permissão de *Call Screening*. Dispara `ScreeningRoleManager.buildRequestIntent()`.

## 3. `telas/configura_o_desconhecidos/` — Desconhecidos (passo 2)

Card com ícone `no_sim`, título **"Chamadas Desconhecidas"**, pergunta e 3 option-cards
single-select com `check_circle` → `ScreeningSettings.unknownPolicy`:
- **Bloquear** (default) — "Recusa a chamada instantaneamente." → `OriginPolicy.BLOCK`
- **Silenciar** — "A chamada chega sem som nem vibração." → `OriginPolicy.SILENCE`
- **Permitir** — "Recebe chamadas normalmente." → `OriginPolicy.RING`

O estilo do BLOCK (rejeitar × caixa postal) fica na tela Proteção (`BlockMode`).
Microcopy: *"Você pode alterar esta configuração a qualquer momento."*

## 4. `telas/configura_o_contatos/` — Contatos (passo 3, **políticas reais**)

Mesmo layout de wizard do mockup (indicador de passo + barra de progresso), H1
**"E as pessoas da sua lista de contatos?"** e os 4 option-cards do mockup →
`ScreeningSettings.contactsPolicy`:
- **Tocar** (default, badge "Padrão") — "As chamadas tocam normalmente no seu telefone." → `RING`
- **Bloquear** — "Bloqueia todas as chamadas, enviando para a caixa postal." → `BLOCK`
- **Silenciar** — "O telefone não vibra nem toca, mas mostra a notificação." → `SILENCE`
- **Nunca Silenciar** — "O Sentinela nunca silencia sua lista de contatos. O 'Não Perturbe' do
  sistema continua valendo." → `NEVER_SILENCE`

Este passo dispara o pedido de **READ_CONTACTS** com a explicação
`contacts_permission_rationale` (leitura 100% local, nada armazenado). Card informativo
honesto: as políticas por contato valem no modo filtro **enquanto a leitura da agenda estiver
concedida** — se o usuário revogar, o Android nem aciona o Sentinela para contatos conhecidos e
eles voltam a tocar pelo caminho nativo, sem aviso (link "saiba mais" → Proteção).
Toggle extra real: **"Bloquear números privados"** (config que o mockup não expôs).

## 5. `telas/configura_o_whitelist/` — Whitelist (passo 4)

Card "O que é a Whitelist?" (manter texto), pergunta **"Como tratar sua Whitelist
Pessoal?"** com os 4 option-buttons do mockup → `ScreeningSettings.whitelistPolicy`:
- **Nunca Silenciar** (default) — "O Sentinela nunca silencia essa origem. O 'Não Perturbe' do
  sistema continua valendo." → `NEVER_SILENCE`
- **Tocar** — "Sempre emitir som." → `RING`
- **Bloquear** — "Rejeitar automaticamente." → `BLOCK`
- **Silenciar** — "Apenas notificação visual." → `SILENCE`

CTA **"Finalizar Configuração"** + "Voltar"; hint fixo sobre poder alterar depois.
Ao finalizar: verificação final (papel concedido? permissão de contatos? proteção ativa?)
e entrada no dashboard.

## 6. `telas/dashboard/` — Início

- Card hero `primary-container` com **"Proteção Ativa"** + dot pulsante + toggle master
  (OFF → "Proteção desativada" com destaque de aviso).
- Estado extra obrigatório: papel perdido → aviso **"O Sentinela não é o filtro de chamadas
  padrão."** + botão **"Corrigir configuração"**.
- Stats: **Total Bloqueado** e **Hoje** (dados do `BlockedCallRepository`).
- **"Última chamada bloqueada"** com número mascarado `+55 11 9****-1234` e motivo real
  (Número Desconhecido / Privado) — **não** usar os rótulos de spam do mockup
  ("Provável Fraude Financeira"): o MVP não classifica spam.
- Quick actions: Whitelist Pessoal, Histórico de Bloqueio.
- Bottom nav 4 abas (Início ativa).

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
- Ações: **Deixar um comentário de apoio** (avaliação) e **Doar em Bitcoin**
  (endereço com botão copiar + toast "Endereço copiado!"; QR opcional).
- O endereço vem de `support_bitcoin_address` — release bloqueado enquanto vazio.

## 10. Proteção (Ajustes) — **sem mockup**

Grupos de configurações em cards `surface-container` (padding 16dp), cada opção com
explicação de uma linha em `on-surface-variant` (nada de opção sem explicação):

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
