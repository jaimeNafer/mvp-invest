CREATE TEMPORARY TABLE validacao_carteiras_v8 (
    registros BIGINT NOT NULL,
    CONSTRAINT ck_v8_exige_cadastros_vazios CHECK (registros = 0)
);
INSERT INTO validacao_carteiras_v8
SELECT (SELECT COUNT(*) FROM titulares) + (SELECT COUNT(*) FROM contas) + (SELECT COUNT(*) FROM instituicoes);
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
    perfil_ref VARCHAR(120) COLLATE utf8mb4_bin NOT NULL COMMENT 'Referência do usuário proprietário da conta.',
    instituicao_id BIGINT UNSIGNED NOT NULL COMMENT 'Instituição de custódia da conta.',
    criado_em DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6) COMMENT 'Data e hora de criação do cadastro em UTC.',
    CONSTRAINT uk_conta_usuario_instituicao UNIQUE (perfil_ref, instituicao_id),
    CONSTRAINT fk_conta_instituicao FOREIGN KEY (instituicao_id) REFERENCES instituicoes(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='Contas de custódia do usuário.';

CREATE TABLE carteira_contas (
    carteira_id BIGINT UNSIGNED NOT NULL COMMENT 'Carteira que reúne a conta.',
    conta_id BIGINT UNSIGNED NOT NULL COMMENT 'Conta vinculada à carteira.',
    PRIMARY KEY (carteira_id, conta_id),
    CONSTRAINT fk_vinculo_carteira FOREIGN KEY (carteira_id) REFERENCES carteiras(id),
    CONSTRAINT fk_vinculo_conta FOREIGN KEY (conta_id) REFERENCES contas(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='Contas que compõem cada carteira.';

INSERT INTO instituicoes(codigo, nome, pais_sede, ativo)
VALUES ('binance', 'Binance', NULL, TRUE), ('bitget', 'BitGet', NULL, TRUE);
