# Requirements: Sentinela — v0.1.0 MVP

**Defined:** 2026-07-27 (revisado 2026-07-28 — adendos do produto)
**Core Value:** "Se não está nos contatos nem na whitelist pessoal, não interrompe o usuário."

Fonte de verdade do escopo: [`docs/PROMPT-MVP.md`](../docs/PROMPT-MVP.md) **+ seção de
adendos no topo do mesmo arquivo** (contatos lidos localmente, modo discador no MVP,
políticas por origem, apoio/avaliação, offline-first com sync futura).

---

## v1 Requirements

### Triagem de Chamadas (SCR)
- [x] **SCR-01**: Onboarding solicita explicitamente o papel `ROLE_CALL_SCREENING` via RoleManager
- [x] **SCR-02**: App verifica continuamente se ainda ocupa o papel e oferece correção na home
- [x] **SCR-03**: Número desconhecido é bloqueado antes de tocar — sem tela de chamada, som, vibração ou heads-up
- [x] **SCR-04**: Número privado/restrito/sem handle é bloqueado por padrão (configurável) — **parcial: só no modo discador (Phase 6).** O AOSP não entrega `PRESENTATION_RESTRICTED/UNKNOWN/UNAVAILABLE/PAYPHONE` ao `CallScreeningService` no modo filtro; a lógica existe e é testada no motor desde a Phase 2 e passa a valer quando o app é o discador padrão
- [x] **SCR-05**: `respondToCall` é chamado exatamente uma vez, muito antes do limite de 5 s da plataforma
- [x] **SCR-06**: Para a política Bloquear, usuário escolhe entre rejeitar imediatamente e encaminhar silenciosamente para a caixa postal
- [~] **SCR-07**: ~~Chamada bloqueada não aparece no histórico nativo por padrão~~ — **WON'T FIX (inatingível, decidido em 2026-07-29).** `CallScreeningServiceFilter` calcula `setShouldAddToCallLog(!skipCallLog || packageType != PACKAGE_TYPE_CARRIER)`; o Sentinela é `PACKAGE_TYPE_USER_CHOSEN`, então a chamada bloqueada **sempre** entra no log nativo como `BLOCKED_TYPE`. Não é variação de OEM e o `ROLE_DIALER` da Phase 6 não destrava — só apps de operadora são isentos. A UI não pode prometer ausência de rastro; ver `docs/LIMITACOES.md`
- [x] **SCR-12**: Chamada repetida do mesmo número dentro de uma janela curta **toca**, mesmo que a política bloquearia — habilitado por padrão, configurável. Racional: emergência real insiste, spam automatizado normalmente não. A regra vive no `CallDecisionEngine`, usa o histórico local já existente e entra depois de contato/whitelist e antes da política de desconhecidos
- [x] **SCR-08**: Notificação nativa de chamada perdida é sempre suprimida (`setSkipNotification(true)`)
- [x] **SCR-09**: Chamadas de saída nunca sofrem interferência
- [x] **SCR-10**: Service protegido contra: resposta duplicada, exceção na normalização, banco indisponível, cold start, corrida config×chamada, timeout, handle nulo, número inválido, dual SIM, processo recriado
- [x] **SCR-11**: Orçamento de performance validado: p95 < 200 ms no cold path local da decisão

### Motor de Decisão (DEC)
- [x] **DEC-01**: `CallDecisionEngine` puro e determinístico concentra toda a regra — nenhuma condição de bloqueio fora dele
- [x] **DEC-02**: Precedência: saída → proteção off → privado → contato → whitelist → falha de consulta → desconhecido
- [x] **DEC-03**: Resultado modelado como domínio: Allow, Silence, Reject, SendSilentlyToVoicemail, BlockWithoutTrace
- [x] **DEC-04**: Reason codes internos sem dado pessoal (outgoing_call, protection_disabled, private_number, contact, personal_whitelist, unknown_number, invalid_number, local_lookup_failure, fallback_policy)
- [x] **DEC-05**: Política de fallback em erro inesperado é explícita e configurável (permitir/bloquear)

### Normalização de Números (NRM)
- [x] **NRM-01**: Normalização para E.164 com libphonenumber (port Android) — nunca normalização improvisada
- [x] **NRM-02**: Padrão brasileiro correto: DDI +55, DDD obrigatório, celular com 9 dígitos, fixos
- [x] **NRM-03**: Formatação bonita é apenas visual; E.164 é a fonte de verdade armazenada
- [x] **NRM-04**: Máscara segura para exibição/log (ex.: `+55 11 9****-1234`)

