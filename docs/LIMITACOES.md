# Limitações Conhecidas

> Honestidade é feature: nada aqui é bug — são fronteiras da plataforma ou decisões de escopo.
> A UI nunca promete além do que está garantido aqui.

## Da plataforma (Android)

1. **Só chamadas telefônicas tradicionais.** WhatsApp/Telegram/VoIP não passam pelo
   `CallScreeningService`. A UI diz isso explicitamente no onboarding.
2. **No modo filtro (padrão), contatos sempre tocam** — sem ser o app de telefone padrão, o
   filtro nem recebe chamadas de contatos. As políticas por contato
   (Tocar/Bloquear/Silenciar/Nunca Silenciar) só têm efeito pleno no **modo discador**;
   a UI deixa isso claro para não parecer defeito.
3. **`setSkipCallLog` é best-effort.** Em algumas versões/OEMs a chamada rejeitada ainda pode
   aparecer como "bloqueada" no histórico nativo. Tratado como configuração de intenção;
   validação por aparelho no roteiro físico.
4. **Janela de 5 s.** Se o aparelho estiver em condição extrema (I/O travado) e o app não
   responder a tempo, a plataforma deixa a chamada seguir — comportamento fail-open do
   Android, alinhado ao fallback padrão do app (permitir).
5. **Papéis são únicos.** Só um app detém `ROLE_CALL_SCREENING` (e um o `ROLE_DIALER`);
   instalar outro bloqueador/discador rouba o papel silenciosamente. A home detecta e
   oferece correção.
6. **Caixa postal depende da operadora.** "Encaminhar silenciosamente" resulta em caixa
   postal apenas se a linha tiver o serviço; sem ele, para quem liga soa como chamada não
   atendida.
7. **"Nunca Silenciar" (ignorar Não Perturbe)** depende de mecanismos que variam por versão
   (canal com bypass de DND, regras de DND). A semântica exata é confirmada na pesquisa da
   Fase 6; até lá a opção é apresentada sem promessa absoluta.

## De OEM (Samsung/One UI) — a validar no roteiro físico

- Comportamento do histórico nativo com chamadas rejeitadas pelo filtro.
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
