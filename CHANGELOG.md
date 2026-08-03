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
- [x] **Modo discador opcional** — Sentinela como app de telefone padrão (ROLE_DIALER + InCallService próprio), políticas valendo também para contatos, reversível pelo seletor do sistema (Phase 6)
- [x] **Telas de chamada próprias** — chamada recebida em tela cheia (inclusive sobre a tela bloqueada, por full-screen intent e **nunca** por overlay), chamada de saída, chamada ativa com cronômetro, mudo, viva-voz e teclado de tons; quatro variantes de identidade (contato, whitelist, desconhecido, privado) (Phase 6)
- [x] **Tela de discagem** — campo somente saída com formatação progressiva pt-BR, sugestão de contato, teclas de 72dp, apagar com toque longo; alvo do pedido de discagem do sistema (Phase 6)
- [x] **Ativação honesta do modo discador** — cards "O que muda" e "O que não muda" com estilo idêntico e o mesmo peso visual, pré-requisito de leitura da agenda explícito, aviso informativo (sem alarme) quando o papel é perdido (Phase 6)
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

**Phase 2 — Motor de decisão e normalização (2026-07-29):**

- [x] **Precedência completa e políticas por origem** — Cobertura caso a caso (saída, proteção off, privado, contato, whitelist, falha de consulta, desconhecido, inválido) e 48 casos de teste parametrizado provando as transições.
- [x] **Normalização libphonenumber** — Integração limpa `LibPhoneNumberNormalizer`, E.164 BR e internacional, máscara de exibição blindada que não vaza número completo.
- [x] **Cascata de região** — SIM/rede → preferência → BR em um `RegionProvider` puro, sem novas permissões.

**Phase 3 — Dados locais (2026-07-29):**

- [x] **Whitelist e histórico no Room** — Schema exportado, dedup E.164, índices medidos (consulta `< 5ms`).
- [x] **Retenção de histórico puramente local** — Poda sem `WorkManager` no caminho de banco (políticas 7/30/90 dias e manual).
- [x] **Configurações em DataStore** — Leitura síncrona cacheada via `snapshot()`, contador de aberturas (para avaliação).
- [x] **Privacidade de dados** — Regras provadas excluindo o Room/DataStore dos backups automáticos do Android.

**Phase 4 — Contatos do aparelho (2026-07-29):**

- [x] **Sonda dupla de agenda** — Lookup local cacheado contra provider (E.164 + número bruto) operando sob p95 de 200ms na cold start, sem enviar dados para a rede.
- [x] **Gestão estrita de READ_CONTACTS** — Máquina de estado, permissão com fail-open (regride para desconhecido, não quebra a triagem).
- [x] **Invariante antivazamento de contatos** — Provado por exportação de schema e Kover que nomes de contato nunca tocam o banco local do Sentinela.

**Phase 5 — Triagem Telecom modo filtro (2026-07-29):**

- [x] **Coordenador puro de Screening** — `AtomicBoolean` garantindo exatamente uma resposta ao `CallScreeningService`, timeout interno resiliente, e redes permissivas para lidar com crash de IO.
- [x] **Notificação silenciosa própria** — Opt-in com máscara de número gerada antes do broadcast.
- [x] **Decisão dentro do motor** — Bloco 7 no script de invariantes provando que lógicas de bloqueio nunca "vazam" para a camada da UI ou do Telecom.

**Phase 6 — Modo discador opcional (2026-07-30):**

- [x] **Quatro permissões novas**, cada uma na matriz de `docs/PERMISSOES.md` **antes** do manifest e na allowlist do script de invariantes no mesmo trabalho: papel de telefone padrão, vínculo do serviço de interface de chamada, originar chamada e ocupar a tela. Zero `INTERNET`, zero permissão de fase futura antecipada
- [x] **Bloco 8 do `scripts/verify-invariants.sh`** — trava a elegibilidade ao papel de telefone padrão: os dois filtros da ação de discagem no manifest **mergeado**, a proibição permanente de desabilitar componente próprio (a plataforma remove o papel e encerra o app), a origem da chamada só pelo gerenciador de telecomunicações e a pureza da camada da sessão. As quatro checagens demonstradas falhando
- [x] **Políticas por contato provadas, não implementadas** — o requisito de as políticas valerem para contatos no modo discador foi provado com o coordenador de triagem real e a agenda real do aparelho virtual **sem alterar uma linha do `CallDecisionEngine`**: a última mudança do motor continua sendo da Fase 5. O que o modo discador muda é *quem chega* à decisão, não a decisão
- [x] **Perder um papel do sistema encerra o processo do app** (medido, com o motivo registrado pelo próprio sistema) — é por isso que o estado do modo discador é derivado de consultas ao sistema e nunca de valor gravado. Provado de **fora** do processo por `scripts/verify-dialer-lifecycle.sh`, porque a instrumentação roda dentro dele; a chamada em curso sobrevive
- [x] **Cores funcionais da chamada fora do Dynamic Color** — atender, recusar e encerrar saem por literal e chegam por parâmetro: um papel de parede não pode aproximar recusar de atender e produzir o único erro irreversível do app
- [x] Suíte JVM de **417 para 603 testes** e instrumentada de **53 para 80**, 0 falhas; cobertura **96,648%** com gate `koverVerify` em 80 (dois excludes novos, ambos por **nome de classe**: a costura da telefonia e o serviço de interface de chamada, que só rodam instrumentados — nenhuma classe pura excluída)
- [x] Documentação honesta: `docs/LIMITACOES.md` ganhou "uma chamada por vez", o encerramento do app ao perder papel e a **correção** do item de número privado, que voltou a dizer **não verificado** no modo discador; `docs/design/TELAS.md` §11 deixou de ser esboço de 6 linhas e virou contrato; roteiro Samsung revisado nos cenários 23–30 e completado de 52 a 60
- [x] Evidência pós-`clean` e sem cache arquivada em `.planning/phases/06-modo-discador-opcional/06-EVIDENCE.md` (78 de 78 tarefas executadas, lint 0, detekt 0, 8 blocos de invariantes verdes, gate demonstrado falhando)

**Phase 7 — UI Onboarding e Home (2026-07-30):**

- [x] **Onboarding honesto** — 6 passos (papel, desconhecidos, agenda, whitelist, notificações, revisão) sem dark patterns; recusa do papel não trava o app.
- [x] **Home e Proteção reativas** — `HomeScreen` com 8 estados degradados provados, status reativo do papel; tela `ProtectionScreen` operando imediatamente (sem botão de salvar).
- [x] **Navegação estrita e limpa** — Grafo de 10 destinos baseado em strings para evitar falsos-positivos na validação.
- [x] **Componentes compartilhados blindados** — 6 componentes de UI genéricos (barras, switch row) com semântica estrita para leitores de tela provada via Compose Rules.
