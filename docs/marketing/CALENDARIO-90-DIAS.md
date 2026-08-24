.github/workflows/marketing.yml
name: Validar marketing

on:
  pull_request:
    paths:
      - "README*"
      - "SECURITY.md"
      - "docs/marketing/**"
      - "docs/loja/**"
      - ".github/ISSUE_TEMPLATE/**"
      - "scripts/verify-marketing-assets.sh"
      - "scripts/sync-play-metadata.py"
  push:
    branches:
      - master
    paths:
      - "README*"
      - "SECURITY.md"
      - "docs/marketing/**"
      - "docs/loja/**"
      - ".github/ISSUE_TEMPLATE/**"
      - "scripts/verify-marketing-assets.sh"
      - "scripts/sync-play-metadata.py"

permissions:
  contents: read

jobs:
  validate:
    name: Validar conteúdo e metadados
    runs-on: ubuntu-latest
    steps:
      - uses: actions/checkout@v4
      - name: Validar ativos editoriais
        run: ./scripts/verify-marketing-assets.sh
      - name: Validar ficha da Google Play
        run: python3 scripts/sync-play-metadata.py --check
docs/marketing/CALENDARIO-90-DIAS.md
# Calendário editorial de 90 dias

Datas são a cadência inicial do plano; se a publicação começar depois, deslocar as semanas sem
criar duas URLs para a mesma intenção.

| Semana | Entrega principal | Apoio | Distribuição |
|---|---|---|---|
| 1 | RicaSoluções: página canônica + trust center | Ricardo: bastidor sem servidor | 3 posts |
| 2 | Pilar telemarketing | fontes Anatel/Não Me Perturbe | 3 adaptações |
| 3 | Guia de desconhecidos + “o que não faz” | FAQ de limites | carrossel + Short |
| 4 | Guia de privados | compatibilidade sem promessa OEM | vídeo |
| 5 | Landing sem Internet | matriz de permissões | outreach FOSS |
| 6 | Não Me Perturbe + filtro local | atualização do pilar | post útil |
| 7 | Por que números diferentes ligam? | README, screenshots e release | pauta individual |
| 8 | Nativo vs. filtro | quadro de modelos | comparativo visual |
| 9 | Guia Samsung, somente se testado | decisão de arquitetura | Short |
| 10 | Guia Motorola, somente se testado | atualização por query | Short |
| 11 | Privacidade em caller ID | landing open source | outreach técnico |
| 12 | Guia Xiaomi, somente se testado | docs recorrentes | Short |
| 13 | Revisão do pilar e FAQ de queries reais | decidir próximos P1/P2 | relatório trimestral |

## Ritual semanal

- **Segunda:** publicar ou atualizar o ativo principal.
- **Terça:** criar links internos e inspecionar a URL no Search Console.
- **Quarta:** adaptar para social sem copiar o artigo inteiro.
- **Quinta:** contribuir em uma comunidade conforme as regras registradas.
- **Sexta:** registrar métricas, dúvidas, feedback e o próximo experimento.

Guia OEM só entra no calendário depois de aparelho, versão e cenário registrados em
[`docs/TESTE-FISICO-SAMSUNG.md`](../TESTE-FISICO-SAMSUNG.md) ou documentação oficial suficiente.
docs/marketing/CONTEUDO-P0.md
# Conteúdo P0

Textos originais para o hub `sierratecnologia.com.br` e para o trust center da RicaSoluções.
Cada bloco é uma URL distinta. O CMS deve adicionar byline, data de publicação, data de revisão,
canonical, Open Graph 1200×630, breadcrumbs e links internos conforme o mapa de
[`SEO-E-CANONICAIS.md`](SEO-E-CANONICAIS.md).

## 1. Como parar ligações de telemarketing no Android

**Slug:** `/como-parar-ligacoes-de-telemarketing-android/`  
**Intenção:** informacional · TOFU  
**Title:** Como parar ligações de telemarketing no Android sem promessas mágicas  
**Description:** Veja as opções nativas, o Não Me Perturbe e quando um filtro local de chamadas faz sentido no Android.  
**CTA:** Veja como bloquear chamadas desconhecidas no Android → `{{HUB_BLOQUEAR_DESCONHECIDO_URL}}`

