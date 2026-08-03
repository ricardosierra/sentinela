---
gsd_state_version: 1.0
milestone: v0.1
milestone_name: milestone
status: unknown
stopped_at: Completed 07-11-PLAN.md
last_updated: "2026-08-03T01:51:00.000Z"
last_activity: 2026-08-03
progress:
  total_phases: 9
  completed_phases: 7
  total_plans: 47
  completed_plans: 47
---

# Project State

## Project Reference

See: `.planning/PROJECT.md` (updated 2026-07-28)
**Core value:** "Se não está nos contatos nem na whitelist pessoal, não interrompe o usuário."
**Current focus:** Phase 08 — UI Whitelist e Historico
Last activity: 2026-08-03

## Current Position

Phase: 08 (UI Whitelist e Historico) — NOT STARTED
Plan: 0 of ?

## Snapshot

- **Esqueleto:** Gradle KTS + catalog, AGP 9.3.0 (Kotlin embutido), Compose BOM 2026.06.01, compileSdk 37 / minSdk 29
- **Domínio:** `CallDecisionEngine` com precedência saída→proteção→privado→contato→whitelist→falha→desconhecido e políticas por origem (`OriginPolicy`)
- **Normalização:** `LibPhoneNumberNormalizer` + `PhoneMask` + cascata de região, ligados em `AppContainer.phoneNumberNormalizer` (util construído 1x, fora do caminho quente)
- **Dados locais:** Room v1 (`whitelist` + `blocked_call`, schema exportado) e DataStore Preferences, ambos como instância única no `AppContainer`; retenção de histórico em 5 políticas, podada na abertura do app
- **Contatos:** `ContactLookupRepository` real ligado no `AppContainer` (singleton preguiçoso, sonda dupla + cache invalidado por observer); nada de identidade de contato toca disco — Bloco 6 de `verify-invariants.sh`
- **Qualidade:** 618 testes JVM (era 417 na Fase 5) + 53 instrumentados; cobertura 97,6351% sobre domain+phone+data+settings+telecom+notifications+permissions com gate `koverVerify` em 80%; lint e detekt zerados; `verify-invariants.sh` com 7 blocos (Bloco 7 trava a regra de decisao dentro do motor); evidencia pos-limpeza em `05-EVIDENCE.md`
- **Modo discador (elegibilidade PRONTA):** manifest com o servico de chamada exportado/protegido, meta-dado de substituicao da interface e os DOIS filtros de discagem; quatro permissoes novas autorizadas na matriz e no script; `SentinelaInCallService` fino + `CallSessionStore` (instancia unica no container) + `TelecomCallControls`; `DialerRoleManager` sobre `SystemRoleGate` e `DialerModeState` puro; `CallActivity`/`DialerActivity` hospedeiras reais; Bloco 8 com 4 checagens
- **Modo discador (nucleo):** nucleo puro em `telecom/call/` — `CallUiState`/`CallSnapshot`, `PlatformCallStateMapper` (12 codigos + ramo final visivel), `CallControls` e `CallSessionCoordinator` com prazo de apresentacao de 2 s e falha ALTA (excecao propaga, zero captura)
- **Telecom:** `UnknownCallScreeningService` LIGADO — delega ao `ScreeningCoordinator` pelo contrato `ScreeningDependencies`, responde uma unica vez e dispara historico/notificacao depois da resposta
- **UI (Fase 6):** tema com `numberXl`/`numberLg`/`timer` (figuras de largura fixa), formas 8/16/24/pilula e as QUATRO cores funcionais da chamada por literal fora do esquema; 74 strings pt-BR das telas de chamada/discagem/ativacao varridas contra promessa desonesta; nove componentes reutilizaveis com alvo >= 48dp e descricao de conteudo em recurso
- **UI (telas de chamada, 06-04):** `IncomingCallScreen` (quatro identidades, ordem de foco declarada, paisagem em duas colunas), `OutgoingCallScreen` (tres pontos em fade, suprimido com reducao de movimento), `ActiveCallScreen` (cronometro congelavel + ramo do estado nao suportado com encerrar habilitado), `DtmfKeypadSheet` e `AudioRouteSheet` ancorados ao rodape; `CallActivity` com `when` exaustivo, confirmacao de apresentacao, gesto de voltar consumido e fechamento apos 1200 ms; contrato do extra de acao da notificacao (`EXTRA_CALL_ACTION` + tres valores) vive em `ui/call/CallActivity.kt`; 20 casos novos e 23 pre-visualizacoes
- **UI (discagem e ativacao, 06-05):** `DialpadScreen` completa (campo somente saida com formatacao progressiva da propria biblioteca e forma nacional quando o numero fica valido, apagar invisivel que ocupa espaco, barra de falha que mantem o numero) e `DialerActivationScreen` com os cinco ramos de estado, cards de custo e beneficio com estilo identico e reversao pelo seletor do sistema; `OutgoingCallPlacer` origina pelo gerenciador de telecomunicacoes com resultados nomeados; `CallPhonePermissionChecker` em `platform/` e `call_phone_permission_asked` no DataStore; 27 casos novos
- **Modo discador (provado no aparelho virtual, 06-07):** 27 casos instrumentados novos (suite 53 -> 80) cobrindo vinculo real do servico de chamada, elegibilidade ao papel pelo comando que a VERIFICA, politica por contato valendo de fato com o motor intocado, independencia dos dois papeis e chamada de saida real; `scripts/verify-dialer-lifecycle.sh` prova de FORA do processo o ciclo completo do papel e a sobrevivencia da chamada a morte do processo
- **Fechamento da Fase 6 (06-08):** documentos sem afirmacao nao medida — item de numero privado de volta a NAO VERIFICADO com o cenario 59 como veredito, item do historico do telefone reconfirmado em execucao com o papel ativo, item novo do encerramento do processo ao perder papel, e o escopo do modo discador (uma chamada por vez, sem video, emergencia sempre nativa); secao 11 de `docs/design/TELAS.md` de esboco de 6 linhas a contrato de 121; roteiro fisico com 60 cenarios (23-30 revisados no lugar, 52-60 novos) e as tres questoes abertas de fabricante; cobertura 96,648% com dois excludes por nome de classe e gate visto vermelho (o exclude da costura caiu no 06-09: 96,69% com a costura em 100%); `06-EVIDENCE.md` pos-clean sem cache (603 JVM + 80 instrumentados + ciclo de vida do papel)
- **UI (navegacao e apoio de teste, 07-02):** `Rotas` com as dez rotas por TEXTO da fase (boas-vindas, seis passos, home, protecao, modo discador) e `NavGraphContractTest` que COMPOE o `NavHost` real e navega — 6 casos, contagem de destinos travada em dez; os tres asserts de dois eixos extraidos para `org.sentinela.app.ui.TouchTargetAsserts`, sem copia, com a suite da Fase 6 em 14 casos antes e depois
- **UI (fundacao de texto e cor, 07-01):** `StatusAttention`/`OnStatusAttention`/`StatusBlocked` como literais fora do Dynamic Color (apelidos semanticos dos tokens destrutivos, igualdade afirmada por teste; ativo reusa `CallAccept`), travados em JVM pura e fixados nos tres esquemas; 44 chaves pt-BR novas da fase — 269 `<string name=` mais o plural real de `settings_clear_history_confirm`; `Phase7StringsTest` com 11 casos varrendo as chaves de nove prefixos contra seis grupos de expressao proibida, sempre por `Context.getString`; os dois `PluralsCandidate` do lint eliminados, incluindo o porcento cru de `dialer_activation_unchanged_4` (corrigido por `formatted="false"`, com o texto visivel intacto)
- **UI (componentes compartilhados, 07-03):** seis componentes em `ui/components/` — `OptionCard` (linha inteira como alvo unico com papel de botao de radio, 72dp, descricao permanente, selo, desabilitado com motivo no PROPRIO no), `StepHeader` (contador por recurso + barra decorativa), `SentinelaTopBar` (marca vinda do recurso, dois tipos de acao com alvo exigido de 48dp), `SettingSwitchRow` (tres nos, zero mesclagem no arquivo), `CheckRow` (estado por icone E texto, acao em no focavel separado) e `SentinelaBottomBar` (quatro destinos, `Role.Tab`, item de 56dp por `requiredHeight`, os dois da Phase 8 desabilitados com motivo textual); `Phase7ComponentSemanticsTest` com 17 casos sob qualificadores de tela reais e tres provas de vermelho, uma delas corrigindo o entendimento da semantica mesclada
- **UI (donos de estado, 07-04):** `HomeViewModel`, `OnboardingViewModel` e `SettingsViewModel` — colaboradores por parametro, `AppContainer` fora inclusive das fabricas (montagem fica na rota), consultas de papel injetadas como FUNCOES para o teste conta-las; `StatValue` fechado em `Loaded`/`Unavailable`/`Loading` torna o zero mentiroso impossivel por assinatura e `LastBlockedUi` carrega so o texto mascarado; falha de leitura do historico vira estado visivel em vez de propagar; chave `onboarding_completed` fora de `ScreeningSettings`; tela Protecao com uma funcao por item e ZERO funcao de salvar (ausencia travada por reflexao), retencao "nao guardar" gravando e podando na mesma corotina; 42 casos novos (suite JVM 698) com seis provas de vermelho — cobertura 96,6157%
- **UI (boas-vindas e dois primeiros passos, 07-05):** `WelcomeScreen` fiel ao layout do mockup e honesta no texto — tres cartoes locais, selo de codigo aberto no lugar do selo de protecao ativa, zero base global de numeros, zero endereco remoto e zero progresso falso, com as tres adaptacoes em KDoc apontando o registro pos-lancamento; `RoleStepScreen` carrega o AVISO OBRIGATORIO de escopo em cartao de peso visual igual, com as tres frases das Fases 5 e 6 por identificador de recurso, e os tres ramos do papel sem travar nem repetir o dialogo (concedido mostra chip de ativo e exige toque; negado avanca com aviso e acao); `UnknownPolicyStepScreen` com o cartao central flutuante, tres opcoes em grupo de escolha unica e bloquear como padrao, sem a politica que nunca silencia e sem o estilo do bloqueio; duracao da transicao de passo publicada para o envelope de navegacao; 18 casos de composicao e tres provas de vermelho, a terceira medida nas duas direcoes
- **UI (passos 3 e 4 do onboarding, 07-06):** `ContactsPolicyStepScreen` com os QUATRO ramos da permissao da agenda por `when` exaustivo (justificativa + pedido, chip de concedido, aviso da consequencia honesta com pedido, aviso de bloqueio com atalho e SEM pedido), quatro politicas sempre editaveis e o interruptor de privados ligado por padrao; `WhitelistPolicyStepScreen` com Nunca Silenciar selado como padrao, cartao tonal no lugar da imagem remota, hint permanente e botao Proximo; `ContactsAndWhitelistStepTest` com 23 casos e tres provas de vermelho; zero chave nova no `strings.xml`
- **UI (home, 07-08):** `StatusHeroCard` com interruptor de PREFERENCIA (papel do sistema e somente-leitura no aviso, e o botao de correcao DESAPARECE quando o aparelho nao oferece o papel), cores de significado por literal e zero mesclagem de descendentes no arquivo; `StatCard` com `when` exaustivo sobre `StatValue` e nenhum parametro numerico na assinatura; `LastBlockedCard` recebendo o numero JA mascarado e sem rotulo de risco; `QuickActionRow` de 72dp exigidos; `HomeScreen` com os OITO estados degradados, precedencia de avisos, teto de dois com excedente levando a Protecao, rolagem total e ordem de travessia declarada por bloco; `relativeTimeLabel` por plurais reais; 29 casos novos (24 + 5) e quatro provas de vermelho, com o zero proibido e a fronteira do numero provados por VARREDURA das duas arvores semanticas; 6 chaves + 2 plurais novos (269 -> 275 `<string name=`)
- **UI (a fiacao, 07-10):** `SentinelaNavHost` com os DEZ destinos por texto escritos um por um (contagem travada por teste) e a transicao de passo de 250 ms suprimida por reducao de movimento — a pendencia que 07-05 deixou; cinco camadas de rota fininhas sao as UNICAS a conhecer o container (`WelcomeRoute`, `OnboardingRoute`, `HomeRoute`, `SettingsRoute`, `DialerActivationRoute`), com o papel reconsultado na retomada de cada uma e o retorno do seletor do sistema servindo so de redundancia; `PassoDoOnboarding` + `AcoesDoPasso` separam o desvio de passo da fiacao para o teste compor producao sem container; **a pendencia de 06-05 fechou** — a tela de ativacao do modo discador tem ponto de entrada pela tela Protecao e pelo aviso da home, sem uma linha alterada nela; `MainActivity` hospeda o grafo com o destino inicial resolvido por `produceState` e espera anunciada, mantendo a guarda de estado salvo da contagem de abertura; `HomeViewModel` ganhou os quatro comandos que a tela exigia (religar o historico mexe nas DUAS configuracoes que o desligam, e tentar de novo REINSCREVE o fluxo); Bloco 9 do script com texto embutido, fronteira do numero e repositorio da agenda, com cinco provas de vermelho incluindo a de nao auto-sabotagem; 21 casos novos (suite JVM 845) — cobertura 96,6157%
- **Git:** repo local sem remote; branch `master`
- **Última tag git:** nenhuma (primeira release será `v0.1.0`)

