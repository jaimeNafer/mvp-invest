CREATE TEMPORARY TABLE catalogo_creditos_v15 (
    codigo VARCHAR(80) COLLATE utf8mb4_bin NOT NULL,
    ticker VARCHAR(32) NOT NULL,
    nome VARCHAR(160) NOT NULL
) DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

INSERT INTO catalogo_creditos_v15 VALUES
('cripto:BNB', 'BNB', 'BNB'),
('cripto:LISTA', 'LISTA', 'LISTA'),
('cripto:IO', 'IO', 'IO');

CREATE TEMPORARY TABLE validacao_creditos_v15 (
    divergencias BIGINT NOT NULL,
    CONSTRAINT ck_catalogo_creditos_v15_compativel CHECK (divergencias = 0)
);

INSERT INTO validacao_creditos_v15
SELECT COUNT(*) FROM ativos a JOIN catalogo_creditos_v15 c ON a.codigo = c.codigo OR a.ticker = c.ticker
WHERE a.codigo <> c.codigo OR BINARY a.ticker <> BINARY c.ticker
   OR BINARY a.tipo <> BINARY 'CRIPTO' OR a.casas_decimais <> 18;

INSERT INTO ativos(codigo, ticker, nome, tipo, casas_decimais, ativo)
SELECT c.codigo, c.ticker, c.nome, 'CRIPTO', 18, TRUE
FROM catalogo_creditos_v15 c WHERE NOT EXISTS (SELECT 1 FROM ativos a WHERE a.codigo = c.codigo);

DROP TEMPORARY TABLE validacao_creditos_v15;
DROP TEMPORARY TABLE catalogo_creditos_v15;
