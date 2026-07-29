# Backlog — manutenção de toolchain

> Itens de manutenção identificados durante a Phase 1 (fundação compilável) e adiados de
> propósito. Nenhum deles bloqueia o MVP; todos têm gatilho de reavaliação registrado.

## 1. `gradlew.bat` ausente

**O que se sabe:** apenas o script `gradlew` (Unix) está versionado. O wrapper batch do Windows
(`gradlew.bat`) não existe no repositório, então um clone em Windows não builda sem gerar o
wrapper manualmente.

**Por que foi adiado:** gerar o `.bat` exige rodar a task `wrapper`, que pode alterar a versão do
Gradle validada nesta fase (9.6.1) e o hash de distribuição. A Phase 1 trava um baseline
verificado; mexer no wrapper agora invalidaria a evidência do build.

**Por que importa:** o produto será divulgado como open source (UIX-13) e contribuidores em
Windows precisam de um clone que builda de primeira.

**Gatilho:** Phase 9 (release), junto com a revisão final da toolchain.

## 2. AGP 9.3.1

**O que se sabe:** o check `AndroidGradlePluginVersion` do Android Lint reporta a existência da
versão 9.3.1, mas a página oficial de release notes do Android Gradle Plugin lista apenas a
**9.3.0** como stable (20/07/2026). Não há notas publicadas para a 9.3.1.

**Por que foi adiado:** não bumpar versão de plugin de build sem release notes — não há como
avaliar breaking changes. Este é exatamente o motivo do `disable "AndroidGradlePluginVersion"`
registrado no bloco `lint { }` do `app/build.gradle.kts`.

**Gatilho:** Phase 9 (release) — reavaliar se as release notes saíram e se a 9.3.1 (ou superior)
é segura.

## 3. Reavaliar `disable "UnusedResources"`

**O que se sabe:** as 132 ocorrências de `UnusedResources` são strings pt-BR pré-escritas para as
telas das Fases 5–9. São ativos legítimos, não lixo — apagá-las quebraria as fases seguintes.

**Por que foi adiado:** enquanto as telas não existem, a regra não consegue distinguir recurso
órfão de recurso ainda-não-consumido, e produz 100% de falso positivo.

**Por que importa:** quando as telas reais consumirem as strings, a regra volta a ser sinal
legítimo de recurso órfão. Manter o `disable` além disso esconderia lixo de verdade.

**Gatilho:** Phase 9 — pendência explícita: reativar `UnusedResources` e tratar o que sobrar.

## 4. Aviso de deprecation do Gradle 10

**O que se sabe:** origem confirmada — `ReportingExtension.file(String)`, usada internamente pelo
plugin **detekt 1.23.8**, não por nenhum script deste repositório.

**Por que foi adiado:** zero impacto no Gradle 9.6.1 e não há ação possível do nosso lado — o
aviso some quando o detekt 2.x sair do alpha e for adotado.

**Gatilho:** quando o detekt 2.x estabilizar, ou quando a migração para Gradle 10 for avaliada.