### Contatos do Aparelho (CTT)
- [x] **CTT-01**: `READ_CONTACTS` solicitada em runtime com explicação clara (identificar se quem liga é contato); app permanece funcional no modo filtro se negada
- [x] **CTT-02**: Consulta de contato é local e rápida (cache em memória invalidado por ContentObserver), dentro do orçamento de p95 da decisão
- [x] **CTT-03**: Política para contatos configurável: Tocar (padrão) / Bloquear / Silenciar / Nunca Silenciar — conforme mockup de onboarding
- [x] **CTT-04**: Nomes e dados de contato nunca são persistidos no banco nem enviados a lugar algum; uso apenas em memória no momento da chamada/exibição

### Whitelist Pessoal (WLT)
- [x] **WLT-01**: Adicionar número manualmente com país/DDI e descrição local opcional
- [x] **WLT-02**: Editar, ativar/desativar e excluir entradas
- [x] **WLT-03**: Pesquisar por número ou descrição
- [x] **WLT-04**: Duplicidade detectada e recusada com aviso
- [ ] **WLT-05**: Exportar backup local (arquivo do próprio app)
- [ ] **WLT-06**: Importar backup criado pelo próprio app, com validação de conteúdo, limite de tamanho e confirmação antes de sobrescrever
- [x] **WLT-07**: Consulta da whitelist na decisão é local, indexada e dentro do orçamento de performance
- [x] **WLT-08**: Tratamento da whitelist configurável: Nunca Silenciar (padrão) / Tocar / Bloquear / Silenciar — conforme passo do onboarding

### Histórico Interno (HST)
- [x] **HST-01**: Histórico próprio é opcional e registra o mínimo: número protegido/mascarado, data/hora UTC, motivo, ação, notificação enviada, SIM (se disponível sem permissão invasiva)
- [x] **HST-02**: Retenção configurável: não guardar / 7 dias / 30 dias / 90 dias / até exclusão manual
- [x] **HST-03**: Limpar histórico completo e excluir registro individual
- [x] **HST-04**: Adicionar número à whitelist a partir do histórico
- [x] **HST-05**: Marcar registro como legítimo ou indesejado (status local)
- [x] **HST-06**: Banco sensível excluído do backup automático do Android
- [ ] **HST-07**: Filtros por período e por decisão na tela de histórico

### Notificações (NTF)
- [x] **NTF-01**: Notificação própria desabilitada por padrão; opt-in explícito
- [x] **NTF-02**: `POST_NOTIFICATIONS` solicitada somente quando o usuário habilita a opção
- [x] **NTF-03**: Canal dedicado "Chamadas bloqueadas" com IMPORTANCE_LOW: sem som, vibração, heads-up, full-screen intent ou overlay
- [x] **NTF-04**: Tela bloqueada nunca mostra o número completo — escolha entre mascarado e nenhuma identificação
- [x] **NTF-05**: Tocar na notificação abre o registro interno correspondente
- [x] **NTF-06**: Notificação própria criada somente depois do `respondToCall`

### Modo Discador (DIA)
- [ ] **DIA-01**: Modo discador opcional: usuário pode tornar o Sentinela o app de telefone padrão (`ROLE_DIALER`) com explicação clara do que muda
- [ ] **DIA-02**: `InCallService` com UI própria mínima de chamada: receber, atender, recusar, encerrar, mudo, viva-voz e teclado DTMF
- [ ] **DIA-03**: Discagem mínima: handler de `ACTION_DIAL` e tela de discagem simples (requisito de elegibilidade ao papel)
- [ ] **DIA-04**: No modo discador, a triagem cobre todas as chamadas — inclusive contatos, aplicando as políticas CTT
- [ ] **DIA-05**: Reversão limpa para o discador nativo a qualquer momento; telefonia nunca fica quebrada e o app continua funcional no modo filtro

