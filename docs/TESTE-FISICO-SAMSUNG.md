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

O que a Fase 6 **já provou em automação**, no aparelho virtual, e por isso saiu do escopo de
julgamento destes cenários: elegibilidade ao papel pelo caminho que a verifica, vínculo real do
serviço de interface de chamada, política por contato valendo de fato, independência dos dois
papéis, reversão pelo seletor do sistema, e sobrevivência da chamada ao encerramento do processo.
Nos cenários abaixo o que se julga é o **acabamento no aparelho e na interface do fabricante** —
não o comportamento da plataforma.

| # | Cenário | Passos | Esperado | Resultado |
|---|---------|--------|----------|-----------|
| 23 | Ativação do modo | Ativar modo discador na Proteção | A elegibilidade já é provada em automação; o que resta é o **diálogo do fabricante**: registrar se a One UI insere confirmação extra, quantos toques exige e se aparece toast próprio. Sentinela vira app de telefone padrão e nada quebra | |
| 24 | Chamada recebida (contato, Tocar) | Ligar do contato | UI de chamada do Sentinela aparece; Atender e Recusar funcionam; **áudio de verdade nas duas pontas** (o único juiz do caminho de áudio é o aparelho) | |
| 25 | Contato com política Bloquear | contactsPolicy=Bloquear, ligar do contato | A decisão já é provada em automação; o que resta é a **percepção real**: o telefone não toca, não vibra e não acende, e quem liga ouve o que a operadora der | |
| 26 | Contato com política Silenciar | contactsPolicy=Silenciar, ligar do contato | A decisão já é provada em automação; o que resta é a **percepção real**: chega sem som e sem vibração, visível na tela, sem heads-up sonoro da One UI | |
| 27 | Desconhecido no modo discador | Ligar do número desconhecido | Bloqueado como #1 (paridade com o modo filtro) | |
| 28 | Discagem própria | Discar um número pela tela de discagem | Chamada origina; UI em chamada com mudo/viva-voz/DTMF/encerrar funcionais e o **tom de tecla audível para o outro lado** | |
| 29 | Tela bloqueada | Ligar do contato com aparelho bloqueado | UI de chamada aparece sobre a tela bloqueada da One UI; atender funciona sem desbloquear (detalhe do fabricante no cenário 53) | |
| 30 | Reversão | Desativar modo discador pelo seletor do sistema | A reversão já é provada em automação (papel devolvido, papel de triagem intacto); o que resta é a **interface do fabricante**: o discador nativo volta a ser o padrão, ligações seguem normais, o modo filtro segue ativo e **nenhuma notificação de chamada órfã** fica na One UI depois de o app ser encerrado pela troca | |

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

## Pendências da Phase 6 (modo discador)

Pré-condição: modo discador ativo (cenário 23) e leitura da agenda concedida.

Estes nove cenários são o que **sobrou** depois da automação da Fase 6. Nenhum deles repete
assunto dos cenários 23–30: ali se julga acabamento do fluxo básico, aqui se julgam rota de
áudio real, interface do fabricante, degradação e as três questões abertas de OEM.

