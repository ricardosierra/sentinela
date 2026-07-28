# Sentinela — Prompt original do MVP

> Documento de referência: prompt original que define o escopo do MVP do Sentinela.
> Este texto é a fonte de verdade do escopo. Os documentos de planejamento (`.planning/`)
> e a documentação técnica (`docs/`) derivam daqui.

---

# Sentinela — Implementar o MVP completo do bloqueador local de chamadas desconhecidas

Você atuará como arquiteto Android sênior, engenheiro de segurança, especialista no Android Telecom Framework e responsável pela entrega completa do produto.

Não entregue somente análise, planejamento, pseudocódigo ou exemplos isolados. Inspecione o repositório, planeje, implemente, teste, execute, corrija os problemas encontrados e entregue um MVP funcional, compilável e instalável.

Se o repositório estiver vazio, crie o projeto Android completo. Se já existir código, preserve o que estiver correto e evolua a arquitetura sem reescrever desnecessariamente.

Use sempre as versões estáveis mais recentes e compatíveis entre si no momento da execução. Consulte prioritariamente a documentação oficial do Android, especialmente:

- https://developer.android.com/reference/android/telecom/CallScreeningService
- https://developer.android.com/develop/connectivity/telecom/dialer-app/screen-calls
- https://developer.android.com/develop/ui/compose/notifications/channels

## 1. Objetivo do produto

Criar um aplicativo Android nativo que impeça chamadas telefônicas de números desconhecidos de interromperem o usuário.

O comportamento padrão deve ser:

- Contatos salvos no Android continuam ligando normalmente.
- Números que não estão na agenda são bloqueados antes de tocar.
- Números privados, restritos ou sem identificação são bloqueados.
- A interface nativa de chamada não deve aparecer.
- Não deve haver som, vibração, pop-up ou heads-up.
- A notificação nativa de chamada perdida deve ser suprimida.
- A chamada não deve aparecer no histórico nativo, por padrão.
- O aplicativo pode apresentar uma notificação própria e silenciosa, mas somente se o usuário habilitar essa opção.
- Todo o processamento deve acontecer localmente.
- O aplicativo deve continuar funcionando sem internet.
- Não deve existir servidor, login, analytics, publicidade ou telemetria no MVP.

O aplicativo não precisa identificar o nome de quem está ligando. Sua função principal é:

"Se não está nos contatos nem na whitelist pessoal, não interrompe o usuário."

## 2. Plataforma e stack

Utilize:

- Kotlin.
- Jetpack Compose.
- Material 3.
- Gradle Kotlin DSL.
- Version Catalog.
- Android 10/API 29 como minSdk, salvo justificativa técnica melhor.
- targetSdk e compileSdk estáveis e atuais.
- Coroutines e Flow.
- Room somente se realmente necessário para histórico e whitelist.
- DataStore ou armazenamento local adequado para configurações.
- Android Telecom Framework.
- CallScreeningService.
- RoleManager.ROLE_CALL_SCREENING.
- NotificationChannel.
- Testes unitários, instrumentados e de integração.

Evite frameworks desnecessários que aumentem cold start ou complexidade. O CallScreeningService deve possuir inicialização mínima e previsível.

Centralize o nome do aplicativo, applicationId, cores, strings e configurações para permitir rebranding posterior.

## 3. Restrições obrigatórias

Não:

- Substituir o discador padrão.
- Usar AccessibilityService.
- Criar overlays para esconder a tela de chamada.
- Usar hacks dependentes da Samsung.
- Solicitar READ_CALL_LOG.
- Solicitar READ_PHONE_STATE sem necessidade comprovada.
- Solicitar READ_CONTACTS no MVP.
- Solicitar READ_SMS.
- Solicitar permissões além do mínimo necessário.
- Realizar chamadas de rede durante a decisão de bloqueio.
- Bloquear chamadas realizadas pelo usuário.
- Interferir em chamadas do WhatsApp, Telegram ou outros aplicativos VoIP.
- Gravar chamadas.
- Armazenar números completos em logs técnicos.
- Depender de processo permanentemente executando em foreground.

