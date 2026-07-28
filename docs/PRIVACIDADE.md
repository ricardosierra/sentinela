# Política de Privacidade — Sentinela

*Versão 0.1 — 2026-07-27. Esta política é curta porque o app coleta quase nada.*

## O que o Sentinela faz com seus dados

**Tudo fica no seu aparelho.** O Sentinela não tem servidor, não tem login, não tem
analytics, não tem publicidade e não pede permissão de internet — tecnicamente ele não
consegue enviar nada para fora, mesmo que quisesse.

## O que é armazenado localmente

- **Sua whitelist pessoal**: números que você adicionou, em formato internacional (E.164),
  com descrição opcional.
- **Configurações de proteção**: suas escolhas (bloquear desconhecidos, privados, modo de
  bloqueio, notificação, retenção).
- **Histórico de bloqueios (opcional)**: se habilitado, guarda número mascarado, data/hora,
  motivo da decisão e ação tomada — pelo período de retenção que você escolher
  (nunca guardar, 7, 30, 90 dias ou até exclusão manual).

## O que NUNCA é feito

- Nenhum dado sai do aparelho (sem rede, sem nuvem, sem "parceiros").
- Nenhuma chamada é gravada.
- Seus contatos não são lidos (`READ_CONTACTS` não é sequer solicitada).
- O histórico de chamadas nativo não é lido.
- Números completos não aparecem em logs técnicos nem em notificações na tela bloqueada.

## Backup do Android

O banco de dados do Sentinela (whitelist, histórico e configurações) é **excluído** do backup
automático do Google e da transferência entre aparelhos. Se você trocar de telefone, use o
export manual da whitelist dentro do app.

## Suas escolhas

- A notificação de chamada bloqueada é **desligada por padrão** — você decide se quer saber.
- "Limpar todos os dados" (em Privacidade e sobre) apaga whitelist, histórico e configurações.
- Desinstalar o app remove tudo.

## Limitações honestas

O Sentinela filtra apenas **chamadas telefônicas tradicionais**. Chamadas de WhatsApp,
Telegram e outros apps de VoIP não passam pelo filtro do Android e não são afetadas.
Comportamentos específicos de fabricante podem variar; veja [`LIMITACOES.md`](LIMITACOES.md).
