# Limitações Conhecidas

> Honestidade é feature: nada aqui é bug — são fronteiras da plataforma ou decisões de escopo.
> A UI nunca promete além do que está garantido aqui.

## Da plataforma (Android)

1. **Só chamadas telefônicas tradicionais.** WhatsApp/Telegram/VoIP não passam pelo
   `CallScreeningService`. A UI diz isso explicitamente no onboarding.
2. **Contatos sempre tocam — e isso não é configurável.** Sem ser discador padrão, o filtro
   nem recebe chamadas de contatos. Consequência: as opções "Bloquear/Silenciar contato" do
   mockup original não são implementáveis no MVP (o passo de contatos do onboarding é
   informativo).
3. **`setSkipCallLog` é best-effort.** Em algumas versões/OEMs a chamada rejeitada ainda pode
   aparecer como "bloqueada" no histórico nativo. Tratado como configuração de intenção;
   validação por aparelho no roteiro físico.
4. **Janela de 5 s.** Se o aparelho estiver em condição extrema (I/O travado) e o app não
   responder a tempo, a plataforma deixa a chamada seguir — comportamento fail-open do
   Android, alinhado ao fallback padrão do app (permitir).
5. **Papel único.** Só um app pode deter `ROLE_CALL_SCREENING`; instalar outro bloqueador
   rouba o papel silenciosamente. A home detecta e oferece correção.
6. **Caixa postal depende da operadora.** "Encaminhar silenciosamente" resulta em caixa postal
   apenas se a linha tiver o serviço; sem ele, para quem liga soa como chamada não atendida.

## De OEM (Samsung/One UI) — a validar no roteiro físico

- Comportamento do histórico nativo com chamadas rejeitadas pelo filtro.
- Notificação nativa de perdida em cenários de tela bloqueada/economia de bateria.
- Sobrevivência do papel após "otimização de bateria" agressiva e reinício.
- Interação com Wi-Fi Calling e Dual SIM.
- Registro de resultados: [`TESTE-FISICO-SAMSUNG.md`](TESTE-FISICO-SAMSUNG.md).
  **Nenhum hack de OEM entra no código antes de um item falhar comprovadamente lá.**

## De escopo (MVP)

- Sem identificação de quem liga (caller ID/base de spam) — o produto não classifica, só
  aplica "desconhecido não interrompe".
- Sem bloqueio por prefixo/padrão (ex.: 0303) — candidato a backlog.
- Sem app para tablet/Wear; retrato como orientação principal.
- Import/export cobre só a whitelist (histórico fica no aparelho por decisão de privacidade).
- Sem sincronização entre aparelhos (v2 com Supabase, ver `backlog/supabase-v2.md`).
