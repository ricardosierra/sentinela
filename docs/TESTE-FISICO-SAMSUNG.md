# Roteiro de Validação Física — Samsung Galaxy

> Executar na Phase 9, em pelo menos um Samsung com One UI recente (os cenários 23–30 exigem
> a Fase 6 — modo discador — concluída). Preencher a coluna Resultado a cada rodada e
> registrar a data/aparelho/versão. Nenhum hack de OEM entra no código antes de um item
> falhar comprovadamente aqui.

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
| 22 | Contato com política Silenciar (modo filtro) | contactsPolicy=Silenciar SEM modo discador, ligar do contato | Contato toca normalmente (modo filtro não intercepta contatos); UI explica a limitação | |

## Modo discador (executar após a Fase 6)

Pré-condição: ativar o modo discador na tela Proteção (concede `ROLE_DIALER`; One UI pede
confirmação de app de telefone padrão).

| # | Cenário | Passos | Esperado | Resultado |
|---|---------|--------|----------|-----------|
| 23 | Ativação do modo | Ativar modo discador na Proteção | Diálogo do sistema aparece; Sentinela vira app de telefone padrão; nada quebra | |
| 24 | Chamada recebida (contato, Tocar) | Ligar do contato | UI de chamada do Sentinela aparece; Atender e Recusar funcionam; áudio ok | |
| 25 | Contato com política Bloquear | contactsPolicy=Bloquear, ligar do contato | Contato é bloqueado antes de tocar (política agora efetiva) | |
| 26 | Contato com política Silenciar | contactsPolicy=Silenciar, ligar do contato | Chega sem som/vibração, visível na tela | |
| 27 | Desconhecido no modo discador | Ligar do número desconhecido | Bloqueado como #1 (paridade com o modo filtro) | |
| 28 | Discagem própria | Discar um número pela tela de discagem | Chamada origina; UI em chamada com mudo/viva-voz/DTMF/encerrar funcionais | |
| 29 | Tela bloqueada | Ligar do contato com aparelho bloqueado | UI de chamada aparece sobre a tela bloqueada; atender funciona | |
| 30 | Reversão | Desativar modo discador | App de telefone nativo volta a ser o padrão; ligações seguem normais; modo filtro segue ativo | |

## Pendências herdadas da Phase 1 (fundação)

| # | Cenário | Passos | Esperado | Resultado |
|---|---------|--------|----------|-----------|
| 31 | APK debug instala | `adb install sentinela-debug.apk` | Instala sem erro; ícone aparece na gaveta | |
| 32 | Tema dark Silent Guardian renderiza | Abrir o app com o sistema em dark mode | Fundo `#081425`, texto legível, contraste correto; nenhum flash branco no splash | |
| 33 | Dynamic Color sob One UI | Trocar o papel de parede/paleta do sistema (Android 12+) e reabrir | O app adota a paleta dinâmica sem quebrar legibilidade | |
| 34 | Tema light forçado | Forçar light mode no sistema | App permanece utilizável e legível (produto é dark-first) | |

Origem: Phase 1, criterio 2. Diferido conforme a politica de validacao fisica do ROADMAP (2026-07-28).

## Pendências herdadas da Phase 3 (dados locais)

| # | Cenário | Passos | Esperado | Resultado |
|---|---------|--------|----------|-----------|
| 35 | p95 da consulta da whitelist em hardware real | Instalar o APK de androidTest e rodar `WhitelistPerformanceTest` no aparelho: `./gradlew connectedDebugAndroidTest -Pandroid.testInstrumentationRunnerArguments.tests_regex=WhitelistPerformanceTest` com o Galaxy conectado. Ler a linha `SENTINELA\|contains\|` no logcat | **p95 < 5 ms** com 1.000 entradas na whitelist (p50 < 1 ms já é cobrado no CI). Registrar os três percentis medidos | |

