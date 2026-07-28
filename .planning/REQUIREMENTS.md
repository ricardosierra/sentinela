# Requirements: Sentinela — v0.1.0 MVP

**Defined:** 2026-07-27
**Core Value:** "Se não está nos contatos nem na whitelist pessoal, não interrompe o usuário."

Fonte de verdade do escopo: [`docs/PROMPT-MVP.md`](../docs/PROMPT-MVP.md).

---

## v1 Requirements

### Triagem de Chamadas (SCR)
- [ ] **SCR-01**: Onboarding solicita explicitamente o papel `ROLE_CALL_SCREENING` via RoleManager
- [ ] **SCR-02**: App verifica continuamente se ainda ocupa o papel e oferece correção na home
- [ ] **SCR-03**: Número desconhecido é bloqueado antes de tocar — sem tela de chamada, som, vibração ou heads-up
- [ ] **SCR-04**: Número privado/restrito/sem handle é bloqueado por padrão (configurável)
- [ ] **SCR-05**: `respondToCall` é chamado exatamente uma vez, muito antes do limite de 5 s da plataforma
- [ ] **SCR-06**: Usuário escolhe entre rejeitar imediatamente e encaminhar silenciosamente para a caixa postal
- [ ] **SCR-07**: Chamada bloqueada não aparece no histórico nativo por padrão (`setSkipCallLog` configurável)
- [ ] **SCR-08**: Notificação nativa de chamada perdida é sempre suprimida (`setSkipNotification(true)`)
- [ ] **SCR-09**: Chamadas de saída nunca sofrem interferência
- [ ] **SCR-10**: Service protegido contra: resposta duplicada, exceção na normalização, banco indisponível, cold start, corrida config×chamada, timeout, handle nulo, número inválido, dual SIM, processo recriado
- [ ] **SCR-11**: Orçamento de performance validado: p95 < 200 ms no cold path local da decisão

### Motor de Decisão (DEC)
- [ ] **DEC-01**: `CallDecisionEngine` puro e determinístico concentra toda a regra — nenhuma condição de bloqueio fora dele
- [ ] **DEC-02**: Precedência: saída → proteção off → privado → whitelist → desconhecido → erro/fallback
- [ ] **DEC-03**: Resultado modelado como domínio: Allow, Reject, SendSilentlyToVoicemail, BlockWithoutTrace
- [ ] **DEC-04**: Reason codes internos sem dado pessoal (outgoing_call, protection_disabled, private_number, personal_whitelist, unknown_number, invalid_number, local_lookup_failure, fallback_policy)
- [ ] **DEC-05**: Política de fallback em erro inesperado é explícita e configurável (permitir/bloquear)

### Normalização de Números (NRM)
- [ ] **NRM-01**: Normalização para E.164 com libphonenumber (port Android) — nunca normalização improvisada
- [ ] **NRM-02**: Padrão brasileiro correto: DDI +55, DDD obrigatório, celular com 9 dígitos, fixos
- [ ] **NRM-03**: Formatação bonita é apenas visual; E.164 é a fonte de verdade armazenada
- [ ] **NRM-04**: Máscara segura para exibição/log (ex.: `+55 11 9****-1234`)

### Whitelist Pessoal (WLT)
- [ ] **WLT-01**: Adicionar número manualmente com país/DDI e descrição local opcional
- [ ] **WLT-02**: Editar, ativar/desativar e excluir entradas
- [ ] **WLT-03**: Pesquisar por número ou descrição
- [ ] **WLT-04**: Duplicidade detectada e recusada com aviso
- [ ] **WLT-05**: Exportar backup local (arquivo do próprio app)
- [ ] **WLT-06**: Importar backup criado pelo próprio app, com validação de conteúdo, limite de tamanho e confirmação antes de sobrescrever
- [ ] **WLT-07**: Consulta da whitelist na decisão é local, indexada e dentro do orçamento de performance

