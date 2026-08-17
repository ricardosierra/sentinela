#!/usr/bin/env bash
# Valida o APK release produzido pelo CI sem depender de nome/versionamento externo.
set -euo pipefail

APK="${1:-app/build/outputs/apk/release/app-release.apk}"
if [[ ! -f "$APK" ]]; then
  echo "erro: APK release ausente: $APK" >&2
  exit 2
fi

BUILD_TOOLS="$(find "${ANDROID_HOME:?ANDROID_HOME não definido}/build-tools" -mindepth 1 -maxdepth 1 -type d | sort -V | tail -1)"
if [[ -z "$BUILD_TOOLS" || ! -x "$BUILD_TOOLS/aapt2" || ! -x "$BUILD_TOOLS/apksigner" ]]; then
  echo "erro: aapt2/apksigner não encontrados em $ANDROID_HOME/build-tools" >&2
  exit 2
fi

"$BUILD_TOOLS/apksigner" verify --verbose --print-certs "$APK"
if "$BUILD_TOOLS/aapt2" dump permissions "$APK" | grep -q 'android.permission.INTERNET'; then
  echo "erro: INTERNET encontrada no APK release — o Sentinela precisa permanecer offline" >&2
  exit 1
fi
echo "ok: APK release assinado e sem INTERNET"
