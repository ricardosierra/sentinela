#!/usr/bin/env bash
# Invariantes verificaveis do Sentinela (criterios 3, 4 e 5 da Phase 1).
# Reexecutavel a cada fase: falha quando alguem antecipa permissao de fase futura
# ou quebra a centralizacao de rebranding.
#
# Uso: ./gradlew assembleDebug && bash scripts/verify-invariants.sh
#
# NAO usar `set -e`: `grep -c` sai com codigo 1 quando o resultado e 0, o que
# abortaria o script justamente no caso de sucesso.
set -uo pipefail

export ANDROID_HOME="${ANDROID_HOME:-$HOME/Library/Android/sdk}"
FAILURES=0
fail() { echo "FAIL: $*"; FAILURES=$((FAILURES + 1)); }
ok()   { echo "ok:   $*"; }
skip() { echo "skip: $*"; }

M=app/build/intermediates/merged_manifest/debug/processDebugMainManifest/AndroidManifest.xml
if [ ! -f "$M" ]; then
  echo "Manifest mergeado ausente. Rode ./gradlew assembleDebug antes." >&2
  exit 2
fi

# ---------------------------------------------------------------------------
# Bloco 1 — PRV-01 / criterio 3: manifest MERGEADO (o fonte mente por omissao)
# ---------------------------------------------------------------------------
echo "== Bloco 1: permissoes no manifest mergeado =="

if [ "$(grep -c "android.permission.INTERNET" "$M")" -eq 0 ]; then
  ok "sem android.permission.INTERNET (PRV-01)"
else
  fail "android.permission.INTERNET presente no manifest mergeado — MVP e 100% offline"
fi

# Allowlist literal (NUNCA cardinalidade: quebra no proximo bump de androidx).
# POST_NOTIFICATIONS e autorizada por docs/PERMISSOES.md (Fase 1 manifest / Fase 5 pedido).
# DYNAMIC_RECEIVER_NOT_EXPORTED_PERMISSION e injetada pelo androidx-core, e de
# assinatura, nao concede capacidade e nao e removivel.
# READ_CONTACTS e autorizada por docs/PERMISSOES.md (linha 14): entra no manifest na Fase 4,
# uso exclusivamente local e em memoria, pedido em runtime no onboarding de contatos.
ALLOWLIST="android.permission.POST_NOTIFICATIONS
org.sentinela.app.DYNAMIC_RECEIVER_NOT_EXPORTED_PERMISSION
android.permission.READ_CONTACTS"

DECLARED=$(grep -o 'uses-permission android:name="[^"]*"' "$M" | sed 's/.*name="//;s/"//' | sort -u)

INTRUDERS=0
while IFS= read -r perm; do
  [ -z "$perm" ] && continue
  if echo "$ALLOWLIST" | grep -qx "$perm"; then
    ok "permissao autorizada: $perm"
  else
    fail "permissao fora da allowlist: $perm — ver docs/PERMISSOES.md"
    INTRUDERS=$((INTRUDERS + 1))
  fi
done <<< "$DECLARED"
[ "$INTRUDERS" -eq 0 ] && ok "nenhuma permissao fora da allowlist"

# Permissoes de fase futura (entram no manifest so na fase delas) mais uma entrada que e
# proibida PARA SEMPRE: a gravacao na agenda. O app so le contatos; nenhum manifest — nem o
# de androidTest — pode declarar a capacidade de escrita. Os testes preparam dados adotando
# a identidade de shell da instrumentacao, que nao depende de permissao declarada.
FUTURE="READ_CALL_LOG|READ_PHONE_STATE|READ_SMS|CALL_PHONE|BIND_INCALL_SERVICE|SYSTEM_ALERT_WINDOW|WRITE_CONTACTS"
if [ "$(grep -cE "$FUTURE" "$M")" -eq 0 ]; then
  ok "nenhuma permissao de fase futura antecipada"
else
  grep -oE "$FUTURE" "$M" | sort -u | sed 's/^/      /'
  fail "permissao de fase futura antecipada — ver docs/PERMISSOES.md"
