# Phase 9: Apoio Privacidade Release e Validacao Fisica — Summary

**Completed:** 2026-08-04 (implementação) / 2026-08-06 (reconciliação e correções)
**Status:** Complete — com um item de validação física em aberto para o mantenedor

## O que foi entregue

| Item | Onde | Commit |
|---|---|---|
| Convite de avaliação na 5ª abertura | `settings/AppOpenCounter`, `HomeViewModel`, `RatingBottomSheet` | `5b1b577` |
| Tela Privacidade e Sobre + seção de apoio | `ui/about/` | `40410d6` |
| Limpar tudo com duas confirmações | `AboutScreen` + `AboutViewModel` | `40410d6` |
| R8 / ProGuard e build de release assinado | `app/build.gradle.kts`, `proguard-rules.pro` | — |
| Cenários 69-72 do modo discador no roteiro | `docs/TESTE-FISICO-SAMSUNG.md` | `2515456` |
| CHANGELOG no formato Release Notes | `CHANGELOG.md` | `2515456`, `7ddcad6` |

## Divergências encontradas na reconciliação

1. **Endereço de doação placeholder publicado na v0.1.0.** O `strings.xml` trazia um endereço
   Bitcoin com o comentário `NUNCA publicar com endereço inventado` logo acima dele. Removido
   em `a5c1363`, com o motivo registrado no lugar da string. Detalhes e o caminho para
   reativar em `09-VERIFICATION.md`.
2. **Strings da tela Sobre em Kotlin.** Os dois diálogos de limpar-tudo tinham título e corpo
   embutidos no código. Movidos para `strings.xml` em `a5c1363`.

## Estado dos portões em 2026-08-06

`testDebugUnitTest`, `lint`, `detekt`, `koverVerify` e `assembleRelease` verdes.
`scripts/verify-invariants.sh` verde nos 10 blocos.

## Em aberto

**Validação física em Samsung.** O roteiro tem 51 cenários e nenhum foi executado em aparelho
— o próprio documento os declara como veredito pendente. Isso satisfaz a alternativa
"pendências documentadas" do critério 5, mas o comportamento central do produto (a chamada
bloqueada realmente não toca) segue sem confirmação em hardware real. É a única coisa que
falta para o MVP estar validado de ponta a ponta, e só o mantenedor pode fazê-la.
