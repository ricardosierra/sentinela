---
phase: 02-motor-de-decisao-e-normalizacao
plan: 04
subsystem: phone
tags: [normalizacao, libphonenumber, e164, mascara, privacidade]
requires: ["02-01", "02-03"]
provides:
  - "LibPhoneNumberNormalizer — implementacao real do PhoneNumberNormalizer"
  - "PhoneMask — mascara unica de log e UI"
  - "PhoneNumbers.LIMIAR_CURTO — limiar compartilhado de codigo curto"
  - "phoneNumberUtil(MetadataLoader) — fabrica pura para o AppContainer"
affects:
  - "02-05 (gate koverVerify + injecao no AppContainer)"
  - "Fase 3 (formato de chave persistido na whitelist e no historico)"
  - "Fase 5 (uso real no CallScreeningService)"
tech-stack:
  added: []
  patterns:
    - "Gate de validade sempre em isValidNumber; sucesso de parse nao vale nada"
    - "Regras locais (9o digito, codigo curto) isoladas em funcoes privadas nomeadas com KDoc do porque"
    - "Mascara dentro de runCatching: caminho de log nunca lanca"
key-files:
  created:
    - app/src/main/java/org/sentinela/app/phone/PhoneNumbers.kt
    - app/src/main/java/org/sentinela/app/phone/PhoneMask.kt
    - app/src/main/java/org/sentinela/app/phone/LibPhoneNumberNormalizer.kt
    - app/src/test/java/org/sentinela/app/phone/PhoneMaskTest.kt
    - app/src/test/java/org/sentinela/app/phone/LibPhoneNumberNormalizerTest.kt
    - app/src/test/java/org/sentinela/app/phone/BrazilianRulesNormalizerTest.kt
  modified:
    - app/src/main/java/org/sentinela/app/phone/PhoneNumberNormalizer.kt
decisions:
  - "Chave persistida = E.164, EXCETO codigo curto (< 6 digitos), que e digito cru — contrato para a Fase 3"
  - "9o digito corrigido a mao, aceito so com revalidacao isValidNumber && type == MOBILE"
  - "Assinatura passou de defaultRegion: String = \"BR\" para region: String? = null (delega ao RegionProvider)"
  - "Formato canonico do CLAUDE.md e um FORMATO, nao os digitos do numero de exemplo"
metrics:
  duration: ~35min
  tasks: 3
  files: 7
  completed: 2026-07-29
---

# Phase 02 Plan 04: Normalizacao Real e Mascara Summary

Normalizacao E.164 real com libphonenumber-android substituindo o stub, com as duas regras que a
biblioteca nao cobre (9o digito BR e codigos curtos) implementadas a mao, isoladas e testadas nos
casos negativos, mais a mascara unica de log/UI generalizada a qualquer DDI.

## What Was Built

**Task 1 — `PhoneNumbers` + `PhoneMask`** (commit `bf7700f`)
`LIMIAR_CURTO = 6` como fonte unica, consumida com o operador `<` estrito nos dois lados.
`PhoneMask.mask` deriva a area de `getLengthOfNationalDestinationCode`, funcionando para +55, +1,
+44, 0800 e 4004 sem tabela hardcoded. Corpo inteiro em `runCatching` com `MASCARA_GENERICA`
(`+** ****`) como saida de falha — nunca ecoa a entrada crua. 14 testes, incluindo propriedade de
nao-vazamento do NSN, teto de digitos expostos (`cc + ndc + 5`) e lista de entradas hostis.

**Task 2 — `LibPhoneNumberNormalizer`** (commit `3c5bc60`)
Regiao resolvida por `parametro -> regionProvider -> CascadingRegionProvider.DEFAULT_REGION`. Gate
sempre em `isValidNumber`. `NumberParseException.errorType` mapeado para reason codes `[a-z_]+`
(`nao_e_numero`, `ddi_invalido`, `sem_ddd`, `invalido`) — nenhum reason carrega o numero. Fabrica
`phoneNumberUtil(MetadataLoader)` de nivel de arquivo, sem `Context`, para o `AppContainer` de
02-05 construir a instancia unica fora do `onScreenCall`. 16 testes cobrindo toda a tabela medida.

**Task 3 — regras brasileiras a mao** (commit `ee9753d`)
`codigoCurto(raw)` roda ANTES de qualquer parse (senao `190` viraria `+55190`) e devolve os digitos
crus. `corrigirNonoDigitoBr(parsed)` so dispara com `countryCode == 55`, NSN de 10 digitos e
assinante iniciando em 6..9, e so aceita o resultado se libphonenumber revalidar como
`isValidNumber && type == MOBILE`; caso contrario `Invalid("nono_digito_nao_revalida")`. 11 testes.

## Key Decisions

**Contrato de chave para a Fase 3:** o valor de `NormalizationResult.Valid.e164` e E.164
**exceto** para codigos curtos (menos de `PhoneNumbers.LIMIAR_CURTO` digitos), cujo valor sao os
digitos crus. `190` e persistido e whitelistado como `"190"`. Documentado no KDoc do proprio
`NormalizationResult.Valid` para que a Fase 3 nao precise reler este summary.

**`codigoCurto` recusa entradas com `+` ou letra:** sem isso, `"+55"` (2 digitos apos a
normalizacao) viraria a chave curta `"55"`, um numero inexistente na whitelist.

## Deviations from Plan

### Auto-fixed Issues

