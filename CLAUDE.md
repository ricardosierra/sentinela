# Sentinela — contexto técnico para agentes

> Guia compacto do Sentinela, bloqueador local de chamadas desconhecidas para Android.
> Aqui ficam só os invariantes que todo agente precisa carregar. Produto, arquitetura,
> permissões e roadmap vivem em [`docs/`](docs/INDEX.md) e `.planning/` — leia sob demanda.

Este arquivo e o `AGENTS.md` têm o mesmo conteúdo. Ao mudar um, replique no outro no mesmo commit.

## Produto

App Android nativo e **open source** que impede chamadas de números desconhecidos de
interromperem o usuário — sem propaganda, sem telemetria, sem nuvem, 100% offline.
**Core value:** "Se não está nos contatos nem na whitelist pessoal, não interrompe o usuário."
Dois modos: **filtro** (padrão, call screening puro) e **discador** (opcional, `ROLE_DIALER` +
`InCallService`, habilita políticas por contato). Sync com backend é etapa v0.2.0 do produto —
sempre opt-in, sempre assíncrona, nunca no caminho da decisão.

## Estado atual

Milestone v0.1 fechado (9 de 9 fases). Versão corrente em `app/build.gradle.kts` e no
[`CHANGELOG.md`](CHANGELOG.md). Pendência aberta: rodar
[`docs/TESTE-FISICO-SAMSUNG.md`](docs/TESTE-FISICO-SAMSUNG.md) em aparelho físico.

O que já existe no código:

- **Triagem:** `UnknownCallScreeningService` → `ScreeningCoordinator` (resposta única) → `CallDecisionEngine`.
- **Domínio:** precedência saída → proteção → privado → contato → whitelist → falha → desconhecido, com `OriginPolicy` por origem.
- **Dados:** Room v1 (`whitelist`, `blocked_call`, schema exportado) + DataStore Preferences; retenção do histórico em 5 políticas.
- **Contatos:** `ContactLookupRepository` com sonda dupla e cache — nada de identidade toca disco.
- **Modo discador:** `SentinelaInCallService`, `CallSessionCoordinator`, telas de chamada e discagem, papel reversível pelo seletor do sistema.
- **UI:** onboarding, Home, Proteção, Whitelist (CRUD, busca, import/export por SAF), Histórico (filtro por período **e** por decisão, tempo relativo, motivo em pt-BR), Privacidade e Sobre, convite de avaliação.
- **Qualidade:** gate `koverVerify` em 80%, lint e detekt zerados, `scripts/verify-invariants.sh` com 10 blocos verdes.

Correções recentes que viraram invariante — **não regredir**:

- A resposta ao sistema sai de um ponto único atrás de guarda; falha depois da decisão não gera segunda resposta.
- Exportar whitelist grava os números reais; importar mescla sem duplicar, com confirmação antes de alterar a lista.
- Cadastro rejeitado (número inválido ou duplicado) sempre avisa na tela — nunca fecha em silêncio.
- Histórico mostra o tempo real de cada evento e o motivo traduzido; reason code interno nunca aparece na UI.
- Nenhum texto de interface embutido em Kotlin nas telas de whitelist, histórico e Sobre.

## Stack (travado)

- Kotlin + Jetpack Compose + Material 3. Gradle Kotlin DSL + Version Catalog (`gradle/libs.versions.toml`).
- **AGP 9+ tem Kotlin embutido** — nunca aplicar `org.jetbrains.kotlin.android` (quebra o build).
- minSdk 29 (Android 10, exigência do `ROLE_CALL_SCREENING`), targetSdk/compileSdk estáveis atuais.
- Coroutines + Flow. Room (whitelist/histórico) + DataStore Preferences (configurações + contador de aberturas).
- `CallScreeningService` + `RoleManager.ROLE_CALL_SCREENING`; modo discador opcional com `ROLE_DIALER` + `InCallService`. Normalização com libphonenumber (port Android).
- DI manual no `AppContainer` — nada de Hilt/Koin/Dagger. Nenhum framework que aumente cold start do Service.
- JDK 17 (Homebrew) para o Gradle — o JDK 25 do sistema não é suportado; ver `gradle.properties`.

## Invariantes não negociáveis

**Privacidade.** Nenhuma permissão de `INTERNET` no manifest do MVP. Nenhuma chamada de rede,
telemetria, chave ou segredo. Número de telefone completo **nunca** aparece em log — use sempre
máscara (`+55 11 9****-1234`). Contatos são lidos **apenas em memória** no momento da
chamada/exibição — nome, foto e número da agenda nunca são persistidos nem enviados. Logs
sensíveis saem do release. O banco local fica fora do backup automático (`dataExtractionRules`).
Critérios em [`docs/PRIVACIDADE.md`](docs/PRIVACIDADE.md).

