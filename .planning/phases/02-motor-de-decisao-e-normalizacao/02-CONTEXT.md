# Phase 2: Motor de Decisao e Normalizacao - Context

**Gathered:** 2026-07-29
**Status:** Ready for planning

<domain>
## Phase Boundary

Toda regra de triagem e toda normalização de número existem como **código puro, determinístico
e exaustivamente testado**, antes de qualquer integração real com o Telecom.

Entregas desta fase:
- Precedência completa do `CallDecisionEngine` coberta caso a caso (saída, proteção off,
  privado, contato × 4 políticas, whitelist × 4 políticas, falha de consulta, desconhecido × 3
  políticas, inválido).
- `PhoneNumberNormalizer` **real** com libphonenumber-android, substituindo o stub de interface.
- Máscara de exibição que nunca revela número completo.
- Kover configurado com gate de cobertura ≥ 80% em `domain/` (e `phone/`).

Fora do escopo: persistência (Phase 3), leitura de contatos do aparelho (Phase 4), integração
real do `CallScreeningService` (Phase 5), qualquer UI (Phases 7–8). Validação em aparelho
físico continua diferida para a Phase 9 conforme a política do ROADMAP — nenhum plano desta
fase pode emitir `checkpoint:human-action` ou `checkpoint:human-verify`.

</domain>

<decisions>
## Implementation Decisions

### Normalização (libphonenumber)

- **Número BR sem DDD** (ex.: `98765-4321`) → `NormalizationResult.Invalid` com razão explícita.
  Não inferir DDD: inferência errada envenena a whitelist e faz o motor decidir sobre um número
  que não é o do chamador.
- **Celular BR antigo sem o 9** (ex.: `+55 11 8765-4321`) → normalizar acrescentando o 9 quando
  libphonenumber reconhecer o número como válido-com-correção. Caso de teste explícito.
- **Números curtos e de serviço** (`190`, `0800…`, `4004-…`) → `Valid` com o E.164 que
  libphonenumber devolver quando possível. São números discáveis e o usuário pode legitimamente
  querer colocá-los na whitelist. Não criar categoria separada no MVP.
- **Região padrão — DECISÃO DO USUÁRIO (2026-07-29):** *não* travar em `"BR"`. O app precisa
  funcionar no mundo todo. Resolução da região em cascata:
  1. `TelephonyManager` — `simCountryIso`, com `networkCountryIso` como segunda opção.
  2. Região informada pelo próprio usuário nas configurações (DDI/DDD do usuário). O usuário
     aceitou explicitamente o custo de pedir esse dado quando o aparelho não o fornecer:
     *"se precisar que o cliente fale o DDI e DDD dele, paciência, adicionamos"*.
  3. `"BR"` apenas como último recurso, para não quebrar em aparelho sem SIM.
- A cascata **não** pode violar a arquitetura: `PhoneNumberNormalizer` e o domínio continuam
  sem importar tipo do Android. A leitura do `TelephonyManager` fica atrás de uma interface
  (ex.: `RegionProvider`) com implementação Android injetada pelo `AppContainer`; o domínio vê
  só a interface. Testes usam fake.
- **Verificar antes de implementar:** `simCountryIso`/`networkCountryIso` não podem exigir
  `READ_PHONE_STATE` — essa permissão é proibida sem prova de necessidade
  (`docs/PERMISSOES.md`). Se exigir, a cascata cai direto para o passo 2 (usuário informa) e a
  permissão **não** é adicionada.
- A persistência da região informada pelo usuário é da Phase 3; nesta fase, apenas o contrato
  e o fallback em memória.

### Máscara de exibição

- Formato canônico: `+55 11 9****-1234` — DDI + DDD + primeiro dígito + `****` + últimos 4,
  conforme já fixado no `CLAUDE.md`. Generalizar para outros DDIs mantendo a forma
  "prefixo do país + área + primeiro dígito + asteriscos + últimos 4".
- **Números curtos — DECISÃO DO USUÁRIO (2026-07-29):** número curto demais para ser mascarado
  de forma útil (ex.: `190`) **pode ser exibido na íntegra**. Princípio orientador do usuário:
  *"essas máscaras não podem atrapalhar o usuário"* — a máscara existe para proteger dado
  pessoal, e um número público de serviço não é dado pessoal. Definir um limiar explícito em
  código (número de dígitos abaixo do qual não se mascara) e testá-lo.
