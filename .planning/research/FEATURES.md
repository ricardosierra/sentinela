# Features — telas do MVP e divergências dos mockups

Mapeamento completo tela a tela (com textos literais e componentes) em
[`docs/design/TELAS.md`](../../docs/design/TELAS.md). Design system em
[`docs/design/DESIGN.md`](../../docs/design/DESIGN.md). Este arquivo resume o que
importa para o roadmap.

## Telas → Fases

| Tela (mockup Stitch) | Fase | Observações |
|----------------------|------|-------------|
| boas_vindas_ao_sentinela | 5 | Hero + 3 features + CTA "Começar Configuração" |
| onboarding (papel Call Screening) | 5 | Disclaimer honesto + estado "Solicitando permissão…" |
| configura_o_desconhecidos | 5 | Bloquear (default) / Silenciar / Permitir |
| configura_o_contatos | 5 | **Vira passo informativo** — plataforma não entrega contatos ao filtro |
| configura_o_whitelist | 5 | Simplificar: whitelist sempre permite (opções do mockup são sobre-engenharia) |
| dashboard | 5 | Status + stats + última bloqueada + quick actions |
| whitelist_pessoal | 6 | Busca, CRUD, FAB adicionar; + import/export (não está no mockup) |
| hist_rico_de_bloqueios | 6 | Chips de período, swipe Permitir/Excluir; + filtro por decisão |
| privacidade e sobre | 7 | **Sem mockup** — seguir design system |

## Divergências dos mockups (decididas no bootstrap)

1. **Branding**: mockups oscilam "Sentinela" × "Ultrathink" → tudo Sentinela.
2. **Contagem de passos do onboarding**: "Passo 2 de 4" × "Passo 3 de 3" → fluxo canônico de
   4 passos: boas-vindas → papel → desconhecidos → whitelist (contatos vira card informativo
   dentro do passo de desconhecidos ou passo próprio sem opções).
3. **Bottom nav em inglês** (Home/Whitelist/History/Settings) → Início / Permitidos /
   Histórico / Ajustes.
4. **Tela de contatos com opções (Tocar/Bloquear/Silenciar/Nunca Silenciar)** → não
   implementável sem discador padrão; passo informativo.
5. **"Base Global — milhões de números identificados"** (boas-vindas) → promessa falsa para o
   MVP local; substituir por "Bloqueio Local".
6. **PNG do histórico corrompido** no zip → layout descrito a partir do code.html.
7. **Labels "Provável Fraude"/"Spam Provável"/"ALTO RISCO"** → MVP não classifica spam;
   exibir motivo real (Número Desconhecido / Privado / etc.).
