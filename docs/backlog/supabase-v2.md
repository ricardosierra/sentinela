# Backlog — Etapa 2: Supabase & Sincronização (v0.2.0)

> **Status:** backlog. NADA disto entra no MVP — o v0.1.0 não tem rede por design
> (é a prova técnica da promessa de privacidade). Este arquivo existe para o desenho do MVP
> não fechar portas.

## O que o MVP já deixa pronto

- [x] `PersonalWhitelistRepository` é interface — fonte remota pluga sem tocar no domínio
- [x] E.164 como formato canônico de armazenamento (sincronizável sem ambiguidade)
- [x] Export/import local define o formato de serialização da whitelist (base do sync)

## Itens da etapa (curto, como pede o prompt)

- [ ] **Conta opcional** — auth Supabase (email/OTP); app continua 100% funcional sem conta
- [ ] **Whitelist sincronizada** — fonte remota atrás da interface existente, com merge
      last-write-wins por entrada e resolução de conflito por E.164
- [ ] **Backup criptografado opt-in** — whitelist + configurações cifradas no cliente antes
      de subir (histórico NUNCA sobe)
- [ ] **Whitelist compartilhada** (ex.: empresa compartilha números legítimos com equipe) —
      leitura de uma lista publicada, sempre subordinada à whitelist pessoal
- [ ] **Revisão de privacidade** — atualizar `docs/PRIVACIDADE.md`, pedir INTERNET no manifest
      da variante com sync e explicar a mudança na UI de onboarding

## Regras que continuam valendo na v2

- Decisão de bloqueio **nunca** espera rede — sync é assíncrono, decisão é local.
- Zero telemetria/analytics; a única rede é o sync explícito do usuário.
- Histórico de bloqueios não sai do aparelho em hipótese alguma.