### Telas e UX (UIX)
- [ ] **UIX-01**: Onboarding: explicação do funcionamento, aviso de que só chamadas telefônicas são filtradas, solicitação do papel, política de desconhecidos, política de contatos (com pedido de READ_CONTACTS), tratamento da whitelist, opt-in de notificação e verificação final
- [ ] **UIX-02**: Home: status proteção ativa/inativa, status do papel com botão de correção, contagem de bloqueadas, última bloqueada (respeitando privacidade), atalhos para whitelist e histórico
- [ ] **UIX-03**: Tela Proteção: políticas por origem (desconhecidos/contatos/whitelist), privados, modo de bloqueio, ocultar histórico nativo, notificação silenciosa, política de fallback, modo discador — cada opção com explicação clara
- [ ] **UIX-04**: Tela Whitelist: listagem, busca, cadastro, edição, exclusão, import/export
- [ ] **UIX-05**: Tela Histórico: filtros, marcar legítimo/indesejado, adicionar à whitelist, excluir/limpar
- [ ] **UIX-06**: Tela Privacidade e sobre: dados armazenados, permissões usadas, retenção, limpar tudo, versão, limitações, links para configurações do app e do canal
- [ ] **UIX-07**: Todas as strings em resources pt-BR — nenhum texto hardcoded em Kotlin
- [x] **UIX-08**: Dark mode (dark-first) + Dynamic Color quando disponível, seguindo tokens de `docs/design/DESIGN.md`
- [ ] **UIX-09**: Acessibilidade: TalkBack, alvos de toque ≥ 48 dp, contraste correto
- [ ] **UIX-10**: Estados de carregamento e erro em todas as telas; proteção desativada é comunicada com destaque
- [ ] **UIX-11**: Nenhuma promessa falsa na UI: não afirmar filtro de WhatsApp/VoIP nem "100% garantido"
- [x] **UIX-12**: Nome do app, applicationId, cores e strings centralizados para rebranding (resources + `sentinelaApplicationId` em `app/build.gradle.kts`)
- [ ] **UIX-13**: Seção "Apoie o Sentinela" (em Sobre): destaque de que o app é open source, sem propaganda, sem telemetria, sem nuvem e 100% offline; convite a comentário de apoio e doação em Bitcoin

### Engajamento e Apoio (ENG)
- [x] **ENG-01**: Contador local de aberturas do app (DataStore), sem qualquer telemetria
- [ ] **ENG-02**: Na 5ª abertura, convite para avaliar/apoiar; se recusado, repete a cada 5 aberturas (10ª, 15ª, 20ª…) até o usuário aceitar; após aceite, nunca mais pergunta
- [ ] **ENG-03**: Fluxo de apoio: avaliação (In-App Review quando disponível; senão link), comentário de apoio e doação em Bitcoin (endereço fornecido pelo mantenedor — nunca publicar placeholder como se fosse real)
- [ ] **ENG-04**: Convite nunca interrompe fluxo crítico (não aparece durante onboarding nem chamada) e respeita a escolha do usuário

### Privacidade e Segurança (PRV)
- [x] **PRV-01**: MVP sem permissão de INTERNET no manifest; nenhuma chamada de rede, telemetria, chave ou segredo — sync futura (v0.2) será opt-in e nunca no caminho da decisão
- [ ] **PRV-02**: Números sempre mascarados em logs; logs sensíveis removidos de release (R8 `-assumenosideeffects`)
- [x] **PRV-03**: Room/DataStore excluídos de backup em nuvem e device-transfer (`dataExtractionRules`)
- [ ] **PRV-04**: R8/ProGuard configurado para release; componentes não necessários não exportados
- [ ] **PRV-05**: Import de backup valida entrada: limite de tamanho, formato, prevenção de path traversal e arquivo malformado
- [ ] **PRV-06**: Política de privacidade curta e verdadeira embutida no app ("dados ficam no aparelho"; contatos lidos apenas localmente)
- [ ] **PRV-07**: "Limpar todos os dados" remove whitelist, histórico e configurações

