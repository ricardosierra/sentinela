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

---

## Chave dedicada para a intenção de ativar o modo discador (descoberto durante 07-10)

- **Onde:** `MarcasDePermissao.discador`, em
  `app/src/main/java/org/sentinela/app/ui/navigation/SentinelaNavHost.kt`
- **Achado:** a função de precedência do modo discador (Fase 6) precisa de um quarto sinal — a
  intenção gravada do usuário — para distinguir **papel perdido** de modo apenas **oferecido**. Não
  existe chave persistida para isso, e 07-09 confirmou que nenhuma era necessária para a tela Proteção.
- **O que ficou:** o sinal é derivado da marca do pedido da permissão de originar chamada, cujo único
  caminho de disparo é o toque em ligar da tela de discagem própria — tela que o sistema só encaminha a
  este aplicativo quando ele detém o papel de telefone padrão. A derivação é honesta pelo caminho de
  código, e está registrada em prosa no ponto de uso.
- **Por que NÃO foi resolvido aqui:** chave nova em `DataStoreSettingsRepository` está fora dos arquivos
  deste plano, e o estado do modo é DERIVADO por decisão da Fase 6 — uma marca dizendo "modo ligado"
  vira mentira quando o usuário troca o telefone padrão no sistema. Uma chave nova teria de ser
  explicitamente de intenção histórica ("o usuário já ativou alguma vez"), com teste próprio.
- **Efeito hoje:** nenhum defeito observável. O ramo de papel perdido é alcançado por quem já usou o
  modo discador, que é o público a quem o aviso se destina.

## Três destinos sem tela: permitidos, histórico e privacidade e sobre (descoberto durante 07-10)

- **Onde:** `DestinoEmPreparacao`, em
  `app/src/main/java/org/sentinela/app/ui/navigation/SentinelaNavHost.kt`
- **Achado:** o grafo tem dez destinos, travados por contagem desde 07-02, e nenhum deles é a lista de
  permitidos, o histórico ou a tela de privacidade e sobre. Os dois primeiros já nascem anunciados como
  indisponíveis na barra da home; o terceiro é uma linha ATIVA da tela Proteção, e um toque sem efeito
  ali seria defeito silencioso.
- **O que ficou:** um aviso que diz, com a frase que já existe em recurso, que a tela chega em uma etapa
  seguinte. Nenhuma tela em branco e nenhum controle inerte.
- **Dono:** Phase 8 (permitidos e histórico) e Phase 9 (privacidade e sobre). Cada uma acrescenta o
  destino e, de propósito, tem de mexer na contagem travada do contrato do grafo.