## Decisions

- [Adendos 2026-07-28]: **Dois modos de operação** — filtro (padrão, permissão mínima) e discador (opcional, `ROLE_DIALER` + `InCallService`, habilita políticas por contato). Substituir o discador nativo agora É escopo do MVP
- [Adendos 2026-07-28]: **READ_CONTACTS entra no MVP** — uso exclusivamente local/em memória; nomes nunca persistidos nem enviados
- [Adendos 2026-07-28]: Políticas por origem no motor (contatos: Tocar padrão; whitelist: Nunca Silenciar padrão; desconhecidos: Bloquear padrão) — espelham os mockups
- [Adendos 2026-07-28]: Convite de avaliação/apoio na 5ª abertura, repetindo a cada 5 (10ª, 15ª…) até aceite; seção "Apoie" com open source em destaque + doação Bitcoin
- [Adendos 2026-07-28]: Offline-first permanente — MVP sem INTERNET; sync (v0.2.0) opt-in/assíncrona, inclui envio opcional da lista de números recebidos
- [Adendos 2026-07-28]: Nome antigo dos mockups eliminado de todos os arquivos (docs + HTMLs); branding único Sentinela
- ~~[Bootstrap 2026-07-27]: Bloqueio de desconhecidos no modo filtro apoiado no contrato da plataforma (onScreenCall só recebe não-contatos sem discador padrão)~~ **SUPERADA POR MEDICAO 2026-07-29 (Phase 05, pesquisa na fonte do Android):** a triagem entrega **tambem contatos** enquanto a leitura da agenda estiver concedida; o sistema so dispensa a triagem de quem esta na agenda quando o app nao pode consulta-la. A decisao sobre contato e nossa desde a Fase 4 — a consulta a agenda virou obrigatoria no caminho quente
- [Bootstrap 2026-07-27]: DI manual, sem Hilt/Koin — cold start do Service é orçamento crítico
- [Bootstrap 2026-07-27]: AGP 9 tem Kotlin embutido — plugin `org.jetbrains.kotlin.android` NÃO deve ser aplicado (erro se aplicar)
- [Bootstrap 2026-07-27]: Links GitHub no CHANGELOG usam `ricardosierra/sentinela` como placeholder até o remote existir
- [Phase 01-fundacao-compilavel]: Politica de lint declarada no app/build.gradle.kts (sem lint-baseline); ObsoleteSdkInt corrigido de verdade renomeando res/mipmap-anydpi-v26 para res/mipmap
- [Phase 01]: Permissoes verificadas por allowlist sobre o manifest MERGEADO (scripts/verify-invariants.sh); Phase 4/6 devem atualizar docs/PERMISSOES.md e a allowlist no mesmo commit
- [Phase 01]: DarkColors passou a internal para permitir ThemeTokensTest em JVM pura (sem Robolectric)
- [Phase 01]: Evidencia de build so vale com --no-build-cache alem do clean: FROM-CACHE tem o mesmo defeito probatorio que UP-TO-DATE
- [Phase 01]: POST_NOTIFICATIONS permanece declarada no manifest — docs/PERMISSOES.md e fonte canonica; pedido em runtime fica na Fase 5
- [Phase 02]: Kover 0.9.9 mede domain+phone desde 02-01; gate koverVerify (minBound 80) ligado em 02-05
- [Phase 02]: TestMetadata carrega metadados reais do libphonenumber em JVM pura via android_merged_assets — sem Robolectric, sem createInstance(Context)
- [Phase 02]: MaxMetaspaceSize=1g e obrigatorio com o plugin Kover (512m mata o build)
- [Phase 02]: Matriz de decisao coberta por teste parametrizado (48 casos) com tabela esperada escrita a mao — nao derivada do motor
- [Phase 02]: DecisionReason travado em 9 entradas por teste: reason code novo exige revisao de privacidade
- [Phase 02]: ContactLookup.UNAVAILABLE + WhitelistLookup.HIT: a whitelist vence (o if de falha vem depois) — comportamento agora contratual
- [Phase 02]: Cascata de regiao: aparelho (SIM/rede) -> preferencia do usuario -> BR; nunca travar em BR
- [Phase 02]: TelephonyManager isolado em platform/AndroidRegionProvider; phone/ segue sem import android.*
- [Phase 02]: Chave persistida = E.164, exceto codigo curto (< LIMIAR_CURTO=6 digitos), que e digito cru — contrato para a Fase 3
- [Phase 02]: 9o digito BR corrigido a mao e so aceito com revalidacao isValidNumber && type == MOBILE; senao Invalid(nono_digito_nao_revalida)
- [Phase 02]: normalize passou a receber region: String? = null (delega ao RegionProvider); defaultRegion=BR removido
- [Phase 02]: Mascara unica PhoneMask para log e UI, generalizada por getLengthOfNationalDestinationCode e sempre dentro de runCatching
- [Phase 02]: PhoneNumberUtil construido uma unica vez por lazy no AppContainer; nunca dentro de onScreenCall (p95 < 200 ms)
- [Phase 02]: Gate koverVerify minBound(80) ATIVO sobre domain+phone (atual 97,619%): codigo novo nesses pacotes exige teste
- [Phase 02]: Gate so e aceito depois de demonstrado falhando (bound temporario em 99 quebrou o build); Phase 2 nao deixou pendencia fisica nova
- [Phase 03]: Backup: path="." explicito em todos os <exclude> e datastore sem barra final; -wal/-shm cobertos pela exclusao recursiva de diretorio
- [Phase 03]: BackupRulesTest le os XMLs por DocumentBuilderFactory (nunca regex) e trava zero <include>; falha demonstrada removendo um exclude
- [Phase 03]: connectedDebugAndroidTest automatizado por scripts/run-instrumented-tests.sh: boot headless provado por sys.boot_completed + trap de emu kill
- [Phase 03]: connectedDebugAndroidTest nao aceita --tests (nao e Test task); filtro real e -Pandroid.testInstrumentationRunnerArguments.tests_regex
- [Phase 03]: Script apaga TEST-*.xml antes de rodar: relatorio antigo tem o mesmo defeito probatorio de UP-TO-DATE
- [Phase 03]: Migrations.kt descreve a migracao destrutiva em vez de nomea-la: o invariante casa ate em comentario, porque linha comentada vira linha ativa
- [Phase 03]: schemas/ e input das Test tasks: arquivo lido por teste e nao declarado como input deixa o cache falsificar o verde
- [Phase 03]: Enum persistido por code/name; leitura tolerante cai em UNKNOWN_NUMBER/UNCLASSIFIED em vez de lancar
- [Phase 03]: Retencao do historico persistida por id textual (never/7d/30d/90d/manual), nunca pela posicao da constante
- [Phase 03]: NEVER_STORE e MANUAL tem cutoff nulo; quem os distingue e shouldStore (um nao grava, o outro nunca poda)
- [Phase 03]: Poda do historico com corte ESTRITO (< cutoff): registro exatamente no limite sobrevive, travado por assert instrumentado
- [Phase 03]: Poda roda apos cada gravacao e na abertura do app; nenhum agendador em segundo plano no MVP
- [Phase 03]: Repositorio do historico propaga excecao do DAO: quem decide degradar e o Service da Fase 5
- [Phase 03]: Relogio injetado (clock: () -> Long) em toda regra dependente de tempo
- [Phase 03]: Janela de retencao virou constante nomeada em vez de afrouxar MagicNumber no detekt.yml compartilhado
- [Phase 03]: Whitelist: dedup resolve o id por findByKey ANTES do @Upsert — @Upsert com id 0 em chave com indice unico e no-op silencioso (provado por teste), nao excecao
- [Phase 03]: Indice so e provado por EXPLAIN QUERY PLAN: sem o indice o EQP fica vermelho e o teste de tempo continua VERDE (p95 4,21 ms com full scan) — medido
- [Phase 03]: contains() no caminho quente: p50 estavel 0,19-0,23 ms; p95 no emulador oscila 0,8-5,9 ms e mede o scheduler do host tanto quanto o SQLite
- [Phase 03]: Contador de aberturas vive no DataStore junto das configuracoes; nenhuma tabela Room para ele
- [Phase 03]: snapshot() servido de cache @Volatile aquecido por collector: disco so na primeira leitura (10,9 ms medidos)
- [Phase 03]: DataStore recebido por construtor, nunca por delegate de Context: duas instancias sobre o mesmo arquivo derrubam o processo
- [Phase 03]: @get:Rule combinado com @JvmField desliga a rule do JUnit (TemporaryFolder nunca criada)
- [Phase 03]: p95 da whitelist saiu do assert do emulador e virou cenario 35 da validacao fisica (Phase 9); o numero de 5 ms NAO foi afrouxado — decisao do usuario
- [Phase 03]: p50 < 1 ms e o EXPLAIN QUERY PLAN continuam quebrando o build: sinal estavel e prova estrutural ficam no CI
- [Phase 03]: Kover mede data.* e settings.* excluindo o gerado pelo Room (data.local.db.*, *_Impl, annotatedBy Dao/Database): so roda instrumentado e daria falso-vermelho
- [Phase 03]: Resolucao consistente do AGP: piso de dependencia so-de-teste precisa ser declarado no runtime PRINCIPAL, senao o androidTest herda a versao antiga
- [Phase 03]: MigrationTestHelper(Instrumentation, Class<out RoomDatabase>) e a sobrecarga nao-deprecada do Room 2.8.4
- [Phase 03]: Instancia unica de DataStore e de banco no AppContainer; onCreate da Application nunca faz I/O sincrono (cold start do Service)
- [Phase 04]: READ_CONTACTS entra no manifest no MESMO commit das duas edicoes do verify-invariants.sh (ALLOWLIST + FUTURE)
- [Phase 04]: Gravacao na agenda proibida para sempre em manifest: testes usam adoptShellPermissionIdentity (declarar em androidTest nao funciona, a instrumentacao roda no uid do app)
- [Phase 04]: Flag contacts_permission_asked gravado ao disparar o launcher, nunca no callback: o usuario pode matar o app com o dialogo aberto
- [Phase 04]: Camada que toca ActivityCompat vive em platform/, fora dos pacotes medidos pelo Kover — evita falso-vermelho no gate sem excludes novo
- [Phase 04]: contacts_permission_asked fica fora de ScreeningSettings: nao e configuracao de triagem e nao pesa no snapshot do caminho quente
- [Phase 04]: Chave do cache de contatos vem do LibPhoneNumberNormalizer do app; a coluna normalizada do provider e proibida (medida nula e as vezes errada)
- [Phase 04]: Sonda dupla na agenda (E.164 + nacional): nenhuma das duas sozinha cobre a matriz medida do provider
- [Phase 04]: O cache de contatos se justifica por correcao de chave e corte de cauda, nao por velocidade (sonda direta ja da p50 ~2 ms com 5.000 contatos)
- [Phase 04]: Construcao do cache (1,5-1,8 s) nunca e aguardada: cache frio responde pela sonda direta
- [Phase 04]: Uso do cache provado por contador de consultas ao provider, jamais por cronometro; falsificado antes de aceito
- [Phase 04]: backgroundScope de runTest nao e despachado por advanceUntilIdle nas coroutines 1.11: testes de cache usam escopo proprio sobre o testScheduler
- [Phase 04]: Cobertura caiu para 87,69% porque ContactsContractLookupSource vive em data.* e so roda instrumentado; ajuste do filtro do Kover e do plano 04-05
- [Phase Phase 04]: Sonda dupla provada contra o provider real: com a segunda sonda desligada, o contato em formato nacional e o fixo BR ficam VERMELHOS
- [Phase Phase 04]: Debounce falsificado com DEBOUNCE_MS=0: os mesmos 11 callbacks viraram 3 reconstrucoes; com 750 ms, 1
- [Phase Phase 04]: Coalescencia do provider reconfirmada no nosso AVD: 11 callbacks para 10 transacoes — por isso o assert e sobre reconstrucoes, nunca sobre callbacks
- [Phase Phase 04]: Cache removido deixa o teste de TEMPO verde e so o contador vermelho — a licao da Phase 3 medida de novo na Fase 4
- [Phase Phase 04]: Lookup com 5.000 contatos: p50 0,029 ms (cache quente) e 0,39/0,92 ms (sonda direta); p95 e max so em logcat, veredito na Phase 9
- [Phase Phase 04]: Construcao do cache medida em 2,57 s no AVD (acima dos 1,5-1,8 s da pesquisa): confirma que ela jamais pode ser aguardada
- [Phase Phase 04]: Testes instrumentados de contatos fixam a regiao em BR: a cascata real leria o SIM us do AVD e o teste mediria o emulador
- [Phase 04]: Vazamento de contato para o banco e provado pelo schema EXPORTADO, lido nos VALORES de columnName: casar as CHAVES do JSON daria falso positivo em todo build
- [Phase 04]: Bloco 6.2 casa o USO do provider (import do pacote ou acesso a membro), nunca o nome da classe do app que o encapsula — senao o AppContainer nao poderia constru-la
- [Phase 04]: Exclude do Kover e UMA classe nomeada (ContactsContractLookupSource); cache, repositorio e estado de permissao seguem no denominador — cobertura 87,69% -> 96,68%
- [Phase 04]: contactLookupRepository e singleton preguicoso no AppContainer, com a fonte compartilhada com o cache (duas fontes registrariam dois observadores); nada em Application.onCreate
- [Phase 04]: max da sonda direta em MISS variou 30 ms -> 140 ms entre execucoes sem mudanca de codigo: a cauda so tem veredito em hardware real (cenario 37)
- [Phase 04]: Tela de onboarding de contatos e da Phase 7 por desenho, nao lacuna da Fase 4: esta fase entrega manifest, maquina de estado, flag e string
- [Phase 05]: Harness JVM do Service confirmado: buildService + Proxy de ICallScreeningAdapter no campo mCallScreeningAdapter captura cada resposta
- [Phase 05]: Robolectric fixado em sdk 35 (teto real do JDK 17); sdk 36 exige Java 21 e nao e usado em nenhum teste
- [Phase 05]: Classe de teste nao enxerga membros de outra classe de teste entre sandboxes de SDK do Robolectric: fixtures compartilhadas vivem em objeto neutro
- [Phase 05]: SendSilentlyToVoicemail e emitido identico a Reject: a API nao tem caixa postal e o destino e da operadora
- [Phase 05]: disallowCall + silenceCall e proibido no app apesar de legal na API: o Telecom avalia a recusa primeiro e o par seria enganoso
- [Phase 05]: SCR-12: janela de chamada repetida = 5 min em REPEATED_CALL_WINDOW_MILLIS; regra no motor, nivel 6 de 8, so faz TOCAR e nunca bloquear
- [Phase 05]: hasRecentBlock e a UNICA excecao ao contrato da Fase 3 de propagar excecao do DAO: falha vira LOOKUP_FAILED e a decisao segue a politica normal
- [Phase 05]: Corte da janela e INCLUSIVO (>=), espelho do corte estrito (<) da poda; nenhum indice novo em blocked_call para nao exigir migracao v2
- [Phase 05]: Contagem de abertura saiu de Application.onCreate e foi para MainActivity.onCreate com guarda savedInstanceState == null: start de processo do Telecom nao e abertura
- [Phase 05]: src/main/java declarado como input das Test tasks: teste estrutural que le fonte do disco ficaria UP-TO-DATE e daria falso verde
- [Phase 05]: Regra de permissao em runtime unificada em RuntimePermissionAsk; contactsPermissionState virou fachada que delega, sem tocar nos testes da Fase 4
- [Phase 05]: Notificacao propria segue DESLIGADA por padrao; POST_NOTIFICATIONS so e pedida em runtime no momento do opt-in, nunca no onboarding
- [Phase 05]: Privacidade da notificacao nao depende da visibilidade escolhida: o numero completo nunca entra no objeto, provado varrendo extras e versao publica
- [Phase 05]: AndroidBlockedCallNotifier nao recebe PhoneNumberUtil: a mascara ja vem pronta no registro e ler numberE164 ali e proibido por criterio
- [Phase 05]: Versao publica da notificacao usa sempre o texto anonimo, independente da configuracao de identificacao
- [Phase 05]: notification_permission_asked fica fora de ScreeningSettings e e marcado ao disparar o launcher, nunca no callback
- [Phase 05]: Criterio de aceite por grep nao distingue KDoc de codigo: comentario citando import android. ou IMPORTANCE_LOW derruba o proprio criterio
- [Phase 05]: 05-03: ScreeningCoordinator e puro (zero import da plataforma) e a costura de saida e uma funcao de dominio — a logica de verdade nao depende de Robolectric
- [Phase 05]: 05-03: guarda atomica LOCAL a cada triagem, nunca campo da classe: dual SIM tria duas chamadas ao mesmo tempo
- [Phase 05]: 05-03: falha de consulta sobe para a rede permissiva e a chamada PASSA; so o estouro de prazo passa pelo motor com resultados degradados e politica de reserva
- [Phase 05]: 05-03: decisao permissiva de emergencia reusa LOCAL_LOOKUP_FAILURE — reason code novo exigiria revisao de privacidade e quebraria a contagem travada em 10
- [Phase 05]: 05-03: CallDecisionEngine passou a open para permitir injetar defeito no proprio motor na matriz de falhas
- [Phase 05]: 05-03: as duas redes permissivas sao redundantes de proposito — removida so uma, zero testes vermelhos; removidas as duas, 7 de 11 ficam vermelhos
- [Phase 05]: 05-05: o Service pede colaboradores por um contrato pequeno (ScreeningDependencies) e nunca constroi container — e o que permite hospedar o Service real na JVM
- [Phase 05]: 05-05: a costura de resposta carrega a decisao E as configuracoes que a produziram; a traducao nao le o repositorio uma segunda vez
- [Phase 05]: 05-05: record() do historico devolve o id da linha (0 sem rastro): sem ele toda notificacao colidiria no mesmo identificador
- [Phase 05]: 05-05: guarda de chamada de saida e dupla (Service e coordenador) por desenho — remover a do Service nao deixa teste vermelho
- [Phase 05]: 05-05: nao existe aviso de mudanca do papel de triagem para aplicativo comum; a verificacao e pergunta pontual na retomada da tela (Fase 7)
- [Phase 05]: 05-06: nao responder em chamada de saida esta CORRETO — a classe base do AOSP envia sozinha uma resposta nula assim que onScreenCall retorna, e respondToCall e ignorado fora de DIRECTION_INCOMING
- [Phase 05]: 05-06: o invariante de resposta unica vale sobre chamadas de ENTRADA; a formulacao antiga (todos os caminhos) codificava regra falsa
- [Phase 05]: 05-06: Bloco 7 trava a regra de decisao no motor com 5 checagens; padroes de construcao/chamada exigem parenteses para nao punir mencao em prosa
- [Phase 05]: 05-06: comentario sobre o modo de abortar do shell passou a DESCREVER o literal (precedente do Migrations.kt): criterio por grep nao distingue comentario de codigo
- [Phase 05]: 05-06: caminho de decisao p50 medido 28,7 / 15,5 / 0,79 ms conforme o aquecimento do processo; assert unico em 50 ms, p95 e max so em logcat, veredito na Phase 9
- [Phase 05]: 05-06: teste de tempo carrega assert estrutural sobre a decisao medida — quebradas juntas, a sabotagem de tempo e a de direcao se anulam
- [Phase 05]: 05-07: ocultar a chamada bloqueada do historico do telefone e INATINGIVEL — o Android so honra o pedido para app de operadora; o papel de discador da Fase 6 NAO destrava. SCR-07 fica WONT FIX e a UI diz a verdade
- [Phase 05]: 05-07: o modo nao perturbar NAO e contornavel (filtro paralelo, bypass exigiria permissao proibida); 'Nunca Silenciar' descreve o que o Sentinela faz, nunca o que o sistema faz — zero codigo, so texto
- [Phase 05]: 05-07: SCR-04 (numero oculto) e PARCIAL — nunca entregue no modo filtro, vale no modo discador (Fase 6); codigo e configuracao mantidos, limitacao documentada
- [Phase 05]: 05-07: camada de triagem entrou no Kover SEM exclude novo e a cobertura SUBIU (96,68% -> 97,64%) — consequencia do coordenador ser puro; ele nunca pode aparecer em exclude
- [Phase 05]: 05-07: criterio por grep nao distingue include de exclude (3a autossabotagem da fase); grep prova ausencia de texto, jamais ausencia de comportamento — verificar a intencao no bloco certo
- [Phase 06]: 06-01: nucleo do modo discador e puro (CallUiState/CallStateMapper/CallControls/CallSessionCoordinator) — zero tipo da plataforma, cobertura subiu para 97,70% sem exclude novo
- [Phase 06]: 06-01: falha no caminho da chamada PROPAGA (inverso da rede permissiva da Fase 5) — processo morto o Telecom detecta e religa no discador do aparelho; interface viva e congelada ninguem detecta
- [Phase 06]: 06-01: prazo de apresentacao de 2 s (PRESENTATION_DEADLINE_MILLIS) com confirmPresented() da UI; sem confirmacao a sessao lanca CallPresentationTimeoutException
- [Phase 06]: 06-01: mudo e viva-voz sao provados na costura CallControls por lista ordenada de eventos — pedi-los ao servico sem telefone vinculado e no-op silencioso e daria teste vacuoso
- [Phase 06]: 06-01: tom de teclado tem pareamento obrigatorio (novo digito, saida da sessao e estado terminal encerram o tom pendente) — invariante no nivel da resposta unica da Fase 5
- [Phase 06]: 06-01: as duas defesas do vigia de apresentacao sao redundantes de proposito; quebrar so uma deixa tudo verde — precedente das duas redes da Fase 5
- [Phase 06]: 06-01: kotlin-test nao esta no classpath — matriz de falhas usa assertThrows do JUnit 4.13; @Suppress(TooManyFunctions) local em vez de afrouxar detekt.yml
- [Phase Phase 06]: 06-02: as TRES cores funcionais da chamada saem por literal do arquivo de cores e chegam por parametro — inclusive o vermelho; o tema troca o esquema INTEIRO por um derivado do papel de parede a partir do nivel 31, e ler recusar pelo esquema deixaria o papel de parede aproximar recusar de atender
- [Phase Phase 06]: 06-02: CallReject/OnCallReject sao apelido digito por digito dos tokens destrutivos existentes — teste afirma a igualdade para que apelido novo nunca vire cor nova disfarcada
- [Phase Phase 06]: 06-02: fixacao dos tres esquemas em classe Robolectric separada (CallColorFixationTest); ThemeTokensTest continua em JVM pura, como a Fase 1 decidiu
- [Phase Phase 06]: 06-02: varredura de honestidade da copy le o TEXTO DOS RECURSOS em tempo de teste e e restrita as chaves da fase — rotulo legitimo de fase anterior usa vocabulario aqui proibido
- [Phase Phase 06]: 06-02: src/main/res declarado como input das Test tasks — sem isso mudar so o strings.xml deixa a varredura UP-TO-DATE e o verde antigo vale para texto nunca varrido
- [Phase Phase 06]: 06-02: material-icons-extended entrou no build (ja reservado no version catalog desde o bootstrap para as Fases 5-6): sem call_end/mic/dialpad/shield nao existe icone distinto para atender e recusar
- [Phase Phase 06]: 06-02: fontes Inter/Geist caem na reserva monoespacada do sistema (docs/backlog/fontes-inter-geist.md); nenhuma fonte pode ser resolvida em tempo de execucao, o app nao tem rede
- [Phase Phase 06]: 06-02: prova de vermelho nunca por git checkout de arquivo com trabalho novo — o comando reverteu as 74 strings ainda fora do indice
- [Phase Phase 06]: 06-03: a elegibilidade ao papel de telefone padrao exige o servico de chamada DECLARADO — so a tela de discagem faz o pedido falhar (medido); e os DOIS filtros de discagem, esquema vazio E esquema de telefone, sao exigidos
- [Phase Phase 06]: 06-03: USE_FULL_SCREEN_INTENT e a quarta permissao da fase; entrou na matriz ANTES do manifest e na allowlist no mesmo trabalho, com a lista de fases futuras perdendo as tres
- [Phase Phase 06]: 06-03: detach() do armazem publica o retrato final ANTES de cancelar o espelho — sem isso o encerramento se perdia numa corrida e a tela mostrava chamada ativa ja terminada
- [Phase Phase 06]: 06-03: no piso da plataforma a sobrecarga moderna de recusa NAO EXISTE — verificar sua ausencia estoura com erro de metodo ausente; a prova correta e confirmVerified sobre a chamada
- [Phase Phase 06]: 06-03: uses-feature de telefonia com required=false e obrigatorio junto da permissao de originar chamada (lint reprova como erro) e e a declaracao correta: sem radio o modo discador fica indisponivel
- [Phase Phase 06]: 06-03: reverter e abrir o seletor do sistema; desabilitar componente proprio e proibido PARA SEMPRE (a plataforma remove o papel e encerra o app) e virou checagem 8.2 do script
- [Phase Phase 06]: 06-03: papel detido SEMPRE vence a intencao gravada em DialerModeState; o estado do modo nunca sai de valor persistido
- [Phase Phase 06]: 06-03: cobertura caiu de 97,70% para 96,08% porque telecom.call.* ganhou tres arquivos que falam com a plataforma; alargar o filtro do Kover e do plano 06-08, nao deste
- [Phase Phase 06]: 06-03: o retorno de mudanca de estado pertence ao CallSessionStore (CallSessionObserver), nunca ao servico da plataforma — e por ali que 06-06 liga a notificacao
- [Phase Phase 06]: 06-06: canal de chamada e NOVO e de importancia ALTA (ongoing_calls); reaproveitar o canal discreto da Fase 5 daria pedido de tela cheia que nunca dispara, porque a importancia de um canal e imutavel depois de criada
- [Phase Phase 06]: 06-06: a troca para o aviso de chamada em curso vive no CallSessionStore e acontece so na TRANSICAO para ativa; mudo/viva-voz/teclado republicam o estado ativo varias vezes por chamada
- [Phase Phase 06]: 06-06: capacidade de ocupar a tela entra por costura injetavel (a consulta da versao 34 nao tem sombra no Robolectric 4.16.1) e degradar publica aviso com atender e recusar, nunca silencio
- [Phase Phase 06]: 06-06: acoes da notificacao sao intencao pendente de Activity para a tela de chamada — nenhum receptor novo e nenhuma segunda edicao do manifest na fase
- [Phase Phase 06]: 06-06: chave do extra de acao composta de BuildConfig.APPLICATION_ID (buildConfig ligado); literal do identificador do aplicativo em Kotlin e reprovado pelo Bloco 2, inclusive em KDoc
- [Phase Phase 06]: 06-06: no nivel 31+ as acoes vem do estilo de chamada da plataforma e nao sao adicionadas a mao — somar as duas fontes daria quatro botoes no aviso
- [Phase 06]: 06-04: a chave do extra de acao da notificacao e MONTADA a partir do identificador do aplicativo — o literal ditado quebraria o invariante de rebranding do Bloco 1, e nem em prosa poderia aparecer; o valor exato ficou travado por caso de teste, prova mais forte que grep
- [Phase 06]: 06-04: assert de alvo de toque exige DOIS eixos — o Compose expande sozinho o alvo de qualquer componente interativo ate o minimo da plataforma, entao reduzir um controle de 56dp para 40dp ficava VERDE; so o eixo do tamanho DESENHADO pegou a sabotagem
- [Phase 06]: 06-04: requiredSize e obrigatorio em controle de chamada — size() negocia com o pai e o pai comprimia o circulo de atender de 72dp para 23dp em tela curta, perdendo o contrato em silencio
- [Phase 06]: 06-04: a faixa superior da chamada ativa encolhe ao proprio conteudo em vez de tomar 30% fixos (30% colapsava o encerrar para altura zero) e o painel de tons rola na vertical (sem rolagem a ultima fileira de teclas ficava inalcancavel)
- [Phase 06]: 06-04: numero agrupado para exibicao SO com codigo do pais brasileiro; qualquer outro aparece como a telefonia entregou — adivinhar agrupamento de pais desconhecido produz numero visualmente errado, pior que numero sem espacos
- [Phase 06]: 06-04: assertTouchHeightIsAtLeast e escrito neste projeto — a biblioteca de teste do Compose so oferece igualdade, e igualdade quebraria a cada acerto de acabamento num contrato de minimo
- [Phase Phase 06]: 06-05: a acao direta de ligar por intencao segue proibida em producao — a origem e sempre o gerenciador de telecomunicacoes, e falha volta como resultado nomeado (inverso do nucleo da sessao, porque aqui o usuario esta com o dedo no botao)
- [Phase Phase 06]: 06-05: o formatador progressivo da biblioteca NAO fecha parenteses no Brasil (medido: 11 91234-5678); a forma nacional canonica entra no instante em que o numero fica valido — zero formatacao propria
- [Phase Phase 06]: 06-05: estado desabilitado declarado por clearAndSetSemantics no ramo da discagem, com a acao de clique redeclarada — envolver o componente compartilhado com semantica de mesclagem NAO funciona, porque o no interno dele ja mescla e e ele quem responde as buscas
- [Phase Phase 06]: 06-05: lint reprova a origem da chamada com MissingPermission quando a permissao chega por funcao injetada; runCatching nao conta como tratamento — captura por tipo, e a excecao nunca e registrada (a mensagem pode conter o numero)
- [Phase Phase 06]: 06-05: aparelho padrao do Robolectric e pequeno demais para uma tela inteira; teste de composicao exige qualificadores de tela reais, senao o conteudo sai do viewport e todo assert de exibicao fica vermelho por motivo falso
- [Phase Phase 06]: 06-05: call_phone_permission_asked e o terceiro par do padrao das Fases 4 e 5 — fora de ScreeningSettings e gravado ao disparar o launcher, nunca no retorno
- [Phase 06]: 06-07: perder um papel do sistema ENCERRA o processo do aplicativo (medido 3x, motivo registrado pelo sistema como mudanca de permissao) — vale para telefone padrao e para triagem, e vale igual quando e o usuario que troca nas configuracoes
- [Phase 06]: 06-07: prova cujo objeto e a morte do proprio processo nao cabe na instrumentacao (ela roda dentro dele) — reversao e morte no meio da chamada viraram `scripts/verify-dialer-lifecycle.sh`, dirigido pelo computador, com codigos de saida de verdade; nada foi enfraquecido nem adiado para a Phase 9
- [Phase 06]: 06-07: o encerramento ao perder o papel E o argumento do desenho — e por isso que o estado do modo discador e derivado de perguntas ao sistema e nunca de valor gravado, e por isso que desabilitar componente proprio e proibido para sempre
- [Phase 06]: 06-07: DIA-04 provado, nao implementado — ultima alteracao do CallDecisionEngine segue sendo da Fase 5 (d7d188b); zero linha do motor mudou na fase 06
- [Phase 06]: 06-07: bloquear com configuracoes de fabrica produz a variante que pede para nao registrar no historico do telefone, nunca a rejeicao simples — expectativa errada num teste, motor certo
- [Phase 06]: 06-07: caso de contato exige esperar o conjunto de chaves do processo ficar quente; consulta imediata apos insercao responde pelo conjunto ANTIGO quando outra suite ja o aqueceu (atraso proposital do observador, Fase 4)
- [Phase 06]: 06-07: o atalho de configuracao de telefonia que aponta o discador padrao BYPASSA a qualificacao e por isso nao aparece nem em prosa nos testes — deixaria a suite verde com o manifesto quebrado; prova de vermelho: removida a declaracao do servico de chamada, a concessao volta rc=255
- [Phase 06]: 06-07: comando de sistema no teste instrumentado nao passa por interpretador — sem `;`, sem codigo de saida; o equivalente observavel e a saida de erro separada, sempre acompanhada da conferencia do EFEITO do comando
- [Phase 06]: 06-07: `grep -q` sobre saida grande no shell le SUCESSO como falha (cano fechado + modo do shell) — terceira encarnacao da armadilha no repo; usar contagem em variavel
- [Phase 06]: 06-08: afirmacao de documento sem medicao e defeito — o item de numero privado voltou a NAO VERIFICADO e aponta o cenario 59; a fase provou papel, vinculo, politica e reversao, e nunca mediu entrega de chamada sem identificacao
- [Phase 06]: 06-08: excludes novos do Kover sao dois nomes de classe (costura da telefonia e servico de interface de chamada, so alcancaveis instrumentados) — 95,4741% -> 96,648%, nenhuma classe pura fora do denominador, gate visto vermelho com piso 99 antes de aceito
- [Phase 06]: 06-08: cenario fisico ja escrito e revisado NO LUGAR e nunca duplicado — 23-30 ganharam o que a automacao provou, os novos comecam em 52 e o documento fecha em 60
- [Phase 06]: 06-08: gravar configuracao e triar no instante seguinte e corrida no TESTE, nao no produto — o retrato do repositorio vem de cache mantido por coletor assincrono (Fase 3); o caso passou a esperar o valor gravado ser reportado
- [Phase 06]: 06-09: exclude do Kover da costura da telefonia REMOVIDO — a justificativa antiga (objeto de chamada so montavel pela plataforma) era falsa para mudo e viva-voz, que operam sobre o servico de chamada; costura em 100% de linhas, cobertura do gate 96,69%, piso 99 visto vermelho
- [Phase 06]: 06-09: mudo e viva-voz agora provados NA costura por verificacao de delegacao com argumento exato — chamar e concluir que nao estourou e vacuoso, porque a falha e no-op silencioso; vermelho executado sabotando producao JA COMMITADA
- [Phase 07]: 07-02: a rota tipada da biblioteca de navegacao e falso-verde de COMPILACAO reproduzido neste repo — zero erro de compilacao e SerializationException na primeira composicao do grafo; por isso as rotas sao TEXTO e o guarda-corpo e um teste que COMPOE o NavHost, nunca um assert de compilacao
- [Phase 07]: 07-02: objeto anotado PRIVADO falha por IllegalAccessException em findObjectSerializer, nao pela ausencia do complemento de serializacao — reproduzir a falha certa exige visibilidade nao privada; sonda com objeto privado concluiria a causa errada
- [Phase 07]: 07-02: a entrada do proprio NavGraph tem rota nula — toda leitura da pilha usa mapNotNull, medido
- [Phase 07]: 07-02: os tres asserts de dois eixos vivem em org.sentinela.app.ui (TouchTargetAsserts.kt) e DUPLICA-LOS E PROIBIDO PARA SEMPRE: duas copias deixariam o eixo com dentes divergir
- [Phase 07]: 07-02: contagem de destinos do grafo travada em dez, no molde dos reason codes da Fase 2 — tela nova exige revisao de navegacao
- [Phase 07]: 07-02: MatchingDeclarationName do detekt exige @file:Suppress (a anotacao na declaracao nao surte efeito); desligada no arquivo, nunca no detekt.yml compartilhado
- [Phase 07]: 07-01: cor de significado do estado da protecao sai por literal de Color.kt (StatusAttention/OnStatusAttention/StatusBlocked), pelo mesmo argumento de 06-02: o tema troca o esquema INTEIRO por um derivado do papel de parede a partir do nivel 31, e o papel de parede decidiria a diferenca entre protegido e desprotegido — o estado ativo reusa CallAccept e nao ha verde novo
- [Phase 07]: 07-01: porcento cru em recurso NAO se corrige duplicando o sinal — getString sem argumento nao formata, e a tela passaria a exibir os dois sinais; a correcao e formatted="false", precedente das tres strings de "100%" da Fase 1
- [Phase 07]: 07-01: settings_clear_history_confirm e <plurals> de verdade, nunca "registro(s)": o lint acusa a concatenacao como PluralsCandidate e a secao 12.10 do contrato de design ja a proibia — e e o que faz a contagem fechar em 269 <string name= (43 string + 1 plurals)
- [Phase 07]: 07-01: excecao verdadeira da varredura de copy ("100% offline", "100% open source") e removida do TEXTO antes da busca, jamais isentando a chave — isentar a chave deixaria promessa nova entrar na mesma string
- [Phase 07]: 07-01: settings_fallback_allow e isenta NOMINALMENTE so da varredura de pressao de opt-in: politica de erro nao tem nada a ativar, e omitir qual alternativa preserva a chamada seria pior que dize-lo
- [Phase 07]: 07-01: UIX-07 e UIX-11 seguem PENDENTES apesar do frontmatter do plano — nenhuma tela existe ainda, e marcar requisito antes da tela e o estado falsamente positivo que o item 11 da secao 10.3 proibe
- [Phase 07]: 07-01: dois planos da mesma onda sobre o mesmo app/build produzem EOFException no arquivo binario de resultados e falha do redirecionamento da listagem do APK — falha de ambiente, resolvida esperando e repetindo, nunca sinal de codigo
- [Phase 07]: 07-03: envolver o controle com container que mescla NAO derruba o estado por si — o que derruba e DECLARAR o estado no container; o no do proprio controle e fronteira de mesclagem e continua respondendo as buscas, entao o estado mora sempre no no do controle (medido nas duas direcoes, refinando o registro da Fase 6)
- [Phase 07]: 07-03: os seis componentes compartilhados da fase vivem em ui/components e nenhuma tela reimplementa cartao de opcao, linha de interruptor ou linha de verificacao — cinco copias multiplicariam por cinco a chance de perder um estado
- [Phase 07]: 07-03: a barra inferior entrega os QUATRO destinos, com os dois da Phase 8 desabilitados e com motivo textual (aba que leva a tela em branco sem explicacao e proibida por UIX-10); habilitar e mudar a lista na tela que chama, nunca o componente
- [Phase 07]: 07-03: requiredSizeIn de piso ANTES de requiredHeight — a ordem inversa devolveu 48dp desenhados onde o contrato pedia 56dp, medido pelo eixo do desenho
- [Phase 07]: 07-03: prova de vermelho do eixo desenhado repetida com o mesmo veredito da Fase 6 — item de 56dp reduzido a 40dp deixa os DOIS asserts de alvo de toque verdes, porque o Compose expande o alvo sozinho
- [Phase 07]: 07-04: StatValue fechado em Loaded/Unavailable/Loading torna o zero mentiroso IMPOSSIVEL por assinatura — sem Int nas variantes de ausencia, a regra da secao 8 deixou de ser convencao; o caso do zero verdadeiro e o do proibido ficam lado a lado na mesma classe de teste
- [Phase 07]: 07-04: papel do sistema reconsultado a cada chamada e provado por CONTADOR DE INVOCACOES, nunca por cronometro (licao das Fases 3 e 4 medida de novo); o retorno do seletor do sistema nao serve porque perder papel encerra o processo, e o trio custa 30 us — nao existe argumento de desempenho para cachear
- [Phase 07]: 07-04: os donos de estado NAO conhecem AppContainer nem na fabrica — o criterio de no maximo uma ocorrencia e inatingivel com import mais assinatura (grep conta LINHAS), e o nome totalmente qualificado seria literal do identificador do aplicativo, proibido pelo Bloco 2; as fabricas recebem colaboradores um por um e a montagem fica na rota
- [Phase 07]: 07-04: falha de leitura do historico vira estado visivel em vez de propagar — inverso DELIBERADO do caminho da chamada da Fase 6: processo morto o Telecom detecta, home congelada ninguem detecta exceto o usuario
- [Phase 07]: 07-04: onboarding_completed e chave propria fora de ScreeningSettings, nunca o contador de aberturas — amarrar o onboarding ao numero que o convite de avaliacao da Fase 9 ja usa seria divida garantida
- [Phase 07]: 07-04: ausencia de funcao de salvar na tela Protecao travada por teste de REFLEXAO sobre nomes de metodo — um botao salvar tornaria possivel a tela e a triagem discordarem, e o retrato ja e imediato
- [Phase 07]: 07-04: coletor de teste de dono de estado exige despachante NAO CONFINADO amarrado ao agendador — o backgroundScope do runTest usa o padrao, stateIn(WhileSubscribed) nunca liga o fluxo de origem e 11 de 15 casos ficaram vermelhos por motivo falso reportando o valor inicial; prima da armadilha da Fase 4
- [Phase 07]: 07-05: as tres adaptacoes do mockup na tela de boas-vindas ficam em KDoc apontando o registro pos-lancamento — sem base global de numeros, sem imagem remota (o app nao declara internet, entao ela e impossivel por construcao) e sem sobreposicao de progresso falso, que seria padrao escuro medindo nada
- [Phase 07]: 07-05: a prova de vermelho do container mesclado foi medida NAS DUAS DIRECOES e refuta a formulacao do proprio criterio — envolver o botao num container que mescla fica VERDE; o vermelho so vem quando o estado desabilitado e declarado NO CONTAINER e o botao fica habilitado (terceira medicao do mesmo achado, agora em ecra de onboarding)
- [Phase 07]: 07-05: o grupo de opcoes usa o modificador compartilhado optionCardGroup() e por isso o literal selectableGroup NAO aparece na tela — reescreve-lo seria a duplicacao que 07-03 proibiu, e planta-lo em comentario e o defeito que a Fase 5 registrou tres vezes
- [Phase 07]: 07-05: a transicao entre passos nao mora na tela — a tela publica a duracao de 250 ms e a supressao por reducao de movimento, e o envelope de navegacao (07-09) aplica o movimento, porque so ele conhece os dois passos e o sentido
- [Phase 07]: 07-05: papel concedido nunca avanca sozinho e papel negado nunca trava — o chip e o aviso sao regiao viva EDUCADA, e repetir o dialogo do sistema exige toque explicito na acao do aviso
- [Phase Phase 07]: 07-06: negar a leitura da agenda NAO desabilita nenhuma das quatro opcoes de politica — a escolha e preferencia persistida e passa a valer no instante da concessao, entao desabilitar nao protegeria nada e seria pressao; a prova de vermelho deixou os TRES estados nao concedidos vermelhos e o concedido verde
- [Phase Phase 07]: 07-06: na negacao definitiva da agenda NAO existe botao de pedir a permissao — a plataforma nao mostra mais o dialogo e o toque nao faria nada; o ramo oferece so o atalho para as configuracoes, e sabotar isso deixa DOIS casos vermelhos (perde a saida real e ganha o botao inerte)
- [Phase Phase 07]: 07-06: no passo 4 o aviso temporizado do mockup virou TEXTO PERMANENTE (informacao que desaparece sozinha e informacao perdida, e aviso com tempo e hostil ao leitor de tela) e o quadro ilustrado remoto virou cartao tonal, porque o aplicativo nao declara acesso a internet
- [Phase Phase 07]: 07-06: quatro pre-visualizacoes num arquivo que ja tem varias compostas privadas saem de UM provedor de parametro — quatro funcoes anotadas estouram o limite de funcoes por arquivo do detekt, e afrouxar a regra compartilhada por pre-visualizacao seria o preco errado
- [Phase Phase 07]: 07-06: KDoc que CITA o nome do recurso derruba criterio de aceite por contagem de texto (quarta encarnacao da armadilha das Fases 3 e 5) — o comentario passou a descrever o texto em vez de nomear a chave
- [Phase 07]: 07-07: assert de acao em no separado so tem dentes na arvore MESCLADA — com o botao movido para dentro do no mesclado, os asserts sobre a arvore NAO mesclada ficaram VERDES (ela preserva o no do botao mesmo com o ancestral limpando a semantica) e so tres casos vizinhos pegaram a sabotagem; a prova de vermelho consertou o teste em vez de confirma-lo
- [Phase 07]: 07-07: titulo e cor do veredito final saem do MESMO booleano do papel — nao existem dois sinais para divergir, e por isso circulo verde sobre titulo parcial e impossivel por construcao, no molde do StatValue de 07-04
- [Phase 07]: 07-07: NEVER_SILENCE cai no rotulo de permitir na linha de desconhecidos porque o passo 2 so ofereceu tres opcoes — mostrar na conferencia uma palavra que o usuario nunca viu na tela da escolha seria pior que agrupar
- [Phase 07]: 07-07: UIX-10 segue PENDENTE de proposito — estados de carregamento e erro sao da home (07-08), e marcar requisito antes da tela e o estado falsamente positivo que este proprio plano existe para impedir
- [Phase 07]: 07-08: o interruptor do cartao principal alterna a PREFERENCIA de protecao e nunca o papel do sistema — revogar papel encerra o processo (medido 3x na Fase 6) e o aplicativo nem pode revoga-lo; o papel e estado somente-leitura no aviso, com botao que abre o seletor do sistema e que DESAPARECE quando o aparelho nao oferece o papel
- [Phase 07]: 07-08: o zero mentiroso e provado por VARREDURA da arvore semantica inteira (texto, descricao de conteudo e descricao de estado), nas duas arvores — afirmar so que o traco aparece seria fraco, porque a tela poderia exibir os dois; no ramo de carregamento o zero sabotado foi pego pela descricao de conteudo, nao por texto visivel
- [Phase 07]: 07-08: quarta medicao da semantica mesclada, agora na home — com a descricao de estado movida para um container que mescla, o no do interruptor continuou existindo e alcancavel e o que se perdeu foi o ESTADO; por isso ele mora sempre no modificador do proprio Switch e o arquivo do cartao nao tem uma unica mesclagem de descendentes
- [Phase 07]: 07-08: a home ganhou 6 chaves de texto novas + 2 plurais de tempo (contagem de 07-01 de 269 para 275 <string name=) — tempo relativo por plurais reais, leitura da ultima bloqueada, excedente de avisos e acao de tentar de novo; reusar 'Conceder agora' como nova tentativa de leitura seria texto errado na tela
- [Phase 07]: 07-09: os rotulos das politicas por origem COLIDEM entre os tres grupos (Bloquear/Silenciar/Tocar/Nunca Silenciar) — o caso que trocava a politica de contatos buscando o texto acertava o cartao de desconhecidos e afirmava a coisa errada; politica se clica pela DESCRICAO, e nenhum cabecalho de grupo pode repetir o rotulo de um item seu
- [Phase 07]: 07-09: botao de texto do Material desenha 40dp e os dois asserts de alvo de toque ficam VERDES — quarta medicao do eixo do DESENHO neste projeto; o piso vai em requiredHeightIn, porque heightIn negocia com o pai e volta a 40dp em tela apertada
- [Phase 07]: 07-09: a tela Protecao tem EXATAMENTE dois dialogos, os dois por perda de dado (limpar historico e nao guardar); trocar politica, desligar historico, desligar protecao e ativar o discador nao confirmam — confirmacao excessiva ensina a tocar em sim sem ler
- [Phase 07]: 07-09: a consequencia destrutiva de 'nao guardar' vive SO no corpo do dialogo; repeti-la no cartao criaria duas copias da mesma frase, e a copia esquecida e sempre a que fica errada
- [Phase 07]: 07-09: as cinco janelas de retencao sao as UNICAS opcoes da tela sem descricao propria — a duracao ja esta dita no rotulo e a explicacao do item vive uma vez, como nota do grupo
- [Phase 07]: 07-09: nome totalmente qualificado em pre-visualizacao reprova o Bloco 2 (carrega o identificador do aplicativo) — o inverso do achado de 07-04, onde o nome qualificado era a fuga da contagem de linhas do grep
- [Phase 07]: 07-10: a PILHA de navegacao e a unica fonte da verdade do passo do onboarding — o passo chega a rota como parametro do destino; o contador do dono de estado continuaria em paralelo e divergiria no primeiro gesto de voltar, mostrando a tela de um passo com o cabecalho de outro
- [Phase 07]: 07-10: os dez destinos do grafo sao escritos um por um, sem laco — a contagem e ponto de revisao de navegacao e um laco a esconderia de quem le o arquivo e de quem o verifica de fora
- [Phase 07]: 07-10: destino inicial resolvido ANTES de compor o grafo, por produceState, com espera anunciada — trocar startDestination com o grafo ja composto NAO re-navega, e bloquear a thread principal para decidir seria estragar a partida a frio; a correcao do defeito e a de desempenho sao a mesma linha
- [Phase 07]: 07-10: desvio de passo extraido para PassoDoOnboarding + AcoesDoPasso — e o que permite ao teste de fluxo compor o codigo REAL de producao (rotas, ordem dos passos e descarte inclusivo) sem container, em maquina virtual pura
- [Phase 07]: 07-10: Bloco 9 com tres checagens sobre onboarding/home/settings; 9.2 exclui os donos de estado por desenho de 07-04 (a mascara e aplicada neles e o tipo publicado nao tem campo para digitos), e 9.1 exclui linha de comentario ANTES da contagem — e o que impede o criterio de casar a propria prosa que o descreve
- [Phase 07]: 07-10: sabotar 9.2 com nome totalmente qualificado derruba DOIS invariantes — o Bloco 2 pega o identificador do aplicativo literal em Kotlin no mesmo instante; a armadilha que derrubou quatro executores funciona
- [Phase 07]: 07-10: permitidos, historico e privacidade-e-sobre NAO ganham destino (contagem do grafo travada em dez) — os tres atalhos abrem aviso honesto pela frase que ja existe em recurso, porque a linha de sobre e ATIVA na tela Protecao e toque sem efeito e defeito silencioso; desabilita-la sabotaria ProtectionScreenTest
- [Phase 07]: 07-10: intencao de ativar o modo discador derivada da marca do pedido de originar chamada, sem chave nova — essa permissao so e pedida na tela de discagem propria, que o sistema so encaminha a quem detem o papel de telefone padrao; chave dedicada adiada em deferred-items.md
- [Phase 07]: 07-10: a home ganhou os quatro comandos que a tela de 07-08 exigia e nao tinha — religar historico mexe em DUAS configuracoes (interruptor e retencao que nao guarda), senao o aviso reaparece no quadro seguinte, e tentar de novo REINSCREVE o fluxo em vez de recalcular o mesmo resultado
- [Phase 07]: 07-10: o gesto de voltar na home esvazia a pilha e o destino corrente fica NULO — medido; sair do aplicativo e o comportamento correto, e o assert certo e que nem a pilha nem o destino podem ser de onboarding
- [Phase 07]: 07-10: performScrollTo reprova controle de rodape fixo (14 de 15 casos vermelhos por motivo falso) — a rolagem virou tentativa tolerada e o TOQUE continua obrigatorio
- [Phase 07]: 07-10: UIX-07 segue PENDENTE de proposito — o Bloco 9.1 vigia as tres pastas desta fase, e as de chamada, discagem e componentes ficam fora; fecha em 07-11
- [Phase 07]: 07-10: contagem de planos do STATE corrigida a mao para 45 — o calculo por disco chegou a 46/46 somando duas anomalias que se anulam (a Fase 6 tem um resumo extra legitimo, 06-09, e a Fase 7 tem o 07-11 ainda aberto)

