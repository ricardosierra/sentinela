# Matriz de Permissões

> Leitura **bloqueante** antes de mexer em manifest ou pedir qualquer permissão nova.
> A lista permitida é fechada e cada permissão tem a fase em que entra no manifest —
> adicionar qualquer coisa fora dela é mudança de escopo.

## Permitidas no MVP — modo filtro (padrão)

| Permissão / Mecanismo | Tipo | Entra na | Quando pede | Justificativa |
|-----------------------|------|----------|-------------|---------------|
| `android.permission.BIND_SCREENING_SERVICE` | Atributo do `<service>` | Fase 1 ✓ | — (declarada, não solicitada) | Exigida pela plataforma para o Telecom fazer bind no `CallScreeningService`; garante que só o sistema conecta |
| `ROLE_CALL_SCREENING` (RoleManager) | Papel do sistema | Fase 5 | Onboarding, diálogo nativo | Habilita a triagem; revogável a qualquer momento (home mostra correção) |
| `android.permission.POST_NOTIFICATIONS` | Runtime (API 33+) | Fase 1 ✓ (manifest) / Fase 5 (pedido) | Somente se o usuário habilitar a notificação própria (off por padrão) | Notificação silenciosa opcional de chamada bloqueada |
| `android.permission.READ_CONTACTS` | Runtime | Fase 4 | Onboarding (passo de contatos), com explicação; app funciona no modo filtro se negada | Saber se quem liga é contato (políticas por origem). Uso 100% local e em memória: nomes/números da agenda **nunca** são persistidos nem enviados |

## Permitidas no MVP — modo discador (opcional ao usuário)

| Permissão / Mecanismo | Tipo | Entra na | Quando pede | Justificativa |
|-----------------------|------|----------|-------------|---------------|
| `ROLE_DIALER` (RoleManager) | Papel do sistema | Fase 6 | Só quando o usuário ativa o modo discador | Torna o Sentinela o app de telefone padrão — triagem passa a cobrir contatos |
| `android.permission.BIND_INCALL_SERVICE` | Atributo do `<service>` (InCallService) | Fase 6 | — (declarada) | Exigida pela plataforma para a UI de chamada própria |
| `android.permission.CALL_PHONE` | Runtime | Fase 6 | Ao usar a discagem própria | Originar chamadas pela tela de discagem (elegibilidade ao papel). Medido `granted=false` no install: exige pedido em runtime |
| `android.permission.USE_FULL_SCREEN_INTENT` | Normal, concedida no install a aplicativos de chamada | Fase 6 | — (nunca solicitada em diálogo) | Sem ela, com a tela bloqueada, o usuário **não vê** a chamada chegando: a notificação vira apenas um aviso passageiro de 60 s. É o mecanismo que a própria plataforma indica para telefonia, e o único caminho oficial para uma tela de chamada em cima da tela bloqueada. Restrita ao modo discador — no modo filtro o app não a usa |
| Handler `ACTION_DIAL` | Intent filter | Fase 6 | — | Requisito de elegibilidade ao `ROLE_DIALER` |

> **`USE_FULL_SCREEN_INTENT` pode ser revogada** pelo usuário nas Configurações do sistema, mesmo
> tendo sido concedida no install. O código **nunca** assume a concessão: consulta a permissão
> antes de usar e, quando negada, degrada para notificação com ações de atender e recusar. O app
> não insiste, não repete o pedido e não usa dark pattern para reconquistá-la.

### Elegibilidade ao `ROLE_DIALER` — lista CONFIRMADA por experimento (pesquisa da Fase 6)

A pesquisa isolou a lista em experimento controlado (dois builds, duas instalações, mesmo
emulador). **Declarar apenas a tela de discagem faz o pedido do papel FALHAR** — o comando de
concessão do papel retornou erro e o papel permaneceu com o discador nativo. Com o serviço de
chamada declarado, o mesmo comando passou. Não é dedução de documentação: são dois códigos de
retorno. A lista mínima é:

1. Serviço de chamada (`InCallService`) **declarado** no manifest, `exported` e protegido pela
   permissão de vínculo `BIND_INCALL_SERVICE` (só o sistema conecta).
2. A ação de serviço de chamada (`android.telecom.InCallService`) no filtro de intenção do serviço.
3. O meta-dado de substituição da interface de chamada (`android.telecom.IN_CALL_SERVICE_UI`),
   que declara que o aplicativo assume a tela de chamada em lugar da nativa.
4. **Os DOIS** filtros de `ACTION_DIAL`: um com **esquema vazio** e outro com **esquema `tel`**. O
   sistema aplica os dois em sequência — passar em um só **não** é passar.

O meta-dado que transferiria ao aplicativo a responsabilidade de tocar o toque de chamada
(`IN_CALL_SERVICE_RINGING`) **não** é declarado: o sistema continua tocando o toque escolhido pelo
usuário, e replicar volume, vibração, escalonamento e Não Perturbe seria ampliar escopo sem pedido.

## Proibidas

| Permissão | Por que NÃO |
|-----------|-------------|
| `INTERNET` | MVP é 100% offline; ausência da permissão é prova técnica da promessa. Sync (v0.3.0) será opt-in, em release próprio, com atualização desta matriz |
| `READ_CALL_LOG` / `WRITE_CALL_LOG` | Histórico próprio é interno e opcional; log nativo não é lido nem escrito diretamente |
| `READ_SMS` | Fora de escopo |
| `READ_PHONE_STATE` | Só com necessidade comprovada e documentada — hoje não há |
| `SYSTEM_ALERT_WINDOW` (overlay) | **Continua proibida, inclusive no modo discador.** A tela cheia de chamada recebida é obtida pelo caminho oficial de notificação (canal de importância alta + intenção de tela cheia, com `USE_FULL_SCREEN_INTENT`), que a própria plataforma indica para telefonia. Overlay seria desnecessário e mais invasivo |
| `RECORD_AUDIO` | Nunca; app não grava chamadas |
| AccessibilityService | Proibido — só APIs oficiais do Telecom |

## Regras operacionais

- Toda permissão nova (mesmo permitida em tese) entra por PR com atualização desta matriz e
  justificativa na descrição — sem exceção.
- O manifest só ganha a permissão na fase indicada — nunca antecipar "porque vai precisar".
- `exported` explícito em todo componente; exportados apenas: `MainActivity` (launcher),
  `CallScreeningService` (protegido por `BIND_SCREENING_SERVICE`) e, na Fase 6, o
  `InCallService` (protegido por `BIND_INCALL_SERVICE`) e o handler de discagem.
- Release valida com `aapt dump permissions` que o APK declara exatamente o esperado para o
  conjunto de fases entregue (roteiro em [`RELEASE.md`](RELEASE.md)).
