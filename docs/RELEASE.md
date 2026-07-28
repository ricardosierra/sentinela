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
   # permissões declaradas devem ser APENAS POST_NOTIFICATIONS:
   $ANDROID_HOME/build-tools/36.0.0/aapt dump permissions app/build/outputs/apk/release/app-release.apk
   ```

## Assinatura (release)

- `app/keystore.properties` (fora do git — ver `.gitignore`):
  ```properties
  storeFile=keystore/sentinela-release.keystore
  storePassword=***
  keyAlias=sentinela-key
  keyPassword=***
  ```
- Keystore em `app/keystore/` (fora do git). Gerar uma vez:
  ```bash
  keytool -genkeypair -v -keystore app/keystore/sentinela-release.keystore \
    -alias sentinela-key -keyalg RSA -keysize 4096 -validity 10000
  ```
- Sem `keystore.properties`, `assembleRelease` gera APK não assinado (config condicional).

## R8 / ProGuard

- `minifyEnabled` + `shrinkResources` ativos no release.
- `proguard-rules.pro` remove `Log.v`/`Log.d` via `-assumenosideeffects` (logs sensíveis
  fora do release — exigência de privacidade).
- Guardar `mapping.txt` de cada release fora do git (já ignorado).

## Distribuição do MVP

Sideload: `adb install sentinela-release.apk`. Publicação em loja fica fora do escopo do
v0.1.0 (Play exige justificativa de papel de call screening — tratar quando chegar a hora).
