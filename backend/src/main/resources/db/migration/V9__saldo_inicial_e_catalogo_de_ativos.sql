CREATE TEMPORARY TABLE catalogo_saldo_v9 (
    codigo VARCHAR(80) COLLATE utf8mb4_bin NOT NULL,
    ticker VARCHAR(32) NOT NULL,
    nome VARCHAR(160) NOT NULL,
    tipo VARCHAR(6) NOT NULL,
    casas_decimais TINYINT UNSIGNED NOT NULL
) DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
INSERT INTO catalogo_saldo_v9 VALUES
('fiat:BRL', 'BRL', 'Real brasileiro', 'FIAT', 2),
('fiat:USD', 'USD', 'Dólar americano', 'FIAT', 2),
('cripto:BTC', 'BTC', 'Bitcoin', 'CRIPTO', 18),
('cripto:ETH', 'ETH', 'Ether', 'CRIPTO', 18),
('cripto:USDT', 'USDT', 'Tether USDt', 'CRIPTO', 18),
('cripto:USDC', 'USDC', 'USD Coin', 'CRIPTO', 18);
CREATE TEMPORARY TABLE validacao_ativos_v9 (
    divergencias BIGINT NOT NULL,
    CONSTRAINT ck_catalogo_saldo_compativel CHECK (divergencias = 0)
);
INSERT INTO validacao_ativos_v9
SELECT COUNT(*) FROM ativos a JOIN catalogo_saldo_v9 c ON a.codigo = c.codigo OR a.ticker = c.ticker
WHERE a.codigo <> c.codigo OR BINARY a.ticker <> BINARY c.ticker
   OR a.tipo <> c.tipo OR a.casas_decimais <> c.casas_decimais;
INSERT INTO ativos(codigo, ticker, nome, tipo, casas_decimais, ativo)
SELECT c.codigo, c.ticker, c.nome, c.tipo, c.casas_decimais, TRUE
FROM catalogo_saldo_v9 c WHERE NOT EXISTS (SELECT 1 FROM ativos a WHERE a.codigo = c.codigo);
DROP TEMPORARY TABLE validacao_ativos_v9;
DROP TEMPORARY TABLE catalogo_saldo_v9;

CREATE TABLE pontos_partida (
    perfil_ref VARCHAR(120) COLLATE utf8mb4_bin NOT NULL PRIMARY KEY COMMENT 'Referência do usuário proprietário do histórico.',
    data_referencia DATE NOT NULL COMMENT 'Data de início do acompanhamento financeiro.',
    criado_em DATETIME(6) NOT NULL COMMENT 'Data e hora de criação em UTC.',
    criado_por VARCHAR(120) COLLATE utf8mb4_bin NOT NULL COMMENT 'Referência do autor da criação.',
    alterado_em DATETIME(6) NOT NULL COMMENT 'Data e hora da última alteração em UTC.',
    alterado_por VARCHAR(120) COLLATE utf8mb4_bin NOT NULL COMMENT 'Referência do autor da última alteração.'
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='Início do acompanhamento financeiro por usuário.';

CREATE TABLE operacoes (
    id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT PRIMARY KEY COMMENT 'Identificador da versão do lançamento.',
    perfil_ref VARCHAR(120) COLLATE utf8mb4_bin NOT NULL COMMENT 'Referência do usuário proprietário do lançamento.',
    instituicao_id BIGINT UNSIGNED NOT NULL COMMENT 'Instituição de custódia do lançamento.',
    ativo_id BIGINT UNSIGNED NOT NULL COMMENT 'Ativo movimentado.',
    tipo VARCHAR(20) NOT NULL COMMENT 'Tipo de lançamento: SALDO_INICIAL.',
    data_referencia DATE NOT NULL COMMENT 'Data de negócio do lançamento.',
    ocorrida_em DATETIME(6) NOT NULL COMMENT 'Instante do lançamento em UTC.',
    quantidade DECIMAL(38,18) NOT NULL COMMENT 'Quantidade do ativo.',
    custo_total_brl DECIMAL(38,8) NOT NULL COMMENT 'Custo histórico total em reais.',
    situacao VARCHAR(12) NOT NULL COMMENT 'Situação: CONFIRMADA, SUBSTITUIDA ou CANCELADA.',
    confirmada TINYINT GENERATED ALWAYS AS (IF(situacao = 'CONFIRMADA', 1, NULL)) STORED COMMENT 'Marcador de unicidade da abertura confirmada.',
    substitui_id BIGINT UNSIGNED NULL COMMENT 'Versão anterior substituída pelo lançamento.',
    justificativa_custo_zero VARCHAR(300) NULL COMMENT 'Justificativa do custo histórico zero.',
    motivo_alteracao VARCHAR(300) NULL COMMENT 'Motivo da correção ou cancelamento.',
    chave_requisicao CHAR(36) CHARACTER SET ascii COLLATE ascii_bin NOT NULL COMMENT 'Chave de repetição segura da solicitação.',
    fingerprint CHAR(64) CHARACTER SET ascii COLLATE ascii_bin NOT NULL COMMENT 'Resumo dos dados normalizados da solicitação.',
    criado_em DATETIME(6) NOT NULL COMMENT 'Data e hora de criação em UTC.',
    criado_por VARCHAR(120) COLLATE utf8mb4_bin NOT NULL COMMENT 'Referência do autor da criação.',
    alterado_em DATETIME(6) NOT NULL COMMENT 'Data e hora da última alteração em UTC.',
    alterado_por VARCHAR(120) COLLATE utf8mb4_bin NOT NULL COMMENT 'Referência do autor da última alteração.',
    CONSTRAINT uk_abertura_confirmada UNIQUE (perfil_ref, instituicao_id, ativo_id, confirmada),
    CONSTRAINT uk_operacao_requisicao UNIQUE (perfil_ref, chave_requisicao),
    CONSTRAINT uk_operacao_substituida UNIQUE (substitui_id),
    CONSTRAINT fk_operacao_inicio FOREIGN KEY (perfil_ref) REFERENCES pontos_partida(perfil_ref),
    CONSTRAINT fk_operacao_instituicao FOREIGN KEY (instituicao_id) REFERENCES instituicoes(id),
    CONSTRAINT fk_operacao_ativo FOREIGN KEY (ativo_id) REFERENCES ativos(id),
    CONSTRAINT fk_operacao_anterior FOREIGN KEY (substitui_id) REFERENCES operacoes(id),
    CONSTRAINT ck_operacao_tipo CHECK (tipo = 'SALDO_INICIAL'),
    CONSTRAINT ck_operacao_situacao CHECK (situacao IN ('CONFIRMADA', 'SUBSTITUIDA', 'CANCELADA')),
    CONSTRAINT ck_abertura_quantidade CHECK (quantidade > 0),
    CONSTRAINT ck_abertura_custo CHECK (custo_total_brl >= 0),
    CONSTRAINT ck_abertura_zero CHECK (custo_total_brl > 0 OR COALESCE(CHAR_LENGTH(TRIM(justificativa_custo_zero)), 0) > 0)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='Lançamentos financeiros e suas versões.';
