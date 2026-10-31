# Lastro v3 — operações sob controle, carteira com clareza

> Registre uma vez. Entenda custo, resultado e saúde da carteira sem reconstruir tudo em uma planilha.

O MVP foi simplificado para priorizar a principal dor do usuário: **registrar operações e entender lucro, prejuízo, taxas, preço médio e patrimônio**, com uma base exportável para apoiar a organização fiscal.

A estratégia de cada ativo continua no detalhe da posição. O centro do produto agora é um livro manual que alimenta os consolidados automaticamente.

**Protótipo local, com exemplos fictícios e preços de mercado simulados.** O motor calcula resultados gerenciais a partir dos lançamentos; não calcula imposto devido e não emite DARF. O plano atualizado está em [PLAN.md](PLAN.md).

## O que você consegue fazer

- Registrar compras, vendas, conversões e taxas, com quantidade e moeda originais.
- Informar cotação histórica para converter cada operação em reais.
- Manter saldos e custo de USDC, USD e outros ativos usados nas negociações.
- Separar titulares e consolidar a família sem misturar suas bases de custo.
- Registrar saldos iniciais, aportes, retiradas, transferências próprias e impostos pagos.
- Ver preço médio, custo em aberto, resultado realizado e resultado não realizado.
- Extrair negociações, vendas com lucro/prejuízo/neutras, taxas, impostos e base mensal.
- Revisar tese, invalidação, objetivo de parcial e risco da posição aberta.
- Exportar CSVs e um backup completo JSON; restaurar backups do próprio Lastro.

Catálogo de entrada atual: BTC, ETH, SOL, USDC, USDT, BNB, BRL e USD. Importação de extratos e integração com corretoras são próximas etapas, não funções já disponíveis.

## Primeiro uso

A aplicação abre com registros demonstrativos separados entre dois titulares. Eles não representam a carteira real de ninguém.

1. Abra **Operações** e confira uma venda de exemplo no botão **Ver**.
2. Observe as parcelas de resultado, o custo baixado e as taxas.
3. Na **Visão geral**, veja o preço médio e o saldo derivados do livro.
4. Clique numa posição para ver estratégia, contexto e operações relacionadas.
5. Na **Base fiscal**, consulte o resumo mensal e as pendências de cadastro.
6. Use **Dados e backup** para exportar os exemplos ou iniciar um livro vazio.

Para começar seu próprio livro, primeiro registre aportes ou saldos anteriores com custo conhecido. Uma compra exige saldo da moeda de pagamento na conta selecionada; uma venda exige saldo do ativo. O sistema recusa saídas sem saldo, inclusive quando uma edição retroativa invalida operações posteriores.

## Um exemplo simples de preço médio

Suponha uma carteira em reais:

| Evento | Valor |
| --- | ---: |
| Compra de 2 unidades a R$ 1.000, taxa R$ 20 | Custo R$ 2.020 |
| Compra de 1 unidade a R$ 1.300, taxa R$ 10 | Custo adicional R$ 1.310 |
| Saldo antes da venda | 3 unidades, custo R$ 3.330, PM R$ 1.110 |
| Venda de 1 unidade por R$ 1.500, taxa R$ 15 | Líquido R$ 1.485 |
| Resultado realizado da venda | R$ 375 |
| Saldo restante | 2 unidades, custo R$ 2.220, PM R$ 1.110 |

As taxas já estão no custo ou no líquido. A coluna **Taxas registradas** permite consultar o gasto, sem descontá-lo novamente do resultado.

## Como o livro é calculado

### Ordem e titularidade

As operações são reprocessadas por data/hora em Brasília, sequência de cadastro e identificador. A média móvel ponderada é calculada por **titular + ativo**, consolidando as contas desse titular. Custódia é mantida separada por conta/carteira.

Essa é uma política de controle gerencial. Não determina, por si só, a política exigida em cada regime tributário.

### Compra e venda

