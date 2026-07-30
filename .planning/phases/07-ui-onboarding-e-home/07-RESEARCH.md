# Phase 7: UI Onboarding e Home — Research (técnico/arquitetura)

**Researched:** 2026-07-30
**Domain:** Navegação Compose, state holders sem framework de DI, papel do sistema como estado vivo,
permissão em runtime na composição, teste de fluxo multi-tela em JVM, política de lint
**Confidence:** HIGH (7 das 8 questões medidas neste repositório; 1 parcial)

> **Escopo deste documento.** Metade **técnica** da fase. A metade visual — fidelidade aos mockups,
> tokens, espaçamentos, copy — vive em `07-UI-SPEC.md`, produzido em paralelo. Aqui não há
> especificação visual, e nenhuma decisão deste arquivo autoriza mudar mockup.

---

<user_constraints>
## User Constraints (from CONTEXT.md)

### Locked Decisions

**Fluxo do onboarding**

- **Pode ser pulado.** Botão "pular" leva à home com os defaults dos mockups aplicados. Forçar o
  usuário a atravessar o fluxo é dark pattern, proibido pelo `CLAUDE.md`.
- **Se o papel for negado**, seguir para a home mostrando o **estado real** e o botão de correção.
  Nunca travar num passo nem repetir o pedido em loop.
- **Modo discador NÃO entra no onboarding.** É opcional, é o maior risco técnico do produto e muda
  o telefone padrão do usuário — pertence à tela Proteção, apresentado sem pressa e com a copy
  honesta que a Phase 6 já produziu. A tela `DialerActivationScreen` já existe como composable puro
  com quatro callbacks; **esta fase a liga à navegação** (pendência registrada em 06-05).
- **Aparece só na primeira abertura.** Depois, tudo está acessível na tela Proteção. Não reexibir a
  cada atualização.

**Home e estado real**

- **O status vem de consulta viva ao sistema a cada retomada — nunca de flag persistida.** A Phase 6
  mediu que **perder um papel do sistema mata o processo**
  (`Killing <pid> (adj 0): Permission or app op changed`), inclusive quando o próprio usuário troca
  o app de telefone nas configurações. Estado guardado mente. `DialerModeState` já é derivado assim
  — a home segue o mesmo princípio para o papel de triagem.
- **"Última bloqueada" na home usa número MASCARADO** (`PhoneMask.mask`). A home pode estar visível
  a terceiros. A fronteira estabelecida na Phase 6 continua valendo: número completo **somente** nas
  telas de chamada e discagem.
- **Proteção ativa sem `READ_CONTACTS`:** avisar explicitamente que contatos podem ser tratados como
  desconhecidos, com atalho para conceder. A Phase 5 provou que contatos chegam ao `onScreenCall`
  quando a permissão existe — sem ela, o lookup devolve `UNAVAILABLE` e cai na `FallbackPolicy`.
  Silenciar isso deixaria o usuário sem entender por que perdeu uma ligação.
- **Contagem de bloqueadas** vem do histórico local. Se o histórico estiver **desligado**, mostrar
  esse estado em vez de `0` — zero seria mentira, e mentira em número é pior que ausência de número.

**Tela Proteção e efeito imediato**

- **Efeito imediato, sem botão "salvar".** O `snapshot()` do DataStore com cache `@Volatile` já
  alimenta a triagem; a mudança vale na próxima chamada.
- **Explicação curta e permanente sob cada opção**, não tooltip escondido. O objetivo declarado da
  fase é o usuário *entender* o que cada política significa.
- **Confirmar só o que perde dado** (ex.: limpar histórico). Trocar política não pede confirmação —
  é reversível e confirmação excessiva treina o usuário a clicar em "sim" sem ler.
- **Honestidade herdada, escrita na tela:** o registro no histórico do telefone **sempre** acontece
  (AOSP, provado na Phase 5 — e o `ROLE_DIALER` não destrava), o Não Perturbe **não** é contornável,
  e o app **não** filtra WhatsApp/VoIP. Omitir isso "para não assustar" seria exatamente a desonestidade
  que o plano 05-07 existiu para corrigir. As strings honestas já existem desde a Phase 6.

**Acessibilidade e strings**

- **TalkBack navega o fluxo inteiro** — é critério de sucesso 4, não item de qualidade opcional.
  Alvos ≥ 48dp, `contentDescription` em todo controle, estado nunca comunicado só por cor.
- **100% das strings em `res/values/strings.xml` (pt-BR)**, nada hardcoded em Kotlin. A Phase 6 já
  adicionou 74 chaves; reaproveitar antes de criar.
- Lição da Phase 6 para os testes de toque: **precisam de dois eixos.** O Compose expande a área de
  toque ao mínimo da plataforma por conta própria, então afirmar só a área de toque mede a garantia
  da biblioteca, não o nosso layout — afirmar também o tamanho **desenhado**. Isso pegou quatro bugs
  reais de layout na Phase 6 (um botão de 72dp comprimido a 23dp, outro a altura zero).
- Testes de composição precisam de qualificadores reais de tela (`w411dp-h891dp-xxhdpi`) — o
  dispositivo padrão do Robolectric é pequeno demais e produz falha por motivo falso.

### Claude's Discretion

- Estrutura de navegação, organização dos composables, nomes de ViewModel/state holder e como o
  onboarding guarda o progresso ficam a critério do executor, desde que os 4 critérios de sucesso
  passem e a fidelidade aos mockups seja mantida.

### Deferred Ideas (OUT OF SCOPE)

- Telas de whitelist e histórico — Phase 8.
- Convite de avaliação, tela de apoio/doação, política de privacidade embutida, release R8 —
  Phase 9.
- Assets de fonte Inter/Geist — pendência registrada em `docs/backlog/`; hoje os estilos numéricos
  caem na monoespaçada do sistema.
- Blocos técnicos do CHANGELOG para as Phases 2–5 — registrado para o fechamento de versão.
- Validação do fluxo em Samsung físico com TalkBack real — Phase 9, cenários a partir de 61.
</user_constraints>

---

<phase_requirements>
## Phase Requirements

| ID | Description | Research Support |
|----|-------------|-----------------|
| **SCR-01** | Onboarding solicita explicitamente o papel `ROLE_CALL_SCREENING` via RoleManager | §Q3 — `SystemRoleGate.buildRequestIntent()` já existe; lançar com `StartActivityForResult` (compilado). Papel negado ⇒ segue para a home com estado real |
| **SCR-02** | App verifica **continuamente** se ainda ocupa o papel e oferece correção na home | §Q3 — **medido: o trio de consultas de papel custa p50 30µs**. `LifecycleResumeEffect` reconsulta a cada retomada; não existe observador de papel (confirmado no KDoc do `SystemRoleGate`) |
| **UIX-01** | Onboarding completo (explicação, papel, política de desconhecidos, política de contatos com `READ_CONTACTS`, whitelist, opt-in de notificação, verificação final) | §Q1 grafo de navegação, §Q2 state holder, §Q4 launchers de permissão sobre `RuntimePermissionAsk` |
| **UIX-02** | Home: status de proteção, status do papel com correção, contagem, última bloqueada mascarada, atalhos | §Q3 estado vivo, §Q2 fontes (`settingsRepository`, `blockedCallRepository`, `maskNumber`), §Q8 orçamento de partida |
| **UIX-03** | Tela Proteção: todas as políticas + modo discador, cada opção com explicação | §Q2 (`update {}` de efeito imediato), §Q1 (rota para `DialerActivationScreen`, pendência de 06-05) |
| **UIX-07** | 100% das strings em resources pt-BR | §Q7 — **131 strings pt-BR hoje não usadas, medidas**; buckets por prefixo mapeiam exatamente as telas desta fase |
| **UIX-09** | Acessibilidade: TalkBack, alvos ≥ 48dp, contraste | §Q6 — subconjunto automatizável definido; os 3 asserts de dois eixos da Phase 6 já existem e precisam ser **extraídos** (Wave 0) |
| **UIX-10** | Estados de carregamento e erro em todas as telas | §Q2 — `state_loading`/`state_error` já existem em resources; `collectAsStateWithLifecycle` exige valor inicial explícito |
| **UIX-11** | Nenhuma promessa falsa na UI | §Q5 — a varredura `CallStringsTest` já lê o **texto dos recursos** e é estendida às chaves desta fase |
</phase_requirements>

---

## Summary

**A boa notícia domina esta fase: não há nada para instalar.** `androidx.navigation:navigation-compose
2.9.8`, `lifecycle-viewmodel-compose 2.11.0`, `lifecycle-runtime-compose 2.11.0` e `savedstate 1.4.0`
**já estão no `debugRuntimeClasspath` e já entram no APK hoje** — foram declarados no bootstrap e nunca
consumidos. Todo o custo de tamanho e de partida a frio dessas bibliotecas **já está pago e medido**:
a partida a frio da `MainActivity`, hoje, é de **mediana 680 ms no emulador** (612–917 ms em 8
execuções) com esse classpath inteiro presente. A pergunta "adicionar navigation-compose viola a regra
de cold start?" não se aplica: não há o que adicionar.

**A má notícia é uma armadilha de falso-verde, e ela foi medida.** A navegação **tipada** do Navigation
2.8+ (`composable<MinhaRota>()` com `@Serializable`) **compila em verde neste projeto e explode em tempo
de execução**: `kotlinx.serialization.SerializationException: Serializer for class 'TypeSafeHome' is not
found.` O plugin compilador `kotlinx-serialization` **não** está aplicado (o AGP 9 traz Kotlin embutido,
mas **não** traz esse plugin), então `@Serializable` compila como anotação vazia — `javap` na classe
gerada confirma: **nenhum `$serializer`, nenhum `Companion`, nenhum `serializer()`**. Rotas por **texto**
funcionam. Esta é exatamente a classe de defeito que já pegou este projeto várias vezes (compilador
satisfeito, comportamento ausente) e ela precisa virar guarda-corpo, não nota de rodapé.

