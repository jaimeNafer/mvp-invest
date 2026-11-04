CREATE TEMPORARY TABLE catalogo_cripto_v11 (
    codigo VARCHAR(80) COLLATE utf8mb4_bin NOT NULL,
    ticker VARCHAR(32) NOT NULL,
    nome VARCHAR(160) NOT NULL
) DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

INSERT INTO catalogo_cripto_v11 VALUES
('cripto:SOL', 'SOL', 'Solana'),
('cripto:LINK', 'LINK', 'Chainlink'),
('cripto:NEAR', 'NEAR', 'NEAR Protocol'),
('cripto:LDO', 'LDO', 'Lido DAO'),
('cripto:ARKM', 'ARKM', 'Arkham'),
('cripto:RONIN', 'RONIN', 'Ronin'),
('cripto:PYTH', 'PYTH', 'Pyth Network'),
('cripto:TIA', 'TIA', 'Celestia');

CREATE TEMPORARY TABLE validacao_cripto_v11 (
    divergencias BIGINT NOT NULL,
    CONSTRAINT ck_catalogo_cripto_v11_compativel CHECK (divergencias = 0)
);

INSERT INTO validacao_cripto_v11
SELECT COUNT(*) FROM ativos a JOIN catalogo_cripto_v11 c ON a.codigo = c.codigo OR a.ticker = c.ticker
WHERE a.codigo <> c.codigo OR BINARY a.ticker <> BINARY c.ticker
   OR BINARY a.tipo <> BINARY 'CRIPTO' OR a.casas_decimais <> 18;

INSERT INTO ativos(codigo, ticker, nome, tipo, casas_decimais, ativo)
SELECT c.codigo, c.ticker, c.nome, 'CRIPTO', 18, TRUE
FROM catalogo_cripto_v11 c WHERE NOT EXISTS (SELECT 1 FROM ativos a WHERE a.codigo = c.codigo);

DROP TEMPORARY TABLE validacao_cripto_v11;
DROP TEMPORARY TABLE catalogo_cripto_v11;