- **Compra:** preço × quantidade × cotação histórica determina o valor bruto em BRL. Taxas em moeda de negociação, BRL ou terceiro token são incorporadas ao custo; a moeda usada no pagamento tem seu custo proporcional baixado.
- **Taxa no ativo comprado:** reduz a quantidade recebida, mantendo o custo total pago. O preço médio reflete a quantidade líquida.
- **Venda:** receita líquida em BRL menos custo proporcional baixado resulta no ganho/prejuízo gerencial do ativo.
- **Taxa no ativo vendido:** baixa adicional da quantidade e do custo do próprio ativo.
- **Taxa em terceiro token:** exige saldo anterior desse token e sua cotação em BRL; a perna de disposição e seu custo ficam detalhados.

Cada venda parcial conta como uma venda, não como encerramento integral de uma posição. As contagens de lucro, prejuízo e zero são das vendas registradas; negociações incluem compras, vendas e conversões.

### USDC, câmbio e conversões

Vender BTC por USDC gera resultado do BTC e estabelece custo em reais para os USDC recebidos. Utilizar esses USDC depois pode gerar diferença entre seu valor no momento e seu custo histórico.

Por isso, o app separa:

1. resultado das vendas de ativos;
2. conversões/variação em BRL das moedas utilizadas ou retiradas;
3. resultado das taxas pagas em ativos e despesas de rede.

Uma compra pode mostrar variação realizada da moeda utilizada, sem que o BTC recém-comprado tenha sido vendido. Essa separação evita confundir moedas e evita duplicar o resultado.

**Conversão é um único lançamento:** informe ativo recebido, quantidade recebida, moeda/ativo entregue e unidades entregues por unidade recebida. O motor deriva ambas as pernas. Não registre também uma compra e venda para a mesma conversão.

### Aportes, saldos e transferências

| Tipo | Tratamento no MVP |
| --- | --- |
| Saldo inicial | Quantidade anterior e custo histórico total conhecido. Não é aporte novo nem lucro. |
| Aporte | BRL ou USD que entram na conta. Não é lucro. |
| Retirada | BRL ou USD que saem; a moeda pode gerar variação gerencial em reais. Não é venda de cripto por si só. |
| Transferência própria | Move quantidade entre contas do mesmo titular sem mudar média; eventual taxa reduz o resultado. |
| Imposto pago | Registro manual do valor, competência, referência e origem do pagamento. |

Para entrada de cripto já existente, registre saldo inicial com custo histórico e documentação. Não use isso para simular rendimentos ou operações não suportadas. Transferências entre titulares não são suportadas.

As taxas de saldo inicial, aporte, retirada e imposto não são registradas nesses tipos nesta versão. Taxas de rede pertencem ao tipo transferência. Não registre uma conversão BRL/USDC como retirada ou aporte para contornar custos.

### Impostos pagos

Escolha se o pagamento foi **fora da carteira**, para só registrar a despesa informada, ou se deve **debitar o saldo da conta**. A competência e o comprovante ficam separados da data do pagamento.

O consolidado apresenta impostos no mês do pagamento. O indicador “realizado após impostos informados” é gerencial, pois o pagamento pode se referir a outro período.

## Visão geral

Seis cards mostram patrimônio, realizado, aberto, negociações, taxas e impostos pagos. A tabela de carteira apresenta:

- titular e ativo;
- quantidade;
- preço médio em reais;
- custo em aberto;
- preço de referência e valor marcado;
- lucro/prejuízo não realizado;
- saúde da posição e contexto simulado, quando há estratégia.

Filtros de titular e período afetam consolidados. Saldos são reconstruídos até a data final. Compras anteriores ao período continuam compondo o custo das vendas no período.

### Evolução patrimonial

O gráfico reconstrói patrimônio nas datas dos registros, usando as últimas referências disponíveis em cada ponto. O último ponto usa preços simulados quando a data final é 31/10/2026 ou posterior.

Uma segunda curva mostra saldos iniciais a custo + aportes − retiradas. Ela ajuda a perceber que entrada de capital não é lucro. Não é um histórico de preços diário, nem cálculo de retorno ponderado por tempo ou dinheiro.

Para datas anteriores à referência demonstrativa, a carteira usa os últimos preços registrados até aquela data. Não há avaliação histórica real do contexto.

## Saúde das posições

O detalhe mantém plano e contexto separados, com tese breve, invalidação, objetivo de parcial e limite de risco. Preço médio e quantidade vêm do livro, sem edição direta na estratégia.

