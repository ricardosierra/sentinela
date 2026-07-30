# Roadmap: Sentinela

## Overview

Do esqueleto compilável ao MVP instalável em 9 fases: fundação de build e tema; domínio puro
(motor com políticas por origem + normalização) com testes exaustivos; camada de dados local;
contatos do aparelho com políticas; integração com o Telecom no modo filtro; modo discador
opcional (o app pode substituir o telefone padrão); telas em duas levas; e o fechamento com
apoio/avaliação, privacidade, release e validação física Samsung.

## Milestones

- 🚧 **v0.1.0 MVP** — Phases 1-9 (em curso)
- 📋 **v0.2.0 Sincronização** — pós-MVP (planejado, sem fases; ver `docs/backlog/supabase-v2.md`)

## Phases

**Phase Numbering:**
- Integer phases (1, 2, 3): Planned milestone work
- Decimal phases (2.1, 2.2): Urgent insertions (marked with INSERTED)

- [x] **Phase 1: Fundacao Compilavel** - Esqueleto Android validado: Gradle KTS + catalog, tema com tokens, detekt/lint, build.sh
- [x] **Phase 2: Motor de Decisao e Normalizacao** - Domínio puro com políticas por origem, libphonenumber e suíte exaustiva
- [x] **Phase 3: Dados Locais** - DataStore (configurações + contador de aberturas), Room (whitelist + histórico), retenção e backup exclusion
- [x] **Phase 4: Contatos do Aparelho** - READ_CONTACTS com explicação, lookup local cacheado e políticas por contato
- [x] **Phase 5: Triagem Telecom Modo Filtro** - Service integrado ao motor, papel de call screening, proteções, chamada repetida e notificação silenciosa
- [x] **Phase 6: Modo Discador Opcional** - ROLE_DIALER, InCallService mínimo, discagem e reversão limpa
- [ ] **Phase 7: UI Onboarding e Home** - Fluxo de boas-vindas/permissões/políticas, dashboard e tela Proteção
- [ ] **Phase 8: UI Whitelist e Historico** - CRUD com busca e import/export; histórico com filtros e ações
- [ ] **Phase 9: Apoio Privacidade Release e Validacao Fisica** - Avaliação/apoio (5ª abertura), tela sobre, release R8, roteiro Samsung

## Phase Details

### Phase 1: Fundacao Compilavel
**Goal**: Projeto Android compila, testa e lint-a limpo com o stack travado, pronto para receber as fases seguintes.
**Depends on**: Nothing (first phase)
**Requirements**: PRV-01 (base), QLT-02 (base), UIX-08 (tokens do tema), UIX-12
**Success Criteria** (what must be TRUE):
  1. `./gradlew assembleDebug testDebugUnitTest lint detekt` termina sem erro na máquina de dev
  2. `assembleDebug` produz APK instalável e o tema dark "Silent Guardian" está aplicado no `MainActivity` (verificação em aparelho fica na Phase 9)
  3. Manifest não declara INTERNET e registra o `CallScreeningService` com `BIND_SCREENING_SERVICE`
  4. `CallDecisionEngine` puro existe com a precedência (incluindo políticas por origem) coberta por testes unitários
  5. Nome, applicationId, cores e strings centralizados — rebranding não exige tocar em código Kotlin
**Plans**: 3 plans

Plans:
- [x] 01-01-PLAN.md — Política de lint declarada (`lint {}` com disable justificado) + correção real do `ObsoleteSdkInt` (rename `mipmap-anydpi-v26` → `mipmap`), fechando QLT-02 na letra
- [x] 01-02-PLAN.md — `scripts/verify-invariants.sh` (allowlist de permissões sobre o manifest mesclado + greps de rebranding + domínio puro) e `ThemeTokensTest` cobrindo os tokens Silent Guardian
- [x] 01-03-PLAN.md — Evidência arquivada de build pós-`clean` (`01-EVIDENCE.md`), reconciliação do `POST_NOTIFICATIONS` com `docs/PERMISSOES.md` e registro das pendências físicas (cenários 31–34) para a Phase 9

