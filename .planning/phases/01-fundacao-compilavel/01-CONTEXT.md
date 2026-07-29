# Phase 1: Fundacao Compilavel - Context

**Gathered:** 2026-07-29
**Status:** Ready for planning

<domain>
## Phase Boundary

Entregar o esqueleto Android validado: `./gradlew assembleDebug testDebugUnitTest lint detekt`
verde na máquina de dev, manifest correto (sem INTERNET, com `CallScreeningService` registrado),
`CallDecisionEngine` puro coberto por testes e configuração centralizada para rebranding.

Fora do escopo desta fase: qualquer comportamento novo de produto. Normalização real com
libphonenumber é Phase 2; persistência é Phase 3; integração real do Telecom é Phase 5.
A verificação em aparelho físico é explicitamente diferida para a Phase 9, conforme a política
de validação física registrada no ROADMAP.

</domain>

<decisions>
## Implementation Decisions

### Classificação da fase
- Fase de infraestrutura pura — goal e os 5 critérios de sucesso são todos técnicos
  (comando roda, APK produzido, manifest declara, teste cobre, config centralizada);
  nenhum descreve comportamento visível ao usuário. Discuss de grey areas dispensado
  conforme `autonomous.md:362`.

### Escopo travado antes da fase
- Stack já decidido em `PROJECT.md` e `CLAUDE.md`: Kotlin + Compose + Material 3, AGP 9.3.0
  com Kotlin embutido (nunca aplicar `org.jetbrains.kotlin.android`), Gradle KTS + Version
  Catalog, minSdk 29, compileSdk 37, DI manual, JDK 17.
- Permissões: nesta fase o manifest pode conter `BIND_SCREENING_SERVICE` e `POST_NOTIFICATIONS`
  — `docs/PERMISSOES.md:13` (fonte canônica) autoriza a **declaração** de `POST_NOTIFICATIONS`
  na Fase 1, com o **pedido em runtime** só na Fase 5. Remover a declaração quebraria a Phase 5.
  `READ_CONTACTS` entra na Phase 4; `ROLE_DIALER`/`BIND_INCALL_SERVICE`/`CALL_PHONE` só na
  Phase 6. Antecipar essas é violação registrada em `docs/PERMISSOES.md`.
- A checagem do critério 3 roda sobre o **manifest mergeado**, não o fonte: o AGP injeta
  `org.sentinela.app.DYNAMIC_RECEIVER_NOT_EXPORTED_PERMISSION`, então a verificação usa
  allowlist, não contagem exata.

### Política de lint (decidida em 2026-07-29)
- `QLT-02` ("lint sem issues") é fechado via bloco `lint {}` em `app/build.gradle.kts` com
  supressão justificada, **não** apagando recursos:
  - `disable "UnusedResources"` — as 132 ocorrências são strings pt-BR já redigidas para as
    telas das Fases 5–9; são ativos legítimos, não lixo. Comentário obrigatório no bloco.
  - `disable "Typos"` — o dicionário do lint é en-US e acusa falso positivo em conteúdo pt-BR
    (ex.: "'momento'… did you mean 'memento'?").
  - `ObsoleteSdkInt` é **corrigido de verdade** (remover `mipmap-anydpi-v26`, desnecessário em
    minSdk 29) — não suprimido.
- Reavaliar as supressões na Phase 9, quando as telas reais consumirem as strings.
- Não adotar `lint-baseline.xml` — exigiria regeneração a cada fase que adiciona strings.

### Validação física
- Nenhum plano desta fase pode emitir `checkpoint:human-action` ou `checkpoint:human-verify`.
  Instalar o APK e conferir o tema no aparelho vira pendência registrada para o roteiro único
  da Phase 9 (`docs/TESTE-FISICO-SAMSUNG.md`).

### Claude's Discretion
- Organização interna dos arquivos Gradle, configuração do detekt e estrutura dos testes ficam
  a critério do executor, desde que os 5 critérios de sucesso passem.

</decisions>

<code_context>
## Existing Code Insights

### Reusable Assets
- `CallDecisionEngine`, `CallDecision`, `DecisionReason`, `ScreenedCall` — domínio puro já
  implementado com precedência e políticas por origem (`OriginPolicy`), 20 testes verdes.
- `UnknownCallScreeningService` — registrado em modo pass-through seguro; não interfere na
  telefonia até a Phase 5.
- `AppContainer` — DI manual, ponto de composição único.
- `ScreeningSettings` — modelo de configuração com as políticas por origem.
- Tema Compose com os tokens do design system "Silent Guardian".

### Established Patterns
- Strings em `res/values/strings.xml` (pt-BR), nunca hardcoded em Kotlin.
- Domínio não importa tipo do Android Telecom.
- `sentinelaApplicationId` centralizado em `app/build.gradle.kts` para rebranding.

### Integration Points
- `app/build.gradle.kts` + `gradle/libs.versions.toml` — toolchain e versões.
- `AndroidManifest.xml` — registro do Service e ausência de INTERNET.
- `app/src/main/java/org/sentinela/app/data/contacts/ContactLookupRepository.kt` — stub criado
  fora de fase; pertence à Phase 4. Não expandir aqui.

</code_context>

<specifics>
## Specific Ideas

O build já foi executado verde em 2026-07-28 (`BUILD SUCCESSFUL`, 20/20 testes, lint e detekt
limpos). O trabalho desta fase é formalizar essa validação, confirmar os critérios 3 e 5 por
inspeção e fechar a fase — não reescrever o esqueleto.

</specifics>

<deferred>
## Deferred Ideas

- Instalação do APK e conferência visual do tema dark em aparelho Samsung — Phase 9.
- Kover e gate de cobertura ≥ 80% — Phase 2 (domínio) e Phase 9 (gate de release).
- `ContactLookupRepository` real com cache e ContentObserver — Phase 4.

</deferred>