**Permissões.** A lista permitida é fechada e cada permissão tem fase para entrar no manifest:
`BIND_SCREENING_SERVICE`, papel `ROLE_CALL_SCREENING`, `POST_NOTIFICATIONS` (opt-in),
`READ_CONTACTS` (lookup local) e — só no modo discador opcional — `ROLE_DIALER`,
`BIND_INCALL_SERVICE` e `CALL_PHONE`. Proibidos: `INTERNET` (MVP), `READ_CALL_LOG`, `READ_SMS`,
`READ_PHONE_STATE` (sem prova de necessidade), `SYSTEM_ALERT_WINDOW`, AccessibilityService,
overlays e gravação. **Nunca antecipar permissão no manifest antes da fase dela.** Matriz
completa em [`docs/PERMISSOES.md`](docs/PERMISSOES.md).

**Telecom.** O `UnknownCallScreeningService` é camada fina: chama `respondToCall` **exatamente
uma vez**, responde muito antes do limite de 5 segundos da plataforma, consulta somente dados
locais e cria notificação ou histórico só **depois** de responder ao sistema. Fatos medidos na
fonte do Android: `onScreenCall()` recebe **também contatos** enquanto a leitura da agenda
estiver concedida — o Android só dispensa a triagem de quem está na agenda quando o app não pode
consultá-la; ocultar a chamada bloqueada do histórico do telefone **não funciona** para apps de
terceiros; e o modo "Não Perturbe" **não** é contornável. Perder um papel do sistema encerra o
processo do app — o estado do modo discador é sempre derivado de consulta ao sistema, nunca de
valor gravado. Nada de hack de OEM antes de provar necessidade em aparelho físico. Detalhes em
[`docs/LIMITACOES.md`](docs/LIMITACOES.md).

**Arquitetura.** Toda regra de decisão vive no `CallDecisionEngine` (puro, determinístico,
testado) — inclusive as políticas por origem (`OriginPolicy`: contatos/whitelist/desconhecidos).
Nada de condição de bloqueio espalhada pelo Service, repositório ou UI. Compose não conhece
Telecom; `domain/` não importa `android`. Resultado de decisão é objeto de domínio
(`CallDecision`) com reason code interno sem dado pessoal. Performance é requisito: p95 < 200 ms
no cold path da decisão, cold start mínimo, cobertura ≥ 80% em domínio/dados.

**UI.** Strings sempre em resources, começando por pt-BR — nenhum texto hardcoded em Kotlin, nem
reason code interno exibido ao usuário. Dark mode e Dynamic Color; as cores funcionais da chamada
(atender/recusar/encerrar) ficam fora do esquema dinâmico. Sem dark pattern, sem propaganda.
Nunca afirmar que o app filtra WhatsApp/VoIP nem que o bloqueio é "100% garantido". O convite de
avaliação (5ª abertura, depois a cada 5 até aceite) nunca interrompe onboarding ou chamada.
Nenhum dado de terceiro não verificado (endereço, chave, link de pagamento) vai para a tela —
placeholder inventado em ação irreversível é bug crítico, não detalhe. Referência visual em
[`docs/design/TELAS.md`](docs/design/TELAS.md); branding único: **Sentinela**.

**Testes.** Todo comportamento novo do motor de decisão, normalização, repositórios, ViewModels e
Service recebe teste — inclusive regressão de bug corrigido. A lista obrigatória de casos está na
seção 13 de [`docs/PROMPT-MVP.md`](docs/PROMPT-MVP.md). Teste de migração do Room quando houver
migração.

**Escopo.** Nada além do explicitamente pedido. Melhoria fora de escopo vira item em
`docs/backlog/` e é reportada — sem modificar código. Sincronização/backend é etapa 2: preparar
interfaces, **não** implementar rede. O app deve sempre funcionar 100% offline.

## Onde está a verdade

Se duas fontes divergirem, o código manda. Corrija a documentação viva no mesmo trabalho.

| Vou mexer em / preciso saber | Leia antes |
|---|---|
| Estado real da implementação | código, manifest, gradle e testes — não a doc |
| Escopo do MVP | [`docs/PROMPT-MVP.md`](docs/PROMPT-MVP.md) |
| Service e motor de decisão | [`docs/ARQUITETURA.md`](docs/ARQUITETURA.md) + seções 4–5 e adendos do PROMPT-MVP |
| Permissão ou manifest | [`docs/PERMISSOES.md`](docs/PERMISSOES.md) (leitura bloqueante) |
| Contatos e políticas por origem | adendos do PROMPT-MVP + ARQUITETURA (precedência) |
| Modo discador e InCallService | Fase 6 do `.planning/ROADMAP.md` + [`docs/LIMITACOES.md`](docs/LIMITACOES.md) |
| Whitelist e normalização | seção 6 do PROMPT-MVP |
| Histórico e retenção | seção 7 do PROMPT-MVP |
| Criar ou alterar tela | [`docs/design/TELAS.md`](docs/design/TELAS.md) + [`docs/design/DESIGN.md`](docs/design/DESIGN.md) |
| Avaliação e apoio | adendo 5 do PROMPT-MVP + TELAS §9/§12 |
| Limitações da plataforma | [`docs/LIMITACOES.md`](docs/LIMITACOES.md) |
| Estado e próximas fases | `.planning/{STATE,ROADMAP,REQUIREMENTS}.md` |
| Fechar versão | [`docs/RELEASE.md`](docs/RELEASE.md) |
| Validação em aparelho | [`docs/TESTE-FISICO-SAMSUNG.md`](docs/TESTE-FISICO-SAMSUNG.md) |

