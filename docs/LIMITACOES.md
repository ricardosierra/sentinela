# Limitações Conhecidas

> Honestidade é feature: nada aqui é bug — são fronteiras da plataforma ou decisões de escopo.
> A UI nunca promete além do que está garantido aqui.

## Da plataforma (Android)

1. **Só chamadas telefônicas tradicionais.** WhatsApp/Telegram/VoIP não passam pelo
   `CallScreeningService`. A UI diz isso explicitamente no onboarding.
2. **No modo filtro, as políticas por contato valem — desde que o app possa ler a agenda.**
   O Telecom só deixa de acionar o serviço de triagem quando o número **está na agenda e o
   app não tem permissão de leitura de contatos**. Como o Sentinela tem essa permissão desde
   a Fase 4, chamadas de contatos **chegam** ao serviço e a decisão passa a ser nossa
   (Tocar/Bloquear/Silenciar/Nunca Silenciar já valem no modo filtro).
   **Atenção:** se o usuário revogar a leitura da agenda, o Android volta a nem acionar o
   serviço para contatos conhecidos — eles passam a tocar pelo caminho nativo **sem nenhum
   aviso do sistema e sem o app conseguir interceptar**. O comportamento muda em silêncio.
   Origem: código do Telecom do Android (o filtro do serviço de triagem verifica a permissão
   de leitura de contatos antes de pular o vínculo).
   Roteiro físico: cenários 42 e 43 de [`TESTE-FISICO-SAMSUNG.md`](TESTE-FISICO-SAMSUNG.md).
3. **Ocultar a chamada bloqueada do histórico nativo não funciona para apps de terceiros.**
   Isso **não** é variação de fabricante: é decisão do próprio Android. O Telecom só honra o
   pedido de pular o registro quando o app de triagem é do tipo **operadora**; o Sentinela é
   do tipo escolhido pelo usuário, então a expressão avaliada pelo sistema é sempre
   verdadeira e a chamada bloqueada **sempre** entra no histórico nativo, marcada como
   bloqueada. Virar o discador padrão na Fase 6 **não** destrava isso — a isenção é exclusiva
   de apps de operadora.
   **Reconfirmado em execução na Fase 6**, com o papel de telefone padrão ativo no aparelho
   virtual: bloquear com as configurações de fábrica produz exatamente a variante de decisão que
   *pede* para não registrar no histórico do telefone, e o registro entra no histórico do mesmo
   jeito. Ser o app de telefone padrão não muda o cálculo do sistema — a isenção continua
   amarrada ao tipo operadora, não ao papel de discador.
   O que o bloqueio **de fato** entrega: a chamada não toca, não vibra, não mostra tela de
   chamada e não gera aviso de chamada perdida. O registro no histórico permanece.
   Origem: código do Telecom do Android (cálculo de `shouldAddToCallLog` no filtro do serviço
   de triagem) mais o javadoc da própria API.
   Roteiro físico: cenários 40 e 41 de [`TESTE-FISICO-SAMSUNG.md`](TESTE-FISICO-SAMSUNG.md).
4. **Janela de 5 s.** Se o aparelho estiver em condição extrema (I/O travado) e o app não
   responder a tempo, a plataforma deixa a chamada seguir — comportamento fail-open do
   Android, alinhado ao fallback padrão do app (permitir).
5. **Papéis são únicos.** Só um app detém `ROLE_CALL_SCREENING` (e um o `ROLE_DIALER`);
   instalar outro bloqueador/discador rouba o papel silenciosamente. A home detecta e
   oferece correção.
6. **Caixa postal depende da operadora.** "Encaminhar silenciosamente" resulta em caixa
   postal apenas se a linha tiver o serviço; sem ele, para quem liga soa como chamada não
   atendida.
7. **O app não consegue contornar o modo "Não Perturbe".** O Não Perturbe é avaliado por um
   filtro **separado e paralelo** ao da triagem, que consulta o gerenciador de notificações;
   nenhum campo da resposta de triagem chega até ele. A opção **"Nunca Silenciar"** significa
   que o **Sentinela** nunca silencia aquela origem — e **não** que a chamada vai tocar
   durante o Não Perturbe. Com o Não Perturbe ligado, a chamada continua suprimida pelo
   sistema. O único contorno possível exigiria a permissão de alterar a política de
   notificações, que está **fora da lista permitida** e mudaria uma configuração global do
   usuário — está descartada em definitivo.
   Origem: código do filtro de Não Perturbe do Telecom do Android.
   Roteiro físico: cenário 45 de [`TESTE-FISICO-SAMSUNG.md`](TESTE-FISICO-SAMSUNG.md).