| # | Cenário | Passos | Esperado | Resultado |
|---|---------|--------|----------|-----------|
| 52 | Viva-voz e roteamento real | Em chamada ativa, alternar viva-voz, fone e Bluetooth | O áudio troca de rota de fato e a interface reflete a rota ativa. **Único ponto de DIA-02 impossível no aparelho virtual**, que expõe somente a rota de alto-falante | |
| 53 | Tela cheia sobre a tela bloqueada do fabricante | Aparelho bloqueado, receber chamada de contato | A tela de chamada do Sentinela aparece por cima; atender e recusar funcionam com o aparelho travado | |
| 54 | Permissão de tela cheia revogada | Revogar em Configurações e receber chamada | Degrada para aviso com ações de atender e recusar; **nunca fica em silêncio** | |
| 55 | Morte no meio da chamada em aparelho real | Com chamada ativa, forçar a parada do aplicativo | A chamada continua; o sistema assume com o discador do fabricante e avisa. Confirma no aparelho o que foi medido no aparelho virtual | |
| 56 | Dois chips e escolha de conta de telefone | Com dois chips, originar chamada sem chip padrão definido | Estado tratado com tela informativa e botão de encerrar funcional — **nunca tela em branco** | |
| 57 | Papel tomado por atualização do sistema | Após atualização da interface do fabricante ou instalação de outro discador, reabrir o aplicativo | A tela detecta a perda na retomada e degrada para modo filtro sem alarme e sem quebrar | |
| 58 | Otimização de bateria agressiva | Colocar o aplicativo em suspensão de atividade do fabricante e receber chamada | Registrar se o serviço de chamada ainda é vinculado. Se não for, é **limitação de fabricante, não defeito** | |
| 59 | Número privado no modo discador | Ligar com identificação bloqueada | **Registrar** se a chamada chega à triagem ou somente à interface de chamada. Resolve a questão aberta e decide o texto do item 8 de [`LIMITACOES.md`](LIMITACOES.md) | |
| 60 | Histórico do fabricante como telefone padrão | Bloquear uma chamada com o modo discador ativo e abrir o histórico do fabricante | A chamada **APARECE** como bloqueada. Confirma no aparelho que ser telefone padrão não destrava o pulo do registro | |

Origem: Phase 6, planos 06-03 a 06-07. Tudo que a automação já provou virou registro em
[`LIMITACOES.md`](LIMITACOES.md) (itens 3, 8 e 9) e nota nos cenários 23–30; o que sobrou aqui é
só o que exige aparelho real.

## Registro de comportamento OEM

Anotar aqui QUALQUER desvio (toast do sistema, atraso perceptível, notificação da One UI). A
entrada "bloqueada" no histórico nativo **não** é desvio: é o comportamento documentado do
Android (item 3 de [`LIMITACOES.md`](LIMITACOES.md)) — registre só *onde* ela aparece, no
cenário 41.

- ____________________________________________
- ____________________________________________

**Questões abertas de fabricante da Phase 6** — responder com sim/não e o que foi observado:

1. A One UI insere **confirmação extra** ao trocar o app de telefone padrão (diálogo próprio,
   aviso de segurança, passo adicional)? Cenário 23.
2. O app de telefone nativo **reassume o papel** após atualização do sistema ou da One UI?
   Cenário 57.
3. A **otimização de bateria** do fabricante impede o vínculo do serviço de interface de chamada?
   Cenário 58.

**Nenhum ajuste preventivo de fabricante entra no código** antes de um destes itens falhar
comprovadamente aqui. Resposta esperada de um item que falhe: registro em
[`LIMITACOES.md`](LIMITACOES.md) com aparelho e versão, e só então decisão de código.

## Critérios de aceite cobertos

Este roteiro fecha os critérios 3–11 da seção 16 de [`PROMPT-MVP.md`](PROMPT-MVP.md) e os
adendos de modo discador/políticas por contato. Itens que falharem por comportamento de OEM
vão para [`LIMITACOES.md`](LIMITACOES.md) com o registro do aparelho/versão.

Os cenários **40 a 51** carregam, além disso, os critérios de aceite 1, 2 e 6 da Phase 5 e o
veredito dos percentis de cauda do caminho de decisão. Enquanto eles não forem executados em
Samsung físico, esses critérios permanecem **abertos por desenho** — a Phase 5 entregou o
comportamento e a prova em JVM e emulador, não a prova em hardware.

Os cenários **52 a 60** fecham o que resta do modo discador da Phase 6:
- **DIA-01** (papel de telefone padrão, ativação e reversão) — provado em automação; aqui só o
  acabamento do fabricante: cenários 23, 30 e 57.
- **DIA-02** (interface de chamada própria: atender, recusar, mudo, viva-voz, tons, encerrar) —
  provado em automação e em teste de composição, **exceto o roteamento de áudio real**, que é o
  cenário 52. Nenhum outro ponto de DIA-02 depende de aparelho físico.
- **DIA-03** (discagem própria) — cenários 28 e 56.
- **DIA-04** (políticas por contato valendo no modo discador) — provado em automação sem alterar
  o motor de decisão; percepção real nos cenários 25 e 26.
