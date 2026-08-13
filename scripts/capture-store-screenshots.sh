#!/usr/bin/env bash
#
# Captura as telas da ficha da Google Play em telefone, tablet 7" e tablet 10".
#
# Por que script e não um lote manual: as capturas precisam ser refeitas a cada
# idioma novo e a cada mudança de UI. Feito na mão, o conjunto vira uma mistura de
# versões diferentes do app — e ninguém percebe olhando o PNG.
#
# Uso:
#   ./scripts/capture-store-screenshots.sh                 # pt-BR, en, es
#   ./scripts/capture-store-screenshots.sh pt-BR de fr ja  # os idiomas que pedir
#   ./scripts/capture-store-screenshots.sh --todos         # todo values-XX do projeto
#
# Requer: emulador rodando (adb devices), APK debug instalado e ImageMagick.
# O APK precisa ser DESTE código — traduções que entraram depois do último build
# não aparecem, e o print sai no idioma errado sem nenhum aviso.
set -euo pipefail

RAIZ="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
PKG=org.sentinela.app
SAIDA="$RAIZ/docs/loja/graficos/screenshots"
FUNDO='srgb(250,248,255)'   # cor de superfície da UI clara, para a moldura

# perfil : largura : altura : densidade
# Telefone é 1080x2400 (2,22:1) e a Play recusa acima de 2:1 — por isso a moldura
# até 1200x2400. Os tablets já nascem em 1,6:1 e não precisam.
PERFIS=(
  "telefone:1080:2400:440:1200:2400"
  "tablet7:1200:1920:213:1200:1920"
  "tablet10:1600:2560:320:1600:2560"
)

# Qualificador de recurso do Android -> tag BCP-47 que o `cmd locale` entende.
tag_bcp47() {
  case "$1" in
    in) echo "id" ;;                        # indonésio: qualificador legado
    iw) echo "he" ;;                        # hebraico: idem
    ji) echo "yi" ;;                        # iídiche: idem
    *)  echo "${1/-r/-}" ;;                 # zh-rCN -> zh-CN
  esac
}

locales_do_projeto() {
  # pt-BR mora em values/ (defaultLocale), então entra fixo.
  echo "pt-BR"
  for d in "$RAIZ"/app/src/main/res/values-*/; do
    q="$(basename "$d")"; q="${q#values-}"
    [[ -f "$d/strings.xml" ]] || continue
    tag_bcp47 "$q"
  done
}

if [[ "${1:-}" == "--todos" ]]; then
  mapfile -t LOCALES < <(locales_do_projeto | sort -u)
elif [[ $# -gt 0 ]]; then
  LOCALES=("$@")
else
  LOCALES=(pt-BR en es)
fi

command -v magick >/dev/null || { echo "erro: ImageMagick não encontrado" >&2; exit 1; }
adb get-state >/dev/null 2>&1 || { echo "erro: nenhum emulador/aparelho em adb" >&2; exit 1; }
adb shell pm list packages 2>/dev/null | grep -q "$PKG" \
  || { echo "erro: $PKG não instalado — rode ./gradlew assembleDebug && adb install -r ..." >&2; exit 1; }

# O tutorial de stylus do sistema rouba o foco e aparece por cima do app.
adb shell settings put secure stylus_handwriting_enabled 0 >/dev/null 2>&1 || true

# As telas, na ordem em que a barra inferior as apresenta.
# nome : fração x na barra (por mil) : rolar antes de capturar
TELAS=("1-inicio:126:nao" "2-historico:626:nao" "3-protecao:876:nao" "4-politicas:876:sim" "5-permitidos:376:nao")

capturar_conjunto() {
  local perfil=$1 w=$2 h=$3 alvo_w=$4 alvo_h=$5 loc=$6
  local dir="$SAIDA/$perfil/$loc"
  mkdir -p "$dir"

  local navy=$(( h * 95 / 100 )) meiox=$(( w / 2 ))

  adb shell cmd locale set-app-locales "$PKG" --locales "$loc" >/dev/null 2>&1
  adb shell am force-stop "$PKG" >/dev/null 2>&1; sleep 2
  adb shell am start -n "$PKG/.ui.MainActivity" >/dev/null 2>&1; sleep 9

  # O convite de avaliação abre a cada 5 aberturas e cobriria a tela.
  adb shell uiautomator dump /sdcard/ui.xml >/dev/null 2>&1 || true
  if adb shell cat /sdcard/ui.xml 2>/dev/null | grep -q 'resource-id="[^"]*rate[^"]*"\|Avaliar agora\|Rate now'; then
    adb shell input tap "$meiox" $(( h * 895 / 1000 )) >/dev/null 2>&1; sleep 2
  fi

  local primeira=sim tela tela_nome tela_fx tela_rolar
  for tela in "${TELAS[@]}"; do
    IFS=: read -r tela_nome tela_fx tela_rolar <<< "$tela"
    if [[ $primeira == nao ]]; then
      adb shell input tap $(( w * tela_fx / 1000 )) "$navy" >/dev/null 2>&1; sleep 3
    fi
    [[ $tela_rolar == sim ]] && { adb shell input swipe "$meiox" $(( h*75/100 )) "$meiox" $(( h*25/100 )) 400 >/dev/null 2>&1; sleep 2; }
    primeira=nao

    adb exec-out screencap -p > "$dir/$tela_nome.png"
    # Play: PNG de 24 bits, sem alfa, proporção no máximo 2:1. A moldura só
    # acrescenta margem lateral — nenhum pixel de conteúdo se perde.
    magick "$dir/$tela_nome.png" -background "$FUNDO" -gravity center \
      -extent "${alvo_w}x${alvo_h}" -alpha remove -alpha off \
      -define png:color-type=2 "$dir/$tela_nome.png"
  done
  echo "  ✓ $perfil/$loc"
}

echo "idiomas: ${LOCALES[*]}"
for perfil in "${PERFIS[@]}"; do
  IFS=: read -r p_nome p_w p_h p_dens p_alvo_w p_alvo_h <<< "$perfil"
  echo "$p_nome (${p_w}x${p_h} @ ${p_dens}dpi → ${p_alvo_w}x${p_alvo_h})"
  adb shell wm size "${p_w}x${p_h}" >/dev/null 2>&1
  adb shell wm density "$p_dens" >/dev/null 2>&1
  sleep 6
  for loc in "${LOCALES[@]}"; do
    capturar_conjunto "$p_nome" "$p_w" "$p_h" "$p_alvo_w" "$p_alvo_h" "$loc"
  done
done

adb shell wm size reset >/dev/null 2>&1
adb shell wm density reset >/dev/null 2>&1
adb shell cmd locale set-app-locales "$PKG" --locales "" >/dev/null 2>&1
echo "pronto — $SAIDA"
