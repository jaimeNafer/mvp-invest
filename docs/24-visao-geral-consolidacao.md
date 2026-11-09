# Visão Geral e consolidação financeira

O fluxo usa os registros importados e as associações aplicadas para projetar o livro financeiro. A consulta reconstrói a posição até a data final e filtra resultados e fluxos externos pelo período escolhido. O painel separa custo de aquisição, resultado realizado, capital movimentado e patrimônio estimado a mercado.

## Diagnóstico

Os subtotais conhecidos já chegavam à tela no campo `disponibilidade`, mas os cartões usavam os totais estritos. A composição exigia todos os custos e descartava a participação das posições conhecidas. O resumo e a série mensal somavam apenas operações com resultado completo, enquanto a visão por ativo também somava componentes conhecidos de operações parciais. A consulta patrimonial repetia a projeção do livro para cada dia.

## Contrato

`GET /api/v1/visao-geral` mantém os totais estritos, que podem ser nulos. Em `disponibilidade`, os campos `custo`, `resultado`, `taxas` e `aportesLiquidos` contêm `valorConhecido`, `status`, `componentesConhecidos`, `componentesDesconhecidos` e `motivo`. Os valores em BRL são decimais em texto. Os estados são `COMPLETO`, `PARCIAL` e `INDISPONIVEL`. Sem evidência suficiente para uma parcela conhecida, `valorConhecido` é nulo. Um componente desconhecido pode representar um grupo econômico sem cálculo, um registro sem classificação ou uma lacuna de cobertura.

O custo conhecido soma posições com quantidade disponível e custo apurado. A participação usa esse subtotal e a composição é identificada como parcial quando a cobertura é incompleta. O resultado conhecido usa as mesmas parcelas de negociação, moeda e taxa no total, na evolução mensal e no detalhamento por ativo. A taxa de uma operação conhecida conserva a convenção de ausência de taxa do livro para os formatos de importação já reconhecidos. Uma taxa identificada sem valor continua em aberto.

O contador considera fatos aplicados, incluindo a abertura que já fazia parte do contrato, aportes, retiradas, compras, vendas, conversões, rendimentos e grupos econômicos identificados que ainda não possuem cálculo. Cada grupo entra uma vez. Transferências próprias, movimentações neutras e linhas isoladas sem identificação econômica ficam fora do contador.

Aportes líquidos são os fluxos classificados no livro como aportes e retiradas externas. Abertura, rendimentos, negociações e transferências próprias ficam fora. Um depósito ou uma saída de cripto com origem ou destino indeterminado torna a cobertura parcial, sem inferência de aporte ou retirada.

`GET /api/v1/visao-geral/patrimonio-diario` acrescenta `aportesLiquidosBrl` e `aportesLiquidosConhecidosBrl`, acumulados a partir do primeiro dia da consulta. Patrimônio continua usando quantidades históricas e preços diários disponíveis. Uma posição com quantidade indisponível não entra no subtotal de mercado. A série percorre o livro cronologicamente para registrar os cortes diários. Se uma transferência tiver pernas em lados diferentes de algum corte, usa a projeção por prefixos para preservar a regra existente de contraparte. O gráfico é solicitado ao abrir o painel e respeita o intervalo exibido.