- **DIA-05** (degradação honesta e reversibilidade) — cenários 54, 55, 57 e 58.
- **SCR-04** (números privados) permanece **PARCIAL e não verificado** no modo discador: o
  veredito é o cenário 59, e até lá nenhum documento nem texto de interface afirma que o modo
  discador destrava o recurso.
- **SCR-07** (omitir do histórico do telefone) permanece **WONT FIX** por decisão do Android, e o
  cenário 60 apenas reconfirma isso no aparelho, agora com o papel de telefone padrão ativo.

---

## Phase 7 — Onboarding, home e acessibilidade real (cenários 61–68)

A Phase 7 automatizou tudo que a árvore de semântica alcança: `contentDescription`,
`stateDescription`, semântica de cabeçalho, ordem de foco **declarada** e alvo de toque nos dois
eixos (área de toque **e** tamanho desenhado — o segundo eixo pegou quatro defeitos reais de layout
na Phase 6 e um quinto na Phase 7).

O que resta aqui é o que nenhuma árvore de semântica mede: o que o leitor de tela **fala**, como o
dedo **explora**, e o que o olho **enxerga** sob papel de parede real. Enquanto estes cenários não
rodarem, o critério de aceite 4 da Phase 7 ("TalkBack navega o fluxo inteiro") permanece **aberto por
desenho** — a fase entregou a semântica correta e a prova em JVM, não a prova de locução.

| # | Cenário | Passos | Esperado | Resultado |
|---|---------|--------|----------|-----------|
| 61 | Locução e verbosidade do leitor de tela no onboarding | Ligar o TalkBack e atravessar os 6 passos, ouvindo cada anúncio | Cada opção é anunciada com rótulo, papel e estado. Nada é lido duas vezes, nada fica mudo, e o contador de passo é dito uma vez por tela | |
| 62 | Exploração por toque e deslizamento na home | Com o TalkBack ligado, explorar a home arrastando o dedo e depois deslizando para a direita item a item | Todo controle é alcançável pelos dois gestos. O interruptor de proteção anuncia o estado ligado/desligado, e o aviso de papel ausente é alcançado antes dos atalhos | |
| 63 | Ordem de foco **efetiva** | Deslizar da primeira à última posição em cada tela desta fase | A ordem percorrida é a mesma da leitura visual. A ordem declarada já é automatizada; aqui se confere a efetiva, que pode divergir | |
| 64 | Contraste sob cor dinâmica com papel de parede real | Trocar o papel de parede por um claro, um escuro e um saturado; abrir home e tela Proteção | Texto e ícones seguem legíveis nos três. As cores funcionais de estado (proteção ativa / atenção / bloqueado) **não mudam** com o papel de parede — são literais fora do Dynamic Color | |
| 65 | Escala de fonte a 200% | Configurar o tamanho de fonte do sistema no máximo e atravessar onboarding, home e Proteção | Nenhum texto é cortado, nenhum botão desaparece, nenhuma linha de opção fica ilegível. Rolagem aparece onde for preciso | |
| 66 | Exibição maior (densidade) a 200% | Configurar o tamanho de exibição no máximo e repetir o percurso | Mesmo critério do cenário 65. Atenção especial aos alvos de 48dp, que a Phase 6 já viu comprimirem em tela curta | |
| 67 | Partida a frio percebida em hardware | Fechar o app, matar o processo, abrir e cronometrar até a primeira tela útil | A abertura é percebida como imediata. A mediana de 680 ms foi medida em **aparelho virtual**, onde a cauda mede o hospedeiro tanto quanto o app — o veredito é do aparelho | |
| 68 | Zero a protegido em menos de 2 minutos | Instalação limpa: atravessar o onboarding concedendo tudo, cronometrando | O usuário sai de instalado a protegido em menos de 2 minutos, e ao final entende o que cada política faz. É o objetivo declarado da fase, e só o aparelho o mede | |

**Regra de honestidade herdada:** afirmação de documento sem medição é defeito. Nenhum item acima é
apresentado como verificado; cada um é o veredito pendente do seu critério.