### Histórico Interno (HST)
- [ ] **HST-01**: Histórico próprio é opcional e registra o mínimo: número protegido/mascarado, data/hora UTC, motivo, ação, notificação enviada, SIM (se disponível sem permissão invasiva)
- [ ] **HST-02**: Retenção configurável: não guardar / 7 dias / 30 dias / 90 dias / até exclusão manual
- [ ] **HST-03**: Limpar histórico completo e excluir registro individual
- [ ] **HST-04**: Adicionar número à whitelist a partir do histórico
- [ ] **HST-05**: Marcar registro como legítimo ou indesejado (status local)
- [ ] **HST-06**: Banco sensível excluído do backup automático do Android
- [ ] **HST-07**: Filtros por período e por decisão na tela de histórico

### Notificações (NTF)
- [ ] **NTF-01**: Notificação própria desabilitada por padrão; opt-in explícito
- [ ] **NTF-02**: `POST_NOTIFICATIONS` solicitada somente quando o usuário habilita a opção
- [ ] **NTF-03**: Canal dedicado "Chamadas bloqueadas" com IMPORTANCE_LOW: sem som, vibração, heads-up, full-screen intent ou overlay
- [ ] **NTF-04**: Tela bloqueada nunca mostra o número completo — escolha entre mascarado e nenhuma identificação
- [ ] **NTF-05**: Tocar na notificação abre o registro interno correspondente
- [ ] **NTF-06**: Notificação própria criada somente depois do `respondToCall`

### Telas e UX (UIX)
- [ ] **UIX-01**: Onboarding: explicação do funcionamento, aviso de que só chamadas telefônicas são filtradas, solicitação do papel, opt-in de notificação, verificação final
- [ ] **UIX-02**: Home: status proteção ativa/inativa, status do papel com botão de correção, contagem de bloqueadas, última bloqueada (respeitando privacidade), atalhos para whitelist e histórico
- [ ] **UIX-03**: Tela Proteção: toggles de proteção/desconhecidos/privados, modo rejeitar×silencioso, ocultar histórico nativo, notificação silenciosa, política de fallback — cada opção com explicação clara
- [ ] **UIX-04**: Tela Whitelist: listagem, busca, cadastro, edição, exclusão, import/export
- [ ] **UIX-05**: Tela Histórico: filtros, marcar legítimo/indesejado, adicionar à whitelist, excluir/limpar
- [ ] **UIX-06**: Tela Privacidade e sobre: dados armazenados, permissões usadas, retenção, limpar tudo, versão, limitações, links para configurações do app e do canal
- [ ] **UIX-07**: Todas as strings em resources pt-BR — nenhum texto hardcoded em Kotlin
- [ ] **UIX-08**: Dark mode (dark-first) + Dynamic Color quando disponível, seguindo tokens de `docs/design/DESIGN.md`
- [ ] **UIX-09**: Acessibilidade: TalkBack, alvos de toque ≥ 48 dp, contraste correto
- [ ] **UIX-10**: Estados de carregamento e erro em todas as telas; proteção desativada é comunicada com destaque
- [ ] **UIX-11**: Nenhuma promessa falsa na UI: não afirmar filtro de WhatsApp/VoIP nem "100% garantido"
- [ ] **UIX-12**: Nome do app, applicationId, cores e strings centralizados para rebranding (resources + `sentinelaApplicationId` em `app/build.gradle.kts`)

### Privacidade e Segurança (PRV)
- [ ] **PRV-01**: Manifest sem permissão de INTERNET; nenhuma chamada de rede, telemetria, chave ou segredo
- [ ] **PRV-02**: Números sempre mascarados em logs; logs sensíveis removidos de release (R8 `-assumenosideeffects`)
- [ ] **PRV-03**: Room/DataStore excluídos de backup em nuvem e device-transfer (`dataExtractionRules`)
- [ ] **PRV-04**: R8/ProGuard configurado para release; componentes não necessários não exportados
- [ ] **PRV-05**: Import de backup valida entrada: limite de tamanho, formato, prevenção de path traversal e arquivo malformado
- [ ] **PRV-06**: Política de privacidade curta e verdadeira embutida no app ("dados ficam no aparelho")
- [ ] **PRV-07**: "Limpar todos os dados" remove whitelist, histórico e configurações