**1. [Rule 1 - Bug] Exemplo canonico da mascara era internamente inconsistente**
- **Found during:** Task 1
- **Issue:** O plano mandava assertar `mask("+5511987654321") == "+55 11 9****-1234"`, mas os
  quatro ultimos digitos desse numero sao `4321`. O `+55 11 9****-1234` do CLAUDE.md e um
  exemplo de **formato**, com outro numero por tras.
- **Fix:** O formato canonico continua assertado literalmente, com um numero coerente
  (`+5511987651234`); o numero da tabela medida ganhou teste proprio esperando `-4321`.
- **Files modified:** `app/src/test/java/org/sentinela/app/phone/PhoneMaskTest.kt`
- **Commit:** `bf7700f`

**2. [Rule 1 - Bug] Ordem dos passos da mascara escondia o codigo curto**
- **Found during:** Task 1
- **Issue:** O plano punha `parse` no passo 1 e o corte de codigo curto no passo 3. Mas
  `parse("190", null)` lanca `INVALID_COUNTRY_CODE`, entao `190` cairia na mascara generica —
  o oposto da decisao do usuario de exibi-lo na integra.
- **Fix:** Verificacao de `LIMIAR_CURTO` movida para antes do parse, nos dois lados (mascara e
  normalizer), mantendo a constante e o operador compartilhados.
- **Files modified:** `app/src/main/java/org/sentinela/app/phone/PhoneMask.kt`
- **Commit:** `bf7700f`

**3. [Rule 1 - Bug] Valor esperado do numero do Reino Unido estava errado no plano**
- **Found during:** Task 1
- **Issue:** O plano esperava `+44 20 2****-8750` para `+442071838750`. Com NSN `2071838750` e
  ndc 2, a area e `20` e o resto comeca em `7` — o `2` do valor esperado nao existe nessa posicao.
- **Fix:** Teste assere `+44 20 7****-8750`, o que o algoritmo do §Pattern 3 realmente produz.
- **Files modified:** `app/src/test/java/org/sentinela/app/phone/PhoneMaskTest.kt`
- **Commit:** `bf7700f`

**4. [Rule 1 - Bug] Assercao de entrada hostil era trivialmente falsa para `"+"`**
- **Found during:** Task 1
- **Issue:** `assertFalse(masked.contains(entrada))` falha para a entrada `"+"`, porque a propria
  `MASCARA_GENERICA` (`+** ****`) contem `+`. A assercao media a coisa errada.
- **Fix:** A assercao passou a ser sobre vazamento de **digitos**, aplicada quando a entrada tem
  ao menos `LIMIAR_CURTO` digitos — que e o risco real de privacidade.
- **Files modified:** `app/src/test/java/org/sentinela/app/phone/PhoneMaskTest.kt`
- **Commit:** `bf7700f`

**5. [Rule 3 - Blocking] Criterio de aceite colidia com o proprio KDoc**
- **Found during:** Task 2
- **Issue:** O criterio exigia ausencia literal de `createInstance(context` no arquivo, mas o
  KDoc que o plano mandava escrever citava exatamente essa string ao proibi-la.
- **Fix:** O KDoc passou a proibir "criar o util a partir de um `Context`" em prosa.
- **Files modified:** `app/src/main/java/org/sentinela/app/phone/LibPhoneNumberNormalizer.kt`
- **Commit:** `3c5bc60`

**6. [Rule 3 - Blocking] Caso negativo do 9o digito precisava de entrada valida-de-parse**
- **Found during:** Task 3
- **Issue:** Para exercitar `nono_digito_nao_revalida` e preciso um numero que **passe** no parse
  com NSN de 10 digitos e assinante em 6..9, mas cuja correcao nao revalide. Texto com letras
  lanca `NumberParseException` antes e testaria outro caminho.
- **Fix:** Usados DDDs inexistentes no Brasil (`10` e `20`): `1087654321` e `2087654321` passam no
  parse, disparam a regra e sao recusados na revalidacao.
- **Files modified:** `app/src/test/java/org/sentinela/app/phone/BrazilianRulesNormalizerTest.kt`
- **Commit:** `ee9753d`

### Notas

Nenhum checkpoint foi atingido (plano autonomo, validacao fisica diferida para a Fase 9).
`koverVerify` continua desligado por decisao de 02-01 — apenas `koverLog` foi executado.

## Verification

```
./gradlew assembleDebug testDebugUnitTest koverLog lint detekt   -> BUILD SUCCESSFUL
bash scripts/verify-invariants.sh                                -> todos os invariantes OK
```

- `PhoneMaskTest` 14 testes, `LibPhoneNumberNormalizerTest` 16, `BrazilianRulesNormalizerTest` 11
  — todos com `failures="0" errors="0"`
- Cobertura de linhas `domain` + `phone`: **97,619%** (baseline anterior 94,74%; gate futuro 80)
- `phone/` sem `import android.` — confirmado pelo Bloco 3 do verify-invariants
- detekt e lint sem issues

## Self-Check: PASSED

Todos os 7 arquivos existem em disco e os 3 commits (`bf7700f`, `3c5bc60`, `ee9753d`) estao no
historico do `master`.

## For Next Phase

- **02-05** deve construir o `PhoneNumberUtil` uma unica vez no `AppContainer` via
  `phoneNumberUtil(loader)` e injeta-lo no `LibPhoneNumberNormalizer` junto do
  `CascadingRegionProvider` — nunca dentro do Service.
- **Fase 3** persiste a chave conforme o contrato acima: E.164, exceto codigo curto em digito cru.
