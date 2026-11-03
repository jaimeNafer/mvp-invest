# Backlog de evolução

Registro de complementos operacionais e oportunidades para depois do MVP. Cada item informa seu estado: requisito confirmado para o futuro, funcionalidade já prevista no roteiro ou oportunidade ainda a avaliar. Registro não implica execução imediata ou prioridade aprovada.

O [roteiro de entregas](../03-entregas.md) permanece a referência da sequência de execução. Ao retomar um item, avaliar utilidade, dependências e esforço. Funcionalidades já descritas no roteiro são vinculadas abaixo, sem fichas duplicadas.

## Complementos operacionais

| Funcionalidade | Estado | Valor e encaminhamento |
| --- | --- | --- |
| [Taxas da Binance pagas em BNB](taxas-em-bnb/README.md) | Confirmada para o futuro | Representar a taxa na moeda efetivamente debitada, com saldo e custo de BNB. Fora da próxima entrega. |
| [Transferências próprias com taxa de rede](../03-entregas.md#marco-3--livro-e-resultados-rastreáveis) | Prevista no Marco 3 | Alterar custódia entre instituições do mesmo usuário, preservando custo global e considerando a taxa. |
| [Retirada em BRL](../03-entregas.md#marco-3--livro-e-resultados-rastreáveis) | Prevista no Marco 3 | Registrar saída de dinheiro sem confundi-la com prejuízo. |
| [Conversões e outros pares](../03-entregas.md#marco-3--livro-e-resultados-rastreáveis) | Prevista no Marco 3 | Delimitar os casos que não forem cobertos pelas compras e vendas da próxima entrega. |
| [Conferência com extratos](../03-entregas.md#marco-4--conferência) | Prevista no Marco 4 | Investigar diferenças de quantidade, operações ausentes e taxas incorretas após as movimentações. |
| [Backup e recuperação comprovada](../03-entregas.md#marco-5--organização-fiscal-e-recuperação) | Prevista no Marco 5 | Recuperar o histórico cadastrado. Recomenda-se ensaiar recuperação antes do piloto com dados reais; antecipação ainda não é prioridade aprovada. |
| [Ordens com várias execuções e taxas distintas](ordens-com-multiplas-execucoes/README.md) | A avaliar | Verificar a necessidade com um extrato real, sem presumir consolidação automática. |
| [Importação CSV](../03-entregas.md#marco-6--validação-local) | Adiada até validar o fluxo | Reduzir o esforço manual quando necessário; escolher um formato concreto de corretora antes de planejar. |

Próxima entrega planejada: aporte em BRL, compra e venda, com taxas somente em uma das moedas da própria negociação. Estes complementos não ampliam essa fatia.

## Ideias para depois do MVP

| Ideia | Estado | Objetivo |
| --- | --- | --- |
| [Simulador de risco](simulador-de-risco/README.md) | Para avaliar após o MVP | Explorar hipóteses de risco e retorno de uma operação acompanhada no Lastro. |
