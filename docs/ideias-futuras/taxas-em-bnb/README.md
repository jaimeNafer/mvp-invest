# Taxas da Binance pagas em BNB

Registrada por solicitação de Jaime em 03/11/2026. Estado: **confirmada para o futuro**, fora da próxima entrega de aporte, compra e venda. A sequência de execução será definida no [roteiro](../../03-entregas.md).

## Objetivo

Representar compras e vendas cuja taxa seja efetivamente paga em BNB. O lançamento deve refletir o débito real, sem substituir artificialmente a moeda da taxa por BRL, USDT, USDC ou pelo ativo negociado.

## Dependências

- BNB disponível no catálogo de ativos.
- Saldo de BNB na instituição e custo histórico conhecidos, com possibilidade de registrar sua posição inicial.
- Motor de negociações com prévia, confirmação, custódia, custo histórico e parcelas de resultado.
- Definição da fonte e da representação do valor histórico da taxa em BRL ao detalhar esta entrega futura.

## Comportamento esperado

Debitar a quantidade da taxa do saldo de BNB do usuário na instituição correta, consumir o custo histórico correspondente e considerar o valor da taxa em BRL no cálculo da negociação. Explicar as parcelas envolvidas sem descontar a mesma taxa duas vezes.

## Critérios de aceite futuros

- Saldo insuficiente de BNB recusa a operação inteira, sem efeitos parciais na negociação ou na taxa.
- Prévia e confirmação apresentam efeitos coerentes no BNB e nas moedas negociadas.
- Correção e cancelamento preservam versões e recalculam os efeitos sobre o histórico, recusando alterações que invalidem saídas posteriores.
- Preservar precisão decimal, rastreabilidade e isolamento entre usuários; carteiras sobrepostas não duplicam fatos.
- Quantidade, moeda e valor histórico da taxa podem ser conferidos com os documentos da operação.

## Limites

Não implementar nesta próxima entrega. Até haver suporte, operações com taxa em BNB ficam fora do recorte atendido; não registrá-las como se a taxa tivesse sido paga em outra moeda. Esta ficha não define classes, tabelas, integrações ou regras tributárias.

[Índice do backlog](../README.md)
