# F-Droid: Submissão e Publicação

Guia oficial para publicação do Sentinela no catálogo do [F-Droid](https://f-droid.org).

## Regras e Invariantes de Submissão

1. **Template obrigatório:** Toda submissão de novo aplicativo no GitLab (`fdroid/fdroiddata`) **precisa** utilizar o template de Merge Request **`App inclusion`** e preencher todos os checkboxes obrigatórios. **MRs sem template ou com descrição vazia são fechados sumariamente pelos mantenedores** (conforme ocorrido no MR !46052: *"Closing because Merge Request template is not followed"*).
2. **Formato do título:** O título do Merge Request deve seguir estritamente o padrão `New app: Sentinela` (ou `New app: org.sentinela.app`).
3. **Um aplicativo por MR:** Nunca misture múltiplos aplicativos no mesmo Merge Request ou branch. Cada aplicativo precisa de seu próprio branch e MR isolado.
4. **Repositório 100% público:** O código-fonte declarado em `Repo:` precisa ser acessível publicamente de forma anônima. Se o repositório for privado ou inacessível, o runner do CI do F-Droid falha com `Authentication failed for 'https://github.com/...'` ao tentar clonar.
5. **Metadados via Fastlane:** O Sentinela versiona a árvore `fastlane/metadata/android/` diretamente no repositório Git. O F-Droid extrai título, descrições e gráficos em todos os 74 idiomas automaticamente — **não** adicione descrições redundantes dentro do `fdroiddata`.

---

## Receita do F-Droid (`metadata/org.sentinela.app.yml`)

Arquivo a ser incluído em `metadata/org.sentinela.app.yml` no repositório `fdroiddata`:

```yaml
Categories:
  - Phone & SMS
  - Security
License: MIT
AuthorName: Ricardo Sierra
SourceCode: https://github.com/ricardosierra/sentinela
IssueTracker: https://github.com/ricardosierra/sentinela/issues
Changelog: https://github.com/ricardosierra/sentinela/blob/master/CHANGELOG.md

AutoName: Sentinela

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
UpdateCheckMode: Tags ^v([0-9.]+)$
```

---

## Template para a Descrição do Merge Request

Ao abrir o Merge Request no GitLab em `https://gitlab.com/fdroid/fdroiddata/-/merge_requests/new`, selecione o template **`App inclusion`** ou cole o seguinte conteúdo na descrição:

```markdown
## Required

* [x] The app complies with the [inclusion criteria](https://f-droid.org/docs/Inclusion_Policy)
* [x] The original app author has been notified (and does not oppose the inclusion)
* [x] All related [fdroiddata](https://gitlab.com/fdroid/fdroiddata/issues) and [RFP issues](https://gitlab.com/fdroid/rfp/issues) have been referenced in this merge request
* [x] Builds with `fdroid build` and all pipelines pass
* [x] There is an issue tracker and contact info of the author so that we can report bugs and contact the author.

## Strongly Recommended

* [x] The upstream app source code repo contains the app metadata _(summary/description/images/changelog/etc)_ in a [Fastlane](https://gitlab.com/snippets/1895688) or [Triple-T](https://gitlab.com/snippets/1901490) folder structure
* [x] Releases are tagged and auto update is enabled

## Suggested

* [ ] External repos are added as git submodules instead of srclibs
* [ ] Enable [Reproducible Builds](https://f-droid.org/docs/Reproducible_Builds)
* [ ] Multiple apks for native code

/label ~"New App"
```

---

## Passo a Passo para Submissão

1. No seu fork `ricardosierra/fdroiddata`:
   - Crie uma branch isolada (ex: `add-sentinela`).
   - Adicione apenas o arquivo `metadata/org.sentinela.app.yml`.
   - Faça o commit: `git commit -m "Add org.sentinela.app"`.
   - Envie para o seu fork: `git push origin add-sentinela`.
2. Abra o Merge Request:
   - **Origem (Source):** `ricardosierra/fdroiddata` branch `add-sentinela`
   - **Destino (Target):** `fdroid/fdroiddata` branch `master`
   - **Título:** `New app: Sentinela`
   - **Descrição:** Utilize o template `App inclusion` preenchido acima.
3. Aguarde o pipeline do CI rodar. Com o repositório público e sem conflito de outros apps, o `fdroid build` validará o build com sucesso.
