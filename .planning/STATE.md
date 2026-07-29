---
gsd_state_version: 1.0
milestone: v0.1
milestone_name: milestone
status: unknown
stopped_at: Completed 02-05-PLAN.md
last_updated: "2026-07-29T06:02:59.639Z"
last_activity: 2026-07-29
progress:
  total_phases: 9
  completed_phases: 2
  total_plans: 15
  completed_plans: 8
---

# Project State

## Project Reference

See: `.planning/PROJECT.md` (updated 2026-07-28)
**Core value:** "Se não está nos contatos nem na whitelist pessoal, não interrompe o usuário."
**Current focus:** Phase 03 — Dados Locais
Last activity: 2026-07-29

## Current Position

Phase: 03 (Dados Locais) — EXECUTING
Plan: 1 of 7

## Snapshot

- **Esqueleto:** Gradle KTS + catalog, AGP 9.3.0 (Kotlin embutido), Compose BOM 2026.06.01, compileSdk 37 / minSdk 29
- **Domínio:** `CallDecisionEngine` com precedência saída→proteção→privado→contato→whitelist→falha→desconhecido e políticas por origem (`OriginPolicy`)
- **Normalização:** `LibPhoneNumberNormalizer` + `PhoneMask` + cascata de região, ligados em `AppContainer.phoneNumberNormalizer` (util construído 1x, fora do caminho quente)
- **Qualidade:** 156 testes, cobertura domain+phone 97,619% com gate `koverVerify` em 80%; lint e detekt zerados
- **Telecom:** `UnknownCallScreeningService` registrado em modo pass-through seguro (não interfere até a Phase 5)
- **Git:** repo local sem remote; branch `master`
- **Última tag git:** nenhuma (primeira release será `v0.1.0`)

## Decisions

- [Adendos 2026-07-28]: **Dois modos de operação** — filtro (padrão, permissão mínima) e discador (opcional, `ROLE_DIALER` + `InCallService`, habilita políticas por contato). Substituir o discador nativo agora É escopo do MVP
- [Adendos 2026-07-28]: **READ_CONTACTS entra no MVP** — uso exclusivamente local/em memória; nomes nunca persistidos nem enviados
- [Adendos 2026-07-28]: Políticas por origem no motor (contatos: Tocar padrão; whitelist: Nunca Silenciar padrão; desconhecidos: Bloquear padrão) — espelham os mockups
- [Adendos 2026-07-28]: Convite de avaliação/apoio na 5ª abertura, repetindo a cada 5 (10ª, 15ª…) até aceite; seção "Apoie" com open source em destaque + doação Bitcoin
- [Adendos 2026-07-28]: Offline-first permanente — MVP sem INTERNET; sync (v0.2.0) opt-in/assíncrona, inclui envio opcional da lista de números recebidos
- [Adendos 2026-07-28]: Nome antigo dos mockups eliminado de todos os arquivos (docs + HTMLs); branding único Sentinela
- [Bootstrap 2026-07-27]: Bloqueio de desconhecidos no modo filtro apoiado no contrato da plataforma (onScreenCall só recebe não-contatos sem discador padrão) — confirmado na doc oficial
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

- **Validação física obrigatória** — `setSkipCallLog`/notificação nativa variam por OEM; critérios de aceite centrais só fecham em Samsung físico (Phase 9)
- **Modo discador é o maior risco técnico do MVP** — `InCallService` + elegibilidade ao papel + UX de chamada; pesquisa reforçada antes da Phase 6
- **Robolectric 4.16.1 suporta até SDK 36** — com compileSdk 37, fixar `@Config(sdk = [36])` até o 4.17 estável

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

Last session: 2026-07-29T05:08:56.116Z
Stopped at: Completed 02-05-PLAN.md
Resume file: None
