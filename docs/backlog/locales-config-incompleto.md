# `locales_config.xml` não listava todos os idiomas traduzidos

> Resolvido em 2026-08-17. Este arquivo mantém o diagnóstico para fins de histórico.

O problema original era a divergência entre os diretórios `values-*` e o seletor de idioma do
Android. A correção criou `app/src/main/res/xml/locales_config.xml` com os 20 locales que têm
interface traduzida e adicionou `android:localeConfig` ao Manifest.

Agora `scripts/verify-locales.py` compara automaticamente os dois lados e também confere as 324
entradas traduzíveis e seus placeholders. Um idioma novo só deve entrar no seletor depois que o
`strings.xml` correspondente estiver completo.

Os 74 idiomas da ficha da Play são uma camada independente: o Play aceita metadados localizados
mesmo quando a interface do APK ainda usa o fallback. Eles continuam validados por
`scripts/sync-play-metadata.py --check`.