fi

if [ "$(grep -c "BIND_SCREENING_SERVICE" "$M")" -ge 1 ]; then
  ok "servico protegido por BIND_SCREENING_SERVICE"
else
  fail "BIND_SCREENING_SERVICE ausente no manifest mergeado"
fi

if [ "$(grep -c "android.telecom.CallScreeningService" "$M")" -ge 1 ]; then
  ok "action android.telecom.CallScreeningService registrada"
else
  fail "CallScreeningService nao registrado no manifest mergeado"
fi

# ---------------------------------------------------------------------------
# Bloco 2 — UIX-12 / criterio 5: centralizacao para rebranding
# ---------------------------------------------------------------------------
echo "== Bloco 2: rebranding centralizado =="

check_empty() {
  local label="$1"; shift
  local out
  out=$("$@" 2>/dev/null)
  if [ -z "$out" ]; then
    ok "$label"
  else
    echo "$out" | sed 's/^/      /'
    fail "$label"
  fi
}

LITERAL_ID=$(grep -rn "org.sentinela.app" app/src/main/java --include="*.kt" | grep -v ":package \|:import ")
if [ -z "$LITERAL_ID" ]; then
  ok "nenhum applicationId literal em Kotlin"
else
  echo "$LITERAL_ID" | sed 's/^/      /'
  fail "applicationId literal em Kotlin (fora de package/import)"
fi

check_empty "nenhuma string hardcoded (text = \"...\") em Kotlin" \
  grep -rn 'text = "' app/src/main/java --include="*.kt"

HARDCODED_COLOR=$(grep -rn "Color(0x" app/src/main/java --include="*.kt" | grep -v "/ui/theme/")
if [ -z "$HARDCODED_COLOR" ]; then
  ok "nenhuma cor literal fora de ui/theme"
else
  echo "$HARDCODED_COLOR" | sed 's/^/      /'
  fail "cor literal fora do design system (ui/theme)"
fi

APPID_REFS=$(grep -c "sentinelaApplicationId" app/build.gradle.kts)
if [ "$APPID_REFS" -eq 3 ]; then
  ok "sentinelaApplicationId usado 3x em app/build.gradle.kts"
else
  fail "sentinelaApplicationId aparece ${APPID_REFS}x em app/build.gradle.kts (esperado 3)"
fi

if [ "$(grep -c "app_name" app/src/main/res/values/strings.xml)" -ge 1 ]; then
  ok "app_name definido em strings.xml"
else
  fail "app_name ausente em app/src/main/res/values/strings.xml"
fi

# ---------------------------------------------------------------------------
# Bloco 3 — criterio 4: dominio e normalizacao puros (nao conhecem Android/Telecom)
# ---------------------------------------------------------------------------
echo "== Bloco 3: dominio e normalizacao puros =="

# phone/ entra na Fase 2: LibPhoneNumberNormalizer usa io.michaelrocks.libphonenumber.android,
# que apesar do nome do pacote e codigo JVM puro. Quem conhece Context/TelephonyManager e a
# camada platform/, nunca phone/.
for pkg in domain phone; do
  DIR="app/src/main/java/org/sentinela/app/$pkg"
  [ -d "$DIR" ] || { skip "pacote $pkg ausente"; continue; }
  IMPORTS=$(grep -rn "^import android" "$DIR")
  if [ -z "$IMPORTS" ]; then
    ok "$pkg sem import de android.*"
  else
    echo "$IMPORTS" | sed 's/^/      /'
    fail "$pkg importa android.* — regra de decisao e normalizacao devem ser JVM puras"
  fi
done

# ---------------------------------------------------------------------------
# Bloco 4 — QLT-02: relatorios de qualidade (gate real e o plano 03)
# ---------------------------------------------------------------------------
echo "== Bloco 4: relatorios de qualidade =="