O terceiro achado é que **o desenho pedido pelo CONTEXT é barato**. A consulta viva do papel a cada
retomada — a decisão central da fase, herdada da medição da Phase 6 de que perder um papel **mata o
processo** — custa **p50 de 30 microssegundos** para as três perguntas de uma retomada
(`isRoleAvailable` + `isRoleHeld` de triagem + `isRoleHeld` de discador), com máximo de 284 µs em 200
amostras. Não há orçamento a defender aqui: consultar o sistema em cada `onResume` é gratuito, e
qualquer cache seria otimização de algo que não custa, comprando de volta a mentira que o CONTEXT
proíbe.

**Primary recommendation:** `navigation-compose` que **já está no classpath**, com rotas por **texto**
(nunca tipadas, sob pena de falha em execução), `ViewModel` + `ViewModelProvider.Factory` manual
recebendo colaboradores do `AppContainer`, papel reconsultado por `LifecycleResumeEffect`, permissões
por `rememberLauncherForActivityResult` alimentando o `RuntimePermissionAsk` puro que já existe, e o
fluxo multi-tela testado **inteiro em JVM** sob Robolectric — medido funcionando, incluindo `navigate`,
`popUpTo` e leitura da pilha.

---

## Standard Stack

### Core — tudo já presente, zero instalação

| Library | Version | Purpose | Why Standard |
|---------|---------|---------|--------------|
| `androidx.navigation:navigation-compose` | **2.9.8** | Grafo de navegação, pilha de retorno, botão voltar do sistema | **Já no classpath e no APK** (`libs.versions.toml` L9 + `app/build.gradle.kts` L215). Resolve pilha de retorno, gesto de voltar preditivo e restauração de estado — tudo que um `sealed class` à mão reimplementaria com bugs |
| `androidx.lifecycle:lifecycle-viewmodel-compose` | **2.11.0** | `viewModel(factory = …)` | **Já no classpath** (L214). É o que permite state holder sobreviver a rotação sem DI |
| `androidx.lifecycle:lifecycle-runtime-compose` | **2.11.0** | `LifecycleResumeEffect`, `collectAsStateWithLifecycle` | **Já no classpath** (L213). `LifecycleResumeEffect` é o mecanismo exato do estado vivo do papel |
| `androidx.activity:activity-compose` | **1.13.0** | `rememberLauncherForActivityResult` | **Já no classpath** (L212). Usado hoje pela `DialerActivity` |
| `androidx.savedstate:savedstate` | 1.4.0 | transitiva; `SavedStateHandle` para o progresso do onboarding | Chega por `lifecycle-viewmodel-savedstate 2.11.0` |
| `androidx.navigationevent:navigationevent` | 1.0.0 | transitiva do Navigation 2.9.x (voltar preditivo) | Não precisa ser declarada nem tocada |

**Dependência nova nesta fase: NENHUMA.** Um plano que acrescente biblioteca, plugin ou entrada no
version catalog está errado. Verificado por `./gradlew :app:dependencies --configuration
debugRuntimeClasspath`.

### Alternatives Considered

| Instead of | Could Use | Tradeoff |
|------------|-----------|----------|
| `navigation-compose` (já presente) | `sealed class` + `mutableStateOf` à mão | **Rejeitado.** O argumento normal a favor do à-mão é "não pagar a biblioteca" — mas ela **já está paga e no APK**. Escrever à mão custaria reimplementar pilha de retorno, gesto de voltar e `popUpTo` do "pular onboarding", e o `saveable`/rotação. Custo positivo, benefício zero |
| rotas tipadas `@Serializable` | rotas por `String` | **Obrigatório usar texto.** Tipada **falha em execução** neste projeto (medido, §Q1). Habilitá-la exigiria o plugin `kotlinx-serialization` — dependência/plugin novo, e o CONTEXT não pede tipagem de rota |
| `ViewModel` | state holder simples com `remember` | Ambos válidos; ver §Q2 para o critério por tela. Onboarding e Proteção querem `ViewModel`; telas sem estado próprio não |
| Hilt/Koin/Dagger | DI manual pelo `AppContainer` | **PROIBIDO** pelo `CLAUDE.md`. Não é tradeoff, é invariante |

**Installation:** nada a instalar.

**Version verification:** as versões acima não vêm de treino — foram lidas da resolução real do Gradle
neste repositório em 2026-07-30. `NewerVersionAvailable` do lint aponta só
`kotlinx-serialization-core 1.8.1 → 1.11.0`, e esse piso é **deliberado** (comentário em
`libs.versions.toml` L13-15: o `room-migration 2.8.4` quebra com 1.7.3). **Não subir nesta fase** —
mexer no piso de serialização é risco sem requisito.

---

## Architecture Patterns

### Q1 — Navegação: `navigation-compose`, rotas por TEXTO

**Recomendação: usar `navigation-compose` 2.9.8, que já está no classpath, com rotas por `String`
declaradas em constantes.**

**Isso viola alguma regra do projeto?** Não, e a análise merece ser explícita porque a pergunta é
legítima:

| Regra | Veredito |
|-------|----------|
| "DI manual — nada de Hilt/Koin/Dagger" | `navigation-compose` não é DI. Não viola |
| "Nenhum framework que aumente cold start do Service" | O `CallScreeningService` **nunca** toca navegação. Nada do grafo entra no caminho da triagem. E a biblioteca **já está no APK hoje**: o custo atual já é o custo futuro |
| "Nada além do explicitamente pedido" | A biblioteca foi declarada no bootstrap **para esta fase**; consumi-la é cumprir o plano, não expandir escopo |
| Cold start da Activity | Medido: **mediana 680 ms** já **com** a biblioteca presente. Consumi-la não acrescenta artefato |

**Medido — o grafo funciona e a rota tipada não:**

```
PROBE_TEXTO_RESULTADO=SEM ERRO
PROBE_TIPADA_RESULTADO=kotlinx.serialization.SerializationException:
    Serializer for class 'TypeSafeHome' is not found.
```

E `javap` na classe `@Serializable` compilada, provando a ausência do plugin:

```
public final class org.sentinela.app.probe.TypeSafeHome {
  public static final org.sentinela.app.probe.TypeSafeHome INSTANCE;
  public static final int $stable;
  private org.sentinela.app.probe.TypeSafeHome();
  static {};
}
```

Sem `$serializer`. Sem `Companion`. Sem `serializer()`. A anotação compilou vazia.

**Padrão recomendado — rotas em constantes, nunca literais espalhados:**

```kotlin
// ui/navigation/SentinelaRoutes.kt
internal object Rotas {
    const val BOAS_VINDAS = "boas_vindas"
    const val ONBOARDING = "onboarding"
    const val HOME = "home"
    const val PROTECAO = "protecao"
    const val MODO_DISCADOR = "modo_discador"
}
```

```kotlin
// Verificado compilando neste repositório (AGP 9.3.0 / Nav 2.9.8).
NavHost(navController = nav, startDestination = destinoInicial) {
    composable(Rotas.BOAS_VINDAS) { /* … */ }
    composable(Rotas.ONBOARDING) { /* … */ }
    composable(Rotas.HOME) { /* … */ }
    composable(Rotas.PROTECAO) { /* … */ }
    composable(Rotas.MODO_DISCADOR) { /* liga a DialerActivationScreen — pendência 06-05 */ }
}
```

**"Pular onboarding" e "papel negado" usam a mesma primitiva** — `popUpTo … inclusive`, para que o
gesto de voltar na home **não** devolva o usuário ao onboarding (voltar ao onboarding depois de sair
dele seria a versão acidental do dark pattern que o CONTEXT proíbe):

```kotlin
nav.navigate(Rotas.HOME) {
    popUpTo(Rotas.BOAS_VINDAS) { inclusive = true }
    launchSingleTop = true
}
```

Medido: após esse `popUpTo`, a pilha fica `[home]`.

**"Aparece só na primeira abertura"** decide apenas o `startDestination`. A fonte natural é
`settingsRepository.appOpenCount` (já existe, já incrementado na `MainActivity`) ou uma chave nova de
onboarding concluído. **Atenção ao contrato de partida:** `appOpenCount` é `Flow`, e o incremento de
`onAppOpened()` roda **assíncrono no `Dispatchers.IO`**. Escolher `startDestination` a partir dele exige
um estado de carregamento explícito — recompor o `NavHost` com outro `startDestination` depois de já
composto **não** re-navega. Padrão correto: resolver o destino **antes** de compor o `NavHost` (a
`MainActivity` mantém um estado `Carregando/Decidido` e só chama o grafo no segundo).

**Onde o grafo vive:** `MainActivity` continua a única hospedeira (`CallActivity` e `DialerActivity` são
Activities separadas por exigência da telefonia e **ficam fora do grafo** — não são telas de navegação
do usuário). `MainActivity.onCreate` **mantém** a guarda `savedInstanceState == null` em torno de
`onAppOpened()`.

**Sobre as 4 abas.** `nav_home`, `nav_whitelist`, `nav_history`, `nav_settings` já existem em resources,
não usadas — a estrutura pretendida é uma barra inferior de 4 destinos. Mas **whitelist e histórico são
Phase 8**. Duas condutas aceitáveis, e a escolha é do planejador: (a) barra com os 4 destinos e dois
deles em estado vazio honesto; (b) apenas os destinos desta fase, e a barra cresce na Phase 8. **O que
não é aceitável** é aba que navega para tela em branco sem explicação — `UIX-10` exige estado
comunicado.

### Q2 — State holders sem framework de DI

**Recomendação: `ViewModel` + `ViewModelProvider.Factory` manual, mas só nas telas que têm estado
próprio.** O critério, e não a regra única, é o que importa:

| Tela | Holder | Por quê |
|------|--------|---------|
| Boas-vindas | nenhum | Estática; só dispara navegação |
| Onboarding | **`ViewModel`** | Tem progresso entre passos, sobrevive a rotação e a recriação de Activity pelo diálogo do sistema de permissão |
| Home | **`ViewModel`** | Combina 4 fontes (papel, configurações, contagem, última bloqueada); o estado vivo do papel precisa de dono |
| Proteção | **`ViewModel`** | Escreve no DataStore por `update {}`; a corotina precisa de escopo com ciclo de vida |
| Ativação do modo discador | nenhum novo | `DialerActivationScreen` já é composable **puro** com 4 callbacks; o estado é `DialerModeState`, derivado (§Q3) |