### Phase 2: Motor de Decisao e Normalizacao
**Goal**: Toda regra de triagem (políticas por origem: contato, whitelist, desconhecido) e normalização de números existe como código puro, determinístico e exaustivamente testado — antes de qualquer integração com o Telecom.
**Depends on**: Phase 1
**Requirements**: DEC-01..05, NRM-01..04, CTT-03 (lógica), WLT-08 (lógica), QLT-01 (casos de domínio), QLT-07 (base de cobertura)
**Success Criteria** (what must be TRUE):
  1. Precedência completa implementada e coberta caso a caso: saída, proteção off, privado, contato (4 políticas), whitelist (4 políticas), falha de consulta, desconhecido (3 políticas), inválido
  2. `PhoneNumberNormalizer` real com libphonenumber-android: BR (DDI 55, DDD, 9 dígitos, fixo) e internacional passam nos testes
  3. Máscara de exibição nunca revela o número completo em nenhum formato de entrada
  4. Nenhuma classe de domínio importa tipo do Android Telecom
  5. Cobertura Kover ≥ 80% no pacote domain/
**Plans**: 5 plans

Plans:
- [x] 02-01-PLAN.md — Infra de validação: Kover 0.9.9 + Metaspace 1g, fixture `TestMetadata` (libphonenumber em JVM puro) e invariante de pureza estendido a `phone/`
- [x] 02-02-PLAN.md — Matriz parametrizada política × origem × modo de bloqueio (48 casos), privados/inválidos/fallback, reason codes sem dado pessoal e precedência entre os 7 níveis
- [x] 02-03-PLAN.md — Cascata de região SIM/rede → preferência do usuário → BR (`RegionProvider` puro + `AndroidRegionProvider` isolado), sem permissão nova
- [x] 02-04-PLAN.md — `LibPhoneNumberNormalizer` real: E.164 BR/internacional, regra do 9º dígito à mão com revalidação, códigos curtos por dígitos crus e máscara única
- [x] 02-05-PLAN.md — Wiring no `AppContainer` (loader de assets em `platform/`), gate `koverVerify` em 80% e evidência pós-`clean`

### Phase 3: Dados Locais
**Goal**: Configurações, whitelist, histórico e contador de aberturas persistem localmente com retenção e ficam fora de backup — com a consulta da whitelist dentro do orçamento de performance.
**Depends on**: Phase 2
**Requirements**: WLT-01..04, WLT-07, HST-01..06, ENG-01, QLT-01 (casos de dados), QLT-03, QLT-06 (parcial), PRV-03
**Success Criteria** (what must be TRUE):
  1. `SettingsRepository` (DataStore) expõe Flow + snapshot rápido com defaults do MVP (desconhecidos bloqueados, contatos tocando, whitelist nunca silenciada)
  2. Whitelist Room: CRUD + busca + dedup por E.164, consulta indexada `contains()` medida abaixo do orçamento
  3. Histórico Room: registro mínimo, retenção aplicada (nunca/7/30/90/manual) e limpeza total/individual
  4. Contador de aberturas do app persiste e incrementa corretamente (base do fluxo de avaliação)
  5. Backup do Android comprovadamente exclui os dados (dataExtractionRules validado)
  6. Testes de migração do Room configurados (schemas exportados em `app/schemas/`) e instrumentados de DAO verdes
**Plans**: 7 plans

Plans:
- [x] 03-01-PLAN.md — Wave 0 instrumentado: `androidTestImplementation` de room-testing/test-core, `schemas` como asset do androidTest e `scripts/run-instrumented-tests.sh` (boot headless + polling em `sys.boot_completed` + trap)
- [x] 03-02-PLAN.md — Backup: `path="."` explícito nos dois XMLs (API 31+ e 29-30) e `BackupRulesTest` que lê o XML por DOM e falha se um `<exclude>` sumir
- [x] 03-03-PLAN.md — Schema v1 completo do `sentinela.db` (whitelist + blocked_call, índice único, conversores por code/name), `SchemaExportTest` e invariantes contra `fallbackToDestructiveMigration`
- [x] 03-04-PLAN.md — `RoomWhitelistRepository` com dedup por id resolvido, DAO instrumentado (CRUD/busca/código curto) e prova do índice por `EXPLAIN QUERY PLAN` + percentis medidos
- [x] 03-05-PLAN.md — `RetentionPolicy` pura (nunca/7/30/90/manual), `RoomBlockedCallRepository` com poda sem WorkManager e DAO instrumentado de histórico
- [x] 03-06-PLAN.md — `DataStoreSettingsRepository` com cache `@Volatile`, defaults seguros em arquivo corrompido e contador de aberturas (ENG-01)
- [x] 03-07-PLAN.md — Wiring dos singletons no `AppContainer`, `MigrationHarnessTest`, ampliação do filtro do Kover para `data.*`/`settings.*` e evidência pós-`clean`

