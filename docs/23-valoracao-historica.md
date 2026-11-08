# Valoração histórica

O Lastro consulta preços públicos de candles spot da Binance e mantém um cache global de pares e cotações. A identidade de cada par é explícita no banco; não há paridade presumida entre stablecoins e dólar. O preço de abertura é usado com a granularidade horária para operações e diária para a série patrimonial. A busca aceita cotação anterior em até 24 horas e não utiliza cotações posteriores. Caminhos inversos e indiretos são limitados, sem ciclos, e suas cotações devem ter horários compatíveis.

O comando `POST /api/v1/conciliacao/cotacoes/atualizacoes` agenda os períodos necessários aos registros do usuário. O processamento executa fora das consultas da timeline, com períodos deduplicados, reserva temporária e tentativas limitadas. Quando há arquivo mensal, o conteúdo é conferido pelo SHA-256 publicado pela Binance; a API pública complementa períodos sem arquivo. O cache guarda versão, fonte, instante, granularidade e checksum quando disponível. Nenhuma chave de API é necessária.

O histórico apresenta o estado da valoração, valor em reais e, quando aplicável, fonte, instante e caminho. Valores de candle são identificados como históricos ou estimados conforme a proximidade do instante. A cotação ausente permanece indisponível. Os valores de uma operação não alteram linhas importadas nem os vínculos entre elas.

As rotas `PUT /api/v1/conciliacao/registros/{id}/valoracao` e `PUT /api/v1/conciliacao/associacoes/{numero}/valoracao` recebem `revisao`, `versao` e `valorTotalBrl` decimal como texto, além da chave de idempotência. Cada correção pertence ao usuário, preserva autoria e versão anterior e prevalece sobre preços do cache. A identidade econômica é verificada ao reaplicar a correção; alteração material das parcelas não transfere o valor anterior para uma operação diferente. A alteração recalcula os resultados dependentes nas consultas, sem modificar os originais nem confirmar propostas pendentes.

`GET /api/v1/visao-geral/patrimonio-diario` usa os mesmos filtros da visão geral e retorna, para cada data, valor total quando completo, subtotal conhecido, contagem de ativos com e sem preço e cobertura de quantidades. Lacunas não são apresentadas como zero. A data da posição usa o fuso de Fortaleza; candles diários usam o dia UTC da Binance. Este resultado é patrimônio estimado a preços históricos e não substitui o custo de aquisição.

O processamento é configurado em `lastro.valoracao.processamento-ativo` e `lastro.valoracao.intervalo-processamento`. A cobertura depende de mercados identificados e candles disponíveis; crédito anterior à listagem exige informação manual quando não houver outra evidência confiável.

Os pares são cadastrados a partir de uma lista explícita de ativos suportados pela implementação. Ativos fora dessa lista não são descobertos automaticamente e permanecem indisponíveis até que seu par seja identificado. A fila prioriza períodos horários usados nas operações antes dos períodos diários.

Na resolução entre caminhos válidos, prevalece aquele cuja perna mais antiga é mais recente. Se essa data empatar, prevalece o caminho mais curto. As consultas comparam cotações da mesma granularidade e fonte. Para o patrimônio, a posição fecha imediatamente antes da meia-noite em Fortaleza. A referência de mercado é a abertura do dia UTC que contém esse instante, limitada ao presente. Essa referência é uma estimativa de mercado para o fechamento patrimonial. A interface carrega até 366 datas ao abrir a seção.

A V17 distingue uma consulta iniciada antes de o mês fechar de uma concluída sobre um mês já fechado. O período parcial volta à fila quando chega o fechamento ou quando passa o intervalo de atualização. Um novo pedido explícito pode reabrir uma falha esgotada. A reserva e a unicidade por período continuam compartilhadas entre perfis.

Quando o valor efetivo é conhecido, ele prevalece sobre a correção manual e o cache. A API rejeita um valor manual incompatível. Na baixa parcial de custo, o livro mantém oito casas decimais com HALF_EVEN e consome o custo residual ao encerrar integralmente a posição.
