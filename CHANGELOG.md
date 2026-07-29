# Release Notes

---

## [Futuro]

### ✨ Novidades

- [ ] **Etapa 2 — Sincronização & Backend** — conta opcional, sync de listas, envio opcional da lista de números recebidos, backup criptografado opt-in ([backlog](docs/backlog/supabase-v2.md))
- [ ] Bloqueio por prefixo/padrão (ex.: 0303) — candidato pós-MVP

## [Unreleased](https://github.com/ricardosierra/sentinela/compare/master...develop)

### ✨ Novidades

- [ ] **Bloqueio de desconhecidos antes de tocar** — CallScreeningService + ROLE_CALL_SCREENING, sem tela de chamada, som ou notificação nativa (Phase 5)
- [ ] **Políticas por origem** — contatos (Tocar padrão), whitelist (Nunca Silenciar padrão) e desconhecidos (Bloquear padrão), com opções Tocar/Bloquear/Silenciar/Nunca Silenciar (Phases 2, 4, 7)
- [ ] **Leitura local de contatos** — READ_CONTACTS com explicação, lookup em memória, nada armazenado nem enviado (Phase 4)
- [ ] **Modo discador opcional** — Sentinela como app de telefone padrão (ROLE_DIALER + InCallService próprio), políticas valendo também para contatos, reversível (Phase 6)
- [ ] **Whitelist pessoal local** — CRUD com busca, E.164, tratamento configurável, import/export com validação (Phases 3 e 8)
- [ ] **Histórico interno opcional** — retenção configurável (nunca/7/30/90 dias/manual), ações permitir/indesejado/excluir (Phases 3 e 8)
- [ ] **Notificação silenciosa opt-in** — canal IMPORTANCE_LOW, número mascarado, desabilitada por padrão (Phase 5)
- [ ] **Onboarding completo** — papel de filtro, política de desconhecidos, política de contatos, tratamento da whitelist (Phase 7)
- [ ] **Apoio e avaliação** — convite na 5ª abertura (repete a cada 5 até aceite), seção "Apoie o Sentinela": open source, sem telemetria, 100% offline, comentário de apoio e doação em Bitcoin (Phase 9)
- [ ] **Tela Privacidade e sobre** — dados locais, permissões, retenção, limpar tudo (Phase 9)

### 🔧 Técnico

**Bootstrap (2026-07-27) + adendos do produto (2026-07-28):**

- [x] Esqueleto Android compilável e testado — Gradle 9.6.1 + AGP 9.3.0 (Kotlin embutido), Kotlin DSL + Version Catalog, Compose BOM 2026.06.01, compileSdk 37 / minSdk 29, JDK 17
- [x] `CallDecisionEngine` puro com precedência completa (saída → proteção → privado → contato → whitelist → falha → desconhecido) e políticas por origem — 20 testes unitários verdes
- [x] `UnknownCallScreeningService` registrado no manifest em modo pass-through seguro (não interfere até a Phase 5)
- [x] Tema Compose com tokens "Silent Guardian" (dark-first + Dynamic Color) e strings pt-BR semeadas das telas (políticas, apoio, doação)
- [x] Manifest sem INTERNET; Room/DataStore excluídos de backup (`dataExtractionRules`); R8 remove `Log.v/d` no release
- [x] Qualidade: detekt 1.23.8 + Android Lint verdes; `build.sh` no padrão do ecossistema
- [x] Documentação completa em `docs/` (arquitetura com 2 modos, matriz de permissões por fase, privacidade, limitações, decisões, release, roteiro Samsung com 30 cenários, design system + 8 telas mapeadas + telas sem mockup especificadas)
- [x] GSD inicializado em `.planning/` — 81 requisitos, roadmap de 9 fases, pesquisa de stack/plataforma, perfil quality
- [x] Travas de agente: `CLAUDE.md`/`AGENTS.md` espelhados, `.claude/settings.json` com hooks do graphify
- [x] Nome provisório dos mockups eliminado de todos os arquivos — branding único Sentinela

**Phase 1 — Fundação compilável (2026-07-29):**

- [x] **Política de lint declarada** (QLT-02) — bloco `lint { }` único em `app/build.gradle.kts` com `abortOnError = true` e três supressões justificadas por comentário (`UnusedResources`, `Typos`, `AndroidGradlePluginVersion`); sem `lint-baseline.xml`, de 137 warnings a **0 issues**
- [x] **`ObsoleteSdkInt` corrigido de verdade**, não suprimido — qualificador de API obsoleto removido do ícone adaptativo (`res/mipmap-anydpi-v26/` → `res/mipmap/`), desnecessário em minSdk 29
- [x] **`scripts/verify-invariants.sh`** — gate reexecutável de manifest (allowlist de permissões sobre o manifest **mergeado**, zero `INTERNET`, `BIND_SCREENING_SERVICE` presente), rebranding (`sentinelaApplicationId`, nenhuma string hardcoded, nenhuma `Color(0x` fora de `ui/theme`) e domínio puro (nenhum `import android` em `domain/`)
- [x] **`ThemeTokensTest`** — trava em JVM pura (sem Robolectric) dos tokens "Silent Guardian" e do wiring do `darkColorScheme`
- [x] Matriz `OriginPolicy` × origem fechada no `CallDecisionEngineTest` — suíte de 20 para **28 testes**, 0 falhas
- [x] Evidência auditável do build pós-`clean` arquivada em `.planning/phases/01-fundacao-compilavel/01-EVIDENCE.md` (57 actionable tasks, 34 executadas, lint 0, detekt 0, APK debug 33,8 MB)
- [x] Conflito documental de `POST_NOTIFICATIONS` reconciliado a favor de `docs/PERMISSOES.md` (fonte canônica): declaração mantida na Fase 1, pedido em runtime só na Fase 5
