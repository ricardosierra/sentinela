# Índice da Documentação

> Regra do projeto: todo `.md` novo entra aqui. Se não couber no índice, provavelmente não
> deveria existir como arquivo separado.

## Escopo e produto

- [`PROMPT-MVP.md`](PROMPT-MVP.md) — prompt original do MVP; **fonte de verdade do escopo**
- [`LIMITACOES.md`](LIMITACOES.md) — fronteiras honestas (plataforma, OEM, escopo)
- [`PRIVACIDADE.md`](PRIVACIDADE.md) — política de privacidade embutida no app

## Técnica

- [`ARQUITETURA.md`](ARQUITETURA.md) — camadas, fluxo de decisão, resiliência, orçamento de performance
- [`PERMISSOES.md`](PERMISSOES.md) — matriz de permissões (leitura bloqueante p/ manifest)
- [`DECISOES.md`](DECISOES.md) — decisões arquiteturais (ADR-lite)
- [`RELEASE.md`](RELEASE.md) — processo de release, assinatura, R8, checklist

## Design

- [`design/DESIGN.md`](design/DESIGN.md) — design system "Silent Guardian" (tokens, componentes)
- [`design/TELAS.md`](design/TELAS.md) — mapeamento tela a tela dos mockups + adaptações
- [`design/DESIGN-TOKENS-ORIGINAL.md`](design/DESIGN-TOKENS-ORIGINAL.md) — tokens originais do Stitch (referência histórica)
- [`design/ARQUITETURA-STITCH-ORIGINAL.md`](design/ARQUITETURA-STITCH-ORIGINAL.md) — guia original do Stitch (referência histórica)
- `design/telas/<tela>/` — mockups (screen.png + code.html), 8 telas

## Qualidade e validação

- [`TESTE-FISICO-SAMSUNG.md`](TESTE-FISICO-SAMSUNG.md) — roteiro reproduzível de validação física (30 cenários, incluindo modo discador)

## Backlog

- [`backlog/supabase-v2.md`](backlog/supabase-v2.md) — etapa 2 (Supabase/sync), fora do MVP
- [`backlog/manutencao-toolchain.md`](backlog/manutencao-toolchain.md) — itens de manutenção de toolchain adiados (gradlew.bat, AGP 9.3.1, revisão dos disable de lint)
- [`backlog/fontes-inter-geist.md`](backlog/fontes-inter-geist.md) — empacotar Inter/Geist em `res/font/`; até lá os estilos numéricos usam a família monoespaçada do sistema

## Planejamento (GSD)

Estado vivo do projeto em `.planning/`: [`PROJECT.md`](../.planning/PROJECT.md),
[`REQUIREMENTS.md`](../.planning/REQUIREMENTS.md), [`ROADMAP.md`](../.planning/ROADMAP.md),
[`STATE.md`](../.planning/STATE.md), [`MILESTONES.md`](../.planning/MILESTONES.md),
pesquisa em [`research/`](../.planning/research/SUMMARY.md).
