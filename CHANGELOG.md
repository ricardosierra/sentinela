# Release Notes

---

## [Futuro]

### ✨ Novidades

- [ ] **Etapa 2 — Sincronização & Backend** — conta opcional, sync de listas, envio opcional da lista de números recebidos, backup criptografado opt-in ([backlog](docs/backlog/supabase-v2.md))
- [ ] Bloqueio por prefixo/padrão (ex.: 0303) — candidato pós-MVP

## [Unreleased](https://github.com/ricardosierra/sentinela/compare/v0.2.3...master)

## [v0.2.3 (2026-08-22)](https://github.com/ricardosierra/sentinela/compare/v0.2.2...v0.2.3)

### ✨ Novidades

- [x] **Tcheco, estoniano e polonês na interface** — as três traduções já viajavam dentro do APK publicado, mas nunca tinham entrado no repositório; agora são 20 idiomas versionados, cada um com as mesmas 324 entradas traduzíveis da base

### 🎨 Melhorias

- [x] **Seletor de idioma por app do Android** — `android:localeConfig` liga o aplicativo ao `locales_config.xml`, e os 20 idiomas traduzidos passam a aparecer em Configurações → Apps → Sentinela → Idioma; antes, quem quisesse o Sentinela em outro idioma precisava trocar o idioma do aparelho inteiro, e a tradução que já existia ficava invisível para quem procurava por ela

### 🐛 Correções

- [x] **O APK publicado não podia ser reproduzido a partir do repositório** — os recursos de `cs`, `et` e `pl` e o `locales_config.xml` estavam no binário da v0.2.2 e não no versionamento, porque o release saiu de uma árvore de trabalho suja; nenhuma verificação de build reprodutível tinha como dar certo enquanto o commit e o binário descrevessem apps diferentes
- [x] **Símbolos nativos divergiam entre a máquina do mantenedor e o F-Droid** — o AGP só remove os símbolos de depuração dos `.so` quando encontra um NDK instalado: aqui `libdatastore_shared_counter.so` saía de 10360 para 7784 bytes em `arm64-v8a`, enquanto o buildserver do F-Droid avisava *"Unable to strip the following libraries"* e empacotava o arquivo inteiro — mesmo commit, dois APKs diferentes. `keepDebugSymbols` desliga a remoção dos dois lados e torna o byte idêntico em qualquer máquina

### 🔧 Técnico

- [x] **`scripts/verify-locales.py`** — falha se um idioma declarado não tiver `strings.xml`, se um `strings.xml` não estiver declarado, se as chaves divergirem da base ou se um placeholder mudar de forma; `verify-invariants.sh` passa a chamá-lo, então o desvio quebra o build em vez de aparecer meses depois numa captura de tela
- [x] **`libandroidx.graphics.path.so` ficou de fora da regra de propósito** — ela é citada no mesmo aviso do F-Droid, mas já vem sem símbolos dentro do próprio `.aar` (10096 bytes na origem e no APK), então a ausência de NDK nunca mudou um byte dela e mantê-la na lista só esconderia o motivo real da falha
- [x] **Política de segurança e templates de issue** — `SECURITY.md` define o canal privado (Security Advisory do GitHub), o escopo e o prazo pretendido de resposta; os formulários de bug e de feature pedem relatório sem número, contato ou dump, e a issue em branco foi desligada para empurrar vulnerabilidade ao canal privado
- [x] **Credenciais da Google Play fora do repositório** — o `.gitignore` passa a barrar `.secrets/` e qualquer JSON de service account, e o guia de publicação injeta o segredo pelo ambiente a partir de um caminho externo ao projeto

## [v0.2.2 (2026-08-20)](https://github.com/ricardosierra/sentinela/compare/v0.2.1...v0.2.2)

### ✨ Novidades

- [x] **Aviso sobre contato salvo só no WhatsApp** — quem tem o WhatsApp instalado passa a ver, no passo de contatos, que pessoa salva apenas dentro do WhatsApp não existe na agenda do aparelho e por isso chega como desconhecida; no histórico, o bloqueio de número desconhecido traz a mesma dica ao lado da ação de permitir. O aviso só aparece para quem tem o aplicativo instalado, e a checagem é local

### 🐛 Correções

- [x] **Aviso da chamada recebida podia sumir da barra no instante em que ela chegava** — o cancelamento no `CallSessionStore` reagia à primeira emissão do fluxo, e o retrato de partida da sessão já nasce terminal; como o serviço publica o aviso **antes** de vincular a sessão, a corrida entre os dois apagava da barra a ligação que estava tocando. O cancelamento agora só vale depois de a chamada entrar na sessão e sai uma vez por sessão, com regressão que falha se a primeira emissão voltar a cancelar

### 🔧 Técnico

