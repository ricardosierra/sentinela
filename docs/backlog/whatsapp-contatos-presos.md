# Contatos salvos apenas no WhatsApp não são reconhecidos

## Problema
O WhatsApp mudou a forma de salvar contatos: ao adicionar alguém pelo app, agora ele oferece salvar o contato só no WhatsApp (na nuvem deles), sem gravar na agenda do celular (Contacts Provider).

Como esses contatos não existem no banco de contatos do Android, nenhum app do telefone consegue vê-los — nem o app de Contatos, nem o discador, nem o Sentinela.

O impacto no Sentinela é grave do ponto de vista de UX: o usuário acredita que o contato está salvo, mas para o motor de decisão do Sentinela ele é um número desconhecido e, portanto, a chamada é bloqueada (se a política de desconhecidos estiver como bloqueio).

## Solução (Produto / UX)
Como não há solução técnica via código para ler o banco fechado do WhatsApp (e nem usaríamos permissões intrusivas para tentar), a solução é **educação do usuário**.

Possíveis ações para as próximas versões:
1. **FAQ / Ajuda**: Adicionar uma seção clara nas configurações ou em uma tela de "Ajuda" explicando esse comportamento do WhatsApp e como resolver (salvando na agenda do telefone ou usando a Whitelist).
2. **Onboarding**: Um aviso amigável no onboarding de que apenas contatos salvos **na agenda do telefone** são reconhecidos.
3. **Histórico Interativo**: No histórico de bloqueios, quando o usuário tocar em uma chamada bloqueada de "Desconhecido", adicionar uma dica: *"Se este é um contato recente do WhatsApp, adicione-o à Whitelist ou certifique-se de que ele está salvo na agenda do aparelho."*

## Workaround atual para o usuário
- **Causa raiz**: Ao adicionar contato no WhatsApp, escolher sincronizar/salvar na agenda do celular.
- **Exceção**: Adicionar o número afetado diretamente à whitelist do Sentinela através do histórico.
- **Configuração**: Alterar a política de desconhecidos para apenas silenciar (se o usuário estiver no meio de uma migração ou for muito afetado por isso).
