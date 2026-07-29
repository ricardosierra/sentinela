#!/usr/bin/env bash
# Prova, no aparelho virtual, do ciclo de vida do papel de telefone padrao e da morte do
# processo no meio de uma chamada.
#
# POR QUE ISTO E UM SCRIPT E NAO UM TESTE INSTRUMENTADO — medido na Fase 6, plano 06-07:
# quando o aplicativo PERDE um papel do sistema, a plataforma revoga as permissoes do papel e
# ENCERRA o processo do aplicativo. Logcat, verbatim:
#
#   ActivityManager: Killing <pid>:<pacote do aplicativo>/<uid> (adj 0): Permission or app op changed
#
# O mesmo acontece ao devolver o papel de triagem, e o mesmo acontece quando o proprio usuario
# troca o telefone padrao nas configuracoes do sistema. Como a instrumentacao roda DENTRO do
# processo do aplicativo, um teste que devolva o proprio papel — ou que mate o proprio processo
# no meio de uma chamada, que e o outro cenario desta fase — morre junto com o que quer observar,
# e o resultado e "Process crashed", nunca uma assercao. Provas cujo objeto e a morte do proprio
# processo pertencem, portanto, a quem esta FORA dele: o computador que dirige o aparelho.
#
# Nada aqui e mais fraco que um teste: cada passo confere codigo de saida de verdade, efeito
# observavel no sistema de papeis e diagnostico do sistema de telefonia, e o script sai diferente
# de zero em qualquer divergencia.
#
# Uso: bash scripts/verify-dialer-lifecycle.sh
#   (exige o aplicativo instalado; rode ./gradlew installDebug antes, ou
#    bash scripts/run-instrumented-tests.sh, que instala)
set -uo pipefail

