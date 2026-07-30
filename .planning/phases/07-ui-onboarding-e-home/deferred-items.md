## detekt: UnusedParameter em OptionCard.kt (descoberto durante 07-04)

- **Arquivo:** `app/src/main/java/org/sentinela/app/ui/components/OptionCard.kt:109`
- **Achado:** `Function parameter 'unavailableReason' is unused. [UnusedParameter]`
- **Dono:** plano 07-03 (componentes compartilhados), executado em paralelo nesta mesma onda.
  O arquivo já estava commitado e limpo na árvore quando o 07-04 rodou o detekt.
- **Por que NÃO foi corrigido aqui:** está fora do escopo do 07-04 e não foi causado por nenhuma
  mudança deste plano. O parâmetro provavelmente pertence ao estado desabilitado da linha do modo
  discador (§9.3 do contrato de interface), e quem sabe se ele deve ser consumido ou removido é o
  plano que o escreveu — corrigir por fora arriscaria apagar contrato ainda não ligado à tela.
- **Efeito:** `./gradlew detekt` fica VERMELHO na árvore inteira até o 07-03 fechar. Nenhum arquivo
  do 07-04 aparece no relatório.