Um `remember { }` simples **não** basta para onboarding e Proteção por um motivo concreto desta fase:
o diálogo do sistema de permissão pode causar recriação da Activity, e progresso de onboarding perdido
nesse momento reinicia o fluxo justo depois de o usuário conceder algo. `ViewModel` (e
`SavedStateHandle`, se o progresso precisar sobreviver a morte de processo) resolve.

**Fiação concreta, compilada neste repositório:**

```kotlin
class HomeViewModel(
    private val settings: DataStoreSettingsRepository,
    private val history: RoomBlockedCallRepository,
    private val papelDeTriagem: ScreeningRoleManager,
    private val mask: (String) -> String,
) : ViewModel() {

    companion object {
        fun factory(container: AppContainer) = object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(
                modelClass: Class<T>,
                extras: CreationExtras,
            ): T = HomeViewModel(
                settings = container.settingsRepository,
                history = container.blockedCallRepository,
                papelDeTriagem = ScreeningRoleManager(/* … */),
                mask = container.maskNumber,
            ) as T
        }
    }
}
```

```kotlin
@Composable
fun HomeRoute(container: AppContainer) {
    val vm: HomeViewModel = viewModel(factory = HomeViewModel.factory(container))
    val estado by vm.estado.collectAsStateWithLifecycle()
    // …
}
```

Verificado a compilar: a sobrecarga **`create(modelClass, extras)`** de `ViewModelProvider.Factory`
(a não-deprecada em `lifecycle 2.11.0`), `viewModel(factory = …)`, e
`collectAsStateWithLifecycle()`.

**Como o `AppContainer` chega até lá.** Duas opções; recomendo a primeira:

1. **Parâmetro explícito**, do `MainActivity` para o grafo, do grafo para as rotas. Verboso e
   perfeitamente honesto — é o mesmo estilo que `DialerActivity` já usa
   (`(application as SentinelaApp).container`). Nenhuma mágica, nada de `CompositionLocal` a
   sequestrar a leitura.
2. `staticCompositionLocalOf<AppContainer>` — menos verboso, mas esconde a dependência e cria um
   caminho onde um composable de preview sem provedor explode. `@Preview` é usado intensamente neste
   projeto (23 na Phase 6). **Se** for adotado, todo composable de tela precisa continuar puro, com o
   container só na camada `…Route`.

**Regra que decorre disso e que vale como invariante da fase:** composable de **tela** recebe estado e
callbacks e **nunca** recebe `AppContainer`, exatamente como `DialerActivationScreen` e
`IncomingCallScreen` fazem hoje. Só o `…Route` (fino) conhece o container. Isso é o que mantém todas as
telas testáveis em JVM sem construir container — e a Phase 5 mediu que um **segundo** container no
mesmo processo derruba a aplicação.

**Efeito imediato na tela Proteção** — nada de botão salvar:

```kotlin
fun trocarPoliticaDeDesconhecidos(nova: OriginPolicy) {
    viewModelScope.launch { settings.update { it.copy(unknownPolicy = nova) } }
}
```

O cache `@Volatile` que a Phase 3 construiu, mantido por collector, é o que faz o valor chegar à
triagem. **Armadilha herdada de 06-08, e ela é do teste, não do produto:** gravar e triar no instante
seguinte é corrida no teste — o caso precisa **esperar o valor gravado ser reportado**, nunca assumir
que a escrita já apareceu no retrato.

### Q3 — Papel como estado VIVO

**A API não oferece observador.** O `SystemRoleGate` já documenta isso em KDoc, a Phase 5 registrou
("não existe aviso de mudança do papel de triagem para aplicativo comum") e o único ouvinte que a
plataforma expõe exige permissão fora da lista permitida deste projeto. **Consulta pontual na retomada
é a única forma. Nenhum esforço deve ser gasto procurando o observador que não existe.**

**Medido — a consulta é gratuita** (200 amostras, 20 de aquecimento, `Medium_Phone_API_35`):

| Consulta | p50 | p95 | max |
|----------|-----|-----|-----|
| `isRoleAvailable()` (triagem) | 8,2 µs | 13,8 µs | 1136,9 µs |
| `isRoleHeld()` (triagem) | 9,9 µs | 13,5 µs | 90,7 µs |
| `isRoleHeld()` (discador) | 10,8 µs | 86,8 µs | 153,5 µs |
| **trio completo de uma retomada** | **29,9 µs** | 255,1 µs | 284,3 µs |

30 microssegundos. Três ordens de grandeza abaixo de um quadro de 16 ms. **Não existe justificativa de
desempenho para cachear o papel**, e cachear compraria de volta exatamente a mentira que o CONTEXT
proíbe. O `max` de 1,1 ms na primeira consulta é o binder frio e continua irrelevante.

**Mecanismo — `LifecycleResumeEffect` (compilado aqui):**

```kotlin
LifecycleResumeEffect(Unit) {
    vm.reconsultarPapel()   // trio de 30 µs
    onPauseOrDispose { }
}
```

Por que `LifecycleResumeEffect` e não as alternativas:

| Opção | Veredito |
|-------|----------|
| **`LifecycleResumeEffect`** | **Recomendado.** Roda no `ON_RESUME`, roda de novo a cada retomada, escopo amarrado ao composable. Exatamente a semântica pedida |
| `LifecycleEventEffect(Lifecycle.Event.ON_RESUME)` | Equivalente e aceitável. `LifecycleResumeEffect` é preferível por já dar o par simétrico de descarte |
| `repeatOnLifecycle(STARTED)` num `LaunchedEffect` | **Errado para isto.** É para *coletar fluxo* enquanto visível. Não existe fluxo de papel para coletar; a consulta é pontual |
| Flag persistida | **Proibido pelo CONTEXT**, e a Phase 6 mediu por quê |

**Roteando para o seletor do sistema e detectando o retorno.** `buildRequestIntent()` já existe e já
devolve `null` quando o aparelho não oferece o papel (esse `null` é contrato, não descuido — disparar
uma intenção em aparelho sem o papel levaria a uma tela que não resolve nada). Lançar com
`StartActivityForResult` (compilado):

```kotlin
val seletorDePapel = rememberLauncherForActivityResult(
    ActivityResultContracts.StartActivityForResult(),
) { resultado ->
    // O código de resultado é uma DICA. O veredito é sempre reconsultar.
    vm.reconsultarPapel()
}
```

**Existe callback de resultado, e ele é insuficiente — este é o ponto mais importante da questão.**
Duas razões concretas:

1. **Conceder** o papel volta pelo callback normalmente (`RESULT_OK`). **Perder** o papel, não: a Phase 6
   mediu 3× que perder um papel **encerra o processo** (`Killing <pid> (adj 0): Permission or app op
   changed`). Se o usuário abre o seletor e **remove** o papel do Sentinela, o app **morre** e o callback
   nunca roda. O usuário volta num processo novo — e aí o único mecanismo que existe é a consulta na
   retomada.
2. O usuário pode trocar o app de telefone/triagem **inteiramente fora do nosso app**, pelas
   Configurações do sistema. Nenhum callback nosso participa disso.

**Portanto: o código de resultado nunca é a fonte da verdade. `isRoleHeld()` na retomada é.** O callback
serve apenas para reconsultar mais cedo, e o `LifecycleResumeEffect` cobriria o caso sozinho de todo
modo — o que faz da reconsulta no callback uma redundância deliberada, no molde das duas redes
permissivas da Fase 5 e das duas defesas do vigia da Fase 6.

**Home sem o papel.** `dashboard_role_missing` e `dashboard_fix_configuration` já existem em resources —
o estado e o botão de correção estavam previstos. O botão de correção **é** `buildRequestIntent()` +
`seletorDePapel.launch(...)`, e quando a intenção vem `null` o botão não pode aparecer.

### Q4 — Permissão em runtime na composição

**A regra pura já existe e não deve ser reescrita.** `RuntimePermissionAsk` (4 estados),
`runtimePermissionAsk(granted, alreadyAsked, rationale)`, `canRequest`, `shouldOfferSystemSettings`, a
fachada `ContactsPermissionState`, e as camadas finas `platform/ContactsPermissionChecker` e
`platform/NotificationPermissionChecker`. Esta fase entrega **só** o launcher e a tela.

```kotlin
val activity = LocalActivity.current as ComponentActivity   // ou LocalContext as Activity

val pedirContatos = rememberLauncherForActivityResult(
    ActivityResultContracts.RequestPermission(),
) { concedida ->
    vm.contatosRespondidos(concedida)   // reconsulta o estado pela função pura
}
```

```kotlin
fun pedirLeituraDaAgenda() {
    // Gravado ao DISPARAR, jamais no retorno.
    viewModelScope.launch { settings.markContactsPermissionAsked() }
    // …e só então o launcher.
}
```

**A ordem acima é contrato, não estilo**, e as Fases 4/5/6 já a estabeleceram três vezes
(`contacts_permission_asked`, `notification_permission_asked`, `call_phone_permission_asked`, todos
gravados ao disparar). O motivo: `shouldShowRequestPermissionRationale` devolve `false` nos **dois**
extremos — antes do primeiro pedido e depois da negação permanente. Sem o flag persistido os estados
`NEVER_ASKED` e `DENIED_PERMANENTLY` são indistinguíveis. E o flag precisa ser gravado antes porque o
usuário pode matar o app com o diálogo do sistema aberto; se dependesse do callback, o app voltaria
achando que nunca perguntou.

**Cuidado real de sequenciamento no `ViewModel`:** `markContactsPermissionAsked()` é `suspend` e o
`launch` do launcher não é. Disparar o launcher **dentro** da corotina, depois do `await` da escrita,
é o que casa com o contrato — mas atenção para não deixar o `launch` do launcher ocorrer só depois de
uma escrita lenta em disco, o que atrasaria o diálogo. O padrão da `DialerActivity` (hoje em produção)
dispara os dois em sequência sem esperar a escrita concluir, o que já satisfaz "gravado ao disparar".
**Seguir o precedente existente** em vez de inventar variação.

`POST_NOTIFICATIONS` segue o mesmo molde, e a decisão da Phase 5 permanece: a notificação própria fica
**desligada por padrão** e a permissão é pedida **somente no momento do opt-in**. O CONTEXT lista
"opt-in de notificação" como passo do onboarding — o passo **oferece** o opt-in; o diálogo do sistema só
aparece se o usuário aceitar.