# Relatorio pode ser apagado/regenerado por um build concorrente entre o teste de
# existencia e o grep — por isso a contagem cai para 0 apenas se o arquivo sumiu.
count_in_report() {
  local file="$1" pattern="$2" n
  [ -f "$file" ] || return 1
  # `grep -c` imprime 0 E sai com codigo 1 quando nao ha match; a atribuicao
  # descarta o status. NAO acrescentar um fallback com `echo` no ramo de erro:
  # o zero ja foi impresso e a contagem sairia duplicada ("0\n0").
  n=$(grep -c "$pattern" "$file" 2>/dev/null)
  echo "${n:-0}"
}

DETEKT_REPORT=app/build/reports/detekt/detekt.xml
if DETEKT_ISSUES=$(count_in_report "$DETEKT_REPORT" "<error"); then
  if [ "$DETEKT_ISSUES" -eq 0 ]; then
    ok "detekt sem issues"
  else
    fail "detekt reportou $DETEKT_ISSUES issue(s) em $DETEKT_REPORT"
  fi
else
  skip "detekt.xml ausente (rode ./gradlew detekt)"
fi

LINT_REPORT=app/build/reports/lint-results-debug.xml
# Padrao com espaco: `<issue ` casa so o elemento de issue, nunca a raiz `<issues>`.
if LINT_ISSUES=$(count_in_report "$LINT_REPORT" "<issue "); then
  if [ "$LINT_ISSUES" -eq 0 ]; then
    ok "lint sem issues"
  else
    fail "lint reportou $LINT_ISSUES issue(s) em $LINT_REPORT"
  fi
else
  skip "lint-results-debug.xml ausente (rode ./gradlew lint)"
fi

# ---------------------------------------------------------------------------
# Bloco 5 — Fase 3: integridade do dado local (o risco da fase e PERDER dado)
# ---------------------------------------------------------------------------
echo "== Bloco 5: integridade do dado local =="

# A migracao destrutiva do Room apagaria a whitelist do usuario numa atualizacao.
# O match e proposital ate em comentario: linha comentada hoje vira linha ativa
# amanha. Por isso Migrations.kt descreve o metodo em vez de escrever o nome.
DESTRUCTIVE=$(grep -rn "fallbackToDestructiveMigration" app/src/main --include="*.kt" 2>/dev/null)
if [ -z "$DESTRUCTIVE" ]; then
  ok "sem fallbackToDestructiveMigration (migracao explicita obrigatoria)"
else
  echo "$DESTRUCTIVE" | sed 's/^/      /'
  fail "fallbackToDestructiveMigration proibido (Fase 3): apagaria o dado do usuario"
fi

# allowMainThreadQueries mascara o problema de dispatch em vez de resolve-lo.
MAINTHREAD=$(grep -rn "allowMainThreadQueries" app/src/main --include="*.kt" 2>/dev/null)
if [ -z "$MAINTHREAD" ]; then
  ok "sem allowMainThreadQueries"
else
  echo "$MAINTHREAD" | sed 's/^/      /'
  fail "allowMainThreadQueries proibido (Fase 3)"
fi

# Schema v1 exportado e versionado — o JSON e o oraculo da migracao.
SCHEMAS=$(ls -1 app/schemas/*/1.json 2>/dev/null | wc -l | tr -d ' ')
if [ "${SCHEMAS:-0}" -ge 1 ]; then
  ok "schema Room v1 exportado (app/schemas/*/1.json)"
else
  fail "app/schemas/<db>/1.json ausente — exportSchema desligado?"
fi

# O historico guarda E.164, mas NUNCA nome de contato (docs/PRIVACIDADE.md).
CONTACT_COL=$(grep -rniE "contactName|contact_name|displayName|display_name" \
  app/src/main/java/org/sentinela/app/data/local 2>/dev/null)
if [ -z "$CONTACT_COL" ]; then
  ok "nenhuma coluna de nome de contato na camada de dados"
else
  echo "$CONTACT_COL" | sed 's/^/      /'
  fail "nome de contato na camada de dados — proibido (docs/PRIVACIDADE.md)"
fi

