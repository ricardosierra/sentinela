# Pitfalls — armadilhas conhecidas antes de codar

## Plataforma / Telecom

- **`setSkipCallLog` pode não ser honrado em todo caminho** (chamada rejeitada pode virar
  entrada "bloqueada" no log nativo em algumas versões/OEMs). Tratar como configuração
  best-effort; validar em aparelho físico e documentar em LIMITACOES.md.
- **Janela de 5 s é dura**: qualquer I/O da decisão precisa de timeout interno folgado
  (ex.: 1,5 s) para sempre sobrar tempo de responder com fallback.
- **Processo pode nascer frio para atender uma chamada**: nada de inicialização pesada em
  `Application`/Service (DI manual lazy; sem frameworks).
- **Papel pode ser perdido silenciosamente** (usuário instala outro bloqueador): home revalida
  `isRoleHeld()` em toda retomada.
- **Dual SIM**: identificar SIM apenas se disponível sem permissão invasiva; nunca depender disso.
- **Samsung/One UI**: histórico nativo e notificação de perdida têm fama de comportamento
  próprio; roteiro físico dedicado antes de qualquer hack (e nenhum hack preventivo).
- **VoIP (WhatsApp/Telegram) não passa pelo CallScreeningService** — nunca prometer na UI.

## Toolchain (visto no bootstrap)

- **AGP 9 = Kotlin embutido.** Aplicar `org.jetbrains.kotlin.android` quebra o build com erro
  explícito. O plugin Compose (`org.jetbrains.kotlin.plugin.compose`) continua necessário.
  `kotlin { jvmToolchain() }` não existe sem KGP → usar `java { toolchain { ... } }`.
- **JDK do sistema é 25**: Gradle roda com JDK 17 do Homebrew via `org.gradle.java.home`
  (mesmo padrão do dmconecta-app-client-android).
- **Robolectric 4.16.1 ≤ SDK 36** com compileSdk 37 → `@Config(sdk = [36])` nos testes
  Robolectric até o 4.17 estável.
- **detekt 1.23.8 pré-data Kotlin 2.4**: se o plugin engasgar no Gradle 9.6, rodar via CLI
  como gate manual e registrar em STATE (2.0.0 ainda alpha — não adotar).
- **Plataforma SDK 37 não instalada localmente** (só até 36): primeiro build baixa
  automaticamente com as licenças já aceitas; se falhar, `sdkmanager "platforms;android-37"`.

## Produto / UX

- **Mockups divergem do que a plataforma permite** (tela de política por contato): contatos
  sempre tocam sem discador padrão. O passo vira informativo — não implementar as opções
  bloquear/silenciar contato.
- **Números em UI/notificação/log**: sempre mascarados; a máscara é código testado
  (`PhoneNumberNormalizer.mask`), não formatação ad-hoc espalhada.
- **Import de backup é superfície de ataque**: validar tamanho, formato e caminhos antes de
  tocar no banco; nunca sobrescrever sem confirmação.
