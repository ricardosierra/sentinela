# Backlog — Etapa 2: Sincronização & Backend (v0.3.0)

> **Status:** backlog. NADA disto entra no MVP — o v0.1.0 não tem rede por design
> (é a prova técnica da promessa de privacidade). O app deve **sempre** ser capaz de
> funcionar 100% offline; online serve apenas para sincronizar quando o usuário quiser.
> Este arquivo existe para o desenho do MVP não fechar portas.

## O que o MVP já deixa pronto

- [x] `PersonalWhitelistRepository` é interface — fonte remota pluga sem tocar no domínio
- [x] E.164 como formato canônico de armazenamento (sincronizável sem ambiguidade)
- [x] Export/import local define o formato de serialização da whitelist (base do sync)
- [x] Decisão de bloqueio nunca depende de rede (arquitetura offline-first)

## Itens da etapa

- [ ] **Conta opcional** — auth (Supabase ou backend próprio, email/OTP); app continua 100%
      funcional sem conta
- [ ] **Sincronização de listas** — whitelist e políticas atrás das interfaces existentes,
      com merge last-write-wins por entrada e resolução de conflito por E.164
- [ ] **Envio opcional da lista de números recebidos** — usuário pode optar por enviar ao
      backend os números recebidos/bloqueados (opt-in explícito, revogável, anonimizável) —
      base para recursos futuros de listas comunitárias
- [ ] **Backup criptografado opt-in** — whitelist + configurações cifradas no cliente antes
      de subir (histórico NUNCA sobe sem opt-in específico)
- [ ] **Whitelist compartilhada** (ex.: empresa compartilha números legítimos com equipe) —
      leitura de uma lista publicada, sempre subordinada à whitelist pessoal
- [ ] **Revisão de privacidade e permissões** — atualizar `docs/PRIVACIDADE.md` e
      `docs/PERMISSOES.md`, adicionar INTERNET no manifest da release com sync e explicar a
      mudança na UI de onboarding

## Regras que continuam valendo na v2

- Decisão de bloqueio **nunca** espera rede — sync é assíncrono, decisão é local.
- App permanece 100% funcional offline; sync roda só quando possível/desejado.
- Zero telemetria/analytics; a única rede é o sync explícito do usuário.
- Nada sobe sem opt-in específico e revogável.
