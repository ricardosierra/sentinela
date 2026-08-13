# Processo de Release

> Convenção do ecossistema: primeira release `v0.1.0`; `v1.0.0` reservado para produção
> madura. Bump minor para features, patch para correções. Tag anotada `vX.Y.Z`.

## Checklist de release

1. **Gates de qualidade** (tudo verde antes de qualquer bump):
   ```bash
   ./gradlew testDebugUnitTest lint detekt assembleDebug assembleRelease
   ```
2. **Bump de versão no mesmo commit** — arquivos a atualizar:
   - `app/build.gradle.kts`: `versionCode` (+1) e `versionName` (`X.Y.Z`)
   - `CHANGELOG.md`: mover itens de `## [Unreleased]` para `## [vX.Y.Z (YYYY-MM-DD)](link)` e
     atualizar a base do link do Unreleased para `.../compare/vX.Y.Z...develop`
   - `README.md`: linha "Versão atual"
3. **CHANGELOG no formato Release Notes** (nunca Keep-a-Changelog):
   - Cabeçalho: `## [vX.Y.Z (YYYY-MM-DD)](https://github.com/ricardosierra/sentinela/compare/<prev>...vX.Y.Z)`
   - Primeira versão usa `.../releases/tag/v0.1.0`
   - Seções ✨ Novidades / 🎨 Melhorias / 🐛 Correções / 🔧 Técnico; itens `- [x]`
4. **Commit e tag**:
   ```bash
   git commit -m "release: X.Y.Z"
   git tag -a vX.Y.Z -m "vX.Y.Z"
   ```
5. **Validar o APK de release**:
   ```bash
   ./gradlew assembleRelease
   BT=$(ls -d "$ANDROID_HOME"/build-tools/* | sort -V | tail -1)
   "$BT/aapt2" dump permissions app/build/outputs/apk/release/app-release.apk
   ```
   O conjunto esperado hoje (v0.2.1) é exatamente este — todas justificadas em
   [`PERMISSOES.md`](PERMISSOES.md), nenhuma fora da lista fechada:

   | Permissão | Origem |
   |---|---|
   | `POST_NOTIFICATIONS` | opt-in da notificação própria |
   | `READ_CONTACTS` | lookup local, só em memória (Fase 4) |
   | `CALL_PHONE` | modo discador opcional (Fase 6) |
   | `USE_FULL_SCREEN_INTENT` | tela de chamada sobre a tela bloqueada (Fase 6) |

   `DYNAMIC_RECEIVER_NOT_EXPORTED_PERMISSION` aparece no dump: é gerada pelo AndroidX, não
   é declarada pelo projeto. **`INTERNET` tem de continuar ausente** — é o teste que não pode
   falhar nunca:
   ```bash
   "$BT/aapt2" dump permissions app/build/outputs/apk/release/app-release.apk | grep -c INTERNET  # 0
   ```

## Assinatura (release)

- `app/keystore.properties` (fora do git — ver `.gitignore`):
  ```properties
  storeFile=release.keystore
  storePassword=***
  keyAlias=sentinela
  keyPassword=***
  ```
- `storeFile` é resolvido por `file()` **relativo ao módulo `app/`** — o keystore em uso está em
  `app/release.keystore` (fora do git). Gerar uma vez:
  ```bash
  keytool -genkeypair -v -keystore app/release.keystore \
    -alias sentinela -keyalg RSA -keysize 4096 -validity 10000
  ```
- Sem `keystore.properties`, `assembleRelease` gera APK não assinado (config condicional).
- Chave atual: `SHA256withRSA`, RSA **2048**, válida até **19/12/2053**. A Play exige validade
  até no mínimo 22/10/2033 — atendido. Trocar a chave depois do primeiro envio à loja só é
  possível pelo fluxo de redefinição do Play App Signing; **não perca este keystore.**
- Conferir a validade sem expor senha:
  ```bash
  SP=$(grep '^storePassword' app/keystore.properties | cut -d= -f2-)
  keytool -list -v -keystore app/release.keystore -storepass "$SP" | grep -i "Válido\|Valid"
  ```

## R8 / ProGuard

- `minifyEnabled` + `shrinkResources` ativos no release.
- `proguard-rules.pro` remove `Log.v`/`Log.d` via `-assumenosideeffects` (logs sensíveis
  fora do release — exigência de privacidade).
- Guardar `mapping.txt` de cada release fora do git. Atenção ao nome: o `.gitignore` cobre
  `mapping*.txt` e `releases/`, **não** cobre `sentinela-*-mapping.txt`. Convenção:
  `releases/mapping-vX.Y.Z.txt`.

## Distribuição

**Sideload / GitHub Releases:** `.apk` assinado — `adb install sentinela-vX.Y.Z-release.apk`.

**Google Play: exige `.aab`, não `.apk`.** Desde agosto de 2021 apps novos só entram como
Android App Bundle. O APK continua válido para distribuição direta, mas é recusado no envio.

```bash
./gradlew bundleRelease   # app/build/outputs/bundle/release/app-release.aab
```

O `.aab` sai assinado com a chave de upload; o Google reassina com a chave de distribuição
(Play App Signing). O aviso PKIX do `jarsigner -verify` é esperado para keystore autoassinado
e não indica problema.

**Antes do primeiro envio**, a loja ainda cobra três coisas que não são build:

- Justificativa do papel de call screening no formulário de permissões sensíveis — o app usa
  `ROLE_CALL_SCREENING` como função principal declarada, o que é o caso aceito pela política.
- Política de privacidade publicada e a seção Data safety preenchida. O caso aqui é o mais
  simples possível: nenhum dado coletado, nenhum dado transmitido, nada de rede
  (ver [`PRIVACIDADE.md`](PRIVACIDADE.md)).
- `targetSdk` dentro da janela vigente da política (hoje o projeto está em 37).
