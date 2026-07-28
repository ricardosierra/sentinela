# Decisões Arquiteturais (ADR-lite)

> Uma entrada por decisão relevante, com contexto e consequência. Novas decisões entram no
> topo. Espelho resumido vive em `.planning/PROJECT.md` (Key Decisions).

## 2026-07-27 — Bloquear desconhecidos SEM ler contatos

**Contexto:** o prompt exige "contatos tocam normalmente" e proíbe `READ_CONTACTS`.
**Decisão:** apoiar o produto no contrato da plataforma: sem ser discador padrão, o
`CallScreeningService` só recebe chamadas de números fora da agenda.
**Consequência:** permissão mínima real; em troca, nenhuma política por contato é possível
(limitação documentada e comunicada na UI).

## 2026-07-27 — Motor de decisão puro, Service fino

**Contexto:** janela de 5 s da plataforma + necessidade de testar exaustivamente a regra.
**Decisão:** `CallDecisionEngine` puro (sem tipos Android), Service só monta entrada/traduz
saída; timeout interno → `WhitelistLookup.LOOKUP_FAILED` → política de fallback explícita.
**Consequência:** regra 100% testável em JVM; comportamento sob falha é configuração, não acidente.

## 2026-07-27 — DI manual (sem Hilt/Koin)

**Contexto:** cold start do Service é orçamento crítico (p95 < 200 ms).
**Decisão:** `AppContainer` manual com lazy; zero frameworks de DI/reflexão.
**Consequência:** menos conveniência em troca de inicialização previsível.

## 2026-07-27 — Branding unificado "Sentinela"

**Contexto:** mockups Stitch oscilam entre "Sentinela" e "Ultrathink".
**Decisão:** Sentinela em tudo; applicationId `org.sentinela.app` centralizado em
`app/build.gradle.kts` (val `sentinelaApplicationId`) para rebranding barato.
**Consequência:** telas do mockup com textos "Ultrathink" são adaptadas na implementação.

## 2026-07-27 — AGP 9.3 com Kotlin embutido

**Contexto:** prompt exige versões estáveis atuais; AGP 9 mudou o modelo de plugins.
**Decisão:** AGP 9.3.0 + Gradle 9.6.1 + Kotlin embutido (sem `org.jetbrains.kotlin.android`;
`org.jetbrains.kotlin.plugin.compose` mantido) + KSP 2.3.10 standalone + compileSdk 37.
**Consequência:** build validado em 2026-07-27 (assembleDebug + testes verdes); Robolectric
limitado a `@Config(sdk=[36])` até o 4.17.

## 2026-07-27 — Room + DataStore; histórico opt-in fora de backup

**Contexto:** prompt pede Room "somente se necessário" e proteção de backup.
**Decisão:** Room para whitelist/histórico (consulta indexada + retenção), DataStore
Preferences para configurações; `dataExtractionRules` exclui tudo de backup/transfer.
**Consequência:** troca de aparelho exige export manual da whitelist (documentado na política).

## 2026-07-27 — Notificação própria silenciosa e opt-in

**Contexto:** prompt exige zero interrupção por padrão.
**Decisão:** canal IMPORTANCE_LOW, off por padrão; `POST_NOTIFICATIONS` pedida só no opt-in;
`setSkipNotification(true)` em todo bloqueio para suprimir a nativa.
**Consequência:** usuário que não opta nunca vê nada — comportamento padrão é silêncio total.