Em conflito, vale a regra mais restritiva para produção: privacidade, permissão mínima e resposta
rápida ao Telecom ganham de conveniência. Comportamento que depende de OEM (Samsung) vai
documentado em `docs/LIMITACOES.md` e no roteiro físico — não invente hack preventivo.

## Regras de conduta

**Atribuição de IA.** **NUNCA** adicione rodapé, metadado, link, crédito ou assinatura de IA
("🤖 Generated with Claude Code", "Co-Authored-By: Claude" ou similar) em código, documentação ou
commit. **NUNCA** commite com autoria de bot — sempre a autoria real configurada no git.

**Git.** Formato **Conventional Commits** (`feat:`, `fix:`, `refactor:`, `docs:`, `test:`,
`chore:`, `release:`). Prefira commit de uma linha; corpo, quando existir, com linhas de até 100
caracteres. Não use `--no-verify` sem pedido explícito. Branch padrão: `master`. Tags semver
anotadas `vX.Y.Z`; primeira release é `v0.1.0`; `v1.0.0` fica reservado para maturidade real em
produção.

**Changelog.** `CHANGELOG.md` segue o formato **Release Notes** do usuário: título
`# Release Notes`, `---`, `## [Futuro]`, `## [Unreleased](...)`, cabeçalhos
`## [vX.Y.Z (YYYY-MM-DD)](compare-link)`, seções ✨ Novidades / 🎨 Melhorias / 🐛 Correções /
🔧 Técnico, itens **sempre** em checkbox `- [x]`/`- [ ]`. **NUNCA** Keep a Changelog ou
conventional-changelog.

**Documentação.** Todo `.md` novo entra em [`docs/INDEX.md`](docs/INDEX.md); se não couber no
índice, provavelmente não deveria existir como arquivo separado. Bug, ideia ou dívida técnica vai
para `docs/backlog/` — não crie `.TODO`, `.BUGS*` ou `.SPRINT*` na raiz.

## Ferramentas locais (opcionais)

Graphify e Caveman são ferramentas do perfil local do agente. Não são dependências do Sentinela e
não entram em código, CI, commits ou PRs; saídas como `graphify-out/` ficam ignoradas pelo Git.

Quando `graphify-out/graph.json` existir, use-o antes de varrer o código: `graphify query "<pergunta>"`
para um subgrafo escopado, `graphify path "<A>" "<B>"` para relações e `graphify explain "<conceito>"`
para um conceito. `graphify-out/wiki/index.md` serve para navegação ampla e `GRAPH_REPORT.md` só
para revisão de arquitetura. Depois de mudar código, rode `graphify update .`.

## Comandos úteis

```bash
./build.sh                      # build debug + copia APK para a raiz
./gradlew assembleDebug         # build debug
./gradlew testDebugUnitTest     # testes unitários
./gradlew lint detekt           # lint Android + detekt
./gradlew koverVerify           # gate de cobertura (80%)
./scripts/verify-invariants.sh  # 10 blocos de invariantes do projeto
./gradlew assembleRelease       # release (exige app/keystore.properties)
adb install sentinela-debug.apk # instalar no aparelho
```

## Checklist antes de entregar (TL;DR)

- [ ] Nenhuma permissão nova fora da lista fechada de `docs/PERMISSOES.md` (nem antes da fase dela).
- [ ] Nenhum número completo em log, notificação de tela bloqueada ou crash report.
- [ ] Nenhum nome ou dado de contato persistido em banco, log ou backup.
- [ ] Regra de decisão nova está no `CallDecisionEngine` com teste — não no Service/UI.
- [ ] `respondToCall` continua sendo chamado exatamente uma vez em todos os caminhos.
- [ ] Nenhuma chamada de rede no MVP; decisão nunca depende de rede.
- [ ] String nova em `res/values/strings.xml` (pt-BR); nenhum reason code interno na tela.
- [ ] Erro do usuário (duplicado, inválido, import/export) sempre com aviso visível.
- [ ] `./gradlew testDebugUnitTest lint detekt` e `./scripts/verify-invariants.sh` passam.
- [ ] CHANGELOG atualizado no formato Release Notes (checkbox, emoji, link de compare).
- [ ] Commit sem atribuição de IA.

---
*Última revisão estrutural: 2026-08-06*