O primeiro passo para reduzir telemarketing no Android é escolher o quanto você quer filtrar.
Bloquear um número individual resolve uma ligação, mas não impede que outra linha apareça no dia
seguinte. As configurações nativas do aplicativo Telefone podem ajudar, e o Não Me Perturbe é
uma alternativa complementar para chamadas de setores participantes. Eles não são a mesma coisa
que um filtro local: cada opção tem alcance e limitações próprios.

Se a dor é ser interrompido por qualquer número que não esteja na sua agenda, uma política por
origem pode ser mais previsível do que perseguir números um a um. O Sentinela aplica uma regra
local configurável para contatos, lista pessoal e desconhecidos. Ele não tenta descobrir quem
está ligando, não consulta uma base de spam e não substitui o Não Me Perturbe.

Antes de ativar qualquer bloqueio, cadastre exceções que você realmente espera: escola, médico,
entrega ou trabalho. Depois, confirme o que o aparelho mostra no histórico. No Android, uma
chamada bloqueada pode continuar registrada no histórico nativo mesmo sem tocar ou gerar aviso;
isso é uma limitação da plataforma, não algo que o Sentinela possa apagar.

**Quando um filtro local faz sentido:** quando o objetivo é controlar a interrupção pela origem
da chamada, sem conta, Internet ou identificação do chamador. Leia também [como o Sentinela
funciona]({{HUB_COMO_FUNCIONA_URL}}), [o que ele não faz]({{HUB_O_QUE_NAO_FAZ_URL}}) e
[como bloquear chamadas desconhecidas]({{HUB_BLOQUEAR_DESCONHECIDO_URL}}).

**Fontes:** [Anatel](https://www.gov.br/anatel/pt-br/assuntos/noticias/anatel-adota-iniciativas-para-proteger-consumidor-de-ligacoes-indesejadas),
[Não Me Perturbe](https://www.naomeperturbe.com.br/) e [Google Phone](https://support.google.com/phoneapp/answer/6325463?hl=pt-BR).

## 2. Como bloquear chamadas desconhecidas no Android

**Slug:** `/bloquear-numero-desconhecido/`  
**Intenção:** solução · TOFU/MOFU  
**Title:** Como bloquear chamadas desconhecidas no Android  
**Description:** Compare bloqueio nativo, silêncio e filtro por política antes de escolher como chamadas desconhecidas devem se comportar.  
**CTA:** Conheça o filtro local do Sentinela → `{{RICA_SENTINELA_URL}}`

“Desconhecido” pode significar duas coisas: um número que não está salvo na sua agenda ou uma
chamada sem identificação. O Android e os fabricantes tratam esses casos de formas diferentes,
por isso o resultado de um botão pode não ser o que você imaginou. Comece verificando a opção
nativa do app Telefone e a documentação do seu fabricante.

Se você bloquear números individualmente, terá controle fino, mas precisará repetir o trabalho
quando a origem mudar. Se silenciar desconhecidos, preservará o registro no aparelho, mas talvez
não perceba uma chamada legítima. Um filtro por origem coloca o trade-off à vista: contatos e
uma lista pessoal podem ter uma política, enquanto desconhecidos recebem outra.

No Sentinela, o modo filtro é o padrão e a decisão acontece localmente. A política padrão para
desconhecidos é bloquear, mas ela pode ser alterada para silenciar ou permitir. A lista pessoal
é a exceção explícita para números que ainda não estão na agenda. O modo discador é opcional e
reversível; ele não deve ser necessário para experimentar o filtro padrão.

Faça um teste com um número que você controla e confirme o comportamento no seu aparelho. Não
trate a experiência de outro modelo como garantia: papéis do sistema, Não Perturbe, operadora e
docs/marketing/DISTRIBUICAO.md
docs/marketing/METRICAS-E-EXPERIMENTOS.md
docs/marketing/README.md
docs/marketing/SEO-E-CANONICAIS.md
docs/INDEX.md
docs/PLANO-MARKETING-ORGANICO.md
scripts/verify-marketing-assets.sh