A filtragem deve utilizar exclusivamente as APIs oficiais do Android.

## 4. CallScreeningService

Implemente corretamente um CallScreeningService registrado no AndroidManifest com:

- android.permission.BIND_SCREENING_SERVICE.
- Intent filter android.telecom.CallScreeningService.
- Configuração correta de exported conforme a API atual.
- Solicitação explícita do papel ROLE_CALL_SCREENING.
- Verificação contínua de que o aplicativo ainda ocupa esse papel.

O CallScreeningService deve:

1. Verificar se a chamada é recebida.
2. Obter e normalizar o número quando disponível.
3. Tratar corretamente números privados ou sem handle.
4. Consultar somente dados locais.
5. Passar a chamada pelo CallDecisionEngine.
6. Chamar respondToCall exatamente uma vez.
7. Responder muito antes do limite de cinco segundos.
8. Criar uma eventual notificação somente depois de responder ao sistema.

Para bloqueio total, avaliar e usar corretamente:

- setDisallowCall(true)
- setRejectCall(true)
- setSkipNotification(true)
- setSkipCallLog(configuração do usuário)

Se o usuário escolher "encaminhar silenciosamente", utilizar o comportamento oficial correspondente, sem permitir que a tela da chamada apareça.

Crie proteção contra:

- Respostas duplicadas.
- Exceções durante a normalização.
- Banco local temporariamente indisponível.
- Inicialização fria do processo.
- Corridas entre alteração de configuração e chegada de chamada.
- Timeout.
- Handle nulo.
- Números inválidos.
- Chamadas de saída recebidas pelo serviço.
- Dual SIM.
- Processo recriado pelo Android.

A decisão não pode depender de uma consulta lenta. Estabeleça e valide orçamento de desempenho, buscando p95 inferior a 200 ms em cold path local e tempo muito menor em warm path.

## 5. Motor de decisão

Crie um CallDecisionEngine puro, determinístico e coberto por testes.

Precedência sugerida:

1. Chamada de saída → não interferir.
2. Proteção desabilitada → permitir.
3. Número privado/oculto → aplicar configuração específica, com bloqueio como padrão.
4. Número presente na whitelist pessoal → permitir.
5. Número fora da agenda recebido pelo CallScreeningService → bloquear.
6. Erro inesperado → aplicar a política segura explicitamente configurada.

Modele o resultado como objeto de domínio, por exemplo:

- Allow
- Reject
- SendSilentlyToVoicemail
- BlockWithoutTrace

Inclua reason codes internos, sem expor dados pessoais:

- outgoing_call
- protection_disabled
- private_number
- personal_whitelist
- unknown_number
- invalid_number
- local_lookup_failure
- fallback_policy

Não espalhe condições pelo Service. Toda a regra deve permanecer no motor de decisão.

## 6. Whitelist pessoal local

Inclua uma whitelist local para clientes ou números legítimos que ainda não estejam na agenda.

Permitir:

- Adicionar número manualmente.
- Informar país e DDI.
- Normalizar para E.164.
- Editar descrição local opcional.
- Ativar ou desativar uma entrada.
- Excluir entrada.
- Pesquisar.
- Detectar duplicidade.
- Exportar backup local.
- Importar backup criado pelo próprio aplicativo.
- Confirmar antes de sobrescrever dados.

Considere corretamente o padrão brasileiro:

- DDI +55.
- DDD obrigatório quando aplicável.
- Celulares com nove dígitos.
- Números fixos.
- Formatação apenas visual, mantendo E.164 como fonte de verdade.

Use uma biblioteca consolidada de normalização, como libphonenumber, se ela for compatível com a arquitetura atual. Não crie normalização internacional improvisada.

## 7. Histórico interno opcional

Criar histórico próprio somente se habilitado.

Registrar o mínimo necessário:

