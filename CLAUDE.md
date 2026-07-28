# Sentinela — contexto técnico para agentes

> Guia compacto do Sentinela, bloqueador local de chamadas desconhecidas para Android.
> Ele guarda só invariantes que todo agente precisa carregar. Detalhes de produto,
> arquitetura, permissões e roadmap vivem em [`docs/`](docs/INDEX.md) e `.planning/`
> e devem ser lidos sob demanda, não todos de uma vez.

Este arquivo e o `AGENTS.md` têm o mesmo conteúdo. Ao mudar um, replique no outro no mesmo commit.

## Como ler e resolver conflitos

| Assunto | Fonte canônica |
|---------|----------------|
| Estado real da implementação | código, manifest, gradle e testes |
| Conduta obrigatória de agentes | este arquivo / [`AGENTS.md`](AGENTS.md) |
| Escopo do MVP (fonte de verdade) | [`docs/PROMPT-MVP.md`](docs/PROMPT-MVP.md) |
| Arquitetura e comportamento da plataforma | [`docs/ARQUITETURA.md`](docs/ARQUITETURA.md) |
| Permissões permitidas e proibidas | [`docs/PERMISSOES.md`](docs/PERMISSOES.md) |
| Estado de execução e próximas fases | `.planning/{STATE,ROADMAP,REQUIREMENTS}.md` |
| Telas e design system | [`docs/design/TELAS.md`](docs/design/TELAS.md) e [`docs/design/DESIGN.md`](docs/design/DESIGN.md) |
| Limitações conhecidas | [`docs/LIMITACOES.md`](docs/LIMITACOES.md) |

Se duas fontes do mesmo assunto divergirem, confira primeiro o código. Corrija a documentação viva no mesmo trabalho.

## Produto

App Android nativo que impede chamadas de números desconhecidos de interromperem o usuário.
**Core value:** "Se não está nos contatos nem na whitelist pessoal, não interrompe o usuário."
Tudo local: sem servidor, sem login, sem analytics, sem publicidade, sem telemetria no MVP.

## Stack (travado)

- Kotlin + Jetpack Compose + Material 3. Gradle Kotlin DSL + Version Catalog (`gradle/libs.versions.toml`).
- minSdk 29 (Android 10, exigência do `ROLE_CALL_SCREENING`), targetSdk/compileSdk estáveis atuais.
- Coroutines + Flow. Room (whitelist/histórico) + DataStore Preferences (configurações).
- `CallScreeningService` + `RoleManager.ROLE_CALL_SCREENING`. Normalização com libphonenumber (port Android).
- DI manual — nada de Hilt/Koin/Dagger no MVP. Nenhum framework que aumente cold start do Service.
- JDK 17 (Homebrew) para o Gradle — o JDK 25 do sistema não é suportado; ver `gradle.properties`.

## Diretrizes não negociáveis deste projeto

**Privacidade.** Nenhuma permissão de `INTERNET` no manifest. Nenhuma chamada de rede,
telemetria, chave ou segredo. Número de telefone completo **nunca** aparece em log — use
sempre máscara (`+55 11 9****-1234`). Logs sensíveis são removidos de release. O banco
local fica fora do backup automático (ver `dataExtractionRules`). Critérios em
[`docs/PRIVACIDADE.md`](docs/PRIVACIDADE.md).

**Permissões.** A lista permitida é fechada: `BIND_SCREENING_SERVICE` (via manifest do
Service), papel `ROLE_CALL_SCREENING`, e `POST_NOTIFICATIONS` (opcional, só se o usuário
habilitar a notificação própria). Proibidos no MVP: `READ_CONTACTS`, `READ_CALL_LOG`,
`READ_SMS`, `READ_PHONE_STATE` (sem prova de necessidade), `SYSTEM_ALERT_WINDOW`,
AccessibilityService, overlays e discador padrão. Matriz completa em
[`docs/PERMISSOES.md`](docs/PERMISSOES.md).

**Telecom.** O `UnknownCallScreeningService` é camada fina: chama `respondToCall` **exatamente
uma vez**, responde muito antes do limite de 5 segundos da plataforma, consulta somente dados
locais e cria notificação só **depois** de responder ao sistema. Fato central da plataforma:
sem ser o discador padrão, `onScreenCall()` só recebe chamadas de números **fora da agenda** —
é isso que faz contatos tocarem normalmente sem `READ_CONTACTS`. Nada de hack de OEM antes de
provar necessidade em aparelho físico.

**Arquitetura.** Toda regra de decisão vive no `CallDecisionEngine` (puro, determinístico,
testado). Nada de condição de bloqueio espalhada pelo Service, repositório ou UI. Compose não
conhece Telecom. Resultado de decisão é objeto de domínio (`CallDecision`) com reason code
interno sem dado pessoal. Orçamento de performance: p95 < 200 ms no cold path da decisão.

**UI.** Strings sempre em resources, começando por pt-BR — nenhum texto hardcoded em Kotlin.
Dark mode e Dynamic Color. Sem dark pattern, sem propaganda. Nunca afirmar que o app filtra
WhatsApp/VoIP nem que o bloqueio é "100% garantido". Referência visual em
[`docs/design/TELAS.md`](docs/design/TELAS.md); branding unificado: **Sentinela** (mockups
antigos citam "Ultrathink" — ignorar).

**Testes.** Todo comportamento novo do motor de decisão, normalização, repositórios e Service
recebe teste. A lista obrigatória de casos está na seção 13 de
[`docs/PROMPT-MVP.md`](docs/PROMPT-MVP.md). Testes de migração do Room quando houver migração.

