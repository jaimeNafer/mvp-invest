# Visão geral dos investimentos

## Objetivo e alcance

Página inicial após o login, consolidando todas as instituições do usuário de IUsuarioAtual. Carteiras não criam fatos adicionais. Sem filtro de carteiras nem consolidação entre usuários. Custo não é valor de mercado: não apresentar preços atuais, lucro potencial ou rentabilidade sem fonte e metodologia.

## Período e cálculo

Datas inclusivas em America/Fortaleza; corte superior no início do dia seguinte, exclusivo, convertido para Instant. Padrão: data de referência até hoje; sem ponto de partida, hoje nas duas datas. Rejeitar datas inexistentes, anteriores ao ano 1000, futuras, intervalo invertido ou apenas uma data informada.

Recalcular todos os fatos atualmente confirmados até a data final, incluindo anteriores à data inicial para formar o custo. Somar resultados, taxas, aportes e contagem somente do período. SALDO_INICIAL conta como operação, mas não como aporte. Resultado = parcelas de negociação + moeda + taxa, conforme MotorLivro. Taxas são informativas e não são descontadas novamente.

Posições e composição incluem BRL e demais moedas. Percentual = custo do ativo / custo total × 100, quatro casas HALF_EVEN; arredondamentos independentes podem diferir ligeiramente de 100%. Total zero produz percentual null, preservando quantidades e custos. Período sem movimento pode manter posições anteriores. Sem fatos até a data final, informar ausência de registros, sem afirmar patrimônio zero antes da cobertura cadastrada.

Correções/cancelamentos refletem o histórico vigente, não as versões conhecidas no passado. Metadados inativos continuam legíveis.

## Contrato HTTP

GET /api/v1/visao-geral com dataInicio e dataFim opcionais (AAAA-MM-DD). Omitir ambos solicita o padrão. Identificação exclusivamente pelo Bearer.

| Campo | Conteúdo |
| --- | --- |
| revisao | Revisão do livro como string. |
| dataInicio, dataFim | Datas efetivas. |
| dataReferencia | Início da cobertura cadastrada, ou null. |
| possuiRegistrosAteFim | Existem fatos confirmados até o corte, mesmo sem posições restantes. |
| resumo | custoSaldosBrl, resultadoRealizadoBrl, taxasBrl, aportesBrl, operacoesNoPeriodo. |
| resultado | negociacaoBrl, moedaBrl, taxaBrl. |
| posicoes | ativoId, ativoTicker, ativoNome, quantidade, custoMedioBrl, custoTotalBrl, participacaoPercentual, instituicoes. |
| posicoes[].instituicoes | instituicaoId, instituicaoNome, quantidade. |
| dataSaldos | Data final das posições; distinta de dataReferencia, que é o início da cobertura. |
| saldosMoedas | ativoId, ativoTicker, ativoNome, quantidade, instituicoes; moedas fiat e USDT/USDC com posição. |
| evolucaoResultado | acumuladoInicialBrl e meses. |
| evolucaoResultado.meses[] | dataInicio, dataFim, resultadoBrl, acumuladoBrl, operacoes. |
| resultadosPorAtivo[] | ativoId, ativoTicker, ativoNome, negociacaoBrl, moedaBrl, taxaBrl, resultadoTotalBrl. |
| conferencia | custoZeroDeclarado e referenciasRepetidas; contagem dos registros envolvidos até dataSaldos. |

IDs/revisão/decimais são strings; contagem é inteiro; percentual pode ser null. Posições ordenadas por ticker/ID; custódias por nome/ID. Entrada inválida retorna 400 com mensagem da origem; Bearer ausente/inválido retorna 401. Contratos existentes preservados.

## Responsabilidades e consistência

Controller faz binding e @Valid; IVisaoGeralUseCase coordena contexto/leitura/período/motor/mapeamento. ConsolidadorVisaoGeral agrega resultados; mapper serializa e determina participação para apresentação. AtivoResumo acrescenta nome como metadado escalar interno. Catálogos consultados em lote, sem expor entidades ou enums de outro contexto.

Nova consulta com @Transactional(readOnly = true, isolation = REPEATABLE_READ): ponto, fatos e revisão usam o mesmo snapshot durante escrita concorrente, sem lock de escrita. Comandos existentes preservam READ_COMMITTED, bloqueios, revisão, idempotência e flush. Sem migrations ou alterações de dados.

React usa o cliente HTTP existente via services/visaoGeral.ts. Página coordena filtros e estados; composição/tabela apresentam a resposta. Consultas substituídas são canceladas; respostas canceladas não atualizam a tela. Falhas preservam filtros, sem apresentar totais antigos como sendo do período novo. Sem cálculo financeiro no navegador.

## Precisão e apresentação dos percentuais

A API e as barras preservam o percentual calculado com quatro casas. O texto usa duas casas decimais em pt-BR, com HALF_EVEN e aritmética BigInt no serviço de formatação, sem converter para ponto flutuante. O valor completo está disponível no tooltip. Percentuais positivos abaixo de 0,01% aparecem como < 0,01%, sem confundi-los com zero. Arredondamentos de apresentação não alteram cálculos, quantidades ou custos.

## Layout e verificação

Título/filtros na mesma linha em desktop; quatro indicadores baixos; faixa de saldos em moedas; série mensal e resultado por ativo lado a lado; composição e posições/custódia; faixa de aportes/parcelas e conferência compacta. Espaçamento/preenchimento de 12–16 px. Custódia adicional expansível por linha. Valores completos acessíveis, sem truncamento silencioso.

