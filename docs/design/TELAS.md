# Telas do MVP — mapeamento dos mockups Stitch

> Fonte: 8 telas em [`telas/`](telas/) (screen.png + code.html cada). Tokens e fundamentos em
> [`DESIGN.md`](DESIGN.md). **Branding unificado: Sentinela** — mockups que dizem "Ultrathink"
> são adaptados. Labels da bottom nav em inglês nos mockups viram:
> **Início / Permitidos / Histórico / Ajustes**.

## Fluxo geral

```
boas_vindas → onboarding (papel) → desconhecidos → contatos (informativo) → whitelist → dashboard
                                                                                 ├── whitelist_pessoal
                                                                                 ├── hist_rico_de_bloqueios
                                                                                 ├── proteção (ajustes)
                                                                                 └── privacidade e sobre (sem mockup)
```

Onboarding canônico: **4 passos** (mockups divergem 4 vs 3 — decisão em `DECISOES.md`).

## 1. `telas/boas_vindas_ao_sentinela/` — Boas-vindas

Landing pré-onboarding. Header (escudo + "Sentinela"), hero flutuante com ícone de segurança,
H1 **"Sua primeira linha de defesa contra spam."**, bento grid com 3 feature-cards, imagem
com badge "Proteção Ativa", CTA pill **"Começar Configuração"** + microcopy
*"Leva menos de 2 minutos."*.

**Adaptação obrigatória:** o card "Base Global — Milhões de números identificados" promete o
que o MVP local não faz → substituir pelos cards honestos do mockup de onboarding
(**Bloqueio Local / Silencioso / Sem Internet**).

## 2. `telas/onboarding/` — Papel de Call Screening (passo 1 de 4)

Hero com escudo, explicação de privacidade, bento grid (Bloqueio Local / Silencioso / Sem
Internet), CTA **"Configurar Agora"** com estado "Solicitando permissão…", disclaimer sobre a
permissão de *Call Screening* (manter, trocando Ultrathink→Sentinela). Dispara
`ScreeningRoleManager.buildRequestIntent()`.

## 3. `telas/configura_o_desconhecidos/` — Desconhecidos (passo 2 de 4)

Card com ícone `no_sim`, título **"Chamadas Desconhecidas"**, pergunta e 3 option-cards
single-select com `check_circle`:
- **Bloquear** (default) — "Recusa a chamada instantaneamente." → `BlockMode.REJECT`
- **Silenciar** — "Encaminha em silêncio para a caixa postal." → `BlockMode.SILENT_VOICEMAIL`
- **Permitir** — "Recebe chamadas normalmente." → `blockUnknownNumbers = false`

Microcopy: *"Você pode alterar esta configuração a qualquer momento."*

## 4. `telas/configura_o_contatos/` — Contatos (passo 3 de 4, **vira informativo**)

Mockup oferece 4 políticas por contato (Tocar/Bloquear/Silenciar/Nunca Silenciar) —
**não implementável** sem discador padrão (contatos nem chegam ao filtro). Implementação:
mesmo layout de wizard (indicador "Passo 3 de 4" + barra de progresso), card informativo
explicando que a agenda continua tocando normalmente e que o Sentinela não lê contatos
(string `contacts_explainer`). Botão "Próximo". Adicionar toggle único real:
**"Bloquear números privados"** (config que o mockup não expôs em lugar nenhum).

## 5. `telas/configura_o_whitelist/` — Whitelist (passo 4 de 4)

Card "O que é a Whitelist?" (manter texto), CTA **"Finalizar Configuração"** + "Voltar",
hint fixo sobre poder alterar depois. **Simplificação:** as 4 opções de tratamento do mockup
viram comportamento fixo — whitelist sempre permite (é a definição dela); sem seletor.
Ao finalizar: verificação final de configuração (papel concedido? proteção ativa?) e entrada
no dashboard.

## 6. `telas/dashboard/` — Início

- Card hero `primary-container` com **"Proteção Ativa"** + dot pulsante + toggle master
  (OFF → "Proteção desativada" com destaque de aviso).
- Estado extra obrigatório (prompt): papel perdido → aviso **"O Sentinela não é o filtro de
  chamadas padrão."** + botão **"Corrigir configuração"**.
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
Número). **Adições fora do mockup (exigidas pelo prompt):** menu com **Exportar/Importar
backup** (com confirmação) e toggle ativar/desativar por entrada.

## 8. `telas/hist_rico_de_bloqueios/` — Histórico

> PNG deste mockup veio corrompido no zip; layout extraído do `code.html`.

TopAppBar **"Histórico do Sentinela"** + ação "Limpar tudo" (`delete_sweep` com confirmação);
chips de período **Hoje / 7 dias / 30 dias** + **filtro por decisão** (exigido pelo prompt,
fora do mockup); lista com ícone por motivo (block/visibility_off), número mascarado,
timestamp relativo; ações por item (swipe/menu): **Permitir** (→ whitelist), **Marcar
indesejado**, **Excluir**; empty state "Fim do histórico recente"/“Nenhuma chamada bloqueada
ainda.”. Badge "ALTO RISCO" do mockup **não entra** (MVP não classifica risco).

## 9. Privacidade e sobre — **sem mockup**

Seguir `DESIGN.md`: lista de dados armazenados, permissões usadas (link para matriz),
retenção configurada, **Limpar todos os dados** (confirmação dupla), versão, limitações,
links para configurações do app e do canal de notificação.

## 10. Proteção (Ajustes) — **sem mockup**

Tela exigida pelo §9 do prompt; seguir `DESIGN.md` com grupos de configurações em cards
`surface-container` (padding 16dp), cada opção com switch M3 + explicação de uma linha em
`on-surface-variant` (nada de opção sem explicação):

1. **Ativar proteção** — toggle master (mesmo estado do hero do dashboard); OFF pinta o card
   com `error-container` a 15% e texto "Proteção desativada".
2. **Bloquear números desconhecidos** — switch (`blockUnknownNumbers`).
3. **Bloquear números privados** — switch (`blockPrivateNumbers`).
4. **Modo de bloqueio** — seleção única (option-cards pequenos): "Rejeitar imediatamente" ×
   "Encaminhar silenciosamente" (`BlockMode`), com explicação do efeito para quem liga.
5. **Ocultar do histórico nativo** — switch (`hideFromNativeCallLog`) + nota best-effort
   (ver LIMITACOES.md).
6. **Exibir notificação silenciosa** — switch (`showOwnNotification`); ao ligar pela primeira
   vez dispara o pedido de `POST_NOTIFICATIONS`; sub-opção número mascarado × anônimo.
7. **Retenção do histórico** — seletor (nunca/7/30/90 dias/manual).
8. **Em caso de erro** — seleção única da política de fallback: "Permitir chamada
   (recomendado)" × "Bloquear chamada" (`FallbackPolicy`), com explicação honesta do trade-off.

## Componentes recorrentes (design system aplicado)

- Header fixo h-64dp com escudo + título; backdrop blur ao rolar.
- Option-cards single-select com `check_circle` preenchido no selecionado.
- CTA pill full-width h-56dp fixo no rodapé com gradiente para transparente.
- Cards glass: `surface-container` a 60% + blur 12; radius 12–16dp; pills full.
- Bottom nav com aba ativa em pill `secondary-container`.
- Números sempre em Geist (monoespaçada de dados) e sempre mascarados fora do fluxo de cadastro.