8. **Número oculto/restrito: sem efeito prático no modo filtro; no modo discador, nao verificado
   (nunca foi medido).** O que se sabe, por leitura da fonte do Telecom: sem handle o Android não
   aciona o serviço de triagem, então no **modo filtro** a opção "Bloquear números privados"
   existe e é honrada pelo motor, mas nunca é alcançada por uma chamada real.
   O que **não** se sabe: se deter o papel de telefone padrão faz essas chamadas passarem pela
   triagem — onde o bloqueio ainda é possível — ou se elas só aparecem na interface de chamada,
   quando já é tarde demais para bloquear. As duas hipóteses são compatíveis com o que foi
   medido até aqui: a Fase 6 provou elegibilidade, vínculo, política por contato e reversão,
   e **não** provou entrega de chamada sem identificação, porque simular chamada de entrada
   com identificação bloqueada está fora do alcance do processo de teste.
   Enquanto o cenário 59 do roteiro físico não for executado e registrado, este item **não
   afirma** que o modo discador destrava o recurso. Nenhum texto da interface promete isso:
   a copy da ativação lista o que muda e o que não muda, e número privado não está em nenhuma
   das duas listas.
   Roteiro físico: cenário **59** de [`TESTE-FISICO-SAMSUNG.md`](TESTE-FISICO-SAMSUNG.md).
9. **Perder um papel do sistema encerra o processo do app.** Medido em execução na Fase 6, com o
   motivo registrado pelo próprio sistema como mudança de permissão. Vale para o papel de
   triagem e para o de telefone padrão, e vale igual quando é o **usuário** que escolhe outro
   app de telefone nas configurações do sistema: o Sentinela é morto na hora, sem aviso e sem
   chance de rodar código de despedida.
   Consequências visíveis, nenhuma delas defeito: uma tela do Sentinela aberta no momento da
   troca desaparece; e o app sempre volta em processo novo, por isso o estado do modo discador é
   **derivado** de perguntas ao sistema e nunca de valor gravado — um valor gravado seria mentira
   desde o primeiro instante. É também por isso que o app nunca desliga o modo desabilitando
   componente próprio.
   Uma chamada em curso **sobrevive** ao encerramento: o sistema de telefonia religa no discador
   que vem no aparelho sozinho (medido). Roteiro físico: cenários **55** e **57**.

## De OEM (Samsung/One UI) — a validar no roteiro físico

- **Onde** a chamada bloqueada aparece no histórico da One UI (aba própria de bloqueadas ou
  misturada na lista) — que ela aparece já é certo, ver item 3 acima.
- Notificação nativa de perdida em cenários de tela bloqueada/economia de bateria.
- Sobrevivência dos papéis após "otimização de bateria" agressiva e reinício.
- Troca do app de telefone padrão (modo discador) e reversão na One UI.
- Interação com Wi-Fi Calling e Dual SIM.
- Registro de resultados: [`TESTE-FISICO-SAMSUNG.md`](TESTE-FISICO-SAMSUNG.md).
  **Nenhum hack de OEM entra no código antes de um item falhar comprovadamente lá.**

## De escopo (MVP)

- Sem identificação de quem liga (caller ID/base de spam) — o produto não classifica, só
  aplica as políticas por origem.
- **Modo discador: uma chamada por vez.** Chamada em espera, segunda chamada simultânea,
  conferência e transferência **não são suportadas nesta versão** — a interface de chamada
  atende uma sessão só. Se uma segunda chamada chegar durante a primeira, o comportamento é o
  que o sistema fizer; o Sentinela não a apresenta.
- Sem videochamada no modo discador.
- Sem mensagem rápida ao recusar ("respond via SMS") — exigiria permissão de mensagens, que está
  fora da lista permitida.
- Sem gesto de arrastar para atender: dois botões grandes, com rótulo e ícone distintos. Gesto
  sem affordance clara erra mais e não funciona com leitor de tela. É decisão, não falta.
- **Chamada de emergência é sempre atendida pelo discador que vem no aparelho**, mesmo com o
  papel de telefone padrão nas mãos do Sentinela. O app não intercepta, não apresenta e não
  bloqueia chamada de emergência em nenhuma configuração.
- Sem gravação de chamada.
- Sem bloqueio por prefixo/padrão (ex.: 0303) — candidato a backlog.
- Sem app para tablet/Wear; retrato como orientação principal.
- Import/export cobre só a whitelist (histórico fica no aparelho por decisão de privacidade).
- Sem rede no MVP: sincronização de listas e envio opcional de números recebidos são a etapa
  v0.2.0 (`backlog/supabase-v2.md`) — sempre opt-in, com o app 100% funcional offline.
