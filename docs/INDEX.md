# Índice da Documentação

> Regra do projeto: todo `.md` novo entra aqui. Se não couber no índice, provavelmente não
> deveria existir como arquivo separado.

## Documentação Pública & Comunidade

- [Guia de Contribuição (pt-BR)](../CONTRIBUTING.md) | [EN](../CONTRIBUTING.en.md) | [ES](../CONTRIBUTING.es.md)
- [Licença MIT](../LICENSE)

## Escopo e produto

- [`PROMPT-MVP.md`](PROMPT-MVP.md) — prompt original do MVP; **fonte de verdade do escopo**
- [`LIMITACOES.md`](LIMITACOES.md) — fronteiras honestas (plataforma, OEM, escopo)
- [`PRIVACIDADE.md`](PRIVACIDADE.md) — política de privacidade embutida no app

## Técnica

- [`ARQUITETURA.md`](ARQUITETURA.md) — camadas, fluxo de decisão, resiliência, orçamento de performance
- [`PERMISSOES.md`](PERMISSOES.md) — matriz de permissões (leitura bloqueante p/ manifest)
- [`DECISOES.md`](DECISOES.md) — decisões arquiteturais (ADR-lite)
- [`RELEASE.md`](RELEASE.md) — processo de release, assinatura, R8, checklist

## Loja (Google Play)

- [`PLAY-STORE-AUTOMACAO.md`](PLAY-STORE-AUTOMACAO.md) — preparação única e CI/CD seguro da Play Developer API
- [`loja/PLAY-STORE.md`](loja/PLAY-STORE.md) — índice da ficha da loja; texto por idioma em blocos
- `loja/ficha/bloco-NN.md` — nome, descrição curta, descrição completa e novidades, 10 idiomas por bloco
- [`loja/graficos/README.md`](loja/graficos/README.md) — ícone, banner e capturas: como são gerados e o que a loja exige

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
- [`backlog/locales-config-incompleto.md`](backlog/locales-config-incompleto.md) — `pl`, `th`, `tr` e `vi` estão traduzidos mas fora do `locales_config.xml`
- [`backlog/pt-br-caindo-em-pt-pt.md`](backlog/pt-br-caindo-em-pt-pt.md) — **bloqueia lançamento**: aparelho em pt-BR recebe a interface em português de Portugal
- [`backlog/capacidades-prometidas-nos-mockups.md`](backlog/capacidades-prometidas-nos-mockups.md) — 5 capacidades que os mockups anunciam e o MVP não tem (base global, criptografia, filtros inteligentes, classificação de fraude, lista de spam); copy honesta no MVP, capacidades em versões posteriores
- [`backlog/ideia-captcha-voz.md`](backlog/ideia-captcha-voz.md) — ideia de usuário para CAPTCHA por voz (bloqueado por limitações de permissões e áudio do Android)

## Planejamento (GSD)

Estado vivo do projeto em `.planning/`: [`PROJECT.md`](../.planning/PROJECT.md),
[`REQUIREMENTS.md`](../.planning/REQUIREMENTS.md), [`ROADMAP.md`](../.planning/ROADMAP.md),
[`STATE.md`](../.planning/STATE.md), [`MILESTONES.md`](../.planning/MILESTONES.md),
pesquisa em [`research/`](../.planning/research/SUMMARY.md).