- ID local.
- Número normalizado ou representação protegida.
- Número mascarado para exibição.
- Data/hora em UTC.
- Motivo da decisão.
- Ação tomada.
- Se uma notificação foi enviada.
- SIM utilizada, somente se disponível sem permissão invasiva.
- Status posterior: sem classificação, legítimo ou indesejado.

Configurações de retenção:

- Não guardar.
- 7 dias.
- 30 dias.
- 90 dias.
- Até exclusão manual.

Adicionar:

- Limpar histórico.
- Excluir registro individual.
- Adicionar número à whitelist a partir do histórico.
- Marcar localmente como indesejado.
- Nunca armazenar nome de contato desnecessariamente.
- Excluir o banco sensível de backups automáticos do Android ou proteger corretamente o backup.

## 8. Notificações

A notificação própria deve ser opcional e desabilitada por padrão.

Quando habilitada:

- Solicitar POST_NOTIFICATIONS somente quando necessário.
- Criar canal específico "Chamadas bloqueadas".
- IMPORTANCE_LOW ou configuração equivalente não intrusiva.
- Sem som.
- Sem vibração.
- Sem heads-up.
- Sem full-screen intent.
- Sem aparecer sobre outros aplicativos.
- Não mostrar o número completo na tela bloqueada.
- Permitir escolher entre número mascarado ou nenhuma identificação.
- Abrir o registro interno correspondente ao tocar.

A aplicação deve sempre usar setSkipNotification(true) na resposta da chamada bloqueada para evitar duplicidade com a notificação nativa.

## 9. Telas do MVP

Implementar pelo menos:

### Onboarding

- Explicação simples do funcionamento.
- Explicação de que somente chamadas telefônicas são filtradas.
- Solicitação do papel de filtro de chamadas.
- Solicitação de notificação somente se o usuário optar.
- Verificação final de configuração.

### Tela inicial

- Status: Proteção ativa ou inativa.
- Status: Aplicativo definido ou não como filtro padrão.
- Botão para corrigir configuração.
- Quantidade de chamadas bloqueadas localmente.
- Última chamada bloqueada, respeitando privacidade.
- Atalho para whitelist e histórico.

### Proteção

- Ativar ou desativar proteção.
- Bloquear números desconhecidos.
- Bloquear números privados.
- Rejeitar imediatamente ou encaminhar silenciosamente.
- Ocultar do histórico nativo.
- Exibir notificação silenciosa.
- Política de fallback em caso de erro.
- Explicação clara de cada opção.

### Whitelist pessoal

- Listagem.
- Busca.
- Cadastro.
- Edição.
- Exclusão.
- Importação e exportação.

### Histórico

- Filtros por período e decisão.
- Marcar legítimo.
- Adicionar à whitelist.
- Marcar indesejado.
- Excluir e limpar.

### Privacidade e sobre

- Dados armazenados localmente.
- Permissões utilizadas.
- Retenção.
- Limpar todos os dados.
- Versão do app.
- Limitações conhecidas.
- Link para abrir as configurações do aplicativo e do canal de notificação.

Utilize strings em resources, começando por pt-BR, sem textos hardcoded em Kotlin.

## 10. UX e acessibilidade

- Interface simples e evidente.
- Dark mode.
- Dynamic Color quando disponível.
- Acessibilidade para TalkBack.
- Tamanhos de toque adequados.
- Contraste correto.
- Estados de carregamento e erro.
- Nenhuma propaganda.
- Nenhum dark pattern.
- Explicar claramente quando a proteção está desativada.
- Não afirmar que o aplicativo protege chamadas do WhatsApp.
- Não afirmar que o bloqueio é "100% garantido" sem validar o comportamento no aparelho.

## 11. Segurança e privacidade

- Coletar o mínimo possível.
- Nenhuma comunicação de rede.
- Nenhuma telemetria.
- Nenhuma chave ou segredo.
- Mascarar números em logs.
- Remover logs sensíveis de release.
- Validar inputs de importação.
- Limitar tamanho dos arquivos importados.
- Prevenir path traversal e arquivos malformados.
- Não incluir números e histórico no backup automático sem proteção.
- Configurar R8/ProGuard para release.
- Garantir que componentes não necessários não sejam exportados.

