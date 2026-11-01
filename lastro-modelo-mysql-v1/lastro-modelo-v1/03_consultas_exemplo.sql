-- Parametros ilustrativos. A API deve usar queries parametrizadas e autorizacao.
USE lastro_mvp;
SET @espaco = 1;
SET @calculo = 1;
SET @inicio = '2026-01-01';
SET @fim = '2026-12-31';

-- 1. Status da base: um processamento antigo nao deve parecer atual.
SELECT c.id, c.data_limite, c.versao_motor, c.status,
       c.revisao_livro AS revisao_processada, e.revisao_livro AS revisao_atual,
       CASE WHEN c.status = 'CONCLUIDO' AND c.revisao_livro = e.revisao_livro
            THEN 'VIGENTE' ELSE 'REPROCESSAR_OU_CONFERIR' END AS situacao
FROM execucoes_calculo c JOIN espacos e ON e.id = c.espaco_id
WHERE c.espaco_id = @espaco AND c.id = @calculo;

-- 2. Carteira a CUSTO: nenhum preco de mercado e inferido.
SELECT t.nome AS titular, a.codigo, a.ticker, p.quantidade,
       p.custo_total_brl, p.preco_medio_brl, p.situacao_custo, c.data_limite
FROM posicoes_calculadas p
JOIN titulares t ON t.espaco_id=p.espaco_id AND t.id=p.titular_id
JOIN ativos a ON a.id=p.ativo_id
JOIN execucoes_calculo c ON c.espaco_id=p.espaco_id AND c.id=p.calculo_id
JOIN espacos e ON e.id=p.espaco_id
WHERE p.espaco_id=@espaco AND p.calculo_id=@calculo
  AND c.status='CONCLUIDO' AND c.revisao_livro=e.revisao_livro
ORDER BY t.nome,a.codigo;

-- 3. Mensal por titular. Pagamentos de imposto separados para nao multiplicar linhas.
-- Nao tratar ausencia de processamento ou resultado PENDENTE como zero certificado.
SELECT t.nome AS titular, DATE_FORMAT(o.data_local,'%Y-%m') AS mes,
       SUM(CASE WHEN o.tipo='VENDA' THEN r.receita_liquida_venda_brl ELSE 0 END) AS vendas_liquidas_brl,
       SUM(CASE WHEN o.tipo='VENDA' THEN r.custo_baixado_brl ELSE 0 END) AS custo_vendido_brl,
       SUM(r.resultado_venda_brl) AS resultado_vendas_brl,
       SUM(r.resultado_cambio_brl) AS cambio_conversoes_brl,
       SUM(r.resultado_rede_brl) AS resultado_rede_brl,
       SUM(r.resultado_total_brl) AS realizado_brl,
       SUM(CASE WHEN r.situacao='PENDENTE' THEN 1 ELSE 0 END) AS resultados_pendentes
FROM resultados_operacoes r
JOIN operacoes o ON o.espaco_id=r.espaco_id AND o.titular_id=r.titular_id AND o.id=r.operacao_id
JOIN titulares t ON t.espaco_id=o.espaco_id AND t.id=o.titular_id
JOIN execucoes_calculo c ON c.espaco_id=r.espaco_id AND c.id=r.calculo_id
JOIN espacos e ON e.id=r.espaco_id
WHERE r.espaco_id=@espaco AND r.calculo_id=@calculo
  AND o.data_local BETWEEN @inicio AND @fim
  AND c.status='CONCLUIDO' AND c.revisao_livro=e.revisao_livro
GROUP BY t.id,t.nome,DATE_FORMAT(o.data_local,'%Y-%m') ORDER BY t.nome,mes;

-- 4. Pagamentos ja realizados. Nao e imposto devido nem imposto calculado.
SELECT t.nome, o.data_local AS pago_em, p.competencia, p.quantidade,
       a.ticker AS moeda, p.cotacao_brl, p.valor_pago_brl,
       p.origem_pagamento,p.codigo_receita,p.referencia_documento
FROM pagamentos_imposto p
JOIN operacoes o ON o.espaco_id=p.espaco_id AND o.titular_id=p.titular_id AND o.id=p.operacao_id
JOIN titulares t ON t.espaco_id=p.espaco_id AND t.id=p.titular_id
JOIN ativos a ON a.id=p.moeda_id
WHERE p.espaco_id=@espaco AND o.status='CONFIRMADA'
  AND o.data_local BETWEEN @inicio AND @fim ORDER BY t.nome,o.data_local;

