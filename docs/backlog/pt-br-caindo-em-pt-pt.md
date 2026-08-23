# Aparelho em pt-BR recebia a interface em português de Portugal

> Resolvido em 2026-08-17. Este arquivo mantém o diagnóstico para fins de histórico.

O Android escolhia `values-pt-rPT/` porque as strings brasileiras estavam somente no diretório
default `values/`. A correção adicionou `app/src/main/res/values-pt-rBR/strings.xml` com as
strings brasileiras e registrou `pt-BR` no seletor explícito do app.

Também foi criado `scripts/verify-locales.py`, que falha se o recurso explícito de `pt-BR` sumir ou
se a configuração de idiomas divergir dos diretórios de tradução.

As capturas da loja em `docs/loja/graficos/screenshots/*/pt-BR/` devem ser geradas novamente a
partir de um APK recompilado sempre que as strings mudarem. O roteiro está em
`scripts/capture-store-screenshots.sh`.
