# Decisões Arquiteturais (ADR-lite)

> Uma entrada por decisão relevante, com contexto e consequência. Novas decisões entram no
> topo. Espelho resumido vive em `.planning/PROJECT.md` (Key Decisions).

## 2026-07-28 — Dois modos: filtro (padrão) e discador (opcional)

**Contexto:** adendo do produto — substituir recursos nativos como o discador passa a ser
escopo do MVP, e as políticas por contato dos mockups devem existir de verdade.
**Decisão:** modo filtro continua sendo o padrão (permissão mínima, contatos tocam nativo);
modo discador é opt-in: `ROLE_DIALER` + `InCallService` mínimo + discagem própria, com
reversão limpa. A ativação do modo discador exige `READ_CONTACTS` concedida.
**Consequência:** políticas por contato plenas no modo discador; Fase 6 dedicada com
pesquisa reforçada (maior risco técnico do MVP).

## 2026-07-28 — READ_CONTACTS no MVP, uso só em memória

**Contexto:** adendo do produto — "é importante ler os contatos do telefone".
**Decisão:** permissão pedida em runtime com explicação; `ContactLookupRepository` responde
HIT/MISS/UNAVAILABLE por E.164 com cache em RAM; nome/foto nunca persistidos nem enviados.
**Consequência:** privacidade preservada (nada de contato no banco/backup); sem a permissão
o app segue 100% funcional no modo filtro.

## 2026-07-28 — Políticas por origem no motor (OriginPolicy)

**Contexto:** mockups definem opções Tocar/Bloquear/Silenciar/Nunca Silenciar para contatos
e whitelist; produto quer desconhecidos bloqueados ou silenciados por configuração.
**Decisão:** `OriginPolicy` único aplicado a contatos (padrão Tocar), whitelist (padrão
Nunca Silenciar) e desconhecidos (padrão Bloquear); novo `CallDecision.Silence` mapeia
`setSilenceCall`. "Nunca Silenciar" decide como Allow — bypass de DND é camada de toque.
**Consequência:** motor continua puro e testável (20 testes no esqueleto); UI espelha o
mockup sem regra fora do domínio.

## 2026-07-28 — Avaliação/apoio na 5ª abertura + posicionamento open source

**Contexto:** adendo do produto — convite de avaliação na 5ª abertura (repetindo a cada 5
até aceite) e destaque: open source, sem propaganda, sem telemetria, sem nuvem, 100%
offline, com pedido de comentário de apoio ou doação em Bitcoin.
**Decisão:** contador local de aberturas (DataStore); convite nunca interrompe onboarding ou
chamada; seção "Apoie o Sentinela" em Sobre. Endereço Bitcoin vem do mantenedor (string
vazia até lá — nunca inventar endereço). Licença open source a escolher (pendência).
**Consequência:** engajamento sem telemetria; release bloqueado até endereço real e licença
definidos.

## 2026-07-28 — Offline-first permanente, sync como etapa v0.2.0

**Contexto:** adendo do produto — funcionar 100% offline sempre; online só para sincronizar
listas (incluindo envio opcional de números recebidos), fora do MVP.
**Decisão:** MVP sem INTERNET no manifest; repositórios por interface para a fonte remota
plugar depois; decisão de bloqueio jamais espera rede.
**Consequência:** promessa de privacidade tecnicamente verificável no MVP; matriz de
permissões será revisada junto com a v0.2.0.

## 2026-07-27 — Bloquear desconhecidos sem ler contatos NO MODO FILTRO

**Contexto:** o prompt original proibia `READ_CONTACTS`; o contrato da plataforma entrega ao
filtro apenas números fora da agenda quando o app não é o discador padrão.
**Decisão:** o modo filtro continua se apoiando nesse contrato — mesmo com a permissão
concedida, no modo filtro o lookup de contatos é dispensável (`MISS` direto).
**Consequência:** caminho padrão permanece o mais simples e rápido; a permissão só trabalha
de verdade no modo discador. (Revisado em 2026-07-28 pelos adendos do produto.)

## 2026-07-27 — Motor de decisão puro, Service fino

**Contexto:** janela de 5 s da plataforma + necessidade de testar exaustivamente a regra.
**Decisão:** `CallDecisionEngine` puro (sem tipos Android), Service só monta entrada/traduz
saída; timeout interno → `WhitelistLookup.LOOKUP_FAILED` → política de fallback explícita.
**Consequência:** regra 100% testável em JVM; comportamento sob falha é configuração, não acidente.

## 2026-07-27 — DI manual (sem Hilt/Koin)

**Contexto:** cold start do Service é orçamento crítico (p95 < 200 ms).
**Decisão:** `AppContainer` manual com lazy; zero frameworks de DI/reflexão.
**Consequência:** menos conveniência em troca de inicialização previsível.

## 2026-07-27 — Branding único "Sentinela" (nome antigo eliminado em 2026-07-28)

**Contexto:** parte dos mockups Stitch usava um nome provisório que deixou de existir.
**Decisão:** Sentinela em tudo — inclusive nos HTMLs dos mockups, saneados em 2026-07-28;
applicationId `org.sentinela.app` centralizado em `app/build.gradle.kts`
(val `sentinelaApplicationId`) para rebranding barato.
**Consequência:** nenhuma referência ao nome antigo no repositório.

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
