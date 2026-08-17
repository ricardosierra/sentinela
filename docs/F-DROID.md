# F-Droid: Submissão e Publicação

A estrutura de metadados multilíngues para o F-Droid já foi configurada no repositório do Sentinela. O script `scripts/sync-play-metadata.py` agora exporta a estrutura `fastlane/metadata/android` exigida pelo F-Droid.

## Como publicar (Passo a Passo)

1. Faça login na sua conta no [GitLab](https://gitlab.com).
2. Acesse o repositório oficial de dados do F-Droid: [https://gitlab.com/fdroid/fdroiddata](https://gitlab.com/fdroid/fdroiddata).
3. Faça um **Fork** do repositório para a sua conta.
4. No seu fork, acesse a pasta `metadata/` e crie um novo arquivo chamado `org.sentinela.app.yml`.
5. Cole o conteúdo da receita abaixo no arquivo e salve (faça o commit na master do seu fork ou em uma nova branch).
6. Abra um **Merge Request** do seu fork apontando para o repositório original `fdroid/fdroiddata`.

O bot do F-Droid irá iniciar uma pipeline para validar e construir o app automaticamente.

## Receita do F-Droid (`org.sentinela.app.yml`)

```yaml
Categories:
  - Security
  - Phone
License: MIT
AuthorName: Ricardo Sierra
SourceCode: https://github.com/ricardosierra/sentinela
IssueTracker: https://github.com/ricardosierra/sentinela/issues
Changelog: https://github.com/ricardosierra/sentinela/blob/master/CHANGELOG.md

# F-Droid lerá o nome e as descrições em dezenas de idiomas direto de "fastlane/metadata/android" do repo
AutoName: Sentinela
Summary: Bloqueador local de chamadas desconhecidas para Android
Description: |-
  App Android nativo e open source que impede chamadas de números desconhecidos de interromperem o usuário — sem propaganda, sem telemetria, sem nuvem, 100% offline.

RepoType: git
Repo: https://github.com/ricardosierra/sentinela.git

Builds:
  - versionName: 0.2.1
    versionCode: 3
    commit: v0.2.1
    subdir: app
    gradle:
      - yes

AutoUpdateMode: Version v%v
UpdateCheckMode: Tags
UpdateCheckData: v([0-9.]+)
```

> **Nota**: Não é necessário adicionar os textos traduzidos manualmente nesta receita, pois os servidores do F-Droid extrairão automaticamente a pasta `fastlane/metadata/android` que nós geramos e commitamos no repositório.
