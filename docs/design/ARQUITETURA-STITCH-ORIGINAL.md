# Ultrathink MVP — Guia de Arquitetura e Implementação

## 1. Arquitetura Android Nativa
O projeto segue uma arquitetura baseada em Clean Architecture simplificada, utilizando as bibliotecas recomendadas da Google.

### Estrutura de Pacotes
- `br.com.ultrathink.telecom`: Implementação do `CallScreeningService`.
- `br.com.ultrathink.domain`: Modelos de dados e lógica de decisão pura (`CallDecisionEngine`).
- `br.com.ultrathink.data.local`: Persistência com Room (Whitelist e Histórico) e DataStore (Configurações).
- `br.com.ultrathink.ui`: Telas em Jetpack Compose e ViewModels.
- `br.com.ultrathink.notifications`: Gerenciamento de notificações silenciosas.
- `br.com.ultrathink.phone`: Utilitários de normalização (libphonenumber).

## 2. CallScreeningService e Motor de Decisão
O coração do app é o `UnknownCallScreeningService`.

### Lógica de Bloqueio (CallDecisionEngine)
O motor recebe o número (handle) e retorna um `CallDecision`:
1. **Chamada de Saída**: Ignorada.
2. **Número Privado**: Bloqueado por padrão (configurável).
3. **Número na Whitelist**: Permitido.
4. **Número fora da Agenda**: Bloqueado (Resposta: `setDisallowCall(true)`, `setRejectCall(true)`, `setSkipNotification(true)`).

## 3. Whitelist e Histórico
- **Whitelist**: Números em formato E.164 com descrição opcional.
- **Histórico**: Registros locais com retenção configurável (padrão 30 dias).
- **Segurança**: Números são mascarados em logs e notificações. Sem permissão de Internet no Manifest.

## 4. Stack Tecnológica
- Kotlin, Coroutines, Flow.
- Jetpack Compose, Material 3.
- Room, DataStore.
- RoleManager (ROLE_CALL_SCREENING).

## 5. Permissões Necessárias
- `android.permission.BIND_SCREENING_SERVICE` (Obrigatória no Manifest).
- `POST_NOTIFICATIONS` (Opcional, API 33+).
- *Nota*: Não solicita `READ_CONTACTS` no MVP para máxima privacidade, tratando todo número desconhecido como alvo de bloqueio.

## 6. Configuração de Build
Utilizar Gradle Kotlin DSL e Version Catalog.
`minSdk = 29` (Android 10) para suporte completo ao CallScreeningService.