- Entrada inválida ou não-E.164 passada a `mask()` → devolver máscara genérica segura; **nunca**
  ecoar a entrada crua e nunca lançar exceção (a máscara é usada em caminho de log).
- Uma única função de máscara serve log e UI. Duas implementações divergiriam e uma delas
  acabaria vazando o número.

### Testes e cobertura

- **Kover** como ferramenta de cobertura (já previsto no ROADMAP), não JaCoCo.
- Gate `koverVerify` com regra de ≥ 80% sobre `domain/` e `phone/`, **falhando o build**, e
  incluído no comando padrão de validação da fase.
- Precedência testada por **testes parametrizados sobre a matriz completa** (política × origem),
  somados aos casos nomeados que já existem — a matriz garante que nenhuma combinação nova
  entre sem cobertura.
- libphonenumber: usar a variante JVM pura nos testes unitários se o port `-android` não rodar
  fora do device; runtime continua com o port `-android`. Não introduzir Robolectric (o
  `01-VALIDATION.md` registra que a versão disponível não suporta o SDK alvo).

### Claude's Discretion

- Estrutura interna dos arquivos de teste, nomes das classes de fake, e como exatamente a regra
  do Kover é escrita no Gradle ficam a critério do executor, desde que os 5 critérios de
  sucesso do ROADMAP passem.

</decisions>

<code_context>
## Existing Code Insights

### Reusable Assets
- `app/src/main/java/org/sentinela/app/domain/CallDecisionEngine.kt` — motor puro **já
  implementado** com a precedência de 7 níveis e `OriginPolicy`. Esta fase o completa e cobre,
  não o reescreve.
- `CallDecision`, `DecisionReason`, `ScreenedCall` (`ScreenedNumber.Private` / `.Invalid`),
  `ContactLookup`, `WhitelistLookup` — modelo de domínio pronto.
- `app/src/main/java/org/sentinela/app/settings/ScreeningSettings.kt` — `OriginPolicy`,
  `BlockMode`, `FallbackPolicy`.
- `app/src/test/java/org/sentinela/app/domain/CallDecisionEngineTest.kt` — 24 testes verdes
  (28 na suíte total, com `ThemeTokensTest`).
- `scripts/verify-invariants.sh` (Phase 1) — checa manifest, rebranding e pureza do domínio.
  Se a fase adicionar invariante, é aqui que ela entra.

### Established Patterns
- Domínio não importa tipo do Android; dependência de plataforma entra por interface injetada.
- DI manual via `AppContainer` — nada de Hilt/Koin.
- Strings em `res/values/strings.xml` (pt-BR), nunca hardcoded em Kotlin.
- Nenhum número completo em log — sempre via máscara.

### Integration Points
- `app/src/main/java/org/sentinela/app/phone/PhoneNumberNormalizer.kt` — hoje só interface +
  `NormalizationResult`; recebe a implementação real nesta fase.
- `app/src/main/java/org/sentinela/app/AppContainer.kt` — ponto de injeção do normalizer e do
  futuro `RegionProvider`.
- `app/build.gradle.kts` + `gradle/libs.versions.toml` — entrada da dependência libphonenumber
  e do plugin Kover.
- `app/src/main/java/org/sentinela/app/telecom/UnknownCallScreeningService.kt` — consumidor do
  normalizer a partir da Phase 5; nesta fase permanece pass-through.

</code_context>

<specifics>
## Specific Ideas

- O motor já existe e passa; o risco real desta fase está na **normalização internacional** e
  na cascata de região, não na precedência.
- O usuário priorizou explicitamente alcance mundial sobre simplicidade BR-only, e usabilidade
  da máscara sobre mascaramento máximo. Quando houver conflito entre "mais seguro" e "não
  atrapalha o usuário" em um número **não pessoal**, seguir o usuário.

</specifics>

<deferred>
## Deferred Ideas

- Persistir a região/DDI-DDD informados pelo usuário (DataStore) — Phase 3.
- Tela de configuração para o usuário informar DDI/DDD — Phase 7.
- Uso real do normalizer no `CallScreeningService` — Phase 5.
- Gate de cobertura no pipeline de release — Phase 9.

</deferred>