Origem: decisão humana de 2026-07-29. O assert de p95 saiu do emulador porque falhava 2 de 8
execuções **sem regressão real** — ali ele mede o scheduler do host tanto quanto o SQLite, e
aumentar a amostragem piorou a cauda. O número de 5 ms **não foi afrouxado**: continua sendo o
compromisso de produto, só que cobrado onde a medição significa alguma coisa. No CI seguem
quebrando o build o p50 (< 1 ms) e o `EXPLAIN QUERY PLAN`, que é a prova real do índice.

## Pendências herdadas da Phase 4 (contatos do aparelho)

| # | Cenário | Passos | Esperado | Resultado |
|---|---------|--------|----------|-----------|
| 36 | Agenda REAL do usuário | Com a agenda pessoal do Galaxy (sem fixture), ligar de um contato importado de vCard **sem DDI** e de um contato salvo em formato nacional | Ambos dão HIT: a sonda dupla (E.164 + nacional) alcança as duas grafias de gravação | |
| 37 | Percentis do lookup em hardware real | `./gradlew connectedDebugAndroidTest -Pandroid.testInstrumentationRunnerArguments.tests_regex=ContactLookupPerformanceTest` com o Galaxy conectado. Ler as linhas `SENTINELA\|contacts\|` no logcat | p50 comparável ao emulador (0,03 ms cache quente / 0,4–0,9 ms sonda direta) e **p95 e max** registrados. Só aqui o veredito da cauda vale | |
| 38 | Negação permanente de READ_CONTACTS | Negar a permissão duas vezes. Conferir o estado com `adb shell dumpsys package org.sentinela.app` (flags `USER_SET` / `USER_FIXED`). Reabrir o app. Resetar depois com `adb shell pm clear-permission-flags org.sentinela.app android.permission.READ_CONTACTS user-set user-fixed` | O app **não** repergunta na abertura seguinte e oferece o atalho para as Configurações do sistema; a consulta devolve UNAVAILABLE, nunca MISS | |
| 39 | Provider de contatos do One UI | Adicionar, editar e remover contatos pelo app de agenda da Samsung com o Sentinela instalado | O observador invalida o cache e a mudança aparece na consulta seguinte. A Samsung substitui o **app** de agenda, mas o provider é AOSP — risco baixo, não zero | |

Origem: Phase 4, planos 04-03/04-04. A pesquisa mediu o **mecanismo** no emulador (SIM `us`),
não a frequência de cada grafia numa agenda brasileira real; e a cauda medida no AVD mistura o
scheduler do host. Nenhum hack preventivo de OEM foi escrito: se o cenário 39 desviar, o desvio
vai para [`LIMITACOES.md`](LIMITACOES.md) antes de qualquer código.

## Pendências herdadas da Phase 5 (triagem no modo filtro)