# ---------------------------------------------------------------------------
# Bloco 6 — Fase 4: dado de contato nunca sai da memoria
# ---------------------------------------------------------------------------
# O risco desta fase e privacidade, nao performance: e a primeira vez que o app toca em dado
# pessoal de terceiros. Revisao de codigo nao conta como prova; estas quatro checagens sao a
# prova. A proibicao de gravar na agenda ja esta no Bloco 1 (variavel FUTURE) e NAO e repetida.
#
# ATENCAO ao escopo: todo grep deste bloco aponta para app/schemas ou app/src/main/java, nunca
# para scripts/. Os padroes abaixo sao literais que eles mesmos procuram — incluir este arquivo
# no escopo faria o invariante falhar sozinho, sem nenhuma violacao real.
echo "== Bloco 6: dado de contato apenas em memoria =="

# 6.1 — nenhuma coluna do banco pode carregar identidade de contato.
# O padrao e aplicado aos VALORES de "columnName", jamais as CHAVES do JSON: o schema exportado
# e cheio de chaves chamadas "name" (tableName, fields[].name, indices[].name), e casar contra
# elas daria falso positivo em 100% dos builds.
LEAK_PAT='(^|_)(name|display|contact|photo|lookup|nome|agenda)'
LEAKED=$(grep -ohE '"columnName": "[^"]*"' app/schemas/*/*.json 2>/dev/null \
  | sed 's/.*: "//;s/"//' | sort -u | grep -E "$LEAK_PAT")
if [ -z "$LEAKED" ]; then
  ok "nenhuma coluna de identidade de contato no schema exportado"
else
  echo "$LEAKED" | sed 's/^/      /'
  fail "coluna de identidade de contato no schema exportado — proibido (docs/PRIVACIDADE.md)"
fi

# 6.2 — fronteira: so um pacote fala com o provider da agenda.
# O padrao casa o USO do provider (import do pacote ou acesso a membro), nao o nome da classe do
# app que o encapsula: o container precisa poder construir essa classe sem falar com o provider.
PROVIDER_PAT='android\.provider\.Contacts|ContactsContract\.'
PROVIDER_FORA=$(grep -rnE "$PROVIDER_PAT" app/src/main/java --include="*.kt" 2>/dev/null \
  | grep -v "app/src/main/java/org/sentinela/app/data/contacts/")
if [ -z "$PROVIDER_FORA" ]; then
  ok "provider de contatos so e citado em data/contacts"
else
  echo "$PROVIDER_FORA" | sed 's/^/      /'
  fail "provider de contatos citado fora de data/contacts — a fronteira e o que garante a regra"
fi

# 6.3 — nenhuma coluna de identidade do contato e projetada em lugar nenhum do codigo de app.
IDENT_PAT='DISPLAY_NAME|PHOTO_URI|PHOTO_THUMBNAIL_URI|PHOTO_FILE_ID|LOOKUP_KEY'
IDENT=$(grep -rnE "$IDENT_PAT" app/src/main/java --include="*.kt" 2>/dev/null)
if [ -z "$IDENT" ]; then
  ok "nenhuma coluna de identidade do contato projetada em app/src/main/java"
else
  echo "$IDENT" | sed 's/^/      /'
  fail "coluna de identidade do contato projetada — leia apenas presenca e numero"
fi

# 6.4 — a camada de contatos nao tem como persistir nada: sem Room, sem DataStore, sem arquivo.
PERSIST_PAT='@Entity|@Dao|Room\.|DataStore|edit \{|openFileOutput|SharedPreferences'
PERSIST=$(grep -rnE "$PERSIST_PAT" app/src/main/java/org/sentinela/app/data/contacts \
  --include="*.kt" 2>/dev/null)
if [ -z "$PERSIST" ]; then
  ok "data/contacts sem nenhum mecanismo de persistencia"
else
  echo "$PERSIST" | sed 's/^/      /'
  fail "mecanismo de persistencia em data/contacts — contato vive so em memoria"
fi

# ---------------------------------------------------------------------------
if [ "$FAILURES" -gt 0 ]; then
  echo "== $FAILURES invariante(s) violado(s) =="
  exit 1
fi
echo "== todos os invariantes OK =="