Crie uma política de privacidade curta e verdadeira, explicando que os dados permanecem no aparelho nesta etapa.

## 12. Arquitetura esperada

Separar, sem overengineering:

- telecom/
- domain/
- data/local/
- ui/
- notifications/
- phone/
- settings/

Componentes principais:

- UnknownCallScreeningService
- CallDecisionEngine
- CallDecision
- ScreeningSettings
- SettingsRepository
- PersonalWhitelistRepository
- BlockedCallRepository
- PhoneNumberNormalizer
- BlockedCallNotifier
- ScreeningRoleManager

O Service deve ser uma camada fina. Compose não pode conhecer Telecom diretamente. A infraestrutura não pode conter regra de negócio espalhada.

Prepare interfaces que permitam adicionar uma fonte remota de whitelist no futuro, mas não implemente servidor nem rede agora.

## 13. Testes obrigatórios

Criar testes para:

- Número desconhecido.
- Número privado.
- Número inválido.
- Número presente na whitelist.
- Proteção desabilitada.
- Chamada de saída.
- Reject.
- Envio silencioso para voicemail.
- Ocultar ou manter no call log.
- Notificação habilitada e desabilitada.
- Falha no repositório local.
- Timeout.
- Resposta única.
- Normalização brasileira e internacional.
- Importação duplicada.
- Arquivo de backup inválido.
- Retenção e limpeza do histórico.
- Mudança do papel de Call Screening.
- Processo iniciado a frio.

Executar:

- Unit tests.
- Instrumented tests possíveis no ambiente.
- Lint.
- Detekt ou ferramenta equivalente.
- Build debug.
- Build release.
- Testes de migração do Room, se utilizado.

## 14. Validação em aparelho Samsung

Preparar um roteiro reproduzível de validação física para Samsung Galaxy:

- Tela ligada e desbloqueada.
- Tela bloqueada.
- Aplicativo encerrado.
- Processo removido da memória.
- Modo economia de bateria.
- Dual SIM.
- Ligação de contato.
- Ligação de número fora da agenda.
- Ligação privada.
- Notificação habilitada.
- Notificação desabilitada.
- Histórico nativo habilitado e oculto.
- Wi-Fi Calling, se disponível.
- Caixa postal disponível e indisponível.

Registrar qualquer comportamento específico de OEM encontrado. Não implementar hacks antes de provar que são necessários.

## 15. Documentação e entregáveis

Entregar:

- Código completo.
- README com setup, build e instalação.
- APK debug instalável.
- Configuração de release documentada.
- Arquitetura explicada.
- Matriz de permissões e justificativas.
- Política de privacidade local.
- Roteiro de teste físico.
- Limitações conhecidas.
- Decisões arquiteturais importantes.
- Backlog curto da próxima etapa com Supabase, sem implementá-la.

## 16. Critérios de aceite

O MVP somente estará concluído quando:

1. Compilar sem erros.
2. Lint e testes passarem.
3. Solicitar corretamente o papel de filtro de chamadas.
4. Bloquear número desconhecido antes de tocar.
5. Não apresentar tela de chamada para o número bloqueado.
6. Não gerar notificação nativa de chamada perdida.
7. Não gerar notificação própria quando desabilitada.
8. Gerar somente notificação silenciosa quando habilitada.
9. Permitir chamadas da whitelist pessoal.
10. Não afetar chamadas de saída.
11. Funcionar sem internet.
12. Não solicitar permissões desnecessárias.
13. Ter todo o comportamento validado ou documentado quando depender de aparelho físico.

Ao terminar, apresente:

- Resumo do que foi implementado.
- Arquivos principais.
- Comandos executados.
- Resultado dos testes.
- APK gerado.
- Pendências que dependem exclusivamente de teste físico.
- Riscos restantes reais, sem inventar pendências genéricas.
