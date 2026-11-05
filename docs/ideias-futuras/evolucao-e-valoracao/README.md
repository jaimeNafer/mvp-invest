# Evolução dos saldos, valor de mercado e rentabilidade

Estado: **evoluções priorizadas por Jaime**, após a primeira [Visão geral a custo](../../18-visao-geral.md). Prioridade confirmada em 03/11/2026; cada complemento exige planejamento próprio.

Em 04/11/2026, Jaime priorizou a evolução do painel com **resultado realizado acumulado mensal**, resultado por ativo e conferência simples. Essa série não entrega a série de custo abaixo, nem valor de mercado ou rentabilidade. [Contrato atual](../../18-visao-geral.md).

| Ordem | Entrega | Valor e dependências |
| --- | --- | --- |
| 1 | Série mensal do custo dos saldos | Evolução dos custos com o mesmo motor e cortes históricos. Definir fechamento mensal e meses sem movimento; custo não é preço nem rentabilidade. |
| 2 | Valor de mercado | Comparar posições com preços por ativo/horário. Definir fonte, identidade, conversão BRL, cache, cobertura e tratamento de preços ausentes/desatualizados. Última negociação pessoal não é preço atual. |
| 3 | Rentabilidade | Metodologia aprovada e tratamento de aportes, retiradas, abertura e preços históricos. Separar resultado realizado, diferença de mercado e retorno percentual. |

Proposta para preços manuais/históricos: cotação em BRL por identidade de ativo, instante e origem, separada dos fatos. Cobertura ausente, parcial ou desatualizada deverá ser explícita; não substituir preço ausente por zero ou custo. Não adicionar provedor, tabelas ou cotações fictícias antecipadamente. Relacionado à oportunidade de [cotações históricas com proveniência](../historico-completo-corretoras/README.md), sem duplicar regras.

[Backlog](../README.md) · [Roteiro](../../03-entregas.md)
