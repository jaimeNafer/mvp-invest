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
| [Complementos para históricos de corretoras](historico-completo-corretoras/README.md) | Parcialmente atendida; demais oportunidades a avaliar | Elegibilidade por catálogo entregue; Earn, distribuições, contas internas, pequenos saldos em BNB, valoração histórica, origem/destino, proveniência e eventos especiais. Dependências e evidências descritas na ficha; sem prioridade aprovada. |
| [Importação CSV](../03-entregas.md#marco-6--validação-local) | Adiada até validar o fluxo | Reduzir o esforço manual quando necessário; escolher um formato concreto de corretora antes de planejar. |

O ciclo de aporte em BRL, compra e venda foi entregue, com taxas somente em uma das moedas da própria negociação. Os complementos acima continuam sujeitos a planejamento próprio; o diagnóstico de históricos não amplia automaticamente o escopo atendido.

## Diagnóstico de históricos — 03/11/2026

Concluída análise documental de exportações de 2024–2026, aprofundando 2024, sem carga, limpeza ou alteração de produto. Identificadas sobreposições entre arquivos, repetições legítimas possíveis e linhas fora do período nominal. A seleção usa a data de cada movimento e a comparação preserva multiplicidade e proveniência.

As [novas oportunidades](historico-completo-corretoras/README.md) complementam os registros existentes de taxas em BNB, execuções, transferências, retiradas, conversões e importação. O diagnóstico não aprova prioridades nem comprova custo/saldo final. [Estado e próximos passos](../06-retomada.md#diagnóstico-documental-do-histórico-concluído--03112026).

## Ideias para depois do MVP

| Ideia | Estado | Objetivo |
| --- | --- | --- |
| [Simulador de risco](simulador-de-risco/README.md) | Para avaliar após o MVP | Explorar hipóteses de risco e retorno de uma operação acompanhada no Lastro. |

## Evolução do catálogo implementada

A ampliação controlada foi entregue conforme o [contrato de catálogo](../17-catalogo-de-ativos.md), com oito ativos iniciais adicionais. Novos criptoativos cadastrados não exigem outra lista de elegibilidade no Java/React. Demais produtos, importação e carga real continuam dependentes de planejamento e evidências.
