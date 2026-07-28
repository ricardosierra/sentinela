# Matriz de Permissões

> Leitura **bloqueante** antes de mexer em manifest ou pedir qualquer permissão nova.
> A lista permitida é fechada; adicionar qualquer coisa fora dela é mudança de escopo.

## Permitidas no MVP

| Permissão / Mecanismo | Tipo | Quando | Justificativa |
|-----------------------|------|--------|---------------|
| `android.permission.BIND_SCREENING_SERVICE` | Atributo do `<service>` no manifest | Instalação | Exigida pela plataforma para o Telecom fazer bind no `CallScreeningService`. Não é permissão que o app pede — é proteção que o app declara (só o sistema conecta). |
| `ROLE_CALL_SCREENING` (RoleManager) | Papel do sistema | Onboarding, via diálogo nativo | É o que habilita a triagem. Solicitado com `createRequestRoleIntent`; usuário pode revogar a qualquer momento e a home mostra correção. |
| `android.permission.POST_NOTIFICATIONS` | Runtime (API 33+) | Somente se o usuário habilitar a notificação própria (off por padrão) | Notificação silenciosa opcional de chamada bloqueada. Nunca pedida no onboarding por padrão. |

## Proibidas no MVP (lista explícita do prompt)

| Permissão | Por que NÃO |
|-----------|-------------|
| `INTERNET` | Produto é 100% local; ausência da permissão é prova técnica da promessa de privacidade |
| `READ_CONTACTS` | Desnecessária: a plataforma só entrega ao filtro números fora da agenda |
| `READ_CALL_LOG` / `WRITE_CALL_LOG` | Histórico próprio é interno e opcional; log nativo não é lido |
| `READ_SMS` | Fora de escopo |
| `READ_PHONE_STATE` | Só com necessidade comprovada e documentada — hoje não há |
| `SYSTEM_ALERT_WINDOW` (overlay) | Proibido pelo prompt; nada de esconder tela de chamada com overlay |
| `RECORD_AUDIO` | Nunca; app não grava chamadas |
| AccessibilityService | Proibido pelo prompt |
| Papel de discador padrão (`ROLE_DIALER`) | Proibido pelo prompt; mudaria todo o perfil de risco |

## Regras operacionais

- Toda permissão nova (mesmo permitida em tese) entra por PR com atualização desta matriz e
  justificativa na descrição — sem exceção.
- `exported` explícito em todo componente; nada exportado além de `MainActivity` (launcher) e
  do `CallScreeningService` (protegido por `BIND_SCREENING_SERVICE`).
- Release valida com `aapt dump permissions` que o APK declara apenas `POST_NOTIFICATIONS`
  (roteiro em [`RELEASE.md`](RELEASE.md)).
