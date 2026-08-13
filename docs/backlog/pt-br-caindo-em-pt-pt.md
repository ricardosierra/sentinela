# Aparelho em pt-BR recebe a interface em português de Portugal

Achado ao capturar as telas da ficha da Play (2026-08-07). **Bloqueia lançamento** — atinge o
mercado principal do app.

## Reprodução

```bash
adb shell cmd locale set-app-locales org.sentinela.app --locales pt-BR
adb shell am force-stop org.sentinela.app
adb shell am start -n org.sentinela.app/.ui.MainActivity
adb shell uiautomator dump /sdcard/ui.xml && adb shell cat /sdcard/ui.xml \
  | grep -oE 'text="[^"]{5,40}"' | head -4
```

Pedido: `pt-BR`. Entregue:

```
text="Protecção activa"          # pt-BR seria "Proteção Ativa"
text="Monitorização em tempo real"  # pt-BR seria "Monitoramento em tempo real"
```

Outras telas mostram "Definições" (pt-BR: "Ajustes"), "voice mail" (pt-BR: "caixa postal") e
"de imediato" (pt-BR: "instantaneamente").

## Causa

As strings de pt-BR moram em `app/src/main/res/values/strings.xml` — o default, que **não tem
qualificador de idioma**. Enquanto não existia nenhum `values-pt-*`, isso funcionava: o aparelho
brasileiro não achava nada melhor e caía no default.

Quando `values-pt-rPT/` entrou, a regra de resolução do Android mudou de resultado. Para um
aparelho em `pt-BR` ele procura, nesta ordem:

1. `values-pt-rBR/` — **não existe**
2. um `values-pt-*` qualquer, por ser o mesmo idioma → **acha `values-pt-rPT`** ✋
3. `values/` (default) — nunca chega aqui

O default só é consultado quando nenhuma variante do idioma existe. Uma variante do mesmo idioma
ganha do default, mesmo sendo de outro país.

## Correção

Criar `app/src/main/res/values-pt-rBR/strings.xml` com o conteúdo brasileiro. O `values/`
continua existindo como último recurso, para aparelho em idioma que o app não tem.

```bash
mkdir -p app/src/main/res/values-pt-rBR
cp app/src/main/res/values/strings.xml app/src/main/res/values-pt-rBR/strings.xml
```

Atenção ao que **não** deve ir junto: `values/colors.xml` e os outros recursos não-textuais ficam
onde estão. Só `strings.xml` é traduzível.

Depois, acrescentar `pt-BR` ao `locales_config.xml` já resolve o seletor de idioma — hoje ele
declara `pt-BR` como `defaultLocale`, o que continua correto.

## Verificação depois de corrigir

```bash
./gradlew assembleDebug && adb install -r app/build/outputs/apk/debug/app-debug.apk
adb shell cmd locale set-app-locales org.sentinela.app --locales pt-BR
# esperado: "Proteção Ativa", "Monitoramento em tempo real", "Ajustes"
```

Vale um bloco em `scripts/verify-invariants.sh`: se existir qualquer `values-pt-*`, então
`values-pt-rBR/strings.xml` tem que existir. O mesmo vale para qualquer idioma cujo default esteja
em `values/` — o dia em que alguém acrescentar `values-en-rGB` sem `values-en-rUS`, o mesmo erro
acontece em inglês.

## Impacto nas capturas da ficha

As capturas em `docs/loja/graficos/screenshots/*/pt-BR/` saíram em português europeu por causa
disto. Precisam ser refeitas depois da correção:

```bash
./scripts/capture-store-screenshots.sh pt-BR
```

## Por que não foi corrigido junto

O diretório `app/src/main/res/` estava sendo escrito por outro processo durante o trabalho (as
traduções dos demais idiomas iam chegando). Mexer em `strings.xml` no meio disso arrisca conflito
e perda de tradução.
