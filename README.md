# Sentinela

Bloqueador local de chamadas desconhecidas para Android: se o número não está nos seus
contatos nem na sua whitelist pessoal, a chamada não toca — sem som, sem tela de chamada,
sem notificação. Tudo 100% no aparelho: sem servidor, sem login, sem internet.

**Versão atual:** 0.1.0 (versionCode 1) — em desenvolvimento (esqueleto)
**SDK:** Android 10 (API 29) até Android 17 (API 37)
**Stack:** Kotlin + Jetpack Compose + Material 3 + Telecom `CallScreeningService`
**Status:** Phase 1 de 7 do roadmap (`.planning/ROADMAP.md`)

---

## TL;DR

```bash
./build.sh                        # build debug + copia sentinela-debug.apk para a raiz
adb install sentinela-debug.apk   # instalar no aparelho

./gradlew testDebugUnitTest       # testes unitários
./gradlew lint detekt             # qualidade
```

---

## Como funciona

O Sentinela detém o papel **`ROLE_CALL_SCREENING`** do Android. O truque que sustenta o
produto: quando o app **não** é o discador padrão, o sistema só encaminha ao filtro chamadas
de números **fora da agenda** — contatos tocam normalmente sem o app ler seus contatos
(`READ_CONTACTS` não é sequer solicitada).

```
Chamada de número desconhecido
  → UnknownCallScreeningService (fino, responde < 5 s)
  → CallDecisionEngine (puro: saída? proteção? privado? whitelist? → decisão)
  → respondToCall: rejeitar / caixa postal silenciosa / bloquear sem rastro
  → depois: histórico interno opcional + notificação silenciosa opt-in
```

Detalhes em [`docs/ARQUITETURA.md`](docs/ARQUITETURA.md). O que o app **não** faz (WhatsApp,
política por contato, caller ID): [`docs/LIMITACOES.md`](docs/LIMITACOES.md).

### Por que assim?

- **Privacidade comprovável** — o manifest não declara INTERNET; não há como exfiltrar dados.
- **Regra testável** — toda decisão vive num motor puro coberto por testes JVM.
- **Sem hacks** — só APIs oficiais do Telecom; comportamento de OEM é validado em aparelho,
  não contornado às cegas ([`docs/TESTE-FISICO-SAMSUNG.md`](docs/TESTE-FISICO-SAMSUNG.md)).

---

## Estrutura do repositório

```
app/src/main/java/org/sentinela/app/
  telecom/        UnknownCallScreeningService, ScreeningRoleManager
  domain/         CallDecisionEngine, CallDecision, DecisionReason, ScreenedCall
  settings/       ScreeningSettings, SettingsRepository (DataStore na Phase 3)
  data/local/     PersonalWhitelistRepository, BlockedCallRepository (Room na Phase 3)
  phone/          PhoneNumberNormalizer (libphonenumber na Phase 2)
  notifications/  BlockedCallNotifier (canal silencioso na Phase 4)
  ui/             MainActivity + ui/theme (tokens "Silent Guardian")
docs/             documentação completa — comece por docs/INDEX.md
.planning/        GSD: PROJECT, REQUIREMENTS (65 reqs), ROADMAP (7 fases), STATE, research/
```

---

## Build & Deploy

### Requisitos

- JDK 17 (Homebrew: `brew install openjdk@17`) — o Gradle não roda no JDK 25;
  `gradle.properties` já aponta `org.gradle.java.home`
- Android SDK em `~/Library/Android/sdk` (platform 37 baixa automaticamente no primeiro build)
- Gradle via wrapper (9.6.1) — não instalar globalmente

### Debug

```bash
./build.sh          # ou ./gradlew assembleDebug
```

### Release

```bash
./gradlew assembleRelease   # exige app/keystore.properties (fora do git)
```

Processo completo (bump, changelog, tag, assinatura, validação de permissões):
[`docs/RELEASE.md`](docs/RELEASE.md).

---

## Permissões

| Permissão | Uso | Quando pede |
|-----------|-----|-------------|
| `ROLE_CALL_SCREENING` (papel) | Habilita a triagem de chamadas | Onboarding, diálogo nativo |
| `POST_NOTIFICATIONS` | Notificação silenciosa de bloqueio | Só se o usuário habilitar (off por padrão) |
| `BIND_SCREENING_SERVICE` | Proteção do service (declarada, não solicitada) | — |

**Sem INTERNET, sem READ_CONTACTS, sem READ_CALL_LOG, sem READ_SMS.** Matriz completa e
justificativas: [`docs/PERMISSOES.md`](docs/PERMISSOES.md).

---

## Testes

```bash
./gradlew testDebugUnitTest              # unitários (motor de decisão: 10 casos hoje)
./gradlew connectedDebugAndroidTest      # instrumentados (exige aparelho/emulador)
```

A lista obrigatória de 19 casos do MVP está na seção 13 de
[`docs/PROMPT-MVP.md`](docs/PROMPT-MVP.md); a suíte cresce por fase
(`.planning/REQUIREMENTS.md` → QLT-01). Validação física: 20 cenários em
[`docs/TESTE-FISICO-SAMSUNG.md`](docs/TESTE-FISICO-SAMSUNG.md).

---

## Onde ler primeiro

1. [`docs/INDEX.md`](docs/INDEX.md) — índice de toda a documentação
2. [`docs/PROMPT-MVP.md`](docs/PROMPT-MVP.md) — escopo completo (fonte de verdade)
3. [`docs/ARQUITETURA.md`](docs/ARQUITETURA.md) — como o produto funciona por dentro
4. [`.planning/ROADMAP.md`](.planning/ROADMAP.md) — as 7 fases até o v0.1.0
5. [`CLAUDE.md`](CLAUDE.md) — regras não-negociáveis para agentes

---

## Convenções

- Código em inglês; strings de UI em pt-BR via resources (nada hardcoded); docs em português.
- Conventional Commits, sem atribuição de IA. Changelog no formato **Release Notes**.
- Toda regra de triagem no `CallDecisionEngine` — nunca no Service ou na UI.
- Nenhum número completo em log — sempre mascarado.