`shouldOfferSystemSettings` ⇒ `ContactsPermissionChecker.appSettingsIntent(packageName)`, também já
pronto. Note que ele exige `Activity` (não `Context`) para o `rationale` — o `ViewModel` **não** pode
chamar `state(activity, …)`; a leitura acontece na camada composable/Activity e desce como valor.

**Nenhuma permissão nova nesta fase.** `READ_CONTACTS` e `POST_NOTIFICATIONS` já estão declaradas e já
constam da allowlist do `verify-invariants.sh`. Um plano que toque o manifest de permissões está errado.

### Anti-Patterns to Avoid

- **Rota tipada `@Serializable`.** Compila verde, **falha em execução** (medido). Se algum plano a
  propor, o plano está errado.
- **Cachear ou persistir o papel.** Proibido pelo CONTEXT; medido como desnecessário (30 µs).
- **Confiar no código de resultado do seletor de papel.** Perder papel mata o processo; o callback não
  roda.
- **`AppContainer` dentro de composable de tela.** Mata a testabilidade em JVM e afronta o padrão
  puro-com-callbacks das Fases 5-6.
- **Gravar o flag de "já perguntei" no callback.** Terceira vez que o projeto registra isso.
- **Botão "salvar" na tela Proteção.** Proibido pelo CONTEXT; o cache `@Volatile` já entrega efeito
  imediato.
- **Número completo na home.** Fronteira da Phase 6: `PhoneMask.mask` fora das telas de chamada/discagem.
- **Tocar `container.contactLookupRepository` para saber se a permissão existe.** Ver §Q8: aquele
  `by lazy` registra observador da agenda e dispara construção de cache **medida em 2,57 s**. O estado
  de permissão vem do `ContactsPermissionChecker`, que não toca o repositório.
- **Envolver componente compartilhado com semântica de mesclagem** para declarar desabilitado. Medido
  na Phase 6 (06-05): não funciona, porque o nó interno já mescla e é ele que responde às buscas. Usar
  `clearAndSetSemantics` com a ação de clique redeclarada.

---

## Don't Hand-Roll

| Problem | Don't Build | Use Instead | Why |
|---------|-------------|-------------|-----|
| Pilha de retorno, gesto de voltar, `popUpTo` | `sealed class` + `mutableStateOf` de tela atual | `navigation-compose` **já no APK** | O único argumento a favor do à-mão (não pagar a biblioteca) é falso aqui: ela já está paga |
| Estado sobrevivendo a rotação e ao diálogo do sistema | salvar à mão em `Bundle` | `ViewModel` (+ `SavedStateHandle`) | `lifecycle-viewmodel-compose` já no classpath |
| Saber se a permissão foi negada de vez | ler `shouldShowRequestPermissionRationale` direto | `runtimePermissionAsk(...)` **já existente** | `rationale` é `false` nos dois extremos; a regra pura + flag já resolve e já é coberta pelo Kover |
| Resultado de permissão / de Activity | `onActivityResult` / `onRequestPermissionsResult` | `rememberLauncherForActivityResult` | Contratos deprecados; e a `DialerActivity` já usa o moderno |
| Estado do modo discador | flag "modo ligado" | `dialerModeState(...)` **já existente** | Papel detido sempre vence intenção gravada (06-03) |
| Máscara de número | formatação própria na tela | `PhoneMask.mask` / `container.maskNumber` | Máscara única do projeto, já dentro de `runCatching` |
| Reagir à mudança de papel | procurar listener de papel | consulta na retomada | O listener **não existe** para app comum (KDoc + Phase 5) |
| Assert de alvo de toque ≥ mínimo | `assertTouchHeightIsEqualTo` | os 3 helpers da Phase 6 (§Q6) | A biblioteca só oferece igualdade; igualdade é o assert errado para contrato de mínimo |

**Key insight:** esta fase é a que menos precisa construir infraestrutura em todo o projeto. Cinco
bibliotecas já no classpath, a máquina de permissão pronta e pura, os gates de papel prontos, a máscara
pronta, nove componentes acessíveis prontos, os asserts de dois eixos prontos e 131 strings pt-BR
prontas. O trabalho é **ligar** e **provar**, não inventar. Todo plano que construa mecanismo novo
merece a pergunta "isto já existe desde a Phase 4?".

---

## Common Pitfalls

### Pitfall 1: rota tipada — falso-verde de compilação

**O que dá errado:** `composable<Rota>()` com `@Serializable` compila, o build fica verde, o lint fica
verde, e a primeira composição do `NavHost` lança
`SerializationException: Serializer for class 'X' is not found`.
**Por que acontece:** o plugin compilador `kotlinx-serialization` **não** está aplicado. O AGP 9 traz
Kotlin embutido, mas **não** traz esse plugin. `@Serializable` vira anotação vazia (confirmado por
`javap`: sem `$serializer`, sem `Companion`).
**Como evitar:** rotas por texto, em constantes. E um teste de fluxo que **componha o `NavHost` de
verdade** — nenhum assert de compilação pega isto; só a composição pega.
**Sinais de alerta:** qualquer `@Serializable` num arquivo de navegação; qualquer plano propondo o
plugin de serialização.

### Pitfall 2: `startDestination` decidido a partir de fluxo assíncrono

**O que dá errado:** "onboarding só na primeira abertura" lido de um `Flow` do DataStore; o primeiro
valor chega **depois** de o `NavHost` já ter sido composto, e o grafo **não** re-navega ao trocar
`startDestination`. O usuário veterano vê o onboarding, ou o novato vai direto para a home.
**Por que acontece:** `onAppOpened()` incrementa em `Dispatchers.IO`; a primeira leitura do DataStore
custa disco (**10,9 ms medidos na Phase 3**). O `startDestination` é lido **uma vez**.
**Como evitar:** resolver o destino **antes** de compor o `NavHost` — estado `Carregando` explícito na
`MainActivity` (`state_loading` já existe em resources, e `UIX-10` pede isso de todo modo).
**Sinais de alerta:** `startDestination = if (contador == 0) …` diretamente sobre um `collectAsState`
com valor inicial.

### Pitfall 3: confiar no callback do seletor de papel

**O que dá errado:** o app conclui que o papel continua sendo dele porque nenhum callback disse o
contrário.
**Por que acontece:** perder um papel **encerra o processo** (Phase 6, medido 3×). O callback nunca
roda; o app volta como processo novo. E trocar o app padrão pelas Configurações do sistema não passa
por callback nenhum.
**Como evitar:** `isRoleHeld()` na retomada é sempre o veredito. Custa 30 µs.
**Sinais de alerta:** qualquer campo guardando "papel concedido".

### Pitfall 4: `contactLookupRepository` tocado pela UI

**O que dá errado:** a home fica lenta ou a agenda ganha um observador cedo demais.
**Por que acontece:** `container.contactLookupRepository` é `by lazy` e sua construção registra
observador do provider e dispara a construção do conjunto de chaves — **medida em 2,57 s** com 5.000
contatos na Phase 4, e por isso ela nunca é aguardada.
**Como evitar:** a UI desta fase **não precisa** dele. Estado de permissão vem de
`ContactsPermissionChecker`. Nenhuma tela desta fase precisa consultar contato.
**Sinais de alerta:** `contactLookupRepository` aparecendo em qualquer `ViewModel` desta fase.

### Pitfall 5: assert de acessibilidade em um eixo só

**O que dá errado:** o teste fica verde com o layout quebrado.
**Por que acontece:** o Compose expande o alvo de toque de qualquer componente interativo até o mínimo
da plataforma por conta própria. Afirmar só o alvo mede a garantia da biblioteca.
**Como evitar:** **dois eixos** — alvo de toque **e** tamanho desenhado. Pegou 4 bugs reais na Phase 6
(72dp comprimido a 23dp; outro a altura zero). `requiredSize` quando o contrato é de tamanho fixo;
`size()` negocia com o pai e o pai comprime.
**Sinais de alerta:** um teste com `assertTouchHeightIsAtLeast` sem `assertLayoutHeightIsAtLeast` ao
lado.

### Pitfall 6: semântica mesclada engolindo estado

**O que dá errado:** um controle desabilitado deixa de ser reportado como desabilitado ao TalkBack, em
silêncio.
**Por que acontece:** medido na Phase 6 (06-05) — envolver o componente compartilhado com semântica de
mesclagem **não** funciona, porque o nó interno dele já mescla e é ele que responde às buscas.
**Como evitar:** `clearAndSetSemantics` no ramo, com a ação de clique redeclarada. E asserts com
`useUnmergedTree` quando o alvo for um nó interno.
**Sinais de alerta:** `assertIsNotEnabled` verde num componente que você sabe ter sido envolvido.

### Pitfall 7: teste de composição no dispositivo padrão do Robolectric

**O que dá errado:** todo assert de exibição fica vermelho por motivo falso — o conteúdo sai do
viewport.
**Como evitar:** `@Config(sdk = [35], qualifiers = "w411dp-h891dp-xxhdpi")`. **Sempre** `sdk = [35]`:
`[36]` exige Java 21 e o projeto está em JDK 17.

### Pitfall 8: `%` não escapado em string

**Medido, e é um defeito latente real.** O lint aponta `PluralsCandidate` em
`app/src/main/res/values/strings.xml:266` (`dialer_activation_unchanged_4`, "…continua 100% offline…"):
o `%` cru faz o lint ler um especificador de formato. Enquanto a string é lida por `stringResource`
sem argumentos, nada quebra; no dia em que alguém a passar por `String.format` ou lhe der argumento,
lança em execução. **Escopo legítimo desta fase** ao revisitar a política de lint (§Q7): escapar como
`%%` ou reescrever. Não é bug da Phase 6 — é aviso que só agora tem dono.

### Pitfall 9: auto-sabotagem por grep (já pegou seis executores)

Critério de aceite por contagem-zero casando com o próprio KDoc/comentário que o plano manda escrever.
Nesta fase o risco concreto é um comentário citando a rota tipada, o `@Serializable` ou o identificador
do aplicativo. **Regra:** proibição descrita em **prosa portuguesa**, sem escrever o identificador
vigiado; e critério que precise falar de identificador afirma sobre objeto construído em tempo de teste,
nunca sobre a existência de texto no fonte. O Bloco 2 (identificador do aplicativo literal) já pegou
**três** executores.

