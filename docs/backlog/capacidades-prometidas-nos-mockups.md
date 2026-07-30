# Capacidades prometidas nos mockups — roadmap pós-MVP

**Registrado:** 2026-07-30 (durante a Phase 7)
**Origem:** os mockups entregues em `docs/design/telas/` contêm copy que descreve funcionalidades
que o MVP não possui. Decisão do usuário: **substituir por copy honesta no MVP e trazer as
capacidades em versões posteriores**, cada uma com o seu próprio planejamento.

O MVP (`v0.1.0`) é 100% offline, sem `INTERNET`, com decisão determinística e sem classificação.
Nada abaixo pode ser afirmado na UI até existir de verdade.

---

## 1. Base global de números — "milhões de números"

**Prometido no mockup:** boas-vindas / dashboard.
**Estado real:** inexistente. O app não tem `INTERNET` no MVP.
**Depende de:** backend + sincronização opt-in (já previsto como `v0.2.0`, ver
[`supabase-v2.md`](supabase-v2.md)).
**Cuidados de privacidade que já estão travados pelo projeto:** sync sempre opt-in, sempre
assíncrona, **nunca** no caminho da decisão; o app precisa continuar funcionando 100% offline.
Consultar número em serviço remoto é justamente o tipo de coisa que vazaria com quem o usuário fala
— qualquer desenho aqui tem de resolver isso antes de existir (lista baixada localmente em vez de
consulta por número, por exemplo).

## 2. "Processamento local criptografado"

**Prometido no mockup:** boas-vindas.
**Estado real:** o processamento é local e offline, mas **não há criptografia** — é leitura de
SQLite e DataStore comuns. O banco fica fora do backup automático (`dataExtractionRules`,
verificado por teste desde a Phase 3), o que é uma proteção real, mas não é criptografia.
**Se for implementar:** SQLCipher ou `EncryptedSharedPreferences`/`EncryptedFile`, com o custo
medido no orçamento da decisão (p95 < 200 ms) antes de aceitar. Hoje o caminho quente está em
~23 ms, então há folga — mas o custo precisa ser medido, não presumido.

## 3. "Filtros inteligentes"

**Prometido no mockup:** dashboard.
**Estado real:** a decisão é **determinística**, por precedência de 8 níveis no
`CallDecisionEngine`, coberta por 48 casos parametrizados. Não há IA, heurística nem aprendizado.
**Se for implementar:** teria de ser local (sem `INTERNET`) e explicável — um app cujo valor é
"não interromper" não pode bloquear por motivo que o usuário não consegue entender. Qualquer
heurística precisa de reason code auditável no histórico.

## 4. Classificação de motivo — "Provável Fraude Financeira"

**Prometido no mockup:** dashboard (rótulo em item de histórico).
**Estado real:** o app registra o **reason code interno da decisão** (contato, whitelist,
desconhecido, número inválido, chamada repetida, falha de consulta…), que diz *por que a regra
decidiu*, não *o que a chamada era*. Não há classificação de conteúdo ou intenção.
**Se for implementar:** depende de (1) ou de (3). É a promessa mais delicada da lista, porque errar
a etiqueta é acusar alguém.

## 5. "Seguro contra spam conhecido"

**Prometido no mockup:** boas-vindas.
**Estado real:** não existe lista de spam conhecido. O que existe é a política por origem —
desconhecido é bloqueado por padrão, o que cobre spam **sem precisar conhecê-lo**.
**Nota:** a substituição honesta é mais forte que a promessa original e vale dizer isso na copy: o
Sentinela não depende de reconhecer o número, e por isso funciona com número novo.

---

## Imagens remotas nos mockups

Duas telas dos mockups carregam imagens de `googleusercontent.com`. Impossível sem `INTERNET`, e
carregar imagem remota num app de privacidade seria contraditório de todo modo. Substituídas por
superfície tonal — o que o próprio mockup do passo 1 do onboarding já faz.

---

## Regra permanente

Enquanto uma destas capacidades não existir e não estiver medida, a UI **não** pode sugerir que
existe. Vale o mesmo princípio que já governa `docs/LIMITACOES.md`: o registro no histórico nativo,
o Não Perturbe e o WhatsApp/VoIP são ditos com honestidade justamente porque a alternativa é o
usuário descobrir sozinho que o app mentiu.
