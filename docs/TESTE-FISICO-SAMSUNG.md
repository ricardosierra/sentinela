# Roteiro de Validação Física — Samsung Galaxy

> Executar na Phase 7, em pelo menos um Samsung com One UI recente. Preencher a coluna
> Resultado a cada rodada e registrar a data/aparelho/versão. Nenhum hack de OEM entra no
> código antes de um item falhar comprovadamente aqui.

**Aparelho:** ____________  **Modelo/One UI/Android:** ____________  **Data:** ____________
**Chips:** SIM1 ____________ SIM2 ____________  **Caixa postal ativa?** ____

## Preparação

1. Instalar `sentinela-debug.apk` (`adb install sentinela-debug.apk`).
2. Completar onboarding concedendo o papel de filtro de chamadas.
3. Ter um segundo telefone "desconhecido" (fora da agenda), um contato salvo e acesso a
   chamada com número oculto (`#31#` antes do número, ou config da operadora).

## Matriz de cenários

Config padrão salvo indicação: proteção ON, desconhecidos bloqueados, privados bloqueados,
modo rejeitar, ocultar do histórico nativo ON, notificação própria OFF.

| # | Cenário | Passos | Esperado | Resultado |
|---|---------|--------|----------|-----------|
| 1 | Desconhecido, tela ligada e desbloqueada | Ligar do número desconhecido | Não toca, sem tela de chamada, sem notificação nativa de perdida, sem entrada no histórico nativo | |
| 2 | Desconhecido, tela bloqueada | Idem com aparelho bloqueado | Idem ao #1; tela não acende | |
| 3 | Desconhecido, app encerrado (swipe em recentes) | Fechar app, ligar | Idem ao #1 (service nasce frio) | |
| 4 | Desconhecido, processo morto | `adb shell am force-stop org.sentinela.app`, ligar | Idem ao #1 | |
| 5 | Desconhecido, economia de bateria | Ativar economia máxima + app "otimizado", ligar | Idem ao #1; registrar qualquer atraso | |
| 6 | Contato salvo | Ligar do contato | Toca normalmente, aparece no histórico nativo | |
| 7 | Número privado/oculto | Ligar com ID oculto | Bloqueado como #1 (config privados ON) | |
| 8 | Privados permitidos | Desligar "bloquear privados", ligar oculto | Toca normalmente | |
| 9 | Whitelist | Adicionar o desconhecido à whitelist, ligar | Toca normalmente | |
| 10 | Proteção desligada | Toggle master OFF, ligar desconhecido | Toca normalmente | |
| 11 | Chamada de saída | Ligar DO aparelho para qualquer número | Sem interferência, aparece no histórico nativo | |
| 12 | Modo "encaminhar silenciosamente" | Trocar modo, ligar desconhecido | Sem toque; quem liga cai na caixa postal (se ativa) | |
| 13 | Caixa postal indisponível | Idem #12 em chip sem caixa postal | Sem toque; registrar o que quem liga ouve | |
| 14 | Notificação própria ON | Habilitar notificação, ligar desconhecido | Notificação silenciosa (sem som/vibração/heads-up), número mascarado; tocar abre o registro | |
| 15 | Notificação própria OFF | Config padrão, ligar desconhecido | Nenhuma notificação de nenhum tipo | |
| 16 | Histórico nativo visível | Desligar "ocultar do histórico nativo", ligar desconhecido | Não toca; entrada aparece no log nativo (como bloqueada/recusada — registrar como One UI mostra) | |
| 17 | Dual SIM — chip 2 | Ligar desconhecido para o SIM2 | Idem ao #1; histórico interno registra SIM se disponível | |
| 18 | Wi-Fi Calling | Ativar VoWiFi, ligar desconhecido | Idem ao #1 | |
| 19 | Reboot | Reiniciar aparelho, ligar desconhecido sem abrir o app | Idem ao #1 (papel persiste) | |
| 20 | Papel roubado | Instalar outro app de bloqueio e dar o papel a ele | Home do Sentinela mostra aviso + botão corrigir funciona | |
| 21 | Sem internet | Desligar Wi-Fi e dados móveis, ligar desconhecido | Idem ao #1 — bloqueio funciona 100% offline | |

## Registro de comportamento OEM

Anotar aqui QUALQUER desvio (ex.: entrada "bloqueada" no log nativo mesmo com skip, toast do
sistema, atraso perceptível, notificação da One UI):

- ____________________________________________
- ____________________________________________

## Critérios de aceite cobertos

Este roteiro fecha os critérios 3–11 da seção 16 de [`PROMPT-MVP.md`](PROMPT-MVP.md).
Itens que falharem por comportamento de OEM vão para [`LIMITACOES.md`](LIMITACOES.md) com o
registro do aparelho/versão.
