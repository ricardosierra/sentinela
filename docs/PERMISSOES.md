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
| `android.permission.CALL_PHONE` | Runtime | Fase 6 | Ao usar a discagem própria | Originar chamadas pela tela de discagem (elegibilidade ao papel) |
| Handler `ACTION_DIAL` | Intent filter | Fase 6 | — | Requisito de elegibilidade ao `ROLE_DIALER` |

> A lista exata de requisitos de elegibilidade ao `ROLE_DIALER` é confirmada na pesquisa
> obrigatória da Fase 6; qualquer permissão adicional que ela revele passa por PR + esta matriz.

## Proibidas

| Permissão | Por que NÃO |
|-----------|-------------|
| `INTERNET` | MVP é 100% offline; ausência da permissão é prova técnica da promessa. Sync (v0.2.0) será opt-in, em release próprio, com atualização desta matriz |
| `READ_CALL_LOG` / `WRITE_CALL_LOG` | Histórico próprio é interno e opcional; log nativo não é lido nem escrito diretamente |
| `READ_SMS` | Fora de escopo |
| `READ_PHONE_STATE` | Só com necessidade comprovada e documentada — hoje não há |
| `SYSTEM_ALERT_WINDOW` (overlay) | Nada de esconder tela de chamada com overlay — no modo discador a UI de chamada é `InCallService` oficial |
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