| # | Cenário | Ação | Esperado | Resultado |
|---|---------|------|----------|-----------|
| 40 | Bloqueio real de desconhecido | Ligar de um número fora da agenda e fora da whitelist | Não toca, não vibra, não mostra tela de chamada, **nenhuma** notificação nativa de perdida | |
| 41 | Chamada bloqueada no histórico da One UI | Após o cenário 40, abrir o histórico nativo do telefone Samsung | Registrar **onde** a chamada aparece (aba "Bloqueadas"? misturada na lista?). Confirma o no-op do pedido de não registrar; é registro de comportamento, **não** falha | |
| 42 | Contato toca no modo filtro | Com `READ_CONTACTS` concedida, ligar de um contato da agenda | Toca normalmente. **Este cenário agora exercita o nosso lookup**, não a plataforma: a chamada chega ao serviço e a decisão é do motor | |
| 43 | `READ_CONTACTS` revogada | Revogar a permissão e repetir o cenário 42 | Toca normalmente (o Telecom nem faz o bind). Registrar se o app detecta a revogação e avisa o usuário | |
| 44 | Caixa postal | Política Bloquear = "encaminhar silenciosamente"; ligar de desconhecido | Registrar o que o **chamador** ouve: caixa postal ou tom de não atendida. Depende da operadora — nunca prometido na UI | |
| 45 | Não Perturbe ativo | Com o Não Perturbe ligado, ligar de origem com política "Nunca Silenciar" (whitelist) | **Esperado: continua suprimida pelo Não Perturbe.** Confirma a limitação documentada; não é bug | |
| 46 | Silenciar | Política Silenciar para desconhecidos; ligar | Tela de chamada aparece, **sem som e sem vibração**; entra no histórico nativo | |
| 47 | p95 da decisão em hardware real | `./gradlew connectedDebugAndroidTest -Pandroid.testInstrumentationRunnerArguments.tests_regex=DecisionPerformanceTest` com o Galaxy conectado; ler `SENTINELA\|decision\|` no logcat | **p95 < 200 ms**. Registrar p50/p95/max. Só aqui a cauda tem veredito | |
| 48 | Início a frio por chamada | Forçar parada do app (`adb shell am force-stop org.sentinela.app`) e ligar em seguida | A chamada é bloqueada mesmo com o processo morto — o start pelo bind cabe nos 5 s | |
| 49 | Perda do papel | Instalar outro app de bloqueio e conceder o papel a ele; reabrir o Sentinela | A home detecta a perda e oferece correção (SCR-02) | |
| 50 | Notificação na tela bloqueada | Habilitar a notificação própria e bloquear uma chamada com a tela travada | Silenciosa, sem heads-up, e **sem o número completo** em nenhuma configuração de privacidade da tela bloqueada | |
| 51 | Dual SIM / chamada em espera | Durante uma chamada ativa, receber uma de desconhecido | Bloqueada corretamente, sem afetar a chamada em curso | |

Origem: Phase 5, pesquisa lida na fonte do próprio Android. Tudo que a fonte já provou virou
correção em [`LIMITACOES.md`](LIMITACOES.md) (itens 2, 3, 7 e 8) e **não** volta como cenário;
o que sobrou aqui é só o que exige aparelho real.

**Diferidos explicitamente para a Phase 9, não lacuna da Phase 5:** os critérios de aceite 1, 2
e 6 desta fase no [`ROADMAP`](../.planning/ROADMAP.md) — bloqueio efetivo ponta a ponta, ausência
de aviso nativo de perdida e comportamento sob Não Perturbe — dependem de chamada real e estão
nos cenários 40, 41, 45 e 46. O **veredito do percentil de cauda** (p95 e max do caminho de
decisão) também é diferido: o CI cobra só a mediana, porque no emulador a cauda mede o scheduler
do host tanto quanto o nosso código; o veredito é o cenário 47.

## Registro de comportamento OEM

Anotar aqui QUALQUER desvio (toast do sistema, atraso perceptível, notificação da One UI). A
entrada "bloqueada" no histórico nativo **não** é desvio: é o comportamento documentado do
Android (item 3 de [`LIMITACOES.md`](LIMITACOES.md)) — registre só *onde* ela aparece, no
cenário 41.

- ____________________________________________
- ____________________________________________

## Critérios de aceite cobertos

Este roteiro fecha os critérios 3–11 da seção 16 de [`PROMPT-MVP.md`](PROMPT-MVP.md) e os
adendos de modo discador/políticas por contato. Itens que falharem por comportamento de OEM
vão para [`LIMITACOES.md`](LIMITACOES.md) com o registro do aparelho/versão.

Os cenários **40 a 51** carregam, além disso, os critérios de aceite 1, 2 e 6 da Phase 5 e o
veredito dos percentis de cauda do caminho de decisão. Enquanto eles não forem executados em
Samsung físico, esses critérios permanecem **abertos por desenho** — a Phase 5 entregou o
comportamento e a prova em JVM e emulador, não a prova em hardware.
