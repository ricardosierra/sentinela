# Gráficos da ficha da Google Play

Toda a arte da ficha é **gerada por script**. Não edite PNG na mão: mexa na fonte e rode de novo.

```bash
python3 scripts/build-store-assets.py          # ícone + banner
./scripts/capture-store-screenshots.sh         # capturas (telefone + tablet 7" e 10")
```

O ícone sai de `app/src/main/res/drawable/ic_launcher_foreground.xml` e da cor
`ic_launcher_background` em `values/colors.xml`. É de propósito: o ícone da loja e o ícone que
aparece no aparelho têm que ser a mesma arte. Com PNG solto, alguém troca o vetor e a ficha passa
a mostrar um ícone que o celular não mostra, sem ninguém perceber.

## O que tem aqui

| Arquivo | Tamanho | Formato | Onde entra no Play Console |
|---|---|---|---|
| `icone-512.png` | 512×512 | PNG 32 bits com alfa | Ficha principal → Ícone do app |
| `feature-graphic-<loc>.png` | 1024×500 | PNG 24 bits | Ficha principal → Gráfico de destaque |
| `screenshots/telefone/<loc>/*.png` | 1200×2400 | PNG 24 bits | Capturas → Telefone |
| `screenshots/tablet7/<loc>/*.png` | 1200×1920 | PNG 24 bits | Capturas → Tablet de 7 pol. |
| `screenshots/tablet10/<loc>/*.png` | 1600×2560 | PNG 24 bits | Capturas → Tablet de 10 pol. |

Cinco telas por conjunto: início, histórico, proteção, políticas de bloqueio e lista de
permitidos. A ficha de um idioma sem conjunto próprio usa o conjunto do idioma padrão, então
nenhuma ficha fica sem imagem.

## Regras da loja que estes arquivos atendem

- **Ícone**: PNG de 32 bits **com** canal alfa. O Chrome entrega 24 bits quando o fundo é opaco,
  então o script força o alfa e falha se ele não estiver lá.
- **Capturas**: PNG de 24 bits, **sem** alfa, lado entre 320 e 3840 px, proporção no máximo 2:1.
  O emulador de telefone entrega 1080×2400, que é 2,22:1 e a loja recusa; por isso as capturas de
  telefone são emolduradas até 1200×2400 — 2:1 exato — com a cor de superfície da própria UI.
  É moldura, não recorte: nenhum pixel de conteúdo se perde. Os dois tablets já nascem em 1,6:1.
- **Banner**: sem alfa, conteúdo longe das bordas porque a loja recorta o banner em vários
  formatos.

## Armadilha que já custou uma leva de capturas

**O APK precisa ser mais novo que as traduções.** As capturas saem no idioma que estiver
*compilado no APK instalado* — não no que está em `res/`. Um `values-de/strings.xml` criado
depois do último `assembleDebug` não entra no APK, e o app cai no idioma do sistema. O print sai
em inglês com nome de arquivo `de`, e nada avisa. Antes de capturar:

```bash
./gradlew assembleDebug && adb install -r app/build/outputs/apk/debug/app-debug.apk
```

Para conferir quais idiomas realmente entraram:

```bash
aapt2 dump resources app/build/outputs/apk/debug/app-debug.apk \
  | grep -A 20 "string/welcome_headline"
```

Outros dois tropeços: o tutorial de stylus do sistema rouba o foco dos campos de texto (o script
já desliga), e o convite de avaliação abre a cada 5 aberturas do app e cobriria a tela (o script
detecta e dispensa).

## Decisões de conteúdo

- **Histórico real, não inventado.** As chamadas do histórico foram simuladas no emulador
  (`adb emu gsm call`) e **bloqueadas de verdade** pelo app. Nada foi escrito direto no banco.
- **Lista de permitidos vazia.** A validação só aceita número brasileiro válido, e todo número
  válido pode ser de uma pessoa real — publicar um deles numa página de loja convida ligação para
  um estranho. Se quiser a lista preenchida, use números que você controla.
- **Tablet mostra o layout esticado**, que é o que um tablet realmente mostra: o app não tem
  layout de duas colunas. A captura não promete o que o app não faz.

## Estado

O problema de `pt-BR` cair em `pt-PT` foi corrigido com `values-pt-rBR` e a configuração explícita
de locales. Depois de qualquer mudança de UI ou tradução, reinstale o APK recompilado e refaça as
capturas afetadas com `scripts/capture-store-screenshots.sh`.

As capturas versionadas de `pt-BR` ainda não devem ser publicadas como novas: a tentativa de
regenerá-las neste ambiente foi interrompida por diálogos de “System UI/Messages isn't responding”
do emulador. Repita o roteiro em um emulador estável antes do envio ao Play Console.

**Arte final do ícone.** O vetor de origem se declara provisório:
*"Escudo Silent Guardian — placeholder do MVP; arte final na fase de UI."* Trocar o vetor e rodar
`build-store-assets.py` de novo atualiza ícone e banner juntos.