**Escopo.** Nada além do explicitamente pedido. Melhorias fora de escopo viram item em
`docs/backlog/` e são reportadas — sem modificar código. Supabase/fonte remota de whitelist é
v2: preparar interfaces, **não** implementar rede.

## Regras de Conduta

### Atribuição de IA
- **NUNCA** adicione rodapés, metadados, links, nem créditos automáticos como "🤖 Generated with Claude Code", "Co-Authored-By: Claude", ou qualquer referência similar em commits ou documentação.
- **NUNCA** adicione assinaturas, atribuições ou autoria de IA em nenhum arquivo ou mensagem de commit.
- **NUNCA** faça commits com autoria de bots (nome ou email). Sempre use a autoria do usuário real configurada no git.

### Git
- Use apenas as credenciais git do usuário real (nome e email configurados no git).
- Siga o formato **Conventional Commits** (`feat:`, `fix:`, `refactor:`, `docs:`, `test:`, `chore:`, `release:`).
- Prefira commit de uma linha; corpo, quando existir, com linhas de no máximo 100 caracteres.
- Não use `--no-verify` em `git commit` ou `git push`, salvo pedido explícito do usuário.
- Branch padrão: `master`. Tags semver anotadas `vX.Y.Z`; primeira release é `v0.1.0`; `v1.0.0` fica reservado para maturidade real em produção.

### Changelog
- `CHANGELOG.md` segue o formato **Release Notes** do usuário (referência canônica:
  `~/Dev/Banlek/banlek-uploader/CHANGELOG.md`): título `# Release Notes`, `---`, `## [Futuro]`,
  `## [Unreleased](...)`, cabeçalhos `## [vX.Y.Z (YYYY-MM-DD)](compare-link)`, seções
  ✨ Novidades / 🎨 Melhorias / 🐛 Correções / 🔧 Técnico, itens **sempre** em checkbox `- [x]`/`- [ ]`.
- **NUNCA** usar Keep a Changelog ou conventional-changelog neste projeto.

### Documentação
- Todo `.md` novo precisa entrar em [`docs/INDEX.md`](docs/INDEX.md); se não couber no índice, provavelmente não deveria existir como arquivo separado.
- Bug, ideia ou dívida técnica vai para `docs/backlog/`. Não crie `.TODO`, `.BUGS*` ou `.SPRINT*` na raiz.

## Quando há conflito

- Vale a regra mais restritiva para produção: privacidade, permissão mínima e resposta rápida
  ao Telecom ganham de conveniência.
- Se um comportamento depende de OEM (Samsung), documente em
  [`docs/LIMITACOES.md`](docs/LIMITACOES.md) e no roteiro físico — não invente hack preventivo.

## Navegação por tarefa

| Tarefa | Leia antes |
|--------|-----------|
| Mexer no Service/decisão | `docs/ARQUITETURA.md` + seções 4–5 do `docs/PROMPT-MVP.md` |
| Mexer em permissão/manifest | `docs/PERMISSOES.md` (leitura bloqueante) |
| Criar/alterar tela | `docs/design/TELAS.md` + `docs/design/DESIGN.md` |
| Whitelist/normalização | seção 6 do `docs/PROMPT-MVP.md` |
| Histórico/retenção | seção 7 do `docs/PROMPT-MVP.md` |
| Fechar versão | `docs/RELEASE.md` |
| Validação em aparelho | `docs/TESTE-FISICO-SAMSUNG.md` |

## Ferramentas locais e economia de contexto

Graphify e Caveman são ferramentas opcionais do perfil local do agente. Não são dependências
do Sentinela e não entram em código, CI, commits ou PRs. Saídas como `graphify-out/` ficam
ignoradas pelo Git.

## graphify

This project has a knowledge graph at graphify-out/ with god nodes, community structure, and cross-file relationships.

Rules:
- For codebase questions, first run `graphify query "<question>"` when graphify-out/graph.json exists. Use `graphify path "<A>" "<B>"` for relationships and `graphify explain "<concept>"` for focused concepts. These return a scoped subgraph, usually much smaller than GRAPH_REPORT.md or raw grep output.
- If graphify-out/wiki/index.md exists, use it for broad navigation instead of raw source browsing.
- Read graphify-out/GRAPH_REPORT.md only for broad architecture review or when query/path/explain do not surface enough context.
- After modifying code, run `graphify update .` to keep the graph current (AST-only, no API cost).

## Comandos úteis

```bash
./build.sh                      # build debug + copia APK para a raiz
./gradlew assembleDebug         # build debug
./gradlew testDebugUnitTest     # testes unitários
./gradlew lint detekt           # lint Android + detekt
./gradlew assembleRelease       # release (exige app/keystore.properties)
adb install sentinela-debug.apk # instalar no aparelho
```

## Checklist antes de entregar (TL;DR)

- [ ] Nenhuma permissão nova fora da lista fechada de `docs/PERMISSOES.md`.
- [ ] Nenhum número completo em log, notificação de tela bloqueada ou crash report.
- [ ] Regra de decisão nova está no `CallDecisionEngine` com teste — não no Service/UI.
- [ ] `respondToCall` continua sendo chamado exatamente uma vez em todos os caminhos.
- [ ] String nova está em `res/values/strings.xml` (pt-BR), não hardcoded.
- [ ] `./gradlew testDebugUnitTest lint detekt` passam.
- [ ] CHANGELOG atualizado no formato Release Notes (checkbox, emoji, link de compare).
- [ ] Commit sem atribuição de IA.

---
*Última revisão estrutural: 2026-07-27*
