# CAPTCHA por Voz para Chamadas Desconhecidas

**Origem:** Sugestão de usuário
**Status:** Bloqueado (Limitação do Android)

## A Ideia

Implementar um sistema de verificação (CAPTCHA) para chamadas de números desconhecidos antes de deixar o telefone tocar para o usuário.

**Fluxo sugerido:**
1. Número fora da agenda e da whitelist liga.
2. Sentinela atende a ligação silenciosamente.
3. Sentinela faz uma pergunta de segurança por voz (ex: "Quanto é a raiz quadrada de 10?").
4. Se o chamador responder corretamente, a chamada é liberada e começa a tocar para o usuário.
5. Se não responder ou errar, a chamada é recusada.

## Por que não pode ser implementado hoje (Limitações do Android)

Embora a ideia seja excelente para barrar robocalls, a arquitetura de segurança do Android impede que aplicativos de terceiros a implementem:

1. **Injeção e Interceptação de Áudio Proibidas:** O Android não fornece nenhuma API pública que permita a um aplicativo interceptar o áudio do chamador (*downlink*) ou injetar áudio na linha telefônica (*uplink*) durante uma chamada de rede celular tradicional.
2. **Tempo da Triagem:** A API `CallScreeningService` permite decidir o destino da chamada sem atendê-la, mas impõe um limite estrito de 5 segundos. Não é possível estabelecer áudio bidirecional nesta fase.
3. **Privilégios de Sistema:** Funcionalidades semelhantes (como o Google Call Screen nos aparelhos Pixel) funcionam porque o aplicativo tem permissões de *System App* (embutido na ROM), tendo acesso a APIs privadas de nível de sistema e do rádio de telefonia.

## Condições para reavaliação no futuro

Esta ideia deve permanecer no backlog caso:
- O Google lance uma API pública no Android que permita interagir com áudio de chamadas para apps definidos como discadores padrão (`ROLE_DIALER`).
- O Sentinela venha a suportar chamadas VoIP/SIP nativamente (onde o app controla a camada de mídia).
- Surja a possibilidade legal e técnica de construir o Sentinela como um módulo do Magisk (Root), alterando o escopo do projeto.