Os níveis do plano são em USD. A avaliação de risco os converte pelo dólar simulado atual e compara com o custo médio em BRL e o patrimônio do titular. É uma estimativa; não garante execução ou perda máxima e não inclui taxas futuras, slippage ou gaps.

Os cenários de contexto continuam fictícios: misto, estresse, recuperação, perda de suporte e objetivo alcançado. Trocar cenário muda a marcação da carteira, mas preserva as cotações históricas usadas no custo.

Registrar uma revisão não envia ordens, não altera saldos e não remove alertas. Uma venda de verdade precisa ser registrada no livro.

## Base de apoio fiscal

A tela consolida mensalmente negociações, vendas brutas, resultado das vendas, outras parcelas realizadas, taxas, impostos pagos, aportes e retiradas. Sinaliza instituição sem localização, falta de referência externa e saldo inicial com custo zero declarado.

O usuário informa Brasil/exterior com base na entidade/documentação da instituição. A marca comercial da corretora não determina essa informação automaticamente.

**O MVP não determina alíquota, isenção, regime tributário, compensação, código, vencimento, imposto devido ou obrigação declaratória. Não gera DARF.** Os dados gerenciais precisam de conferência e classificação adequada para uso fiscal. As pendências da tela não constituem auditoria completa.

| Exportação | Conteúdo |
| --- | --- |
| Operações CSV | Dados originais, câmbio, taxas, custos, resultados, pernas derivadas, titular, instituição e referências. |
| Carteira CSV | Quantidade, média, custo, marcação, resultado e custódia na data final. |
| Base mensal CSV | Consolidados por mês e por titular, mantendo bases separadas na visão família. |
| Backup JSON | Todo o livro, titulares, estratégias e revisões; independente dos filtros da tela. |

CSV utiliza UTF-8 com BOM, separador ponto-e-vírgula e vírgula decimal. Textos com prefixos de fórmulas são neutralizados para uso em planilhas. Os arquivos não substituem os comprovantes de origem.

## Executar e verificar

Sem dependências nem build. Com Python 3, na pasta dos arquivos públicos:

```sh
python -m http.server 8000
```

Abra [http://localhost:8000](http://localhost:8000).

Com Node.js, na raiz do repositório:

```sh
node --check app.js
node --check ledger.js
node tests/ledger.test.cjs
```

No checkout do Sites, os arquivos públicos ficam em dist/. O teste aceita ambas as estruturas.

```text
index.html              Estrutura da aplicação
styles.css              Layout responsivo
ledger.js               Motor contábil gerencial, sem DOM
app.js                  Interface, formulários, exportação e persistência
tests/ledger.test.cjs    Casos numéricos e validações
PLAN.md                 Escopo revisado e evolução do MVP
README.md               Instruções e critérios de cálculo
```

O teste cobre média ponderada, vendas parciais, moedas históricas, conversões, taxas em moeda de negociação/ativo/terceiro token, transferência, titulares separados, custódia, períodos parciais, aportes, retiradas, impostos, saldo insuficiente, datas e CSV.

## Persistência e limites

A v3 usa lastro-v3 no localStorage. Os dados de v1/v2 permanecem separados, sem migração automática: um saldo estático sem custo e histórico não deve virar um livro artificialmente.

O backup JSON é validado antes da confirmação de restauração. Ele substitui a v3 local e não importa relatórios de corretoras. Exporte antes de restaurar, limpar o livro ou trocar de dispositivo.

Limites atuais:

- preços e contexto de mercado simulados, sem cotação ao vivo;
- entrada manual e catálogo limitado;
- sem contas de usuário, banco ou histórico de edição em servidor;
- quantidade e valores usam Number do JavaScript, não aritmética decimal financeira certificada;
- sem derivativos, short, DeFi/LP, rendimentos, eventos societários ou transferências entre titulares;
- motor gerencial não homologado para apuração fiscal; concilie saldos e custos com comprovantes.

**Próxima prioridade:** validar o esforço e a confiabilidade do lançamento manual com extratos conciliáveis. Importação e regras fiscais versionadas só entram depois que essa base estiver sólida.
