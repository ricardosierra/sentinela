#!/usr/bin/env bash
# Sobe o emulador headless, roda a suite instrumentada e derruba o emulador.
# Decisao da Fase 3: testes de DAO/migracao rodam DE VERDADE em emulador; subir
# emulador e automatizavel, entao nao existe parada humana para isto.
#
# Uso: bash scripts/run-instrumented-tests.sh [args extras do gradlew]
#   bash scripts/run-instrumented-tests.sh --tests "*WhitelistDaoTest"
set -uo pipefail

export ANDROID_HOME="${ANDROID_HOME:-$HOME/Library/Android/sdk}"
ADB="$ANDROID_HOME/platform-tools/adb"
EMULATOR="$ANDROID_HOME/emulator/emulator"
AVD="${SENTINELA_AVD:-Medium_Phone_API_35}"
STARTED_BY_US=0
EMU_SERIAL=""

# `connectedDebugAndroidTest` NAO aceita `--tests` (nao e uma Test task do Gradle;
# e uma DeviceProviderInstrumentTestTask). O filtro equivalente e o argumento
# `tests_regex` do AndroidJUnitRunner, casado contra "pacote.Classe#metodo".
# Traduzimos `--tests <glob>` para manter a ergonomia dos outros comandos do repo.
GRADLE_ARGS=()
TEST_REGEX=""
while [ "$#" -gt 0 ]; do
  case "$1" in
    --tests)
      [ "$#" -ge 2 ] || { echo "FAIL: --tests exige um padrao" >&2; exit 2; }
      PATTERN=${2//\*/.*}
      if [ -n "$TEST_REGEX" ]; then TEST_REGEX="$TEST_REGEX|$PATTERN"; else TEST_REGEX="$PATTERN"; fi
      shift 2
      ;;
    *)
      GRADLE_ARGS+=("$1")
      shift
      ;;
  esac
done
if [ -n "$TEST_REGEX" ]; then
  GRADLE_ARGS+=("-Pandroid.testInstrumentationRunnerArguments.tests_regex=($TEST_REGEX).*")
fi

cleanup() {
  if [ "$STARTED_BY_US" -eq 1 ] && [ -n "$EMU_SERIAL" ]; then
    echo "== derrubando $EMU_SERIAL =="
    "$ADB" -s "$EMU_SERIAL" emu kill >/dev/null 2>&1
  fi
}
trap cleanup EXIT

[ -x "$ADB" ] || { echo "FAIL: adb ausente em $ADB" >&2; exit 2; }

# `grep -c` sai 1 quando conta 0 — capturar em variavel descarta o status.
DEVICES=$("$ADB" devices | grep -cw "device")
if [ "${DEVICES:-0}" -ge 1 ]; then
  echo "== device ja conectado; reaproveitando =="
else
  [ -x "$EMULATOR" ] || { echo "FAIL: emulator ausente em $EMULATOR" >&2; exit 2; }
  if ! "$EMULATOR" -list-avds | grep -qx "$AVD"; then
    echo "FAIL: AVD '$AVD' nao encontrado (emulator -list-avds)" >&2
    exit 2
  fi
  echo "== subindo $AVD headless =="
  nohup "$EMULATOR" -avd "$AVD" \
    -no-window -no-audio -no-boot-anim -no-snapshot -gpu swiftshader_indirect \
    > /tmp/sentinela-emulator.log 2>&1 &
  STARTED_BY_US=1

  # `adb wait-for-device` NAO basta: o device fica "offline" por varios segundos.
  # A unica prova de boot e sys.boot_completed (medido na pesquisa da Fase 3).
  echo "== esperando boot (ate 600 s) =="
  if ! timeout 600 "$ADB" wait-for-device shell \
      'while [ "$(getprop sys.boot_completed)" != "1" ]; do sleep 2; done; echo BOOTED'; then
    echo "FAIL: emulador nao completou o boot em 600 s (log: /tmp/sentinela-emulator.log)" >&2
    EMU_SERIAL=$("$ADB" devices | awk '/^emulator-/ {print $1; exit}')
    exit 3
  fi
fi

EMU_SERIAL=$("$ADB" devices | awk '/^emulator-/ {print $1; exit}')

# Relatorios antigos viram falso-verde: a suite so vale como evidencia se o XML
# tiver nascido desta execucao.
rm -f app/build/outputs/androidTest-results/connected/debug/TEST-*.xml

# bash 3.2 (padrao do macOS) trata array vazio como unbound sob `set -u`.
echo "== ./gradlew :app:connectedDebugAndroidTest ${GRADLE_ARGS[@]+${GRADLE_ARGS[@]}} =="
./gradlew :app:connectedDebugAndroidTest ${GRADLE_ARGS[@]+"${GRADLE_ARGS[@]}"}
STATUS=$?

echo "== relatorios =="
ls -1 app/build/outputs/androidTest-results/connected/debug/TEST-*.xml 2>/dev/null
exit "$STATUS"
