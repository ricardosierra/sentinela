# Itens fora de escopo encontrados no plano 06-08

- **`CHANGELOG.md` não tem blocos técnicos das Phases 2, 3, 4 e 5.** O arquivo salta da Phase 1
  direto para a Phase 6, que este plano acrescentou. Não é defeito deste plano e escrever quatro
  fases de changelog de memória seria pior que a lacuna: cada bloco precisa sair do respectivo
  `SUMMARY.md`. Candidato natural ao fechamento da versão (`docs/RELEASE.md`), quando o
  `[Unreleased]` virar `[v0.1.0]`.
- **Cinco tarefas do Gradle aparecem `UP-TO-DATE` mesmo depois de `clean`** (`preBuild`,
  `preDebugBuild`, `generateDebugAssets`, `preDebugUnitTestBuild`, `preReleaseBuild`). São tarefas
  de ciclo de vida sem saída própria e nenhuma é actionable; registrado em `06-EVIDENCE.md` em vez
  de silenciado, porque a evidência das fases anteriores afirma "zero up-to-date" sem essa ressalva.
