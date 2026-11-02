CREATE TEMPORARY TABLE validacao_carteiras_v8 (
    registros BIGINT NOT NULL,
    CONSTRAINT ck_v8_exige_cadastros_vazios CHECK (registros = 0)
);
INSERT INTO validacao_carteiras_v8
SELECT (SELECT COUNT(*) FROM titulares) + (SELECT COUNT(*) FROM contas);
DROP TEMPORARY TABLE validacao_carteiras_v8;

DROP TABLE contas;
DROP TABLE titulares;

CREATE TABLE carteiras (
    id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT PRIMARY KEY COMMENT 'Identificador da carteira.',
    perfil_ref VARCHAR(120) COLLATE utf8mb4_bin NOT NULL COMMENT 'Referência do usuário proprietário da carteira.',
    nome VARCHAR(160) NOT NULL COMMENT 'Nome da carteira, único por usuário.',
    ativo BOOLEAN NOT NULL DEFAULT TRUE COMMENT 'Situação da carteira: 1 = ativa; 0 = arquivada.',
    criado_em DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6) COMMENT 'Data e hora de criação do cadastro em UTC.',
    CONSTRAINT uk_carteira_nome UNIQUE (perfil_ref, nome)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='Carteiras de investimento do usuário.';

CREATE TABLE contas (
    id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT PRIMARY KEY COMMENT 'Identificador da conta.',
    carteira_id BIGINT UNSIGNED NOT NULL COMMENT 'Carteira proprietária da conta.',
    instituicao_id BIGINT UNSIGNED NOT NULL COMMENT 'Instituição de custódia da conta.',
    criado_em DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6) COMMENT 'Data e hora de criação do cadastro em UTC.',
    CONSTRAINT uk_conta_carteira_instituicao UNIQUE (carteira_id, instituicao_id),
    CONSTRAINT fk_conta_instituicao FOREIGN KEY (instituicao_id) REFERENCES instituicoes(id),
    CONSTRAINT fk_conta_carteira FOREIGN KEY (carteira_id) REFERENCES carteiras(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='Contas de custódia do usuário.';

INSERT INTO instituicoes(codigo, nome, pais_sede, ativo)
SELECT 'binance', 'Binance', NULL, TRUE
WHERE NOT EXISTS (SELECT 1 FROM instituicoes WHERE codigo = 'binance');

INSERT INTO instituicoes(codigo, nome, pais_sede, ativo)
SELECT 'bitget', 'BitGet', NULL, TRUE
WHERE NOT EXISTS (SELECT 1 FROM instituicoes WHERE codigo = 'bitget');
