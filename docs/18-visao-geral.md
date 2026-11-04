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

IDs/revisão/decimais são strings; contagem é inteiro; percentual pode ser null. Posições ordenadas por ticker/ID; custódias por nome/ID. Entrada inválida retorna 400 com mensagem da origem; Bearer ausente/inválido retorna 401. Contratos existentes preservados.

## Responsabilidades e consistência

Controller faz binding e @Valid; IVisaoGeralUseCase coordena contexto/leitura/período/motor/mapeamento. ConsolidadorVisaoGeral agrega resultados; mapper serializa e determina participação para apresentação. AtivoResumo acrescenta nome como metadado escalar interno. Catálogos consultados em lote, sem expor entidades ou enums de outro contexto.

Nova consulta com @Transactional(readOnly = true, isolation = REPEATABLE_READ): ponto, fatos e revisão usam o mesmo snapshot durante escrita concorrente, sem lock de escrita. Comandos existentes preservam READ_COMMITTED, bloqueios, revisão, idempotência e flush. Sem migrations ou alterações de dados.

React usa o cliente HTTP existente via services/visaoGeral.ts. Página coordena filtros e estados; composição/tabela apresentam a resposta. Consultas substituídas são canceladas; respostas canceladas não atualizam a tela. Falhas preservam filtros, sem apresentar totais antigos como sendo do período novo. Sem cálculo financeiro no navegador.

## Precisão e apresentação dos percentuais

A API e as barras preservam o percentual calculado com quatro casas. O texto usa duas casas decimais em pt-BR, com HALF_EVEN e aritmética BigInt no serviço de formatação, sem converter para ponto flutuante. O valor completo está disponível no tooltip. Percentuais positivos abaixo de 0,01% aparecem como < 0,01%, sem confundi-los com zero. Arredondamentos de apresentação não alteram cálculos, quantidades ou custos.

## Layout e verificação

Título/filtros na mesma linha em desktop; quatro indicadores baixos; composição e tabela lado a lado; faixa compacta de aportes/parcelas. Espaçamento/preenchimento de 12–16 px. Custódia adicional expansível por linha. Valores completos acessíveis, sem truncamento silencioso.

Controles mínimos de 36 px em desktop e 44 px no toque. Em telas menores, indicadores em duas colunas e conteúdo empilhado. Tabela pode rolar horizontalmente; sem áreas independentes de rolagem vertical. A 1366×768, seis ativos e custódia resumida devem caber sem rolagem vertical; mais dados permitem rolagem natural.

Verificar API/MySQL, arquitetura, custo anterior ao período, taxas, abertura, limites de datas, inativos, isolamento, versões, concorrência, cinco mil fatos sintéticos e estados React. Navegador sem fatos fictícios no banco pessoal. Evidências finais na retomada.

## Evolução priorizada

Sequência confirmada por Jaime: série mensal do custo; valor de mercado com fonte/instante; rentabilidade com metodologia e fluxos. [Backlog](ideias-futuras/evolucao-e-valoracao/README.md). Sem implementar esses complementos nesta entrega.
