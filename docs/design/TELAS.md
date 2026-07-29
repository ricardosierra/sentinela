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
                                                                                  └── privacidade e sobre + apoio (sem mockup)
```

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

## 11. UI de chamada (modo discador, Fase 6) — **sem mockup**

`InCallService` próprio, design system aplicado (dark, primary "Security Blue"):
- **Chamada recebida**: nome do contato (lookup em memória) ou número mascarado + botões
  grandes Atender / Recusar (alvos ≥ 64dp, funciona na tela bloqueada).
- **Em chamada**: timer, Mudo, Viva-voz, Teclado (DTMF), Encerrar.
- Escopo mínimo: uma chamada por vez; sem conferência/vídeo/gravação.

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
