# Release Notes

---

## [Futuro]

### ✨ Novidades

- [ ] **Etapa 2 — Supabase & Sincronização** — conta opcional, whitelist sincronizada/compartilhada e backup criptografado opt-in ([backlog](docs/backlog/supabase-v2.md))
- [ ] Bloqueio por prefixo/padrão (ex.: 0303) — candidato pós-MVP

## [Unreleased](https://github.com/ricardosierra/sentinela/compare/master...develop)

### ✨ Novidades

- [ ] **Bloqueio de desconhecidos antes de tocar** — CallScreeningService + ROLE_CALL_SCREENING, sem tela de chamada, som ou notificação nativa (Phases 4–5)
- [ ] **Whitelist pessoal local** — CRUD com busca, E.164, import/export com validação (Phases 3 e 6)
- [ ] **Histórico interno opcional** — retenção configurável (nunca/7/30/90 dias/manual), ações permitir/indesejado/excluir (Phases 3 e 6)
- [ ] **Notificação silenciosa opt-in** — canal IMPORTANCE_LOW, número mascarado, desabilitada por padrão (Phase 4)
- [ ] **Onboarding em 4 passos** — papel de filtro, desconhecidos, contatos (informativo) e whitelist (Phase 5)
- [ ] **Tela Privacidade e sobre** — dados locais, permissões, retenção, limpar tudo (Phase 7)

### 🔧 Técnico

**Bootstrap (2026-07-27):**

- [x] Esqueleto Android compilável e testado — Gradle 9.6.1 + AGP 9.3.0 (Kotlin embutido), Kotlin DSL + Version Catalog, Compose BOM 2026.06.01, compileSdk 37 / minSdk 29, JDK 17
- [x] `CallDecisionEngine` puro com a precedência do prompt implementada — 10 testes unitários verdes
- [x] `UnknownCallScreeningService` registrado no manifest em modo pass-through seguro (não interfere até a Phase 4)
- [x] Tema Compose com tokens "Silent Guardian" (dark-first + Dynamic Color) e strings pt-BR semeadas das telas
- [x] Manifest sem INTERNET; Room/DataStore excluídos de backup (`dataExtractionRules`); R8 remove `Log.v/d` no release
- [x] Qualidade: detekt 1.23.8 + Android Lint verdes; `build.sh` no padrão do ecossistema
- [x] Documentação completa em `docs/` (arquitetura, permissões, privacidade, limitações, decisões, release, roteiro Samsung com 20 cenários, design system + 8 telas mapeadas)
- [x] GSD inicializado em `.planning/` — 65 requisitos, roadmap de 7 fases, pesquisa de stack/plataforma
- [x] Travas de agente: `CLAUDE.md`/`AGENTS.md` espelhados, `.claude/settings.json` com hooks do graphify
