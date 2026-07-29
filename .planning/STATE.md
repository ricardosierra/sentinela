---
gsd_state_version: 1.0
milestone: v0.1
milestone_name: milestone
status: unknown
stopped_at: Completed 05-06-PLAN.md
last_updated: "2026-07-29T17:20:14.473Z"
last_activity: 2026-07-29
progress:
  total_phases: 9
  completed_phases: 4
  total_plans: 27
  completed_plans: 26
---

# Project State

## Project Reference

See: `.planning/PROJECT.md` (updated 2026-07-28)
**Core value:** "Se não está nos contatos nem na whitelist pessoal, não interrompe o usuário."
**Current focus:** Phase 05 — Triagem Telecom Modo Filtro
Last activity: 2026-07-29

## Current Position

Phase: 05 (Triagem Telecom Modo Filtro) — EXECUTING
Plan: 7 of 7

## Snapshot

- **Esqueleto:** Gradle KTS + catalog, AGP 9.3.0 (Kotlin embutido), Compose BOM 2026.06.01, compileSdk 37 / minSdk 29
- **Domínio:** `CallDecisionEngine` com precedência saída→proteção→privado→contato→whitelist→falha→desconhecido e políticas por origem (`OriginPolicy`)
- **Normalização:** `LibPhoneNumberNormalizer` + `PhoneMask` + cascata de região, ligados em `AppContainer.phoneNumberNormalizer` (util construído 1x, fora do caminho quente)
- **Dados locais:** Room v1 (`whitelist` + `blocked_call`, schema exportado) e DataStore Preferences, ambos como instância única no `AppContainer`; retenção de histórico em 5 políticas, podada na abertura do app
- **Contatos:** `ContactLookupRepository` real ligado no `AppContainer` (singleton preguiçoso, sonda dupla + cache invalidado por observer); nada de identidade de contato toca disco — Bloco 6 de `verify-invariants.sh`
- **Qualidade:** 296 testes JVM + 53 instrumentados; cobertura domain+phone+data+settings 96,6759% com gate `koverVerify` em 80%; lint e detekt zerados; `verify-invariants.sh` com 7 blocos (Bloco 7 trava a regra de decisao dentro do motor)
- **Telecom:** `UnknownCallScreeningService` LIGADO — delega ao `ScreeningCoordinator` pelo contrato `ScreeningDependencies`, responde uma unica vez e dispara historico/notificacao depois da resposta
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

Last session: 2026-07-29T17:19:58.600Z
Stopped at: Completed 05-06-PLAN.md
Resume file: None
