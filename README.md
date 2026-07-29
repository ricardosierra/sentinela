# Sentinela

Bloqueador local de chamadas desconhecidas para Android: se o número não está nos seus
contatos nem na sua whitelist pessoal, a chamada não toca — sem som, sem tela de chamada,
sem notificação. **Open source, sem propaganda, sem telemetria, sem envio de dados para a
nuvem — 100% offline, rodando no seu próprio celular.**

**Versão atual:** 0.1.0 (versionCode 1) — em desenvolvimento (esqueleto)
**SDK:** Android 10 (API 29) até Android 17 (API 37)
**Stack:** Kotlin + Jetpack Compose + Material 3 + Telecom (`CallScreeningService` / `InCallService`)
**Status:** Phase 1 de 9 do roadmap (`.planning/ROADMAP.md`)

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

O Sentinela tem **dois modos de operação**:

- **Modo filtro (padrão)** — o app detém o papel **`ROLE_CALL_SCREENING`**. O Android só
  encaminha ao filtro chamadas de números **fora da agenda**; contatos tocam normalmente.
  Desconhecido sem whitelist = bloqueado (ou silenciado, conforme configuração) antes de tocar.
- **Modo discador (opcional)** — o Sentinela vira o app de telefone padrão (**`ROLE_DIALER`**
  + UI de chamada própria via `InCallService`). A triagem passa a cobrir **todas** as
  chamadas, habilitando políticas também para contatos
  (Tocar/Bloquear/Silenciar/Nunca Silenciar). Reversível a qualquer momento.

A leitura de contatos é local e efêmera: nomes e números da agenda nunca são gravados nem
saem do aparelho.

```
Chamada recebida
  → UnknownCallScreeningService (fino, responde < 5 s)
  → CallDecisionEngine (puro: saída? proteção? privado? contato? whitelist? → política)
  → respondToCall: permitir / silenciar / rejeitar / caixa postal / bloquear sem rastro
  → depois: histórico interno opcional + notificação silenciosa opt-in
```

Detalhes em [`docs/ARQUITETURA.md`](docs/ARQUITETURA.md). O que o app **não** faz (WhatsApp,
caller ID, garantias absolutas): [`docs/LIMITACOES.md`](docs/LIMITACOES.md).

### Por que assim?

- **Privacidade comprovável** — o manifest do MVP não declara INTERNET; não há como
  exfiltrar dados. Sync com backend (v0.2.0) será opt-in e o app seguirá 100% funcional offline.
- **Regra testável** — toda decisão vive num motor puro coberto por testes JVM
  (20 testes hoje, cobertura ≥ 80% como gate de release).
- **Sem hacks** — só APIs oficiais do Telecom; comportamento de OEM é validado em aparelho,
  não contornado às cegas ([`docs/TESTE-FISICO-SAMSUNG.md`](docs/TESTE-FISICO-SAMSUNG.md)).

---

## Estrutura do repositório

```
app/src/main/java/org/sentinela/app/
  telecom/        UnknownCallScreeningService, ScreeningRoleManager (+ InCallService na Fase 6)
  domain/         CallDecisionEngine, CallDecision, DecisionReason, ScreenedCall
  settings/       ScreeningSettings, OriginPolicy, SettingsRepository (DataStore na Fase 3)
  data/local/     PersonalWhitelistRepository, BlockedCallRepository (Room na Fase 3)
  data/contacts/  ContactLookupRepository (READ_CONTACTS, só memória — Fase 4)
  phone/          PhoneNumberNormalizer (libphonenumber na Fase 2)
  notifications/  BlockedCallNotifier (canal silencioso na Fase 5)
  ui/             MainActivity + ui/theme (tokens "Silent Guardian")
docs/             documentação completa — comece por docs/INDEX.md
.planning/        GSD: PROJECT, REQUIREMENTS (81 reqs), ROADMAP (9 fases), STATE, research/
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
| `READ_CONTACTS` | Saber se quem liga é contato (uso 100% local, nada armazenado) | Onboarding (passo de contatos), recusável |
| `POST_NOTIFICATIONS` | Notificação silenciosa de bloqueio | Só se o usuário habilitar (off por padrão) |
| `ROLE_DIALER` + `CALL_PHONE` | Modo discador opcional (políticas para contatos + UI de chamada) | Só ao ativar o modo discador |

**Sem INTERNET, sem READ_CALL_LOG, sem READ_SMS.** Matriz completa, fases e justificativas:
[`docs/PERMISSOES.md`](docs/PERMISSOES.md).

---

## Testes

```bash
./gradlew testDebugUnitTest              # unitários (motor de decisão: 20 casos hoje)
./gradlew connectedDebugAndroidTest      # instrumentados (exige aparelho/emulador)
```

Qualidade é requisito do produto: suíte obrigatória da seção 13 do
[`docs/PROMPT-MVP.md`](docs/PROMPT-MVP.md) + novos casos dos adendos, testes instrumentados,
testes de migração do Room e cobertura ≥ 80% em domínio/dados (Kover) como gate de release
(`.planning/REQUIREMENTS.md` → QLT). Validação física: 30 cenários em
[`docs/TESTE-FISICO-SAMSUNG.md`](docs/TESTE-FISICO-SAMSUNG.md).

---

## Apoie o projeto

O Sentinela é **open source** e vive sem propaganda, sem telemetria e sem nuvem. Se ele te
poupou de interrupções: deixe um comentário de apoio/avaliação — ou doe em Bitcoin
(endereço na tela "Apoie o Sentinela" do app, quando publicado). Licença: a definir
(pendência registrada em `.planning/STATE.md`).

---

## Onde ler primeiro

1. [`docs/INDEX.md`](docs/INDEX.md) — índice de toda a documentação
2. [`docs/PROMPT-MVP.md`](docs/PROMPT-MVP.md) — escopo completo (prompt original + adendos)
3. [`docs/ARQUITETURA.md`](docs/ARQUITETURA.md) — como o produto funciona por dentro
4. [`.planning/ROADMAP.md`](.planning/ROADMAP.md) — as 9 fases até o v0.1.0
5. [`CLAUDE.md`](CLAUDE.md) — regras não-negociáveis para agentes

---

## Convenções

- Código em inglês; strings de UI em pt-BR via resources (nada hardcoded); docs em português.
- Conventional Commits, sem atribuição de IA. Changelog no formato **Release Notes**.
- Toda regra de triagem no `CallDecisionEngine` — nunca no Service ou na UI.
- Nenhum número completo em log — sempre mascarado; nenhum dado de contato persistido.