### Qualidade e Entrega (QLT)
- [ ] **QLT-01**: Suíte de testes cobre os 19 casos obrigatórios da seção 13 do prompt (desconhecido, privado, inválido, whitelist, proteção off, saída, reject, voicemail, call log, notificação on/off, falha de repo, timeout, resposta única, normalização BR/intl, import duplicado, backup inválido, retenção, mudança de papel, cold start)
- [ ] **QLT-02**: Lint + detekt sem issues; builds debug e release compilam
- [ ] **QLT-03**: Testes de migração do Room quando houver migração de schema
- [ ] **QLT-04**: Roteiro reproduzível de validação física Samsung executado ou documentado (`docs/TESTE-FISICO-SAMSUNG.md`)
- [ ] **QLT-05**: Entregáveis: README, APK debug, matriz de permissões, política de privacidade, limitações, decisões, backlog Supabase e relatório final de entrega (resumo, arquivos principais, comandos, resultado dos testes, pendências físicas, riscos reais)
- [ ] **QLT-06**: Testes instrumentados possíveis no ambiente executam verdes (`connectedDebugAndroidTest`): Room DAO/migrações, DataStore e bind do CallScreeningService

## v2 Requirements

Deferido para a próxima etapa. Rastreado, fora do roadmap atual.

### Supabase & Sincronização (SUP)
- **SUP-01**: Fonte remota de whitelist plugável atrás de `PersonalWhitelistRepository`
- **SUP-02**: Sincronização opcional de configurações entre aparelhos
- **SUP-03**: Backup em nuvem opt-in criptografado

Detalhe em [`docs/backlog/supabase-v2.md`](../docs/backlog/supabase-v2.md).

## Out of Scope

| Feature | Reason |
|---------|--------|
| Caller ID / base global de spam | Produto não precisa saber quem liga, só se interrompe |
| Discador padrão | Proibido pelo prompt; muda perfil de risco e revisão de loja |
| AccessibilityService / overlays / hacks OEM | Proibidos; só APIs oficiais do Telecom |
| Política por contato da agenda (bloquear/silenciar contato) | Plataforma não entrega contatos ao filtro sem discador padrão |
| Filtrar WhatsApp/Telegram/VoIP | Fora do alcance do CallScreeningService |
| Gravação de chamadas | Proibido |
| Servidor, login, analytics, ads, telemetria | Fora do MVP por definição |

## Traceability

| Requirement | Phase | Status |
|-------------|-------|--------|
| SCR-01..02 | 4, 5 | Pending |
| SCR-03..11 | 4 | Pending |
| DEC-01..05 | 2 | Pending |
| NRM-01..04 | 2 | Pending |
| WLT-01..07 | 3 (dados), 6 (UI) | Pending |
| HST-01..07 | 3 (dados), 6 (UI) | Pending |
| NTF-01..06 | 4 | Pending |
| UIX-01..03 | 5 | Pending |
| UIX-04..05 | 6 | Pending |
| UIX-06 | 7 | Pending |
| UIX-07..11 | 5, 6, 7 | Pending |
| UIX-12 | 1 | Pending |
| PRV-01 | 1 | Pending |
| PRV-02, PRV-04 | 7 | Pending |
| PRV-03 | 3 | Pending |
| PRV-05 | 6 | Pending |
| PRV-06..07 | 7 | Pending |
| QLT-01 | 2, 3, 4 | Pending |
| QLT-02 | 1, 7 | Pending |
| QLT-03 | 3 | Pending |
| QLT-04..05 | 7 | Pending |
| QLT-06 | 3, 4 | Pending |

**Coverage:**
- v1 requirements: 65 total
- Mapped to phases: 65
- Unmapped: 0

---
*Requirements defined: 2026-07-27*
*Last updated: 2026-07-27 — bootstrap do projeto*