### Phase 4: Contatos do Aparelho
**Goal**: O Sentinela sabe — local e instantaneamente — se quem liga está na agenda, sem nunca armazenar ou vazar dados de contato.
**Depends on**: Phase 3
**Requirements**: CTT-01..04 (CTT-03 já completo desde a Phase 2)
**Success Criteria** (what must be TRUE):
  1. Pedido de `READ_CONTACTS` acontece com explicação clara e o app permanece 100% funcional no modo filtro se negado
  2. `ContactLookupRepository` responde HIT/MISS/UNAVAILABLE por E.164 com cache em memória invalidado por ContentObserver
  3. Lookup medido dentro do orçamento de p95 da decisão (inclusive cold start)
  4. Nenhum nome/dado de contato aparece em banco, logs ou backup — verificado por teste e inspeção do schema
**Plans**: 5 plans

Plans:
- [x] 04-01-PLAN.md — Wave 0: `READ_CONTACTS` no manifest + as DUAS edições de `verify-invariants.sh` (allowlist e FUTURE, com gravação de agenda barrada para sempre) e `ContactsTestFixture` por `adoptShellPermissionIdentity`
- [x] 04-02-PLAN.md — Máquina de estado da permissão: enum de 4 estados por função pura, flag `contacts_permission_asked` no DataStore existente e camada fina de plataforma (a tela é da Phase 7)
- [x] 04-03-PLAN.md — `ContactsContractLookupSource` com sonda dupla (E.164 + nacional) e projeção mínima, `ContactKeyCache` normalizado pelo app com debounce, e o repositório HIT/MISS/UNAVAILABLE
- [x] 04-04-PLAN.md — Instrumentados no emulador: sonda dupla contra o provider real, invalidação por ContentObserver e percentis com 5.000 contatos (assert primário na mediana)
- [x] 04-05-PLAN.md — Bloco 6 de invariantes + `SchemaExportTest` de vazamento, wiring no `AppContainer`, exclude nomeado do Kover e evidência pós-`clean`

