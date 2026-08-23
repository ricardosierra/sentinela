# Automação da Google Play

Depois da preparação única da conta, toda release é publicada pelo workflow
[`google-play.yml`](../.github/workflows/google-play.yml): ele valida qualidade, assina o APK/AAB,
confere que `INTERNET` continua ausente, gera a ficha da loja a partir de `docs/loja/` e envia bundle,
textos e gráficos pela Google Play Developer API.

## Limite real de automação

A API publica versões e metadados em uma edição transacional, mas não substitui a preparação da
conta nem a análise de políticas. Faça uma única vez, manualmente, antes da primeira publicação:

1. Crie e verifique a conta de desenvolvedor e o aplicativo `org.sentinela.app` no Play Console.
2. Ative Play App Signing e envie o primeiro AAB pelo Console. A própria ferramenta de publicação
   informa essa limitação para o primeiro envio.
3. Preencha política de privacidade, Data safety, classificação de conteúdo, público-alvo e a
   declaração do papel de triagem de chamadas. Confira [`PRIVACIDADE.md`](PRIVACIDADE.md) e
   [`PERMISSOES.md`](PERMISSOES.md) antes de declarar qualquer item.
4. Crie um projeto Google Cloud, ative **Google Play Developer API**, crie uma service account e a
   convide em **Usuários e permissões** do Play Console. Dê apenas acesso ao app e as permissões
   necessárias: *Manage testing tracks*, *Manage store presence* e, somente para promover para
   produção, *Release to production*.

O fluxo de API usa uma *edit*: bundle, track, textos e imagens só entram em vigor quando a edição é
confirmada, em conjunto. Consulte o [guia oficial de início](https://developers.google.com/android-publisher/getting_started), a
[referência de edits](https://developers.google.com/android-publisher/edits) e a
[documentação do Gradle Play Publisher](https://github.com/Triple-T/gradle-play-publisher).

Contas pessoais novas também podem precisar concluir teste fechado antes de ganhar acesso à produção;
isso é uma regra de conta, não algo que uma automação consiga pular. Veja a
[orientação oficial de testes](https://support.google.com/googleplay/android-developer/answer/9845334).

## Segredos e proteções do GitHub

Crie os Environments `play-internal`, `play-alpha`, `play-beta` e `play-production`. Configure
revisores obrigatórios em `play-production` e armazene estes segredos nos Environments que podem
publicar:

| Segredo | Conteúdo |
|---|---|
| `ANDROID_KEYSTORE_BASE64` | `base64 -i app/release.keystore | tr -d '\\n'` |
| `ANDROID_KEYSTORE_PASSWORD` | senha do keystore de upload |
| `ANDROID_KEY_ALIAS` | alias da chave de upload (`sentinela`) |
| `ANDROID_KEY_PASSWORD` | senha da chave de upload |
| `GOOGLE_PLAY_SERVICE_ACCOUNT_JSON` | JSON inteiro da service account com acesso mínimo no Play Console |

Nunca versione a chave, `keystore.properties` ou o JSON. O workflow cria os dois primeiros apenas no
runner efêmero; o Gradle Play Publisher lê o JSON em
`ANDROID_PUBLISHER_CREDENTIALS`, sem arquivo de credencial no repositório.

Para publicar localmente, salve o JSON fora deste projeto e injete-o somente no processo da tarefa:

```bash
PLAY_CREDENTIALS_FILE="/caminho/fora-do-projeto/google-play-service-account.json"
ANDROID_PUBLISHER_CREDENTIALS="$(< "$PLAY_CREDENTIALS_FILE")" \
  ./gradlew publishReleaseBundle publishReleaseListing
```

No CI, use o segredo `GOOGLE_PLAY_SERVICE_ACCOUNT_JSON` do Environment correspondente. Nunca cole o
JSON em Kotlin, Gradle, Markdown, `.env`, commit ou mensagem.

## Operação

Uma tag `vX.Y.Z` dispara automaticamente a publicação como **DRAFT** no track `internal`.
O workflow calcula `versionCode = major × 1.000.000 + minor × 1.000 + patch`; assim cada tag SemVer
nova produz código crescente (a base atual é 3). Antes de criar a tag, atualize `versionName`,
`versionCode`, `CHANGELOG.md` e os READMEs conforme [`RELEASE.md`](RELEASE.md).

Para promover uma versão já enviada, execute manualmente **Publicar na Google Play** sobre a tag da
versão, escolha `promote`, o track de origem e o destino. Esse caminho promove exatamente o
`versionCode` calculado da tag pela API — não reenvia nem reconstrói o AAB. Para enviar um AAB inédito
manualmente, escolha `publish`. `IN_PROGRESS` exige fração entre `0` e `1`; `COMPLETED` entrega 100%;
`HALTED` interrompe uma release já ativa. A aprovação do Environment é a barreira intencional antes de
produção.

Localmente, valide a fonte da ficha sem credenciais:

```bash
python3 scripts/verify-locales.py
python3 scripts/sync-play-metadata.py --check
```

O primeiro comando verifica os idiomas realmente traduzidos no APK, as chaves, placeholders e o
seletor de idioma do Android. O segundo valida a ficha. O mesmo script de metadados cria
`app/src/main/play/` (ignorado pelo Git) quando chamado sem `--check`; ele converte os 74 idiomas,
notas de versão e os gráficos existentes para o layout do plugin. Para
inspecionar tarefas disponíveis, rode `./gradlew tasks --group publishing`.

Os 74 idiomas são a cobertura dos metadados da loja. A interface do APK, por segurança, só lista
no seletor os 20 locales com tradução completa; cadastrar um locale sem `strings.xml` faria o
Android exibir o fallback em português e seria uma localização falsa.