export ANDROID_HOME="${ANDROID_HOME:-$HOME/Library/Android/sdk}"
ADB="$ANDROID_HOME/platform-tools/adb"
PKG="$(grep -o 'sentinelaApplicationId = "[^"]*"' app/build.gradle.kts | head -1 | cut -d'"' -f2)"
PRELOADED_DIALER="com.google.android.dialer"
ROLE_DIALER="android.app.role.DIALER"
ROLE_SCREENING="android.app.role.CALL_SCREENING"
NUMERO_TESTE="5551234"

FAILURES=0
fail() { echo "FAIL: $*"; FAILURES=$((FAILURES + 1)); }
ok()   { echo "ok:   $*"; }

[ -x "$ADB" ] || { echo "FAIL: adb ausente em $ADB" >&2; exit 2; }
[ -n "$PKG" ] || { echo "FAIL: applicationId nao resolvido de app/build.gradle.kts" >&2; exit 2; }

DEVICES=$("$ADB" devices | grep -cw "device")
[ "${DEVICES:-0}" -ge 1 ] || { echo "FAIL: nenhum aparelho conectado" >&2; exit 2; }

PACOTES=$("$ADB" shell pm list packages 2>/dev/null | tr -d '\r')
if ! printf '%s\n' "$PACOTES" | grep -q "^package:${PKG}$"; then
  echo "FAIL: $PKG nao esta instalado no aparelho (rode ./gradlew installDebug)" >&2
  exit 2
fi

# Contagem em vez de `grep -q`: com saida grande, o grep que para no primeiro acerto fecha o cano
# e quem escreve morre de cano quebrado — o caso de SUCESSO seria lido como falha. `grep -c` le a
# entrada inteira e a contagem vai para variavel, onde nenhum codigo de saida atrapalha.
contem() { local n; n=$(printf '%s' "$1" | grep -c "$2"); [ "${n:-0}" -ge 1 ]; }

holders() { "$ADB" shell cmd role get-role-holders "$1" | tr -d '\r' | tr -d '\n'; }
pid_do_app() { "$ADB" shell pidof "$PKG" | tr -d '\r' | tr -d '\n'; }

restaurar() {
  echo "== restaurando o aparelho =="
  "$ADB" shell telecom cleanup-stuck-calls >/dev/null 2>&1
  "$ADB" shell cmd role remove-role-holder "$ROLE_DIALER" "$PKG" >/dev/null 2>&1
  "$ADB" shell cmd role remove-role-holder "$ROLE_SCREENING" "$PKG" >/dev/null 2>&1
}
trap restaurar EXIT

# ---------------------------------------------------------------------------
# 1 — ponto de partida: o aparelho tem telefone padrao e nao e o nosso
# ---------------------------------------------------------------------------
echo "== 1: ponto de partida =="
INICIAL=$(holders "$ROLE_DIALER")
if [ "$INICIAL" = "$PRELOADED_DIALER" ]; then
  ok "telefone padrao inicial e o discador de fabrica ($INICIAL)"
else
  fail "detentor inicial inesperado: '$INICIAL' (esperado $PRELOADED_DIALER)"
fi

# ---------------------------------------------------------------------------
# 2 — elegibilidade: a concessao que VERIFICA os requisitos precisa passar
#     (o atalho de configuracao de telefonia bypassaria a verificacao e por isso
#      nao aparece neste script)
# ---------------------------------------------------------------------------
echo "== 2: concessao do papel de triagem e do papel de telefone padrao =="
if "$ADB" shell cmd role add-role-holder "$ROLE_SCREENING" "$PKG"; then
  ok "papel de triagem concedido (codigo de saida 0)"
else
  fail "papel de triagem negado"
fi

if "$ADB" shell cmd role add-role-holder "$ROLE_DIALER" "$PKG"; then
  ok "papel de telefone padrao concedido pelo caminho que verifica elegibilidade"
else
  fail "papel de telefone padrao NEGADO — manifesto perdeu o servico de interface de chamada ou um dos dois filtros de discagem"
fi

DEPOIS=$(holders "$ROLE_DIALER")
if [ "$DEPOIS" = "$PKG" ]; then
  ok "o aplicativo e o telefone padrao do aparelho"
else
  fail "detentor apos a concessao: '$DEPOIS' (esperado $PKG)"
fi

TRIAGEM=$(holders "$ROLE_SCREENING")
if [ "$TRIAGEM" = "$PKG" ]; then
  ok "os dois papeis convivem no mesmo aplicativo (um unico vinculo de triagem, medido na pesquisa)"
else
  fail "papel de triagem: '$TRIAGEM' (esperado $PKG)"
fi

# ---------------------------------------------------------------------------
# 3 — morrer no meio de uma chamada NAO derruba a chamada
# ---------------------------------------------------------------------------
echo "== 3: morte do processo no meio de uma chamada =="
"$ADB" shell telecom cleanup-stuck-calls >/dev/null 2>&1
"$ADB" logcat -c >/dev/null 2>&1
"$ADB" shell am start -a android.intent.action.CALL -d "tel:$NUMERO_TESTE" >/dev/null 2>&1

ATIVA=0
for _ in $(seq 1 20); do
  sleep 1
  DUMP=$("$ADB" shell dumpsys telecom 2>/dev/null)
  if contem "$DUMP" "state=ACTIVE\|state=DIALING"; then ATIVA=1; break; fi
done
if [ "$ATIVA" -eq 1 ]; then
  ok "chamada de saida em curso no sistema de telefonia"
else
  fail "nenhuma chamada em curso apos originar — cenario da morte nao pode ser montado"
fi

PID_ANTES=$(pid_do_app)
if [ -n "$PID_ANTES" ]; then
  ok "processo do aplicativo vivo durante a chamada (pid $PID_ANTES)"
else
  fail "o processo do aplicativo nao estava vivo durante a chamada"
fi

"$ADB" shell am force-stop "$PKG"
sleep 3

PID_DEPOIS=$(pid_do_app)
if [ -z "$PID_DEPOIS" ] || [ "$PID_DEPOIS" != "$PID_ANTES" ]; then
  ok "o processo morreu de fato (era $PID_ANTES, agora '${PID_DEPOIS:-nenhum}')"
else
  fail "o processo continuou o mesmo apos ser encerrado — a morte nao aconteceu"
fi

DUMP=$("$ADB" shell dumpsys telecom 2>/dev/null)
if contem "$DUMP" "state=ACTIVE\|state=DIALING"; then
  ok "A CHAMADA SOBREVIVEU a morte do nosso processo"
else
  fail "a chamada caiu quando o processo morreu — pior falha possivel da fase"
fi

# Pipeline com `grep -q` sobre saida grande e proibido aqui: o grep fecha o cano ao primeiro
# acerto, o diagnostico morre de cano quebrado e o modo do shell reprova a linha inteira — ou
# seja, o caso de SUCESSO seria lido como falha. Por isso a saida vai para variavel primeiro.
DIAGNOSTICO=$("$ADB" logcat -d 2>/dev/null)
if contem "$DIAGNOSTICO" "InCallController.*$PRELOADED_DIALER"; then
  ok "o sistema de telefonia religou no discador de fabrica sozinho"
else
  echo "      (aviso: nao localizei no diagnostico a religacao no discador de fabrica; a sobrevivencia da chamada, que e o criterio, foi confirmada acima)"
fi

"$ADB" shell telecom cleanup-stuck-calls >/dev/null 2>&1
sleep 1
DUMP=$("$ADB" shell dumpsys telecom 2>/dev/null)
if contem "$DUMP" "state=ACTIVE\|state=DIALING"; then
  fail "chamada presa apos a limpeza"
else
  ok "limpeza de chamadas presas devolveu o aparelho ao estado neutro"
fi

# ---------------------------------------------------------------------------
# 4 — a chamada seguinte volta a ser entregue ao aplicativo
# ---------------------------------------------------------------------------
echo "== 4: a chamada seguinte volta a ser nossa =="
"$ADB" logcat -c >/dev/null 2>&1
"$ADB" shell am start -a android.intent.action.CALL -d "tel:$NUMERO_TESTE" >/dev/null 2>&1
sleep 6
DIAGNOSTICO=$("$ADB" logcat -d 2>/dev/null)
if contem "$DIAGNOSTICO" "InCallController.*$PKG"; then
  ok "o sistema voltou a vincular o servico de interface de chamada do aplicativo"
else
  fail "o sistema nao voltou a vincular o nosso servico na chamada seguinte"
fi
"$ADB" shell telecom cleanup-stuck-calls >/dev/null 2>&1

# ---------------------------------------------------------------------------
# 5 — reversao: devolver o papel restaura o discador de fabrica, encerra o
#     aplicativo e NAO toca no papel de triagem
# ---------------------------------------------------------------------------
echo "== 5: reversao =="
"$ADB" shell monkey -p "$PKG" -c android.intent.category.LAUNCHER 1 >/dev/null 2>&1
sleep 4
PID_VIVO=$(pid_do_app)
"$ADB" logcat -c >/dev/null 2>&1

if "$ADB" shell cmd role remove-role-holder "$ROLE_DIALER" "$PKG"; then
  ok "devolucao do papel aceita (codigo de saida 0)"
else
  fail "devolucao do papel falhou"
fi
sleep 3

FINAL=$(holders "$ROLE_DIALER")
if [ "$FINAL" = "$PRELOADED_DIALER" ]; then
  ok "o discador de fabrica voltou a ser o telefone padrao — telefonia nunca fica sem aplicativo"
else
  fail "detentor apos a reversao: '$FINAL' (esperado $PRELOADED_DIALER)"
fi

TRIAGEM_FINAL=$(holders "$ROLE_SCREENING")
if [ "$TRIAGEM_FINAL" = "$PKG" ]; then
  ok "o papel de triagem SOBREVIVEU a reversao — o modo filtro continua valendo sem reconfiguracao"
else
  fail "papel de triagem apos a reversao: '$TRIAGEM_FINAL' (esperado $PKG)"
fi

if [ -n "$PID_VIVO" ]; then
  PID_POS=$(pid_do_app)
  if [ "$PID_POS" != "$PID_VIVO" ]; then
    ok "a plataforma encerrou o aplicativo ao retirar o papel (era $PID_VIVO, agora '${PID_POS:-nenhum}')"
  else
    fail "o processo sobreviveu a perda do papel — contradiz a medicao desta fase"
  fi
  DIAGNOSTICO=$("$ADB" logcat -d 2>/dev/null)
  if contem "$DIAGNOSTICO" "Killing.*$PKG.*Permission or app op changed"; then
    ok "motivo registrado pelo sistema: mudanca de permissao ao perder o papel"
  else
    echo "      (aviso: o motivo do encerramento nao apareceu no diagnostico desta janela)"
  fi
fi

echo
if [ "$FAILURES" -eq 0 ]; then
  echo "TODOS os passos do ciclo de vida do modo discador OK"
  exit 0
fi
echo "$FAILURES passo(s) do ciclo de vida FALHARAM"
exit 1