### Qualidade e Entrega (QLT)
- [x] **QLT-01**: Suíte de testes cobre os 19 casos obrigatórios da seção 13 do prompt (desconhecido, privado, inválido, whitelist, proteção off, saída, reject, voicemail, call log, notificação on/off, falha de repo, timeout, resposta única, normalização BR/intl, import duplicado, backup inválido, retenção, mudança de papel, cold start) **mais** os novos casos: política por contato, tratamento da whitelist, silenciar, contatos indisponíveis e contador de aberturas
- [x] **QLT-02**: Lint + detekt sem issues; builds debug e release compilam
- [x] **QLT-03**: Testes de migração do Room quando houver migração de schema
- [ ] **QLT-04**: Roteiro reproduzível de validação física Samsung executado ou documentado (`docs/TESTE-FISICO-SAMSUNG.md`), incluindo cenários do modo discador
- [ ] **QLT-05**: Entregáveis: README, APK debug, matriz de permissões, política de privacidade, limitações, decisões, backlog de sincronização e relatório final de entrega (resumo, arquivos principais, comandos, resultado dos testes, pendências físicas, riscos reais)
- [x] **QLT-06**: Testes instrumentados possíveis no ambiente executam verdes (`connectedDebugAndroidTest`): Room DAO/migrações, DataStore, bind do CallScreeningService e fluxo mínimo do InCallService
- [x] **QLT-07**: Cobertura de testes ≥ 80% nas camadas de domínio e dados (Kover), medida no gate de release

## v2 Requirements

Deferido para a próxima etapa (v0.2.0). Rastreado, fora do roadmap atual.
O MVP funciona 100% offline; a sincronização é sempre opt-in e assíncrona — a decisão de
bloqueio nunca espera rede.

### Sincronização & Backend (SUP)
- **SUP-01**: Fonte remota de whitelist plugável atrás de `PersonalWhitelistRepository`
- **SUP-02**: Sincronização opcional de configurações e listas entre aparelhos
- **SUP-03**: Backup em nuvem opt-in criptografado no cliente
- **SUP-04**: Envio opcional da lista de números recebidos/bloqueados para o backend (opt-in explícito, anonimizável)

Detalhe em [`docs/backlog/supabase-v2.md`](../docs/backlog/supabase-v2.md).

## Out of Scope

| Feature | Reason |
|---------|--------|
| Caller ID / base global de spam | Produto não precisa saber quem liga, só se interrompe |
| AccessibilityService / overlays / hacks OEM | Proibidos; só APIs oficiais do Telecom |
| Filtrar WhatsApp/Telegram/VoIP | Fora do alcance do CallScreeningService |
| Gravação de chamadas | Proibido |
| Rede no caminho da decisão | Decisão é sempre local; sync (v0.2) é assíncrona e opt-in |
| Servidor, login, analytics, ads, telemetria no MVP | MVP é 100% offline por definição |

## Traceability

| Requirement | Phase | Status |
|-------------|-------|--------|
| SCR-01..02 | 5, 7 | Pending |
| SCR-03..11 | 5 | Pending |
| DEC-01..05 | 2, 5 (integração) | Pending |
| NRM-01..04 | 2 | Pending |
| CTT-01..02, CTT-04 | 4 | Pending |
| CTT-03 | 2 (lógica), 4 | Complete |
| WLT-01..04, WLT-07 | 3 | Pending |
| WLT-05..06 | 8 | Pending |
| WLT-08 | 2 (lógica), 8 (UI) | Complete |
| HST-01..06 | 3 | Pending |
| HST-07 | 8 | Pending |
| NTF-01..06 | 5 | Pending |
| DIA-01..05 | 6 | Pending |
| UIX-01..03 | 7 | Pending |
| UIX-04..05 | 8 | Pending |
| UIX-06 | 9 | Pending |
| UIX-07, UIX-09..11 | 7, 8, 9 | Pending |
| UIX-08 | 1 (tokens), 7, 8, 9 | Complete |
| UIX-12 | 1 | Complete |
| UIX-13 | 9 | Pending |
| ENG-01 | 3 | Complete |
| ENG-02..04 | 9 | Pending |
| PRV-01 | 1 | Complete |
| PRV-02, PRV-04 | 9 | Pending |
| PRV-03 | 3 | Complete |
| PRV-05 | 8 | Pending |
| PRV-06..07 | 9 | Pending |
| QLT-01 | 2, 3, 5 | Complete |
| QLT-02 | 1, 9 | Complete |
| QLT-03 | 3 | Complete |
| QLT-04..05 | 9 | Pending |
| QLT-06 | 3, 5, 6 | Complete |
| QLT-07 | 2, 9 | Complete |

**Coverage:**
- v1 requirements: 81 total
- Mapped to phases: 81
- Unmapped: 0

---
*Requirements defined: 2026-07-27*
*Last updated: 2026-07-28 — adendos do produto: contatos (CTT), modo discador (DIA), apoio (ENG), WLT-08, UIX-13, QLT-06..07*
