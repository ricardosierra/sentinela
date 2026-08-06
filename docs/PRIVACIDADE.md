# Política de Privacidade — Sentinela

*Versão 0.2 — 2026-07-28. Esta política é curta porque o app coleta quase nada.*

## O que o Sentinela faz com seus dados

**Tudo fica no seu aparelho.** O Sentinela é open source, não tem servidor, não tem login,
não tem analytics, não tem publicidade e — nesta versão — não pede permissão de internet:
tecnicamente ele não consegue enviar nada para fora, mesmo que quisesse.

## Contatos

Para decidir se quem liga está na sua agenda, o Sentinela pode ler seus contatos
(permissão pedida com explicação, e recusável). A leitura é **100% local e em memória**:

- Nomes, fotos e números da agenda **nunca** são gravados no banco do app.
- Nada de contato sai do aparelho — não há rede.
- Sem a permissão, o app continua funcionando no modo filtro (o próprio Android garante que
  só números fora da agenda chegam ao filtro).

## O que é armazenado localmente

- **Sua whitelist pessoal**: números que você adicionou, em formato internacional (E.164),
  com descrição opcional.
- **Configurações de proteção**: suas escolhas (políticas para desconhecidos, contatos e
  whitelist, privados, modo de bloqueio, notificação, retenção, modo discador).
- **Histórico de bloqueios (opcional)**: se habilitado, guarda o número completo bloqueado (armazenado estritamente no banco local para que você possa consultar quem te ligou), o número mascarado (para notificações), data/hora,
  motivo da decisão e ação tomada — pelo período de retenção que você escolher
  (nunca guardar, 7, 30, 90 dias ou até exclusão manual).
- **Contador de aberturas do app**: um número local, usado apenas para o convite de
  avaliação/apoio — nunca enviado a lugar algum.

## O que NUNCA é feito

- Nenhum dado sai do aparelho (sem rede, sem nuvem, sem "parceiros", sem telemetria).
- Nenhuma chamada é gravada.
- O histórico de chamadas nativo não é lido.
- Números completos não aparecem em logs técnicos nem em notificações na tela bloqueada.

## Backup do Android

O banco de dados do Sentinela (whitelist, histórico e configurações) é **excluído** do backup
automático do Google e da transferência entre aparelhos. Se você trocar de telefone, use o
export manual da whitelist dentro do app.

## Suas escolhas

- A notificação de chamada bloqueada é **desligada por padrão** — você decide se quer saber.
- O modo discador é **opcional** e reversível a qualquer momento.
- "Limpar todos os dados" (em Privacidade e sobre) apaga whitelist, histórico e configurações.
- Desinstalar o app remove tudo.

## Futuro (transparência)

Uma versão futura (v0.3.0) oferecerá **sincronização opcional** de listas com um backend —
sempre opt-in, sempre explicada, e o app continuará funcionando 100% offline para quem não
quiser. Esta política será atualizada antes de qualquer recurso online existir.

## Limitações honestas

O Sentinela filtra apenas **chamadas telefônicas tradicionais**. Chamadas de WhatsApp,
Telegram e outros apps de VoIP não passam pelo filtro do Android e não são afetadas.
Comportamentos específicos de fabricante podem variar; veja [`LIMITACOES.md`](LIMITACOES.md).