- [x] **`pt-BR` deixou de cair em `pt-PT`** — as strings brasileiras ganharam `values-pt-rBR/` próprio; enquanto moravam só no `values/` sem qualificador, a variante `values-pt-rPT` vencia o padrão na resolução do Android e o mercado principal do app recebia português europeu
- [x] **Metadados legíveis pelo scanner do F-Droid** — `applicationId`, `namespace`, `versionCode` e `versionName` voltaram a ser literais entre aspas, que é o que o leitor de metadados do F-Droid consegue extrair, e a toolchain rígida deu lugar a `compileOptions` Java 17 para o build subir com o JDK do buildserver
- [x] **Publicação da Google Play automatizada** — tags SemVer validam qualidade, assinam AAB, verificam o APK release sem `INTERNET`, geram os 74 listings e publicam pelo Play Developer API; promoções entre tracks e rollout gradual usam a mesma automação, com Environment obrigatório antes de produção

## [v0.2.1 (2026-08-12)](https://github.com/ricardosierra/sentinela/compare/v0.2.0...v0.2.1)

### ✨ Novidades

- [x] **Doação de volta na tela Sobre** — com os endereços oficiais do mantenedor, em **Bitcoin** (on-chain) e **Liquid (L-BTC)**, cada um com botão de copiar e uma linha lembrando de conferir o endereço antes de enviar; a v0.1.0 tinha publicado um placeholder e o botão ficou fora da tela até existir endereço real
- [x] **Mascarar números recebidos** — nova opção em Proteção → Privacidade. Ligada, o número aparece como `+55 11 9****-1234` no histórico, nas notificações e na tela inicial; desligada (padrão), aparece completo, porque sem os dígitos o usuário não reconhece quem ligou. A máscara continua obrigatória em log, e a versão pública da notificação — a única que a tela bloqueada mostra — nunca carrega número em nenhuma das duas opções

### 🐛 Correções

- [x] **Notificação de chamada ficava presa na barra** — ao encerrar a ligação, o aviso "Chamada em curso" não saía da barra em aparelhos onde o processo era reciclado entre o estado terminal ser publicado e o `onCallRemoved` do serviço ser chamado; o cancelamento agora acontece no `CallSessionStore`, no instante exato da transição, independentemente do ciclo de vida do serviço
- [x] **Discador demorava a tocar em aparelhos lentos** — o prazo de apresentação da tela de chamada era de 2 segundos, insuficiente para cold start em aparelhos com pouca RAM ou com `fullScreenIntent` bloqueada por DND; aumentado para 5 segundos, mantendo o mecanismo de falha rápida para tela congelada
- [x] **Bipe extra ao atender a chamada** — a substituição da notificação "Chamada recebida" pela notificação "Chamada em curso" tocava o som do canal novamente; a notificação de chamada em curso agora entra com `setOnlyAlertOnce(true)`, que atualiza o conteúdo na barra sem redisparar o som do canal
- [x] **Tela de chamada podia ficar aberta em estado não suportado** — quando a chamada ia para um estado que esta versão não desenha (ex.: espera), a tela nunca fechava e a notificação ficava presa; estado `Unsupported` agora é tratado como terminal para efeito de fechamento de tela e de cancelamento de notificação
- [x] **Botão Voltar bloqueado em estado não suportado** — em estado `Unsupported`, o `BackHandler` impedia a saída da tela mesmo quando o botão de encerrar não funcionava, deixando o usuário preso; o Voltar agora é liberado nesse estado

### 🔧 Técnico

- [x] `strings.xml` é a **fonte única** dos endereços — nenhuma cópia em Kotlin, doc ou README, porque endereço duplicado é endereço que um dia diverge
- [x] `SupportAddressTest` decodifica o valor que vai para o APK e confere alfabeto, checksum, rede e tamanho do payload (bech32 P2WPKH em `bc`; blech32 com chave de ofuscação de 33 bytes em `lq`), com um caso que corrompe um caractere para provar que o teste reprova. Erro de digitação em endereço de doação agora quebra o build em vez de mandar dinheiro de doador para um estranho
- [x] `CallSessionStoreCancellerTest` — regressão: canceller chamado exatamente uma vez na transição para `Ended` e para `Unsupported`, e ausência de canceller não lança em estado terminal
- [x] **Ficha da Google Play em `docs/loja/`** — nome, descrição curta, descrição completa e novidades em 74 idiomas (8 blocos), mais ícone, banner e capturas geradas por script (`build-store-assets.py`, `capture-store-screenshots.sh`) a partir da mesma arte do launcher
- [x] Locale dos testes fixado em `pt-BR` no `build.gradle.kts` — a JVM do desenvolvedor podia rodar a suíte em outro idioma e trocar formatação de número e data sem nenhum aviso

## [v0.2.0 (2026-08-06)](https://github.com/ricardosierra/sentinela/compare/v0.1.0...v0.2.0)

### ✨ Novidades

- [x] **Filtro do histórico por decisão** — além do período, dá para ver só as chamadas sem classificação, as marcadas como legítimas ou as marcadas como indesejadas; os dois eixos valem ao mesmo tempo
- [x] **Backup da whitelist de verdade** — exportar grava os seus números no arquivo escolhido e importar mescla sem duplicar, com confirmação antes de alterar a lista

### 🐛 Correções