### Pitfall 10: `grep -c` e evidência de cache

`grep -c` sai com código 1 quando conta zero — capturar em variável (`[ "$(grep -c …)" -eq 0 ]`),
**nunca** `|| echo 0`, e `set -e` continua proibido em `verify-invariants.sh`. Evidência só vale com
`clean` **e** `--no-build-cache`: `FROM-CACHE` tem o mesmo defeito probatório que `UP-TO-DATE`. Teste
que lê arquivo do disco precisa do input declarado — `src/main/java`, `src/main/res` e `schemas` já
estão declarados em `app/build.gradle.kts`; **nenhuma edição nova é necessária** para as telas desta
fase.

---

## Code Examples

Todos verificados a **compilar** neste repositório (AGP 9.3.0, Kotlin 2.4.10, Nav 2.9.8,
lifecycle 2.11.0) e, onde indicado, a **executar** sob Robolectric.

### Fluxo multi-tela testado em JVM — executado, não hipotético

```kotlin
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35], qualifiers = "w411dp-h891dp-xxhdpi")
class FluxoDeOnboardingTest {

    @get:Rule val compose = createComposeRule()

    private lateinit var nav: NavHostController

    @Test
    fun pularOnboardingNaoDeixaVoltarParaEle() {
        compose.setContent {
            nav = rememberNavController()
            NavHost(navController = nav, startDestination = Rotas.BOAS_VINDAS) {
                composable(Rotas.BOAS_VINDAS) { /* … */ }
                composable(Rotas.ONBOARDING) { /* … */ }
                composable(Rotas.HOME) { /* … */ }
            }
        }
        compose.onNodeWithText("Começar").performClick()
        compose.waitForIdle()
        assertEquals(Rotas.ONBOARDING, nav.currentDestination?.route)

        compose.onNodeWithText("Concluir").performClick()
        compose.waitForIdle()
        assertEquals(Rotas.HOME, nav.currentDestination?.route)

        // Após popUpTo(BOAS_VINDAS) { inclusive = true } sobra só a home.
        assertEquals(
            listOf(Rotas.HOME),
            nav.currentBackStack.value.mapNotNull { it.destination.route },
        )
    }
}
```

Saída real da sonda equivalente:

```
PROBE_APOS_1=onboarding
PROBE_APOS_2=home
PROBE_PILHA=[home]
```

**Detalhe medido que economiza uma depuração:** `currentBackStack` inclui a entrada do próprio
`NavGraph`, cuja `route` é `null`. O `mapNotNull` acima não é enfeite — sem ele a lista esperada tem um
elemento fantasma.

### Reconsulta viva do papel

```kotlin
@Composable
fun HomeRoute(container: AppContainer, vm: HomeViewModel) {
    LifecycleResumeEffect(Unit) {
        vm.reconsultarPapel()   // trio medido em p50 30 µs
        onPauseOrDispose { }
    }

    val seletorDePapel = rememberLauncherForActivityResult(
        ActivityResultContracts.StartActivityForResult(),
    ) { vm.reconsultarPapel() }   // redundância deliberada; o veredito é a retomada

    val estado by vm.estado.collectAsStateWithLifecycle()
    HomeScreen(
        estado = estado,
        onCorrigirConfiguracao = {
            // null quando o aparelho não oferece o papel — o botão não pode nem aparecer.
            vm.intencaoDePedidoDoPapel()?.let(seletorDePapel::launch)
        },
    )
}
```

### Fábrica manual de `ViewModel`

```kotlin
companion object {
    fun factory(container: AppContainer) = object : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>, extras: CreationExtras): T =
            HomeViewModel(container.settingsRepository, container.blockedCallRepository) as T
    }
}
```

### Asserts de acessibilidade em dois eixos (já escritos, a extrair)

```kotlin
compose.onNodeWithText(rotulo)
    .assertTouchHeightIsAtLeast(48.dp)     // o dedo alcança
    .assertLayoutHeightIsAtLeast(48.dp)    // …e o desenho não encolheu — o eixo com dentes
```

---

## State of the Art

| Old Approach | Current Approach | When Changed | Impact |
|--------------|------------------|--------------|--------|
| `onActivityResult` / `onRequestPermissionsResult` | `rememberLauncherForActivityResult` + contratos | Activity 1.2+ | Já é o padrão em uso na `DialerActivity` |
| `create(modelClass)` de `ViewModelProvider.Factory` | `create(modelClass, extras: CreationExtras)` | Lifecycle 2.5+ | A sobrecarga com `extras` é a não-deprecada; **verificada a compilar** aqui |
| `collectAsState()` | `collectAsStateWithLifecycle()` | Lifecycle 2.6+ | Para de coletar em background; **verificada** |
| `DisposableEffect` + `LifecycleObserver` à mão | `LifecycleResumeEffect` / `LifecycleEventEffect` | lifecycle-runtime-compose 2.7+ | **Verificado**; é o mecanismo do estado vivo do papel |
| rotas por texto | rotas tipadas `@Serializable` | Navigation 2.8+ | **NÃO adotável aqui** — medido falhando em execução por ausência do plugin de serialização |

**Deprecado/desatualizado:**
- `Robolectric @Config(sdk = [36])` — impossível em JDK 17. **Sempre `[35]`.**
- `androidx.compose.material` (M2) — o projeto é M3.
- Bloco `testImplementation` duplicado em `app/build.gradle.kts` L247-249 e L253-255: as **mesmas três
  linhas** (`platform(compose-bom)`, `compose-ui-test-junit4`, `compose-ui-test-manifest`) aparecem duas
  vezes, com dois comentários diferentes da Phase 6. Inofensivo para o build, mas é ruído; remover a
  segunda cópia é faxina legítima de baixo risco enquanto esta fase já mexe no arquivo por §Q7.

---

## Q7 — A supressão de lint da Phase 1: MEDIDO

**Método:** removida a linha `"UnusedResources"` do `disable` de `app/build.gradle.kts`, executado
`./gradlew :app:lintDebug --rerun-tasks`, contados os achados no relatório SARIF. Arquivo restaurado
depois; árvore verificada limpa.

**Número real — 133 achados de `UnusedResources`:**

| Tipo | Quantidade |
|------|-----------|
| `R.string.*` | **131** |
| `R.color.sentinela_primary` | 1 |
| `R.mipmap.ic_launcher_round` | 1 |
| **Total** | **133** |

(O `strings.xml` tem 226 chaves no total, então **95 já são consumidas** pelas telas das Fases 5-6.)

**Achado que muda a leitura do problema:** com `UnusedResources` **reabilitado**, `./gradlew lintDebug`
**passou — exit 0.** `UnusedResources` é de severidade **warning**, `abortOnError = true` só aborta em
**erro**, e `warningsAsErrors` não está configurado. **A supressão da Phase 1 nunca foi necessária para
manter o build verde**; ela servia para manter o relatório limpo. Isso deve ser dito com franqueza no
plano, em vez de repetir a premissa de que 132 strings "quebravam o build".

**Distribuição por prefixo — é isto que decide o veredito:**

| Prefixo | Qtd | Fase que consome |
|---------|-----|------------------|
| `settings_*` | 19 | **7** (tela Proteção) |
| `contacts_*` | 12 | **7** |
| `dashboard_*` | 11 | **7** |
| `welcome_*` | 10 | **7** |
| `onboarding_*` | 8 | **7** |
| `unknown_*` | 8 | **7** |
| `nav_*` | 4 | **7** (parcial — 2 rótulos são de destinos da Phase 8) |
| `action_*`, `state_*` | 4 | **7** |
| `notification_*`, `dialpad_*`, `dialer_*` | 3 | 7 / resíduo de fase anterior |
| **subtotal Phase 7** | **~79** | |
| `whitelist_*` | 25 | **8** |
| `history_*` | 11 | **8** |
| `about_*` | 7 | **9** |
| `support_*` | 5 | **9** |
| `review_*` | 4 | **9** |
| **subtotal Phases 8-9** | **52** | |

**Veredito: a supressão NÃO pode ser removida nesta fase, mas PODE ser narrowed — e o narrowing foi
testado.** Removê-la de vez deixaria ~52 achados de fases futuras poluindo o relatório e a supressão
voltaria na Phase 8, o que é pior que narrowing.

**Mecanismo verificado funcionando** (`app/lint.xml`, criado, executado e removido):

```xml
<?xml version="1.0" encoding="utf-8"?>
<lint>
    <issue id="UnusedResources">
        <ignore regexp="R\.string\.(whitelist|history|about|support|review)_" />
    </issue>
</lint>
```

**Medida: 133 → 81 achados.** Exatamente os 52 de fases futuras silenciados, e **nada mais** — o
`<ignore regexp>` casa a mensagem do achado, e isso foi confirmado pela aritmética, não presumido.

**Recomendação concreta para o último plano da fase:**
1. Remover `"UnusedResources"` do `disable` em `app/build.gradle.kts`.
2. Criar `app/lint.xml` com o `<ignore regexp>` restrito aos prefixos das Phases 8-9, **cada prefixo
   comentado com a fase que o consome** — para que a Phase 8 saiba exatamente qual linha apagar.
3. Resolver os 2 achados não-string: `R.color.sentinela_primary` e `R.mipmap.ic_launcher_round`. **Não
   são consumíveis de fase futura** — são resíduo real do esqueleto e devem ser usados, apagados ou
   ignorados com justificativa nominal. `ic_launcher_round` relaciona-se com a pendência do ícone
   registrada em `STATE.md`.
4. **Prova de vermelho:** acrescentar uma string com prefixo desta fase e não usá-la ⇒ o achado deve
   aparecer. Sem isso, o narrowing pode estar silenciando mais do que se pensa. E fazer o `disable`
   voltar não é prova — a medida acima mostra que ele nunca foi o que segurava o build.
5. **Não** ampliar `warningsAsErrors` de carona. Fora do escopo, e transformaria os 81 achados
   remanescentes em build vermelho.