### Phase 5: Triagem Telecom Modo Filtro
**Goal**: Chamada de número desconhecido é bloqueada de verdade antes de tocar, com o Service fino, resiliente e dentro do orçamento — o critério de aceite central do produto.
**Depends on**: Phase 4
**Requirements**: SCR-01..06, SCR-08..12 (SCR-07 WON'T FIX), NTF-01..06, DEC-01..05 (integração), QLT-01 (casos de service), QLT-06 (parcial)
**Success Criteria** (what must be TRUE):
  1. Com o papel concedido, número fora da agenda não toca, não mostra tela de chamada e não gera notificação nativa de perdida
  2. Contato da agenda toca normalmente no modo filtro (agora depende do NOSSO lookup: com READ_CONTACTS concedida a plataforma entrega contatos ao Service)
  3. `respondToCall` exatamente 1× em todos os caminhos, inclusive exceção/timeout interno — coberto por teste
  4. Notificação própria silenciosa só aparece se habilitada, depois da resposta, com número mascarado ou anônimo
  5. p95 da decisão < 200 ms em cold path; mediana trava o build, cauda tem veredito na Phase 9
  6. Política Silenciar toca sem som/vibração e Encaminhar silenciosamente cai na caixa postal quando selecionados
**Plans**: 7 plans

Plans:
- [x] 05-01-PLAN.md — Wave 0 do domínio: regra SCR-12 (chamada repetida toca) no `CallDecisionEngine` com janela em constante nomeada, `hasRecentBlock` sobre o histórico existente (sem mudar schema) e correção do contador de aberturas
- [x] 05-02-PLAN.md — Wave 0 do Telecom: harness Robolectric com captura das respostas emitidas, `ScreenedCallFactory` e `CallResponseFactory` (tabela de tradução validada em `sdk=[35]` e `[29]`)
- [x] 05-03-PLAN.md — `ScreeningCoordinator` puro: timeout interno de 1 s, garantia de resposta única por `AtomicBoolean` + rede permissiva, matriz de exceção em cada ponto e ordem provada por lista de eventos
- [x] 05-04-PLAN.md — Notificação própria opt-in: canal `IMPORTANCE_LOW`, conteúdo mascarado ou anônimo sem número completo, `PendingIntent` imutável e máquina de estado genérica de permissão em runtime
- [x] 05-05-PLAN.md — Fiação: `UnknownCallScreeningService` delegando ao coordenador, `AppContainer` com notificador e pós-resposta, testes do Service real e do papel de triagem
- [x] 05-06-PLAN.md — Prova instrumentada: bind real do Service (QLT-06), percentis da decisão com assert na mediana (SCR-11) e Bloco 7 de `verify-invariants.sh` (regra só no motor)
- [x] 05-07-PLAN.md — Honestidade e fechamento: `docs/LIMITACOES.md` itens 2/3/7 corrigidos com a fonte AOSP, rótulos revistos, cenários 40–51 do roteiro Samsung, `koverVerify` e `05-EVIDENCE.md`

### Phase 6: Modo Discador Opcional
**Goal**: Usuário que optar pode tornar o Sentinela o telefone padrão — habilitando políticas também para contatos — com experiência de chamada própria mínima e reversão limpa.
**Depends on**: Phase 5
**Requirements**: DIA-01..05, QLT-06 (parcial)
**Pesquisa obrigatória antes do planejamento**: elegibilidade ao `ROLE_DIALER` (handlers exigidos), ciclo de vida do `InCallService`, semântica de `setSilenceCall`/DND bypass por versão, comportamento Samsung/One UI ao trocar o app de telefone.
**Success Criteria** (what must be TRUE):
  1. Ativação do modo discador solicita `ROLE_DIALER` com explicação honesta do que muda e exige READ_CONTACTS concedida
  2. Chamada recebida no modo discador usa a UI própria: atender, recusar, encerrar, mudo, viva-voz e DTMF funcionam
  3. Política por contato (Tocar/Bloquear/Silenciar/Nunca Silenciar) é aplicada de verdade a chamadas de contatos
  4. Discar um número pela tela de discagem funciona (ACTION_DIAL atendido)
  5. Reverter para o discador nativo restaura tudo sem quebrar telefonia; modo filtro continua operante
**Plans**: 8 plans

Plans:
- [x] 06-01-PLAN.md — Núcleo puro: estados da chamada, mapa exaustivo dos 13 estados da plataforma, costura de comandos (mudo/viva-voz provados aqui) e coordenador com prazo de apresentação que falha alto
- [x] 06-02-PLAN.md — Fundação visual: tipografia numérica com figuras tabulares, cores fixas de atender/recusar fora do Dynamic Color, formas, as 46 strings pt-BR e nove componentes com alvo ≥ 48dp
- [x] 06-03-PLAN.md — `docs/PERMISSOES.md` primeiro (4ª permissão `USE_FULL_SCREEN_INTENT`), manifest mínimo medido (InCallService + os DOIS filtros de `ACTION_DIAL`), serviço fino, store no `AppContainer`, `DialerRoleManager` e Bloco 8 de invariantes
- [x] 06-04-PLAN.md — Telas de chamada (recebida em tela cheia, saída, ativa, teclado DTMF, rota de áudio) com as 4 variantes de identidade, semântica TalkBack e a fronteira número completo × mascarado travada por teste
- [x] 06-05-PLAN.md — Discagem (`ACTION_DIAL`): origem por `placeCall`, `CALL_PHONE` em runtime, tela de discagem nos dois contextos e tela de ativação/reversão com copy honesta
- [x] 06-06-PLAN.md — Tela cheia oficial da chamada recebida: canal `IMPORTANCE_HIGH` separado do canal da Fase 5, `setFullScreenIntent`, degradação para heads-up com ações e privacidade da notificação
- [x] 06-07-PLAN.md — Prova instrumentada: bind real (QLT-06), elegibilidade real ao papel, DIA-04 provado com o motor intocado, reversão limpa e morte mid-call
- [x] 06-08-PLAN.md — Honestidade e fechamento: item 8 de `LIMITACOES.md` corrigido, `TELAS.md` §11 reescrita, cenários 23–30 revisados no lugar + 52–60 novos, `koverVerify` e `06-EVIDENCE.md`
- [x] 06-09-PLAN.md — Lacuna da verificação: mudo/viva-voz e tradutor da máscara de rotas provados NA costura da telefonia; exclude do Kover da costura removido

### Phase 7: UI Onboarding e Home
**Goal**: Usuário sai do zero ao protegido em menos de 2 minutos, entendendo exatamente o que o app faz, o que cada política significa e o que o modo discador oferece.
**Depends on**: Phase 6
**Requirements**: SCR-01..02 (superfície), UIX-01..03, UIX-07..11 (parcial)
**Success Criteria** (what must be TRUE):
  1. Onboarding concede o papel e configura: política de desconhecidos, política de contatos (com pedido de READ_CONTACTS) e tratamento da whitelist — com defaults do mockup
  2. Home mostra status real do papel/proteção com botão de correção que funciona
  3. Tela Proteção altera cada configuração (incluindo modo discador) com explicação clara e efeito imediato na triagem
  4. TalkBack navega o fluxo inteiro; strings 100% em resources pt-BR
**Plans**: 11 plans

Plans:
- [ ] 07-01-PLAN.md — Fundacao de texto e cor: tres cores semanticas fixas fora do Dynamic Color, as 43 chaves pt-BR novas (226 -> 269), varredura de honestidade sobre o TEXTO dos recursos e correcao do porcento cru medido
- [ ] 07-02-PLAN.md — Wave 0 de teste: extracao dos tres asserts de dois eixos para pacote neutro (nunca duplicados) + dez rotas por TEXTO com contrato do grafo provado em EXECUCAO (rota tipada e falso-verde de compilacao, medido)
- [ ] 07-03-PLAN.md — Seis componentes compartilhados (cartao de opcao, cabecalho de passo, barra superior, linha de interruptor, linha de verificacao, barra inferior) com quatro dos cinco pontos de risco de semantica mesclada travados
- [ ] 07-04-PLAN.md — Tres donos de estado: tipo FECHADO do valor de estatistica (zero mentiroso impossivel), papel vivo provado por contador de consultas, marca de permissao ao disparar e efeito imediato sem botao salvar
- [ ] 07-05-PLAN.md — Boas-vindas + passos 1-2: copy honesta no lugar das cinco promessas dos mockups, aviso obrigatorio de escopo com peso visual, papel negado que nao trava
- [ ] 07-06-PLAN.md — Passos 3-4: quatro ramos da permissao da agenda com a consequencia honesta na tela, opcoes sempre editaveis, imagem remota substituida por superficie tonal
- [ ] 07-07-PLAN.md — Passos 5-6: opt-in de notificacao sem pressao e verificacao final com veredito nunca falsamente positivo
- [ ] 07-08-PLAN.md — Home: interruptor que alterna a PREFERENCIA (nunca o papel), oito estados degradados, zero proibido em tres deles e ultima bloqueada mascarada provada por varredura da arvore semantica
- [ ] 07-09-PLAN.md — Tela Protecao: 16 itens com explicacao permanente, efeito imediato, exatamente duas confirmacoes (so por perda de dado) e caso de COMPLETUDE que pega item esquecido
- [ ] 07-10-PLAN.md — Fiacao: grafo real, quatro camadas de rota, hospedeira sem bloqueio da thread principal, Bloco 9 de invariantes e fluxo de ponta a ponta em JVM; fecha a pendencia de 06-05 (ativacao do modo discador ganha ponto de entrada)
- [ ] 07-11-PLAN.md — Fechamento: supressao de lint reabilitada e estreitada nominalmente (133 -> 81 medido), faxina dos defeitos incidentais, TELAS.md reescrita, cenarios fisicos 61+, koverVerify e 07-EVIDENCE.md

### Phase 8: UI Whitelist e Historico
**Goal**: Usuário gerencia exceções e audita bloqueios sem sair do app.
**Depends on**: Phase 7
**Requirements**: UIX-04..05, UIX-07..11 (transversal), WLT-05..06 (import/export), WLT-08 (UI), HST-07, PRV-05
**Success Criteria** (what must be TRUE):
  1. Cadastro de número BR normaliza para E.164, detecta duplicado e aparece na busca
  2. Export gera arquivo local; import valida formato/tamanho, pede confirmação e mescla sem duplicar
  3. Histórico filtra por período/decisão e cada registro permite: permitir (→ whitelist), marcar indesejado, excluir
  4. Tratamento da whitelist (Nunca Silenciar/Tocar/Bloquear/Silenciar) é configurável na UI
  5. Números aparecem sempre mascarados nas listas e notificações
**Plans**: TBD

### Phase 9: Apoio Privacidade Release e Validacao Fisica
**Goal**: MVP instalável, auditável e honesto: fluxo de apoio/avaliação respeitoso, política de privacidade embutida, release assinado com R8 e comportamento validado (ou documentado) em Samsung físico.
**Depends on**: Phase 8
**Requirements**: UIX-06, UIX-07..11 (final), UIX-13, ENG-02..04, PRV-02, PRV-04, PRV-06..07, QLT-02, QLT-04..05, QLT-07 (gate)
**Success Criteria** (what must be TRUE):
  1. Na 5ª abertura o convite de avaliação/apoio aparece; recusa reapresenta a cada 5 aberturas; aceite encerra os convites — coberto por teste
  2. Seção "Apoie o Sentinela" destaca open source / sem propaganda / sem telemetria / sem nuvem / 100% offline, com comentário de apoio e doação em Bitcoin (endereço real do mantenedor)
  3. Tela "Privacidade e sobre" lista dados, permissões, retenção, versão e limitações reais, com limpar-tudo funcional
  4. `assembleRelease` gera APK minificado assinado; logs sensíveis comprovadamente ausentes do release; cobertura Kover ≥ 80% em domain/dados
  5. Roteiro `docs/TESTE-FISICO-SAMSUNG.md` (incluindo cenários do modo discador) executado em Samsung com resultados registrados ou pendências documentadas
  6. Todos os critérios de aceite da seção 16 do prompt (com adendos) verificados ou justificados, com relatório final de entrega produzido (QLT-05)
**Plans**: TBD

## Progress

**Execution Order:**
Phases execute in numeric order: 1 → 2 → 3 → 4 → 5 → 6 → 7 → 8 → 9

**Política de validação física (decidida em 2026-07-28):**
Nenhum plano das fases 1–8 pode emitir `checkpoint:human-action` ou `checkpoint:human-verify`.
Todo critério que exige aparelho físico (instalar APK, confirmar que a chamada não toca,
exercitar a UI de chamada do modo discador, comportamento Samsung/One UI) é registrado como
pendência e concentrado no roteiro único da Phase 9 (`docs/TESTE-FISICO-SAMSUNG.md`), executado
manualmente pelo mantenedor. Nas fases 1–8 o verifier deve tratar esses itens como
"deferred to Phase 9", não como gap.

| Phase | Milestone | Plans Complete | Status | Completed |
|-------|-----------|----------------|--------|-----------|
| 1 | v0.1.0 | 3/3 | Complete | 2026-07-29 |
| 2 | v0.1.0 | 5/5 | Complete | 2026-07-29 |
| 3 | v0.1.0 | 7/7 | Complete | 2026-07-29 |
| 4 | v0.1.0 | 5/5 | Complete | 2026-07-29 |
| 5 | v0.1.0 | 7/7 | Complete | 2026-07-29 |
| 6 | v0.1.0 | 9/8 | Complete | 2026-07-30 |
| 7 | v0.1.0 | 2/11 | In Progress | - |
| 8 | v0.1.0 | 0/? | Not started | - |
| 9 | v0.1.0 | 0/? | Not started | - |

---
*Last updated: 2026-07-28 — adendos do produto: contatos, modo discador, apoio/avaliação; roadmap expandido de 7 para 9 fases*