- [x] **Chamada bloqueada podia receber uma segunda resposta** — uma falha depois da decisão somava uma resposta "permitir" à mesma chamada, desfazendo em silêncio um bloqueio já decidido; agora a resposta ao sistema acontece em um ponto único, atrás de guarda, com regressão coberta por teste
- [x] **Exportar whitelist gerava arquivo vazio** — o backup gravava uma lista fixa vazia em vez dos seus números; quem confiou nele não tinha backup nenhum
- [x] **Importar whitelist não fazia nada** — o arquivo era lido e descartado
- [x] **Número duplicado ou inválido sumia sem aviso** — o cadastro fechava como se tivesse dado certo; agora diz o que aconteceu
- [x] **Histórico mostrava "Agora" em toda linha** — um histórico de trinta dias parecia ter acontecido inteiro no último minuto; agora mostra o tempo real de cada bloqueio
- [x] **Histórico mostrava código interno** — a legenda exibia `UNKNOWN_NUMBER` em vez do motivo em português
- [x] **Tocar na notificação não abria o registro** — a notificação já carregava o identificador da chamada bloqueada, mas nada lia esse valor; agora abre o histórico direto no registro
- [x] **Botão "Sobre" da tela inicial não levava a lugar nenhum** — mostrava "em preparação" mesmo com a tela Privacidade e Sobre pronta

### 🔧 Técnico

**Auditoria retroativa das Fases 8 e 9 (2026-08-06):**

As duas fases tinham sido implementadas por commits diretos, fora do fluxo de planejamento, e a
v0.1.0 saiu sem passar por verificação. A auditoria encontrou 8 defeitos — os 6 acima, mais os
dois abaixo — e produziu os artefatos de verificação que faltavam.

- [x] Endereço de doação em Bitcoin **removido da tela**: a v0.1.0 publicou um endereço placeholder que tinha, no próprio arquivo, o comentário mandando nunca publicar endereço inventado. Doação para endereço errado é irreversível. O botão só volta com endereço real gerado em carteira sob custódia do mantenedor
- [x] 557 arquivos de um virtualenv Python (`.venv/`) saíram do versionamento e entraram no `.gitignore`
- [x] `scripts/verify-invariants.sh` estava vermelho em 3 dos 10 blocos no master e passa nos 10
- [x] Strings de interface que tinham ficado embutidas em Kotlin (whitelist, histórico e tela Sobre) foram para `strings.xml`
- [x] Testes novos: resposta única sob falha posterior, avisos de duplicado/inválido, ligação real do export/import, filtro por decisão e formatação de tempo e motivo

## [v0.1.0 (2026-08-04)](https://github.com/ricardosierra/sentinela/releases/tag/v0.1.0)

### ✨ Novidades

- [x] **Bloqueio de desconhecidos antes de tocar** — CallScreeningService + ROLE_CALL_SCREENING, sem tela de chamada, som ou notificação nativa (Phase 5)
- [x] **Políticas por origem** — contatos (Tocar padrão), whitelist (Nunca Silenciar padrão) e desconhecidos (Bloquear padrão), com opções Tocar/Bloquear/Silenciar/Nunca Silenciar (Phases 2, 4, 7)
- [x] **Leitura local de contatos** — READ_CONTACTS com explicação, lookup em memória, nada armazenado nem enviado (Phase 4)
- [x] **Modo discador opcional** — Sentinela como app de telefone padrão (ROLE_DIALER + InCallService próprio), políticas valendo também para contatos, reversível pelo seletor do sistema (Phase 6)
- [x] **Telas de chamada próprias** — chamada recebida em tela cheia (inclusive sobre a tela bloqueada, por full-screen intent e **nunca** por overlay), chamada de saída, chamada ativa com cronômetro, mudo, viva-voz e teclado de tons; quatro variantes de identidade (contato, whitelist, desconhecido, privado) (Phase 6)
- [x] **Tela de discagem** — campo somente saída com formatação progressiva pt-BR, sugestão de contato, teclas de 72dp, apagar com toque longo; alvo do pedido de discagem do sistema (Phase 6)
- [x] **Ativação honesta do modo discador** — cards "O que muda" e "O que não muda" com estilo idêntico e o mesmo peso visual, pré-requisito de leitura da agenda explícito, aviso informativo (sem alarme) quando o papel é perdido (Phase 6)
- [x] **Whitelist pessoal local** — CRUD com busca, E.164, tratamento configurável, import/export com validação (Phases 3 e 8)
- [x] **Histórico interno opcional** — retenção configurável (nunca/7/30/90 dias/manual), ações permitir/indesejado/excluir (Phases 3 e 8)
- [x] **Notificação silenciosa opt-in** — canal IMPORTANCE_LOW, número mascarado, desabilitada por padrão (Phase 5)
- [x] **Onboarding completo** — papel de filtro, política de desconhecidos, política de contatos, tratamento da whitelist (Phase 7)
- [x] **Apoio e avaliação** — convite na 5ª abertura (repete a cada 5 até aceite), seção "Apoie o Sentinela": open source, sem telemetria, 100% offline, comentário de apoio e doação em Bitcoin (Phase 9)
- [x] **Tela Privacidade e sobre** — dados locais, permissões, retenção, limpar tudo (Phase 9)

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