**Nota de sequenciamento:** este trabalho pertence ao **último** plano da fase, no precedente do
`koverVerify`/excludes (06-08/06-09). Reabilitar o lint antes de as telas consumirem as strings deixaria
o relatório artificialmente ruim e nada provaria.

**Outros achados de lint hoje, todos warning, todos reais:**

| Regra | Local | Nota |
|-------|-------|------|
| `PluralsCandidate` | `strings.xml:266` | `%` não escapado — Pitfall 8. Escopo desta fase |
| `AutoboxingStateCreation` | `ui/dialer/DialpadScreen.kt:99` | `mutableStateOf` de `Int` ⇒ `mutableIntStateOf`. Faxina trivial, opcional |
| `ModifierFactoryExtensionFunction` | `ui/call/CallControlButton.kt:54` | Resíduo de estilo da Phase 6. Opcional |
| `NewerVersionAvailable` | `libs.versions.toml:16` | **Ignorar.** O piso 1.8.1 é deliberado (`room-migration`) |

---

## Q8 — Orçamento de partida a frio da Activity: MEDIDO

**Método:** `assembleDebug` + `installDebug` no `Medium_Phone_API_35`; 8 partidas a frio com
`am force-stop` antes de cada uma, `am start -W`. Código de produção **inalterado** (sondas removidas
antes da medição).

| Execução | TotalTime | WaitTime |
|----------|-----------|----------|
| 1 | 917 ms | 941 ms |
| 2 | 619 ms | 620 ms |
| 3 | 716 ms | 720 ms |
| 4 | 620 ms | 634 ms |
| 5 | 643 ms | 644 ms |
| 6 | 612 ms | 616 ms |
| 7 | 775 ms | 776 ms |
| 8 | 849 ms | 851 ms |

**Mediana 680 ms** (612–917 ms). Conforme a doutrina do projeto, o número de referência é a
**mediana**; a cauda é reportada e não vira assert — no emulador ela mede o escalonador do hospedeiro
tanto quanto o app.

**Veredito: nada precisa ser diferido, e o motivo é estrutural, não sortudo.**

1. **O `AppContainer` não constrói nada na partida.** Todo colaborador é `by lazy` e `Application.onCreate`
   não faz I/O síncrono — invariante da Phase 3, ainda intacto. Os 680 ms acima são a
   `PlaceholderScreen` mais o custo de processo/Compose, com todo o classpath (Room, DataStore,
   libphonenumber, navigation, lifecycle) **presente mas não construído**.
2. **O único trabalho da partida já está fora da thread principal.** `onAppOpened()` faz
   `appScope.launch { incrementAppOpenCount(); pruneNow() }` em `Dispatchers.IO` — é ali que DataStore e
   Room de fato nascem, sem bloquear o primeiro quadro.
3. **O que esta fase acrescenta é barato e medido.** Consultas de papel: **30 µs**. Primeira leitura do
   DataStore: **10,9 ms** (Phase 3), e fora da thread principal. `navigation-compose` e `lifecycle` já
   estão no APK e já contam nos 680 ms.

**As duas regras a preservar, e ambas são fáceis de violar sem perceber:**

- **`contactLookupRepository` continua proibido para a UI desta fase.** Ele registra observador da
  agenda e dispara construção de cache medida em **2,57 s** (Phase 4). Nenhuma tela desta fase precisa
  dele.
- **O primeiro quadro não espera disco.** O estado inicial de `collectAsStateWithLifecycle` precisa ser
  um valor honesto de carregamento (`state_loading` já existe), nunca um `runBlocking` sobre `snapshot()`.
  Um `runBlocking` na thread principal para decidir o `startDestination` é a forma mais provável de
  alguém estragar estes 680 ms — e é a mesma tentação descrita no Pitfall 2, cuja solução correta
  (estado `Carregando` explícito) também é a solução de desempenho.

**Recomendação de guarda-corpo:** um caso instrumentado que afirme a **mediana** de `TotalTime` abaixo
de um teto folgado (sugestão: 1200 ms no AVD, ~1,75× a mediana medida) e reporte a cauda em logcat.
Assert de tempo é frágil por natureza — no molde de 05-06, ele deve carregar junto um assert
**estrutural** (nenhuma construção do container na thread principal), porque as duas sabotagens se
anulariam se o assert fosse só de cronômetro. Alternativa mais barata e mais estável, no molde do
Bloco 7: uma checagem em `verify-invariants.sh` de que `Application.onCreate` não toca o container além
do necessário. **Cronômetro não prova estrutura** — lição acumulada do projeto.

---

## Q5 / Q6 — Teste de Compose e acessibilidade

### Q5 — o que já está no classpath, e o que o fluxo multi-tela exige

Já declarados em `testImplementation` (Phase 6, `app/build.gradle.kts` L247-255):
`platform(compose-bom)`, `compose-ui-test-junit4`, `compose-ui-test-manifest`, `robolectric 4.16.1`,
`androidx-test-core`, `junit4`, `mockk`, `turbine`, `coroutines-test`. **Nada a acrescentar.**

**Confirmado por execução:** `createComposeRule` sob Robolectric hospeda um `NavHost` **real**, e
`navigate`, `popUpTo`, `currentDestination`, `currentBackStack` e
`currentBackStackEntryAsState` todos funcionam em JVM. **O fluxo inteiro desta fase é testável sem
emulador** — o que é a diferença mais importante em relação à Phase 6, onde o vínculo do serviço de
chamada obrigava instrumentação.

**Setup obrigatório para tela inteira:**

```kotlin
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35], qualifiers = "w411dp-h891dp-xxhdpi")
```

**Três diferenças de um teste de fluxo em relação a um de composable isolado:**

1. **O `NavHostController` precisa escapar do `setContent`** para os asserts de destino — capturar num
   `lateinit var` do próprio teste (padrão do exemplo acima).
2. **`compose.waitForIdle()` depois de cada `performClick` que navega.** A transição de destino não é
   síncrona com o clique.
3. **A entrada do `NavGraph` tem `route` nula** em `currentBackStack` — filtrar (medido).

**Fábrica de `ViewModel` nos testes:** o teste constrói o `ViewModel` com dublês e o injeta, **nunca**
constrói `AppContainer` (a Phase 5 mediu que um segundo container derruba o processo). É isso que a
regra "tela recebe estado e callbacks" compra.

### Q6 — o subconjunto automatizável de "TalkBack navega o fluxo inteiro"

O critério 4 do CONTEXT é frequentemente tratado como não-automatizável. **A maior parte dele é.** A
tabela separa honestamente o que tem dentes do que não tem:

| Propriedade | Automatizável em JVM? | Como |
|-------------|----------------------|------|
| `contentDescription` presente em todo controle | **SIM** | `onNodeWithContentDescription`, ou varrer a árvore semântica exigindo descrição em todo nó com ação de clique |
| Descrição vindo de **resources** (não hardcoded) | **SIM** | Precedente `CallStringsTest`: ler o texto do recurso em tempo de teste e comparar com o nó |
| Alvo de toque ≥ 48dp | **SIM — e em dois eixos** | `assertTouchHeightIsAtLeast` **+** `assertLayoutHeightIsAtLeast` |
| `stateDescription` em controle com estado | **SIM** | Ler `SemanticsProperties.StateDescription` do nó |
| Estado não comunicado só por cor | **SIM (por proxy)** | Exigir `stateDescription` **ou** texto para cada estado. Nenhum teste lê cor; a exigência de rótulo textual é o proxy com dentes |
| Ordem de travessia declarada | **SIM** | `SemanticsProperties.TraversalIndex` / `isTraversalGroup` (precedente `CallScreenSemanticsTest.kt:230`) |
| Habilitado/desabilitado alcançável | **SIM, com cuidado** | Pitfall 6: semântica mesclada engole o estado. Usar `useUnmergedTree` quando necessário |
| Rótulo de ação de clique | **SIM** | `SemanticsActions.OnClick.label` |
| Fluxo **navegável** de ponta a ponta | **SIM** | Teste de fluxo do §Q5: percorrer boas-vindas → onboarding → home só por nós **com descrição**, provando que o caminho existe sem depender de posição na tela |
| Ordem de foco **real** do TalkBack | **NÃO** | O varredor é do sistema. Automatizável é a ordem **declarada** |
| Locução, ritmo, verbosidade | **NÃO** | Cenários físicos ≥ 61, Phase 9 |
| Gestos do TalkBack (explorar por toque, deslizar) | **NÃO** | Phase 9 |
| Contraste de cor medido | **NÃO nesta fase** | O tema é fixado por `ThemeTokensTest`/`CallColorFixationTest` desde as Fases 1/6; medir razão de contraste seria escopo novo |

**Resumo honesto:** as **nove** primeiras linhas são automatizáveis em JVM e devem ser, cobrindo a maior
parte de `UIX-09` e do critério 4. Só locução, gestos e ordem de foco **efetiva** ficam para o aparelho.
Isso segue o padrão do projeto: automatizar o automatizável, deferir só hardware — e **não** deferir a
coisa inteira para a Phase 9 porque a palavra "TalkBack" aparece no critério.

**Item de Wave 0 concreto.** Os três helpers (`assertTouchHeightIsAtLeast`, `assertTouchWidthIsAtLeast`,
`assertLayoutHeightIsAtLeast`) hoje são funções `internal` de nível de arquivo **dentro de**
`app/src/test/java/org/sentinela/app/ui/call/CallScreenSemanticsTest.kt` (L265-290), no pacote
`…ui.call`. As telas desta fase vivem em outros pacotes. **Extrair para um arquivo de apoio neutro** —
ex. `app/src/test/java/org/sentinela/app/ui/TouchTargetAsserts.kt` — na Wave 0, sem alterar
comportamento, e reapontar o teste da Phase 6. Motivo adicional além da ergonomia: a Phase 5 registrou
que classe de teste **não** enxerga membros de outra classe de teste entre sandboxes de SDK do
Robolectric, e um arquivo de apoio neutro é exatamente a forma que já resolveu isso antes. Duplicar os
helpers seria a alternativa errada — o eixo com dentes divergiria entre as duas cópias.

---

## Validation Architecture

### Test Framework

