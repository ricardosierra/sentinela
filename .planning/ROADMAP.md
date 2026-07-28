# Roadmap: Sentinela

## Overview

Do esqueleto compilável ao MVP instalável em 7 fases: fundação de build e tema; domínio puro
(motor de decisão + normalização) com testes exaustivos; camada de dados local; integração com
o Telecom (o coração do produto); e então as telas em duas levas, fechando com privacidade,
release e o roteiro de validação física Samsung.

## Milestones

- 🚧 **v0.1.0 MVP** — Phases 1-7 (em curso)
- 📋 **v0.2.0 Supabase** — pós-MVP (planejado, sem fases; ver `docs/backlog/supabase-v2.md`)

## Phases

**Phase Numbering:**
- Integer phases (1, 2, 3): Planned milestone work
- Decimal phases (2.1, 2.2): Urgent insertions (marked with INSERTED)

- [ ] **Phase 1: Fundação Compilável** - Esqueleto Android validado: Gradle KTS + catalog, tema com tokens, detekt/lint, build.sh
- [ ] **Phase 2: Motor de Decisão e Normalização** - Domínio puro completo com libphonenumber e suíte exaustiva
- [ ] **Phase 3: Dados Locais** - DataStore (configurações), Room (whitelist + histórico), retenção e backup exclusion
- [ ] **Phase 4: Triagem Telecom** - Service integrado ao motor, papel de call screening, proteções e notificação silenciosa
- [ ] **Phase 5: UI Onboarding e Home** - Fluxo de boas-vindas/permissões, dashboard e tela Proteção
- [ ] **Phase 6: UI Whitelist e Histórico** - CRUD com busca e import/export; histórico com filtros e ações
- [ ] **Phase 7: Privacidade, Release e Validação Física** - Tela sobre/privacidade, release R8 assinado, roteiro Samsung

## Phase Details

### Phase 1: Fundação Compilável
**Goal**: Projeto Android compila, testa e lint-a limpo com o stack travado, pronto para receber as fases seguintes.
**Depends on**: Nothing (first phase)
**Requirements**: PRV-01 (base), QLT-02 (base), UIX-08 (tokens do tema), UIX-12
**Success Criteria** (what must be TRUE):
  1. `./gradlew assembleDebug testDebugUnitTest lint detekt` termina sem erro na máquina de dev
  2. APK debug instala e abre mostrando o tema dark "Silent Guardian"
  3. Manifest não declara INTERNET e registra o `CallScreeningService` com `BIND_SCREENING_SERVICE`
  4. `CallDecisionEngine` puro existe com a precedência do prompt coberta por testes unitários
  5. Nome, applicationId, cores e strings centralizados — rebranding não exige tocar em código Kotlin
**Plans**: 1 plan

Plans:
- [ ] 01-01: Validar esqueleto (build + testes + lint/detekt) e corrigir toolchain AGP 9/Kotlin built-in

### Phase 2: Motor de Decisão e Normalização
**Goal**: Toda regra de triagem e normalização de números existe como código puro, determinístico e exaustivamente testado — antes de qualquer integração com o Telecom.
**Depends on**: Phase 1
**Requirements**: DEC-01..05, NRM-01..04, QLT-01 (casos de domínio)
**Success Criteria** (what must be TRUE):
  1. Precedência completa do §5 do prompt implementada e coberta caso a caso (desconhecido, privado, inválido, whitelist, proteção off, saída, fallback)
  2. `PhoneNumberNormalizer` real com libphonenumber-android: BR (DDI 55, DDD, 9 dígitos, fixo) e internacional passam nos testes
  3. Máscara de exibição nunca revela o número completo em nenhum formato de entrada
  4. Nenhuma classe de domínio importa tipo do Android Telecom
**Plans**: TBD

### Phase 3: Dados Locais
**Goal**: Configurações, whitelist e histórico persistem localmente com retenção e ficam fora de backup — com a consulta da whitelist dentro do orçamento de performance.
**Depends on**: Phase 2
**Requirements**: WLT-01..04, WLT-07, HST-01..06, QLT-01 (casos de dados), QLT-03, QLT-06 (parcial), PRV-03
**Success Criteria** (what must be TRUE):
  1. `SettingsRepository` (DataStore) expõe Flow + snapshot rápido com defaults do MVP
  2. Whitelist Room: CRUD + busca + dedup por E.164, consulta indexada `contains()` medida abaixo do orçamento
  3. Histórico Room: registro mínimo, retenção aplicada (nunca/7/30/90/manual) e limpeza total/individual
  4. Backup do Android comprovadamente exclui os dados (dataExtractionRules validado)
  5. Testes de migração do Room configurados (schemas exportados em `app/schemas/`)
