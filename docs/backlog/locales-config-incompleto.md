# `locales_config.xml` não lista 4 idiomas que já estão traduzidos

Achado ao preparar as capturas de tela da ficha da Play (2026-08-07).

## O que está errado

`app/src/main/res/xml/locales_config.xml` declara 13 idiomas. O `res/` tem 17 conjuntos de
strings. Quatro estão traduzidos e **não** aparecem no arquivo:

| Idioma | `values-XX` | Declarado em `locales_config.xml` |
|---|---|---|
| Polonês | `values-pl` (315 strings) | ❌ |
| Tailandês | `values-th` (315 strings) | ❌ |
| Turco | `values-tr` (315 strings) | ❌ |
| Vietnamita | `values-vi` (315 strings) | ❌ |

Conferência:

```bash
comm -13 \
  <(grep -oE 'android:name="[^"]+"' app/src/main/res/xml/locales_config.xml \
     | sed 's/.*"\(.*\)"/\1/' | sort) \
  <(ls -d app/src/main/res/values-* | sed 's|.*/values-||;s/^zh-rCN$/zh-CN/' | sort)
```

(`pt-BR` aparece como declarado sem `values-pt-BR` — isso está certo: é o `defaultLocale`, e as
strings dele moram em `values/`.)

## Por que importa

O arquivo existe para habilitar o seletor de idioma por app do Android 13+
(Configurações → Apps → Sentinela → Idioma). Quem está nesses quatro idiomas continua recebendo a
tradução **se o aparelho inteiro estiver naquele idioma**, mas não consegue escolher o Sentinela
naquele idioma sem trocar o idioma do sistema — e a tradução, que já existe e já foi paga em
esforço, fica invisível para quem procura por ela.

O próprio comentário no topo do arquivo define a regra que está sendo quebrada:

> Regra: um locale só entra aqui depois que o `values-XX/strings.xml` correspondente existe e está
> completo.

A regra foi escrita para impedir o erro oposto (declarar sem traduzir). Este caso é o contrário:
traduzido e não declarado. Vale checar se a regra tem alguma verificação automática — se não tiver,
o desvio volta a acontecer na próxima leva de tradução.

## Correção sugerida

Acrescentar as quatro linhas em `locales_config.xml` e, de preferência, um bloco em
`scripts/verify-invariants.sh` que compare os dois lados e falhe na divergência — nos dois
sentidos.

## Fora de escopo agora

Não foi corrigido porque o pedido em curso era gerar os gráficos da ficha da Play. Mexer em
`locales_config.xml` muda o comportamento do app e pede teste próprio.