Controles mínimos de 36 px em desktop e 44 px no toque. Em telas menores, indicadores em duas colunas e conteúdo empilhado. Tabela pode rolar horizontalmente; sem áreas independentes de rolagem vertical. A evolução de 04/11/2026 substitui o aceite anterior de caber integralmente em 1366×768: os painéis adicionais permitem rolagem natural, preservando densidade e legibilidade. Não comprimir ou esconder dados para cumprir a altura anterior.

Verificar API/MySQL, arquitetura, custo anterior ao período, taxas, abertura, limites de datas, inativos, isolamento, versões, concorrência, cinco mil fatos sintéticos e estados React. Navegador sem fatos fictícios no banco pessoal. Evidências finais na retomada.

## Evolução mensal e resultado por ativo — 04/11/2026

A série mensal é de **resultado realizado líquido**, não de custo, patrimônio ou rentabilidade. Começa em zero no início do período selecionado. Todos os meses são emitidos, incluindo os limites parciais; meses sem operações têm resultado zero e conservam o acumulado. O último acumulado equivale exatamente a resumo.resultadoRealizadoBrl. A contagem mantém a mesma definição do resumo, incluindo aberturas e aportes, sem transformá-los em lucro.

AnaliseResultado agrega as parcelas já calculadas por MotorLivro, executado uma única vez até dataSaldos. Custos anteriores continuam formando o custo das vendas e moedas gastas. Não executar o motor novamente por mês, nem reproduzir regras de custo no frontend.

A realização de uma venda é atribuída ao ativo vendido: negociação e efeito das taxas. Na compra com USDT/USDC, a realização da moeda gasta e o efeito da taxa pertencem à moeda utilizada, não ao ativo adquirido. Compras em BRL, aportes e aberturas não criam linhas de realização. Um ativo com realização líquida zero permanece na lista, inclusive se sua posição já foi esgotada. Ordenação: resultado líquido decrescente, ticker e ID como desempate. A soma dos ativos e de cada parcela reconcilia com o resumo; taxas não são descontadas novamente.

Gráfico SVG sem dependência adicional, eixo de zero e perdas visíveis, valores mensais acessíveis em tabela expansível. Conversões para Number ficam restritas às coordenadas do gráfico; formatação e valores completos continuam usando strings decimais. Linhas de resultado usam sinal textual além da cor.

Saldos em moeda destacam quantidades fiat e USDT/USDC separadamente. Não somar moedas diferentes ou afirmar que a quantidade está livre de bloqueios na instituição: o sistema conhece somente os registros cadastrados. Posições completas e custódia continuam disponíveis na tabela.

## Conferência de registros

Os avisos são oportunidades de revisão, sem diagnóstico automático de erro:

- CUSTO_ZERO_DECLARADO: somente SALDO_INICIAL confirmado com custo explicitamente zero. A justificativa é exibida. Não confundir com custo desconhecido, que o cadastro rejeita, nem com custoTotalBrl nulo de negociações, derivado pelo motor.
- REFERENCIA_REPETIDA: referências não vazias exatamente iguais no mesmo usuário/instituição, respeitando maiúsculas e minúsculas. Conta os registros envolvidos, preservando multiplicidade; execuções legítimas podem compartilhar referência. Não cancelar nem deduplicar automaticamente.

A conferência considera versões atualmente confirmadas até dataSaldos, incluindo registros anteriores ao período dos resultados. Não detecta operações nunca cadastradas, destinos ou contrapartidas ausentes sem documentos externos. Não cria estado de pendência/resolução nem tabelas.

GET /api/v1/visao-geral/avisos:

| Parâmetro | Regra |
| --- | --- |
| dataFim | Obrigatória, AAAA-MM-DD, existente, entre o ano 1000 e hoje em Fortaleza. |
| tipo | Obrigatório: CUSTO_ZERO_DECLARADO ou REFERENCIA_REPETIDA. |
| pagina | Opcional, a partir de zero; padrão 0. |
| tamanho | Opcional, entre 1 e 50; padrão 10. |

Resposta: revisao, pagina, tamanho, total e itens com operacaoId, ocorridaEm (UTC), instituicaoNome, ativoTicker, referencia e justificativa. Ordenação por instante decrescente e ID crescente. Sem valores financeiros nos avisos ou logs. O detalhe reutiliza GET /api/v1/operacoes/{id} e mostra o estado atual do registro no momento da consulta.

Leitura dos avisos também usa REPEATABLE_READ. A consulta é feita somente ao abrir a lista. Revisão diferente da apresentada no painel exige atualizar o painel antes de exibir a lista; respostas canceladas não substituem resultados recentes. Detalhes, paginação e nova tentativa usam os componentes e cliente HTTP existentes. Parâmetros inválidos retornam 400 e identificação ausente/inválida retorna 401.

## Complementos futuros e cotações

Retiradas continuam como entrega separada no roteiro; não há esse tipo no modelo vigente. Esta entrega não implementa taxas em terceira moeda, conversões, transferências ou conferência automática de extratos.

A série mensal de **custo dos saldos** continua distinta e pendente. A prioridade atual passou a ser a série de resultado realizado acima. Valor de mercado e rentabilidade dependem de proposta própria. [Backlog](ideias-futuras/evolucao-e-valoracao/README.md).

Proposta simples para cotações históricas/manuais: valor em BRL por identidade de ativo, instante e origem rastreável, separado dos fatos financeiros. Uma futura consulta deverá informar cobertura ausente, parcial ou desatualizada; ausência de preço não será zero nem custo de aquisição. Valor de mercado e retorno terão indicadores próprios. Não criar agora tabelas, clients, provedores ou preços simulados.