**Plans**: TBD

### Phase 4: Triagem Telecom
**Goal**: Chamada de número desconhecido é bloqueada de verdade antes de tocar, com o Service fino, resiliente e dentro do orçamento — o critério de aceite central do produto.
**Depends on**: Phase 3
**Requirements**: SCR-01..11, NTF-01..06, DEC-01..05 (integração), QLT-01 (casos de service), QLT-06 (parcial)
**Success Criteria** (what must be TRUE):
  1. Com o papel concedido, número fora da agenda não toca, não mostra tela de chamada e não gera notificação nativa de perdida
  2. Contato da agenda toca normalmente (comportamento da plataforma verificado em aparelho)
  3. `respondToCall` exatamente 1× em todos os caminhos, inclusive exceção/timeout interno — coberto por teste
  4. Notificação própria silenciosa só aparece se habilitada, depois da resposta, com número mascarado ou anônimo
  5. p95 da decisão < 200 ms em cold path medido em bench local
  6. Encaminhamento silencioso para caixa postal funciona quando selecionado
**Plans**: TBD

### Phase 5: UI Onboarding e Home
**Goal**: Usuário sai do zero ao protegido em menos de 2 minutos, entendendo exatamente o que o app faz e não faz.
**Depends on**: Phase 4
**Requirements**: SCR-01..02 (superfície), UIX-01..03, UIX-07..11 (parcial)
**Success Criteria** (what must be TRUE):
  1. Onboarding de 4 passos (boas-vindas → papel → desconhecidos → whitelist) concede o papel e configura os padrões
  2. Home mostra status real do papel/proteção com botão de correção que funciona
  3. Tela Proteção altera cada configuração com explicação clara e efeito imediato na triagem
  4. Passo de contatos comunica que a agenda continua tocando (sem prometer política por contato)
  5. TalkBack navega o fluxo inteiro; strings 100% em resources pt-BR
**Plans**: TBD

### Phase 6: UI Whitelist e Histórico
**Goal**: Usuário gerencia exceções e audita bloqueios sem sair do app.
**Depends on**: Phase 5
**Requirements**: UIX-04..05, UIX-07..11 (transversal), WLT-05..06 (import/export), HST-07, PRV-05
**Success Criteria** (what must be TRUE):
  1. Cadastro de número BR normaliza para E.164, detecta duplicado e aparece na busca
  2. Export gera arquivo local; import valida formato/tamanho, pede confirmação e mescla sem duplicar
  3. Histórico filtra por período/decisão e cada registro permite: permitir (→ whitelist), marcar indesejado, excluir
  4. Números aparecem sempre mascarados nas listas e notificações
**Plans**: TBD

### Phase 7: Privacidade, Release e Validação Física
**Goal**: MVP instalável, auditável e honesto: release assinado com R8, política de privacidade embutida e comportamento validado (ou documentado) em Samsung físico.
**Depends on**: Phase 6
**Requirements**: UIX-06, UIX-07..11 (transversal), PRV-02, PRV-04, PRV-06..07, QLT-02, QLT-04..05
**Success Criteria** (what must be TRUE):
  1. Tela "Privacidade e sobre" lista dados, permissões, retenção, versão e limitações reais, com limpar-tudo funcional
  2. `assembleRelease` gera APK minificado assinado; logs sensíveis comprovadamente ausentes do release
  3. Roteiro `docs/TESTE-FISICO-SAMSUNG.md` executado em aparelho Samsung com resultados registrados (ou pendências explicitamente documentadas)
  4. Todos os 13 critérios de aceite da seção 16 do prompt verificados ou justificados
  5. Relatório final de entrega produzido: resumo, arquivos principais, comandos executados, resultado dos testes, APK, pendências físicas e riscos reais (QLT-05)
**Plans**: TBD

## Progress

**Execution Order:**
Phases execute in numeric order: 1 → 2 → 3 → 4 → 5 → 6 → 7

| Phase | Milestone | Plans Complete | Status | Completed |
|-------|-----------|----------------|--------|-----------|
| 1 | v0.1.0 | 0/1 | In progress | - |
| 2 | v0.1.0 | 0/? | Not started | - |
| 3 | v0.1.0 | 0/? | Not started | - |
| 4 | v0.1.0 | 0/? | Not started | - |
| 5 | v0.1.0 | 0/? | Not started | - |
| 6 | v0.1.0 | 0/? | Not started | - |
| 7 | v0.1.0 | 0/? | Not started | - |

---
*Last updated: 2026-07-27 — bootstrap do projeto*