| Property | Value |
|----------|-------|
| **Framework (JVM puro)** | JUnit 4 `4.13.2` + MockK `1.14.11` + `kotlinx-coroutines-test 1.11.0` + Turbine `1.2.1`, sobre AGP 9.3.0 / Gradle 9.6.1 / **JDK 17** |
| **Framework (Compose + navegação)** | `createComposeRule` sob **Robolectric 4.16.1**, `@Config(sdk = [35], qualifiers = "w411dp-h891dp-xxhdpi")`. **`NavHost` real funciona em JVM — medido.** Nenhuma captura de imagem |
| **Framework (instrumentado)** | `AndroidJUnitRunner` + `androidx.test:rules`, AVD `Medium_Phone_API_35` |
| **Config file** | `app/build.gradle.kts` (`testOptions.unitTests`, bloco `lint`, bloco `kover`, inputs de `Test`) |
| **Cobertura** | Kover `0.9.9`, gate `koverVerify minBound(80)`, atual **96,69%** |
| **Quick run command** | `./gradlew testDebugUnitTest --rerun-tasks` |
| **Instrumented command** | `bash scripts/run-instrumented-tests.sh [--tests "*Padrao"]` — `connectedDebugAndroidTest` **não aceita** `--tests` |
| **Full suite command** | `./gradlew clean && ./gradlew --no-build-cache assembleDebug testDebugUnitTest koverVerify lint detekt && bash scripts/verify-invariants.sh && bash scripts/run-instrumented-tests.sh` |
| **Estimated runtime** | quick ~40-60 s · instrumentado ~60 s incremental + 2-4 min de boot a frio (medido: boot em ~10 s nesta sessão, com o AVD já aquecido) · full ~8-12 min |
| **Relatórios** | JVM: `app/build/test-results/testDebugUnitTest/*.xml` · Kover: `app/build/reports/kover/` · lint: `app/build/reports/lint-results-debug.{html,sarif}` (**o SARIF é o que permite contagem por regra** — usado em §Q7) · androidTest: `app/build/outputs/androidTest-results/connected/debug/TEST-*.xml` |

**Dependência nova: NENHUMA.** Runtime e teste, ambos completos. Um plano que acrescente biblioteca,
plugin ou entrada no version catalog está errado.

**Permissão nova: NENHUMA.** `READ_CONTACTS` e `POST_NOTIFICATIONS` já declaradas e já na allowlist.
Um plano que edite as permissões do manifest está errado.

**Inputs de task já declarados e suficientes:** `schemas`, `src/main/java` e `src/main/res` já são
inputs de todas as `Test` tasks. As telas desta fase não exigem input novo — **mas** se um plano criar
um teste que leia qualquer outro arquivo do disco, o input precisa ser declarado, sob pena de
`UP-TO-DATE` dar verde falso.

### Phase Requirements → Test Map

| Req ID | Behavior | Test Type | Automated Command | File Exists? |
|--------|----------|-----------|-------------------|--------------|
| SCR-01 | Onboarding pede o papel; negado ⇒ home com estado real, sem laço | Compose+Robolectric (gate de papel dublado) | `./gradlew testDebugUnitTest --tests "*OnboardingFlowTest" --rerun-tasks` | ❌ Wave 0 |
| SCR-02 | Papel reconsultado a **cada** retomada; correção funciona; nunca de flag | Compose+Robolectric (contador de consultas por retomada) | `./gradlew testDebugUnitTest --tests "*RoleLiveStateTest" --rerun-tasks` | ❌ Wave 0 |
| SCR-02 | Custo da consulta de papel no aparelho | instrumentado (p50) | `bash scripts/run-instrumented-tests.sh --tests "*RoleQueryCostTest"` | ❌ opcional — já medido em 30 µs na pesquisa |
| UIX-01 | Fluxo completo, incluindo **pular** e pilha sem retorno ao onboarding | Compose+Robolectric (fluxo multi-tela) | `./gradlew testDebugUnitTest --tests "*OnboardingFlowTest" --rerun-tasks` | ❌ Wave 0 |
| UIX-01 | Flag de "já perguntei" gravado ao **disparar**, nunca no retorno | unit puro (ordem de eventos) | `./gradlew testDebugUnitTest --tests "*PermissionAskOrderTest" --rerun-tasks` | ❌ Wave 0 |
| UIX-02 | Home: papel, proteção, contagem, aviso de agenda negada | Compose+Robolectric | `./gradlew testDebugUnitTest --tests "*HomeScreenStateTest" --rerun-tasks` | ❌ Wave 0 |
| UIX-02 | Última bloqueada **mascarada**; número completo nunca sai na home | unit puro (fronteira de privacidade) | `./gradlew testDebugUnitTest --tests "*HomePrivacyTest" --rerun-tasks` | ❌ Wave 0 |
| UIX-02 | Histórico desligado mostra **esse estado**, não `0` | unit puro (state holder) | `./gradlew testDebugUnitTest --tests "*HomeViewModelTest" --rerun-tasks` | ❌ Wave 0 |
| UIX-03 | Cada opção grava e o retrato reporta (efeito imediato, sem salvar) | Compose+Robolectric + repositório real em `TemporaryFolder` | `./gradlew testDebugUnitTest --tests "*ProtectionScreenTest" --rerun-tasks` | ❌ Wave 0 |
| UIX-03 | Rota para a ativação do modo discador nos 5 ramos de estado | Compose+Robolectric | `./gradlew testDebugUnitTest --tests "*DialerRouteTest" --rerun-tasks` | ❌ Wave 0 |
| UIX-07 | Zero texto embutido em Kotlin nas telas da fase | grep estrutural em `verify-invariants.sh` (**Bloco 9**) | `bash scripts/verify-invariants.sh` | ✅ estender |
| UIX-09 | Alvo ≥ 48dp em **dois eixos**, descrição, `stateDescription`, travessia | Compose+Robolectric | `./gradlew testDebugUnitTest --tests "*Phase7SemanticsTest" --rerun-tasks` | ❌ Wave 0 (helpers a extrair) |
| UIX-10 | Carregando e erro em toda tela; proteção inativa com destaque | Compose+Robolectric | `./gradlew testDebugUnitTest --tests "*HomeScreenStateTest" --tests "*ProtectionScreenTest" --rerun-tasks` | ❌ Wave 0 |
| UIX-11 | Nenhuma promessa falsa nas chaves da fase | Robolectric (lê o **texto** dos resources) | `./gradlew testDebugUnitTest --tests "*CallStringsTest" --rerun-tasks` | ✅ **estender às chaves da fase** |
| — | **Rota tipada proibida** (falso-verde de compilação) | Compose+Robolectric **ou** Bloco 9 | `./gradlew testDebugUnitTest --tests "*NavGraphContractTest" --rerun-tasks` | ❌ Wave 0 |
| — | Partida a frio dentro do orçamento + nenhuma construção de container na thread principal | instrumentado (mediana) + assert estrutural | `bash scripts/run-instrumented-tests.sh --tests "*ColdStartTest"` | ❌ opcional (baseline 680 ms medido) |
| — | Lint com `UnusedResources` reabilitado e narrowed | lint | `./gradlew lint --rerun-tasks` + contagem no SARIF | ✅ **último plano da fase** |

Nenhum requisito da fase fica sem verificação automatizada. **O único ponto genuinamente manual é
locução/gestos reais do TalkBack** (Phase 9, cenários ≥ 61) — e ele é **deferred, não gap**, porque as
nove propriedades semânticas automatizáveis de §Q6 ficam nesta fase.

### Sampling Rate

- **Por commit de task:** `./gradlew testDebugUnitTest --rerun-tasks` (< 60 s). `--rerun-tasks` é
  obrigatório: testes desta fase leem resources do disco.
- **Por merge de wave:** `./gradlew --rerun-tasks assembleDebug testDebugUnitTest lint detekt &&
  bash scripts/verify-invariants.sh`. Usar `./gradlew koverLog` até o último plano; `koverVerify` volta
  a ser cobrado no plano final, no precedente de 06-08/06-09.
- **Phase gate:** suite JVM **e** instrumentada verdes pós-`clean`, com `--no-build-cache` e
  `N actionable tasks: M executed` com **M > 0**. Arquivado em `07-EVIDENCE.md`.
- **Nenhum comando em modo de observação.** Emulador sobe uma vez por sessão.
- **Prova de vermelho obrigatória** para cada guarda-corpo: quebrar, ver falhar, restaurar, transcrever
  no SUMMARY. **A sabotagem incide sobre código JÁ COMMITADO** — um executor da Phase 6 perdeu 74
  strings usando `git checkout` sobre trabalho novo.
- **Assert primário na mediana** em qualquer medida de tempo; cauda só reportada em logcat.

**Provas de vermelho recomendadas para esta fase:**

| # | Guarda-corpo | Como quebrar |
|---|--------------|--------------|
| 1 | Estado vivo do papel | Fazer o state holder cachear a resposta do papel ⇒ o contador de consultas por retomada cai |
| 2 | Rota tipada proibida | Trocar um destino por rota anotada ⇒ o teste de contrato do grafo deve ficar vermelho, **não** o compilador |
| 3 | Pular onboarding sem retorno | Remover o `inclusive = true` do `popUpTo` ⇒ a pilha esperada muda |
| 4 | Alvo de toque, eixo desenhado | Reduzir um controle de 56dp para 40dp ⇒ **só** o eixo desenhado pega (medido na Phase 6) |
| 5 | Fronteira do número na home | Fazer a home receber o número sem máscara |
| 6 | Flag "já perguntei" ao disparar | Mover a gravação para o callback ⇒ a lista ordenada de eventos inverte |
| 7 | Histórico desligado mostra estado, não zero | Fazer o state holder devolver `0` com histórico desligado |
| 8 | Efeito imediato sem salvar | Fazer a tela acumular e só gravar num botão ⇒ o retrato não reporta |
| 9 | Narrowing do lint (§Q7) | Acrescentar string com prefixo **desta** fase e não usá-la ⇒ o achado deve aparecer |
| 10 | Sem texto embutido em Kotlin | Escrever um literal de UI numa tela ⇒ Bloco 9 vermelho |

### Wave 0 Gaps

