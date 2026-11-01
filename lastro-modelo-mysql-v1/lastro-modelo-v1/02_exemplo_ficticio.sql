-- Executar SOMENTE em uma instalacao nova, depois de 01_schema_lastro.sql.
-- IDs fixos para facilitar estudo. Nao e carga de producao nem dados pessoais reais.
USE lastro_mvp;
SET time_zone = '+00:00';
START TRANSACTION;
INSERT INTO usuarios (id,nome,email,auth_emissor,auth_sujeito) VALUES
(1,'Usuario de demonstracao','demo@example.invalid','https://auth.example.invalid','demo-1');
INSERT INTO espacos (id,nome,criado_por,revisao_livro) VALUES (1,'Familia demonstracao',1,3);
INSERT INTO membros_espaco (espaco_id,usuario_id,papel) VALUES (1,1,'ADMIN');
INSERT INTO titulares (id,espaco_id,nome) VALUES (1,1,'Titular A'),(2,1,'Titular B');
INSERT INTO instituicoes (id,codigo,nome,pais_sede) VALUES (1,'demo-br','Corretora ficticia','BR');
INSERT INTO ativos (id,codigo,ticker,nome,tipo,casas_decimais) VALUES
(1,'fiat:BRL','BRL','Real','FIAT',2),(2,'crypto:BTC','BTC','Bitcoin','CRIPTO',8),(3,'crypto:USDC','USDC','USD Coin','CRIPTO',6);
INSERT INTO contas (id,espaco_id,titular_id,instituicao_id,nome,tipo) VALUES
(1,1,1,1,'Corretora A','CORRETORA'),(2,1,2,1,'Corretora B','CORRETORA'),(3,1,1,NULL,'Carteira propria','CARTEIRA');
-- Aporte 10.000 BRL. Compra .01 BTC por 4.000 BRL + taxa 10 BRL.
-- Venda .004 BTC por 1.800 BRL - taxa 5 BRL. Todos os movimentos sao fatos brutos.
INSERT INTO operacoes (id,espaco_id,titular_id,conta_principal_id,tipo,status,ocorreu_em_utc,data_local,ordem_livro,ativo_preco_id,moeda_preco_id,preco_unitario,pais_instituicao,referencia_externa,idempotencia,criado_por) VALUES
(1,1,1,1,'APORTE','CONFIRMADA','2026-08-01 13:00:00','2026-08-01',1,NULL,NULL,NULL,'BR','DEMO-APORTE',UNHEX(REPEAT('01',32)),1),
(2,1,1,1,'COMPRA','CONFIRMADA','2026-08-02 13:00:00','2026-08-02',2,2,1,400000,'BR','DEMO-COMPRA',UNHEX(REPEAT('02',32)),1),
(3,1,1,1,'VENDA','CONFIRMADA','2026-09-01 13:00:00','2026-09-01',3,2,1,450000,'BR','DEMO-VENDA',UNHEX(REPEAT('03',32)),1);
INSERT INTO movimentos (id,espaco_id,titular_id,operacao_id,conta_id,ativo_id,papel,direcao,quantidade,cotacao_brl,valor_referencia_brl,fonte_cotacao) VALUES
(1,1,1,1,1,1,'CAIXA','ENTRADA',10000,1,10000,'Exemplo ficticio'),
(2,1,1,2,1,2,'ATIVO','ENTRADA',0.01,400000,4000,'Preco da compra'),
(3,1,1,2,1,1,'LIQUIDACAO','SAIDA',4000,1,4000,'BRL'),
(4,1,1,2,1,1,'TAXA','SAIDA',10,1,10,'BRL'),
(5,1,1,3,1,2,'ATIVO','SAIDA',0.004,450000,1800,'Preco da venda'),
(6,1,1,3,1,1,'LIQUIDACAO','ENTRADA',1800,1,1800,'BRL'),
(7,1,1,3,1,1,'TAXA','SAIDA',5,1,5,'BRL');
-- Resultados abaixo sao fixtures esperadas, preenchidas manualmente para estudo.
-- O DDL nao possui um motor que produza estes dados automaticamente.
INSERT INTO execucoes_calculo (id,espaco_id,revisao_livro,data_limite,versao_motor,status,concluido_em) VALUES
(1,1,3,'2026-12-31','fixture-gerencial-v1','CONCLUIDO','2026-11-01 13:00:00');
INSERT INTO resultados_operacoes (espaco_id,titular_id,calculo_id,operacao_id,receita_liquida_venda_brl,custo_baixado_brl,custo_incorporado_brl,resultado_venda_brl,resultado_cambio_brl,resultado_rede_brl,resultado_total_brl,situacao) VALUES
(1,1,1,1,NULL,0,10000,0,0,0,0,'CALCULADO'),
(1,1,1,2,NULL,NULL,4010,0,0,0,0,'CALCULADO'),
(1,1,1,3,1795,1604,NULL,191,0,0,191,'CALCULADO');
-- A taxa da compra incorpora custo no BTC; a da venda reduz receita liquida.
-- Perna TAXA nao reconhece novamente a mesma despesa nos resultados.
INSERT INTO resultados_movimentos (espaco_id,titular_id,calculo_id,operacao_id,movimento_id,custo_baixado_brl,custo_incorporado_brl,resultado_venda_brl,resultado_cambio_brl,resultado_rede_brl,situacao) VALUES
(1,1,1,1,1,0,10000,0,0,0,'CALCULADO'),
(1,1,1,2,2,0,4010,0,0,0,'CALCULADO'),
(1,1,1,2,3,4000,0,0,0,0,'CALCULADO'),
(1,1,1,2,4,10,0,0,0,0,'CALCULADO'),
(1,1,1,3,5,1604,0,191,0,0,'CALCULADO'),
(1,1,1,3,6,0,1800,0,0,0,'CALCULADO'),
(1,1,1,3,7,5,0,0,0,0,'CALCULADO');
INSERT INTO posicoes_calculadas (espaco_id,titular_id,calculo_id,ativo_id,quantidade,custo_total_brl,preco_medio_brl,situacao_custo) VALUES
(1,1,1,1,7785,7785,1,'CONHECIDO'),(1,1,1,2,0.006,2406,401000,'CONHECIDO');
INSERT INTO saldos_contas (espaco_id,titular_id,calculo_id,conta_id,ativo_id,quantidade) VALUES
(1,1,1,1,1,7785),(1,1,1,1,2,0.006);
INSERT INTO conciliacoes_saldo (id,espaco_id,titular_id,conta_id,ativo_id,data_saldo,quantidade_extrato,referencia_documento,informado_por) VALUES
(1,1,1,1,2,'2026-12-31',0.006,'EXTRATO FICTICIO - apenas teste',1);
COMMIT;
