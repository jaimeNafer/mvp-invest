CREATE TABLE titulares (
    id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT PRIMARY KEY,
    espaco_id BIGINT UNSIGNED NOT NULL,
    nome VARCHAR(160) NOT NULL,
    ativo BOOLEAN NOT NULL DEFAULT TRUE,
    criado_em DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    CONSTRAINT uk_titular_espaco UNIQUE (espaco_id, id),
    CONSTRAINT fk_titular_espaco FOREIGN KEY (espaco_id) REFERENCES espacos(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE instituicoes (
    id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT PRIMARY KEY,
    codigo VARCHAR(80) COLLATE utf8mb4_bin NOT NULL,
    nome VARCHAR(160) NOT NULL,
    pais_sede CHAR(2) NULL,
    ativo BOOLEAN NOT NULL DEFAULT TRUE,
    CONSTRAINT uk_instituicao_codigo UNIQUE (codigo)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE ativos (
    id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT PRIMARY KEY,
    codigo VARCHAR(80) COLLATE utf8mb4_bin NOT NULL,
    ticker VARCHAR(32) NOT NULL,
    nome VARCHAR(160) NOT NULL,
    tipo VARCHAR(6) NOT NULL,
    casas_decimais TINYINT UNSIGNED NOT NULL,
    ativo BOOLEAN NOT NULL DEFAULT TRUE,
    CONSTRAINT uk_ativo_codigo UNIQUE (codigo),
    CONSTRAINT ck_ativo_tipo CHECK (tipo IN ('FIAT', 'CRIPTO')),
    CONSTRAINT ck_ativo_casas CHECK (casas_decimais <= 18)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE contas (
    id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT PRIMARY KEY,
    espaco_id BIGINT UNSIGNED NOT NULL,
    titular_id BIGINT UNSIGNED NOT NULL,
    instituicao_id BIGINT UNSIGNED NULL,
    nome VARCHAR(120) NOT NULL,
    tipo VARCHAR(10) NOT NULL,
    ativo BOOLEAN NOT NULL DEFAULT TRUE,
    criado_em DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    CONSTRAINT uk_conta_titular UNIQUE (espaco_id, titular_id, id),
    CONSTRAINT uk_conta_nome UNIQUE (espaco_id, titular_id, nome),
    CONSTRAINT ck_conta_tipo CHECK (tipo IN ('CORRETORA', 'CARTEIRA', 'BANCO')),
    CONSTRAINT fk_conta_titular FOREIGN KEY (espaco_id, titular_id) REFERENCES titulares(espaco_id, id),
    CONSTRAINT fk_conta_instituicao FOREIGN KEY (instituicao_id) REFERENCES instituicoes(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