## Convenções operacionais do GSD

- Toda fase exige `$gsd-discuss-phase`, com perguntas formuladas e respondidas, antes de qualquer planejamento, inclusive no modo autônomo.
- Pesquisa permanece obrigatória e habilitada antes do planejamento (config `research: true`).
- Phase 5 (Telecom) e Phase 6 (Modo Discador) têm pesquisa obrigatória reforçada: semântica exata de `setSkipCallLog`/`setSilenceCall`/DND por versão, elegibilidade ao `ROLE_DIALER`, ciclo de vida do `InCallService` e comportamento Samsung.

## Pending Todos

- Fazer o commit inicial do bootstrap (nada commitado ainda; usuário decide a hora)
- Criar remote no GitHub e ajustar links do CHANGELOG se o slug divergir de `ricardosierra/sentinela`
- **Escolher licença open source** (sugestão: GPL-3.0 ou MIT) e adicionar `LICENSE` — produto será divulgado como open source
- **Obter endereço Bitcoin real do mantenedor** para a doação (string `support_bitcoin_address` está vazia de propósito — nunca publicar com placeholder)
- Decidir arte final do ícone (placeholder vetorial de escudo no esqueleto)

## Blockers/Concerns

- **Validação física obrigatória** — critérios de aceite centrais só fecham em Samsung físico (Phase 9), cenários 40-51. Correção 2026-07-29: pular o registro no histórico do telefone **não** é variação de OEM — é no-op do próprio Android para apps que não sejam de operadora; o que resta medir no aparelho é **onde** a chamada bloqueada aparece na One UI
- **Modo discador é o maior risco técnico do MVP** — `InCallService` + elegibilidade ao papel + UX de chamada; pesquisa reforçada antes da Phase 6
- ~~**Robolectric 4.16.1 suporta até SDK 36** — fixar a configuração de SDK dos testes em 36~~ **SUPERADO POR MEDICAO 2026-07-29 (Phase 05):** o teto real e a versao do **Java do projeto**, nao a do Robolectric — a versao 36 do Android exige Java 21 e o projeto roda em JDK 17. O valor correto e fixo em **35**, ja aplicado em todos os testes sob Robolectric
- ~~p95 de containsCabeNoOrcamentoMedido falha ~1 em 5 execucoes no emulador~~ **RESOLVIDO 2026-07-29 (decisao do usuario):** o p95 saiu do assert do emulador e virou o cenario 35 de `docs/TESTE-FISICO-SAMSUNG.md`, medido em Samsung fisico na Phase 9. O numero de 5 ms **nao foi afrouxado**; p50 < 1 ms e o `EXPLAIN QUERY PLAN` seguem quebrando o build

## Accumulated Context

### Decisions

(ver seção Decisions acima — consolidar aqui a partir da Phase 2)

### Pending Todos

None yet.

### Blockers/Concerns

None yet.

### Roadmap Evolution

- Roadmap inicial criado com 7 fases para o milestone v0.1.0 MVP (2026-07-27)
- Roadmap expandido para 9 fases (2026-07-28): + Phase 4 Contatos do Aparelho e Phase 6 Modo Discador; fase final ganhou apoio/avaliação; requisitos de 65 → 81 (CTT, DIA, ENG, WLT-08, UIX-13, QLT-06..07)

## Session Continuity

Last session: 2026-07-30T06:13:01.670Z
Stopped at: Completed 07-10-PLAN.md
Resume file: None