-- 5. Comparacao de quantidade na MESMA data e revisao do livro.
-- Falta de calculo/conclusao NAO recebe COALESCE para virar coincidencia falsa.
SELECT cs.id,t.nome,ct.nome AS conta,a.ticker,cs.data_saldo,cs.quantidade_extrato,
       CASE WHEN c.id IS NULL THEN NULL ELSE COALESCE(sc.quantidade,0) END AS quantidade_livro,
       CASE WHEN c.id IS NULL THEN NULL ELSE COALESCE(sc.quantidade,0)-cs.quantidade_extrato END AS diferenca,
       CASE WHEN c.id IS NULL THEN 'AGUARDA_CALCULO'
            WHEN ABS(COALESCE(sc.quantidade,0)-cs.quantidade_extrato) <= 0.000000000000000001
                 THEN 'QUANTIDADE_COINCIDE' ELSE 'CONFERIR_DIFERENCA' END AS situacao
FROM conciliacoes_saldo cs
JOIN titulares t ON t.espaco_id=cs.espaco_id AND t.id=cs.titular_id
JOIN contas ct ON ct.espaco_id=cs.espaco_id AND ct.titular_id=cs.titular_id AND ct.id=cs.conta_id
JOIN ativos a ON a.id=cs.ativo_id
JOIN espacos e ON e.id=cs.espaco_id
LEFT JOIN execucoes_calculo c ON c.espaco_id=cs.espaco_id AND c.id=@calculo
 AND c.data_limite=cs.data_saldo AND c.status='CONCLUIDO' AND c.revisao_livro=e.revisao_livro
LEFT JOIN saldos_contas sc ON sc.espaco_id=cs.espaco_id AND sc.calculo_id=c.id
 AND sc.titular_id=cs.titular_id AND sc.conta_id=cs.conta_id AND sc.ativo_id=cs.ativo_id
WHERE cs.espaco_id=@espaco;

-- 6. Comparativo anual. Informe calculos vigentes para as duas datas de fechamento.
-- @calculo_anterior permanece NULL na fixture: ausencia de historico NAO e custo zero.
SET @calculo_anterior = NULL;
SET @ano = 2026;
WITH chaves AS (
  SELECT espaco_id,titular_id,ativo_id FROM posicoes_calculadas
  WHERE espaco_id=@espaco AND calculo_id=@calculo
  UNION
  SELECT espaco_id,titular_id,ativo_id FROM posicoes_calculadas
  WHERE espaco_id=@espaco AND calculo_id=@calculo_anterior
)
SELECT t.nome,a.ticker,pa.quantidade AS quantidade_anterior,pa.custo_total_brl AS custo_anterior,
       pf.quantidade AS quantidade_final,pf.custo_total_brl AS custo_final,
       CASE WHEN pa.ativo_id IS NULL THEN 'SEM_POSICAO_ANTERIOR_REGISTRADA' ELSE pa.situacao_custo END AS situacao_anterior,
       pf.situacao_custo AS situacao_final
FROM chaves k
JOIN titulares t ON t.espaco_id=k.espaco_id AND t.id=k.titular_id
JOIN ativos a ON a.id=k.ativo_id
JOIN espacos e ON e.id=k.espaco_id
LEFT JOIN execucoes_calculo ca ON ca.espaco_id=k.espaco_id AND ca.id=@calculo_anterior
 AND ca.status='CONCLUIDO' AND ca.revisao_livro=e.revisao_livro
 AND ca.data_limite=STR_TO_DATE(CONCAT(@ano-1,'-12-31'),'%Y-%m-%d')
LEFT JOIN execucoes_calculo cf ON cf.espaco_id=k.espaco_id AND cf.id=@calculo
 AND cf.status='CONCLUIDO' AND cf.revisao_livro=e.revisao_livro
 AND cf.data_limite=STR_TO_DATE(CONCAT(@ano,'-12-31'),'%Y-%m-%d')
LEFT JOIN posicoes_calculadas pa ON pa.espaco_id=k.espaco_id AND pa.calculo_id=ca.id
 AND pa.titular_id=k.titular_id AND pa.ativo_id=k.ativo_id
LEFT JOIN posicoes_calculadas pf ON pf.espaco_id=k.espaco_id AND pf.calculo_id=cf.id
 AND pf.titular_id=k.titular_id AND pf.ativo_id=k.ativo_id
WHERE cf.id IS NOT NULL ORDER BY t.nome,a.ticker;
