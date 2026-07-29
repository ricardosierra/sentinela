# Features — telas do MVP e divergências dos mockups

Mapeamento completo tela a tela (com textos literais e componentes) em
[`docs/design/TELAS.md`](../../docs/design/TELAS.md). Design system em
[`docs/design/DESIGN.md`](../../docs/design/DESIGN.md). Este arquivo resume o que
importa para o roadmap.

## Telas → Fases

| Tela (mockup Stitch) | Fase | Observações |
|----------------------|------|-------------|
| boas_vindas_ao_sentinela | 7 | Hero + 3 features honestas + CTA "Começar Configuração" |
| onboarding (papel Call Screening) | 7 | Disclaimer honesto + estado "Solicitando permissão…" |
| configura_o_desconhecidos | 7 | Bloquear (default) / Silenciar / Permitir |
| configura_o_contatos | 7 | Políticas reais por contato (Tocar padrão) + pedido de READ_CONTACTS |
| configura_o_whitelist | 7 | Tratamento configurável (Nunca Silenciar padrão) — como no mockup |
| dashboard | 7 | Status + stats + última bloqueada + quick actions |
| whitelist_pessoal | 8 | Busca, CRUD, FAB adicionar; + import/export (não está no mockup) |
| hist_rico_de_bloqueios | 8 | Chips de período, swipe Permitir/Excluir; + filtro por decisão |
| proteção (ajustes) | 7 | **Sem mockup** — inclui modo discador; ver TELAS.md §10 |
| privacidade e sobre + apoio | 9 | **Sem mockup** — open source, doação Bitcoin; ver TELAS.md §9/§11 |
| UI de chamada (modo discador) | 6 | **Sem mockup** — InCallService mínimo; design system aplicado |

## Divergências dos mockups (estado após adendos de 2026-07-28)

1. **Contagem de passos do onboarding**: mockups divergem ("Passo 2 de 4" × "Passo 3 de 3")
   → fluxo canônico: boas-vindas → papel → desconhecidos → contatos (políticas + permissão)
   → whitelist (tratamento) → verificação final.
2. **Bottom nav em inglês** (Home/Whitelist/History/Settings) → Início / Permitidos /
   Histórico / Ajustes.
3. **"Base Global — milhões de números identificados"** (boas-vindas) → promessa falsa para o
   MVP local; substituir por "Bloqueio Local" + destaque open source/offline.
4. **PNG do histórico corrompido** no zip → layout descrito a partir do code.html.
5. **Labels "Provável Fraude"/"Spam Provável"/"ALTO RISCO"** → MVP não classifica spam;
   exibir motivo real (Número Desconhecido / Privado / etc.).
6. **Nome antigo em parte dos mockups** → eliminado dos arquivos; branding único Sentinela.
7. **Política por contato do mockup agora É implementada** (adendo do produto) — plenamente
   efetiva no modo discador; no modo filtro contatos não passam pelo app (tocam nativo).
