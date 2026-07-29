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

- [ ] **Phase 1: Fundação Compilável** - Esqueleto Android validado: Gradle KTS + catalog, tema com tokens, detekt/lint, build.sh
- [ ] **Phase 2: Motor de Decisão e Normalização** - Domínio puro com políticas por origem, libphonenumber e suíte exaustiva
- [ ] **Phase 3: Dados Locais** - DataStore (configurações + contador de aberturas), Room (whitelist + histórico), retenção e backup exclusion
- [ ] **Phase 4: Contatos do Aparelho** - READ_CONTACTS com explicação, lookup local cacheado e políticas por contato
- [ ] **Phase 5: Triagem Telecom (Modo Filtro)** - Service integrado ao motor, papel de call screening, proteções e notificação silenciosa
- [ ] **Phase 6: Modo Discador (Opcional ao Usuário)** - ROLE_DIALER, InCallService mínimo, discagem e reversão limpa
- [ ] **Phase 7: UI Onboarding e Home** - Fluxo de boas-vindas/permissões/políticas, dashboard e tela Proteção
- [ ] **Phase 8: UI Whitelist e Histórico** - CRUD com busca e import/export; histórico com filtros e ações
- [ ] **Phase 9: Apoio, Privacidade, Release e Validação Física** - Avaliação/apoio (5ª abertura), tela sobre, release R8, roteiro Samsung

## Phase Details

### Phase 1: Fundação Compilável
**Goal**: Projeto Android compila, testa e lint-a limpo com o stack travado, pronto para receber as fases seguintes.
**Depends on**: Nothing (first phase)
**Requirements**: PRV-01 (base), QLT-02 (base), UIX-08 (tokens do tema), UIX-12
**Success Criteria** (what must be TRUE):
  1. `./gradlew assembleDebug testDebugUnitTest lint detekt` termina sem erro na máquina de dev
  2. `assembleDebug` produz APK instalável e o tema dark "Silent Guardian" está aplicado no `MainActivity` (verificação em aparelho fica na Phase 9)
  3. Manifest não declara INTERNET e registra o `CallScreeningService` com `BIND_SCREENING_SERVICE`
  4. `CallDecisionEngine` puro existe com a precedência (incluindo políticas por origem) coberta por testes unitários
  5. Nome, applicationId, cores e strings centralizados — rebranding não exige tocar em código Kotlin
**Plans**: TBD

### Phase 2: Motor de Decisão e Normalização
**Goal**: Toda regra de triagem (políticas por origem: contato, whitelist, desconhecido) e normalização de números existe como código puro, determinístico e exaustivamente testado — antes de qualquer integração com o Telecom.
**Depends on**: Phase 1
**Requirements**: DEC-01..05, NRM-01..04, CTT-03 (lógica), WLT-08 (lógica), QLT-01 (casos de domínio), QLT-07 (base de cobertura)
**Success Criteria** (what must be TRUE):
  1. Precedência completa implementada e coberta caso a caso: saída, proteção off, privado, contato (4 políticas), whitelist (4 políticas), falha de consulta, desconhecido (3 políticas), inválido
  2. `PhoneNumberNormalizer` real com libphonenumber-android: BR (DDI 55, DDD, 9 dígitos, fixo) e internacional passam nos testes
  3. Máscara de exibição nunca revela o número completo em nenhum formato de entrada
  4. Nenhuma classe de domínio importa tipo do Android Telecom
  5. Cobertura Kover ≥ 80% no pacote domain/
**Plans**: TBD

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
**Plans**: TBD

### Phase 4: Contatos do Aparelho
**Goal**: O Sentinela sabe — local e instantaneamente — se quem liga está na agenda, sem nunca armazenar ou vazar dados de contato.
**Depends on**: Phase 3
**Requirements**: CTT-01..04
**Success Criteria** (what must be TRUE):
  1. Pedido de `READ_CONTACTS` acontece com explicação clara e o app permanece 100% funcional no modo filtro se negado
  2. `ContactLookupRepository` responde HIT/MISS/UNAVAILABLE por E.164 com cache em memória invalidado por ContentObserver
  3. Lookup medido dentro do orçamento de p95 da decisão (inclusive cold start)
  4. Nenhum nome/dado de contato aparece em banco, logs ou backup — verificado por teste e inspeção do schema
**Plans**: TBD

### Phase 5: Triagem Telecom (Modo Filtro)
**Goal**: Chamada de número desconhecido é bloqueada de verdade antes de tocar, com o Service fino, resiliente e dentro do orçamento — o critério de aceite central do produto.
**Depends on**: Phase 4
**Requirements**: SCR-01..11, NTF-01..06, DEC-01..05 (integração), QLT-01 (casos de service), QLT-06 (parcial)
**Success Criteria** (what must be TRUE):
  1. Com o papel concedido, número fora da agenda não toca, não mostra tela de chamada e não gera notificação nativa de perdida
  2. Contato da agenda toca normalmente no modo filtro (comportamento da plataforma verificado em aparelho)
  3. `respondToCall` exatamente 1× em todos os caminhos, inclusive exceção/timeout interno — coberto por teste
  4. Notificação própria silenciosa só aparece se habilitada, depois da resposta, com número mascarado ou anônimo
  5. p95 da decisão < 200 ms em cold path medido em bench local
  6. Política Silenciar toca sem som/vibração e Encaminhar silenciosamente cai na caixa postal quando selecionados
**Plans**: TBD

### Phase 6: Modo Discador (Opcional ao Usuário)
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
**Plans**: TBD

### Phase 7: UI Onboarding e Home
**Goal**: Usuário sai do zero ao protegido em menos de 2 minutos, entendendo exatamente o que o app faz, o que cada política significa e o que o modo discador oferece.
**Depends on**: Phase 6
**Requirements**: SCR-01..02 (superfície), UIX-01..03, UIX-07..11 (parcial)
**Success Criteria** (what must be TRUE):
  1. Onboarding concede o papel e configura: política de desconhecidos, política de contatos (com pedido de READ_CONTACTS) e tratamento da whitelist — com defaults do mockup
  2. Home mostra status real do papel/proteção com botão de correção que funciona
  3. Tela Proteção altera cada configuração (incluindo modo discador) com explicação clara e efeito imediato na triagem
  4. TalkBack navega o fluxo inteiro; strings 100% em resources pt-BR
**Plans**: TBD

### Phase 8: UI Whitelist e Histórico
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

### Phase 9: Apoio, Privacidade, Release e Validação Física
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
| 1 | v0.1.0 | 0/1 | In progress | - |
| 2 | v0.1.0 | 0/? | Not started | - |
| 3 | v0.1.0 | 0/? | Not started | - |
| 4 | v0.1.0 | 0/? | Not started | - |
| 5 | v0.1.0 | 0/? | Not started | - |
| 6 | v0.1.0 | 0/? | Not started | - |
| 7 | v0.1.0 | 0/? | Not started | - |
| 8 | v0.1.0 | 0/? | Not started | - |
| 9 | v0.1.0 | 0/? | Not started | - |

---
*Last updated: 2026-07-28 — adendos do produto: contatos, modo discador, apoio/avaliação; roadmap expandido de 7 para 9 fases*
