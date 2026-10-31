# Lastro — plano de produto revisado (MVP v3)

## Diretriz de produto

O primeiro valor é controlar operações com pouco esforço e responder rapidamente: quanto tenho, qual é meu custo, quanto realizei, quanto paguei em taxas e impostos, e como estão minhas posições abertas?

A tese permanece como camada de orientação do ativo. O livro de operações passa a ser a fonte dos saldos, custos e resultados.

## Público inicial e fluxo prioritário

Investidor individual em cripto que lança operações manualmente e deseja organizar os dados para acompanhamento e apoio à apuração/declaração. Titularidade é uma dimensão obrigatória, inclusive quando há visão consolidada da família.

Fluxo:

1. Criar titular e registrar saldos iniciais com custo conhecido ou aportes.
2. Registrar compra, venda ou conversão, incluindo taxas e cotação histórica.
3. Receber prévia de custo/resultado e salvar.
4. Consultar consolidados, preço médio e posições abertas.
5. Revisar a estratégia no detalhe do ativo sem modificar o histórico financeiro.
6. Exportar operações, custos e base mensal para conferência fiscal.

## Entrega deste MVP

| Camada | Conteúdo |
| --- | --- |
| Livro manual | Saldo inicial, aporte, compra, venda, conversão, retirada, transferência própria e imposto pago. |
| Contabilidade gerencial | Reprocessamento cronológico, média móvel ponderada por ativo/titular e baixa proporcional de custo. |
| Multimoeda | Quantidade e preço originais, conversão histórica em BRL e custo da moeda usada no pagamento. |
| Custódia | Saldos por conta/carteira e bloqueio de saldo negativo; preço médio consolidado por titular. |
| Visão geral | Patrimônio, realizado, aberto, negociações, vendas com lucro/prejuízo/neutras, taxas e impostos pagos. |
| Evolução | Patrimônio por datas de registro, separado de capital de referência. |
| Posições | Quantidade, preço médio, custo em aberto, preço de referência, valor marcado, resultado e saúde. |
| Estratégia | Invalidação, parcial, limite de risco e revisão registrada; sem execução de ordens. |
| Apoio fiscal | Exportações CSV detalhadas e mensais, identificação de lacunas e impostos pagos por competência informada. |
| Persistência | localStorage, backup JSON e restauração validada. |

## Critérios de aceite

- Compra altera quantidade e custo; taxas elegíveis são incorporadas uma única vez.
- Venda reduz quantidade/custo e gera resultado com base no histórico anterior.
- Saídas parciais preservam o custo unitário do saldo restante.
- Operações com USDC preservam seu próprio custo histórico em reais.
- Transferência própria não é tratada como venda; sua taxa é separada.
- Aporte, retirada e saldo inicial não são confundidos com lucro.
- Titulares não compartilham base de custo; contas do mesmo titular compartilham média gerencial por ativo.
- Filtro de período não apaga compras anteriores necessárias ao custo.
- Alteração ou exclusão é recusada se invalidar saldos posteriores.
- Exportações explicam as parcelas de resultado sem descontar taxas novamente.
- Registro de revisão não altera saldos ou alertas.
- Imposto pago é informado; não há obrigação tributária calculada ou DARF emitido.

## Próxima etapa: validar uso manual

Observar o próprio usuário cadastrando um conjunto conciliável com extrato, começando pelos saldos e custos anteriores. Conferir quantidade, custo, taxas, vendas e moedas recebidas com os comprovantes. Medir esforço por lançamento e dúvidas recorrentes.

O sucesso inicial é o usuário conseguir reconciliar e explicar os números, encontrar pendências e extrair a base sem refazer cálculos em outra planilha.

## Evolução posterior, condicionada ao uso

1. **Persistência de produção:** contas, banco, histórico de alterações, backups em servidor, aritmética decimal e reconciliação automatizada.
2. **Preços e posições:** cotações reais, histórico de mercado, fonte/horário e múltiplas moedas de apresentação. Hoje o preço de mercado é simulado.
3. **Importação de corretora:** adaptar CSVs para o mesmo esquema do livro, prévia, mapeamento de colunas, idempotência, referências externas e confirmação antes da escrita. A restauração JSON atual é backup do Lastro, não importador de corretora.
4. **Apoio fiscal ampliado:** validação contábil especializada, classificação por natureza/entidade/localização, regras versionadas por exercício e rastreabilidade das cotações. Só então avaliar cálculo assistido e geração de insumos específicos para DARF e IR.
5. **Saúde da posição:** fontes técnicas e macro reais e testes do motor de alertas. IA pode ajudar a estruturar teses futuramente, com decisão sempre do investidor.

## Fora do escopo atual

Alavancagem, derivativos, posições vendidas, staking, rendimentos, DeFi/LP, presentes, heranças, transferências entre titulares, corporate actions, compensação tributária, alíquotas e isenções automáticas. Não registrar tais eventos como compra ou aporte genérico para forçar um saldo.

## Limites técnicos a resolver antes de produção

Os cálculos usam Number do JavaScript, com tolerância para resíduos e arredondamento apenas na apresentação. Não representam um livro contábil certificado. A política de médias, taxas e reconhecimento cambial é gerencial e precisa ser validada para cada regime fiscal. Custódia exige saldo anterior completo; quantidade sem custo conhecido não pode gerar resultado confiável.

Uma tela simples depende de uma base auditável. O objetivo da v3 é provar essa ligação antes de expandir automações.
