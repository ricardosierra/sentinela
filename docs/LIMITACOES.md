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
8. **Número oculto/restrito não é entregue ao serviço no modo filtro.** Sem handle, o Android
   não aciona a triagem: a opção "Bloquear números privados" existe e é honrada pelo motor,
   mas só tem efeito real no **modo discador** (Fase 6). No modo filtro ela fica sem efeito
   prático — a UI não deve prometer o contrário.
   Roteiro físico: cenário do modo discador, após a Fase 6.

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
- Modo discador mínimo: uma chamada por vez, sem conferência, sem gravação, sem videochamada.
- Sem bloqueio por prefixo/padrão (ex.: 0303) — candidato a backlog.
- Sem app para tablet/Wear; retrato como orientação principal.
- Import/export cobre só a whitelist (histórico fica no aparelho por decisão de privacidade).
- Sem rede no MVP: sincronização de listas e envio opcional de números recebidos são a etapa
  v0.2.0 (`backlog/supabase-v2.md`) — sempre opt-in, com o app 100% funcional offline.