- [ ] `app/src/test/java/org/sentinela/app/ui/TouchTargetAsserts.kt` — **extrair** os três asserts de
      dois eixos de `CallScreenSemanticsTest.kt` (L265-290) para arquivo neutro, sem mudar
      comportamento, e reapontar o teste da Phase 6. Bloqueia todo assert de acessibilidade da fase
- [ ] `ui/navigation/SentinelaRoutes.kt` — constantes de rota; **toda** tela e todo teste de fluxo
      dependem delas. Rotas por **texto**
- [ ] `ui/navigation/SentinelaNavHost.kt` — o grafo; bloqueia os testes de fluxo
- [ ] Contrato do state holder (estado de UI + interface de fontes) para Home/Onboarding/Proteção —
      bloqueia os testes de cada tela; sem ele as telas não são testáveis sem container
- [ ] Fábrica manual de `ViewModel` com a sobrecarga `create(modelClass, extras)`
- [ ] Estender `CallStringsTest` (ou irmão `Phase7StringsTest`) às chaves desta fase — a varredura de
      honestidade precisa existir **antes** de as telas serem escritas, precedente de 06-02
- [ ] Strings pt-BR que faltarem depois de auditar as **131 já existentes** — reaproveitar antes de
      criar; nenhuma tela pode ser escrita antes, porque texto embutido em Kotlin é proibido
- [ ] Instalação de framework: **nenhuma**. Dependência nova: **nenhuma**. Permissão nova: **nenhuma**

**Deliberadamente NÃO é Wave 0:**

- **Bloco 9 de `verify-invariants.sh`** — fica no plano que cria as telas que ele vigia. Ligá-lo antes
  deixaria o script vermelho sem defeito real (precedente das Fases 3, 4, 5 e 6).
- **A política de lint de §Q7** — fica no **último** plano da fase, junto de `koverVerify` e de qualquer
  exclude, no precedente de 06-08/06-09. Reabilitar antes de as telas consumirem as strings deixaria o
  relatório artificialmente ruim e nada provaria.
- **Atualização de `docs/design/TELAS.md`** e dos cenários físicos (a partir de **61**) — trabalho de
  fase, no último plano, porque só faz sentido depois que os testes dizem o que de fato foi verificado.

---

## Open Questions

1. **Barra inferior com 4 abas ou 2?**
   - Sabemos: `nav_home`, `nav_whitelist`, `nav_history`, `nav_settings` existem em resources; whitelist
     e histórico são Phase 8.
   - Incerto: se os mockups mostram 4 abas desde a home (`07-UI-SPEC.md` responde isso, não este
     documento).
   - Recomendação: seguir o mockup. Se ele mostra 4, entregar 4 com estado vazio honesto em duas —
     nunca aba que leva a tela em branco sem explicação (`UIX-10`).

2. **`SavedStateHandle` para o progresso do onboarding?**
   - Sabemos: `ViewModel` cobre rotação e recriação de Activity; `savedstate 1.4.0` está disponível.
   - Incerto: se vale cobrir **morte de processo** no meio do onboarding. É plausível aqui, porque
     conceder um papel pode reiniciar o processo — mas conceder o papel é justamente o passo após o
     qual seguir para a home é aceitável.
   - Recomendação: começar sem `SavedStateHandle`; acrescentar só se um caso concreto de perda de
     progresso aparecer. É discrição do executor pelo CONTEXT.

3. **Onde mora "onboarding concluído"?**
   - Sabemos: `appOpenCount` existe; o CONTEXT diz "só na primeira abertura, não reexibir a cada
     atualização".
   - Incerto: `appOpenCount > 1` é suficiente, ou merece chave própria.
   - Recomendação: **chave própria** (ex. `onboarding_completed`). `appOpenCount` já tem outro dono — o
     convite de avaliação da Phase 9 (5ª abertura, depois a cada 5) — e amarrar duas decisões de produto
     ao mesmo contador é dívida garantida. Chave nova no DataStore, **fora** de `ScreeningSettings`
     (não é configuração de triagem e não deve pesar no retrato do caminho quente) — exatamente o
     padrão dos três flags de permissão.

4. **Teto do assert de partida a frio.**
   - Sabemos: mediana 680 ms, faixa 612-917 ms no AVD.
   - Incerto: qual teto não fica intermitente. A cauda no emulador mede o hospedeiro.
   - Recomendação: se houver assert, mediana < 1200 ms **acompanhado de assert estrutural**; e
     considerar que a checagem estrutural sozinha (nada construído na thread principal) pode ser o
     guarda-corpo melhor. Veredito de desempenho real fica na Phase 9, em Samsung físico.

---

## Sources

### Primary (HIGH confidence) — medições neste repositório, 2026-07-30

- `./gradlew :app:dependencies --configuration debugRuntimeClasspath` — navigation-compose 2.9.8,
  lifecycle 2.11.0 (viewmodel-compose, runtime-compose, viewmodel-savedstate), savedstate 1.4.0,
  activity-compose 1.13.0, navigationevent 1.0.0 **já no classpath**
- `./gradlew :app:compileDebugKotlin` sobre sonda descartável — `NavHost`/`composable` por texto,
  `LifecycleResumeEffect`, `collectAsStateWithLifecycle`, `viewModel(factory=)`,
  `ViewModelProvider.Factory.create(modelClass, extras)`, `StartActivityForResult`,
  `RequestPermission` **todos compilam**
- `javap -p` sobre a classe `@Serializable` compilada — **sem `$serializer`, sem `Companion`**: plugin
  de serialização ausente
- `./gradlew testDebugUnitTest` sobre sonda descartável, Robolectric `sdk=[35]` +
  `w411dp-h891dp-xxhdpi` — rota por texto `SEM ERRO`; rota tipada
  `SerializationException: Serializer for class 'TypeSafeHome' is not found`; fluxo de 3 telas com
  `navigate`/`popUpTo`/`currentBackStack` **funcionando em JVM**
- `./gradlew :app:lintDebug --rerun-tasks` com `UnusedResources` reabilitado + relatório SARIF —
  **133 achados** (131 strings, 1 cor, 1 mipmap), **build exit 0** (severidade warning);
  distribuição por prefixo; `app/lint.xml` com `<ignore regexp>` **reduz a 81**
- `am start -W` × 8 com `force-stop`, `Medium_Phone_API_35` — partida a frio **mediana 680 ms**
  (612-917 ms)
- Sonda instrumentada de papel, 200 amostras + 20 de aquecimento — trio de uma retomada
  **p50 29,9 µs**, p95 255,1 µs, max 284,3 µs

Todas as sondas foram removidas; `git status` limpo, confirmado antes e depois de cada medição.

### Primary — código e documentos do próprio projeto

- `app/src/main/java/org/sentinela/app/telecom/SystemRoleGate.kt` — três consultas de papel; KDoc
  registra a **ausência de observador** para app comum
- `permissions/RuntimePermissionAsk.kt`, `data/contacts/ContactsPermissionState.kt`,
  `platform/ContactsPermissionChecker.kt` — máquina pura de 4 estados + camada fina
- `telecom/call/DialerModeState.kt` — precedência com papel detido vencendo intenção gravada
- `ui/dialer/DialerActivity.kt` — precedente em produção de `rememberLauncherForActivityResult` +
  flag gravado ao disparar
- `ui/dialer/DialerActivationScreen.kt` — composable puro, 4 callbacks, 5 ramos; **a ligar à navegação**
- `AppContainer.kt` — todo colaborador `by lazy`; custo de 2,57 s do cache de contatos documentado
- `settings/DataStoreSettingsRepository.kt` — `settings`/`appOpenCount`/3 flags de permissão/`update`/
  `cachedSnapshot`
- `app/src/test/java/.../ui/call/CallScreenSemanticsTest.kt` L265-290 — os três asserts de dois eixos
- `.planning/STATE.md`, `.planning/phases/06-*/06-{VALIDATION,RESEARCH}.md`,
  `.planning/phases/07-*/07-CONTEXT.md`, `CLAUDE.md`

### Secondary (MEDIUM confidence)

- Semântica de `RoleManager.createRequestRoleIntent` devolver `RESULT_OK` na concessão: documentação
  oficial do Android + precedente instrumentado da Phase 6. **Não reverificado nesta pesquisa** — e o
  documento não depende disso, porque a recomendação é justamente **não** confiar no código de
  resultado.
- Comportamento do `<ignore regexp>` do lint casando o **texto da mensagem**: inferido da aritmética
  exata da redução 133 → 81 (52 achados, exatamente os 5 prefixos). Consistente, mas o mecanismo
  interno não foi lido na fonte do lint.

### Tertiary (LOW confidence) — marcado para validação

- Nenhum. Todas as afirmações centrais deste documento vêm de medição local ou de código do projeto.
  Onde não houve medição, está dito.

---

## Metadata

**Confidence breakdown:**

| Área | Nível | Razão |
|------|-------|-------|
| Stack (Q1, Q2) | **HIGH** | Resolvido pelo Gradle e compilado neste repositório; zero dependência nova |
| Rota tipada proibida | **HIGH** | Compilada, `javap` inspecionado **e** falha em execução reproduzida |
| Papel como estado vivo (Q3) | **HIGH** | Custo medido em aparelho virtual; ausência de observador confirmada em código e em 2 fases anteriores |
| Permissão em runtime (Q4) | **HIGH** | Máquina pura já existe e já tem 3 precedentes em produção |
| Teste de Compose/navegação (Q5) | **HIGH** | Fluxo de 3 telas executado em JVM, com asserts de pilha |
| Acessibilidade (Q6) | **MEDIUM-HIGH** | Propriedades semânticas são API estável e há precedente na Phase 6; a fronteira automatizável/manual é julgamento fundamentado, não medição |
| Política de lint (Q7) | **HIGH** | Contado no SARIF; narrowing testado com a aritmética fechando |
| Partida a frio (Q8) | **HIGH para o número, MEDIUM para o teto** | 8 execuções no AVD; o teto de assert é recomendação, e a cauda de emulador mede o hospedeiro |

**Research date:** 2026-07-30
**Valid until:** ~30 dias. As versões estão travadas em version catalog e o achado central (ausência do
plugin de serialização) só muda se alguém aplicar o plugin — o que este documento recomenda **não**
fazer.
