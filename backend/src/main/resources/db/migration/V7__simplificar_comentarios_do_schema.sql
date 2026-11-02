ALTER TABLE titulares
    COMMENT = 'Titulares dos investimentos.',
    MODIFY COLUMN id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT
        COMMENT 'Identificador do titular.',
    MODIFY COLUMN perfil_ref VARCHAR(120) COLLATE utf8mb4_bin NOT NULL
        COMMENT 'Referência do perfil responsável pelo titular.',
    MODIFY COLUMN nome VARCHAR(160) NOT NULL
        COMMENT 'Nome do titular.',
    MODIFY COLUMN ativo BOOLEAN NOT NULL DEFAULT TRUE
        COMMENT 'Situação do titular: 1 = ativo; 0 = arquivado.',
    MODIFY COLUMN criado_em DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6)
        COMMENT 'Data e hora de criação do cadastro em UTC.';

ALTER TABLE instituicoes
    COMMENT = 'Instituições de custódia dos investimentos.',
    MODIFY COLUMN id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT
        COMMENT 'Identificador da instituição.',
    MODIFY COLUMN codigo VARCHAR(80) COLLATE utf8mb4_bin NOT NULL
        COMMENT 'Código único da instituição.',
    MODIFY COLUMN nome VARCHAR(160) NOT NULL
        COMMENT 'Nome da instituição.',
    MODIFY COLUMN pais_sede CHAR(2) NULL
        COMMENT 'Código do país da sede no padrão ISO 3166-1 alpha-2.',
    MODIFY COLUMN ativo BOOLEAN NOT NULL DEFAULT TRUE
        COMMENT 'Situação da instituição: 1 = ativa; 0 = inativa.';

ALTER TABLE ativos
    COMMENT = 'Catálogo de moedas e criptoativos.',
    MODIFY COLUMN id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT
        COMMENT 'Identificador do ativo.',
    MODIFY COLUMN codigo VARCHAR(80) COLLATE utf8mb4_bin NOT NULL
        COMMENT 'Código único do ativo.',
    MODIFY COLUMN ticker VARCHAR(32) NOT NULL
        COMMENT 'Símbolo de exibição do ativo.',
    MODIFY COLUMN nome VARCHAR(160) NOT NULL
        COMMENT 'Nome do ativo.',
    MODIFY COLUMN tipo VARCHAR(6) NOT NULL
        COMMENT 'Tipo do ativo: FIAT ou CRIPTO.',
    MODIFY COLUMN casas_decimais TINYINT UNSIGNED NOT NULL
        COMMENT 'Número de casas decimais da quantidade, de 0 a 18.',
    MODIFY COLUMN ativo BOOLEAN NOT NULL DEFAULT TRUE
        COMMENT 'Situação do ativo: 1 = ativo; 0 = inativo.';

ALTER TABLE contas
    COMMENT = 'Contas de custódia dos titulares.',
    MODIFY COLUMN id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT
        COMMENT 'Identificador da conta.',
    MODIFY COLUMN titular_id BIGINT UNSIGNED NOT NULL
        COMMENT 'Titular proprietário da conta.',
    MODIFY COLUMN instituicao_id BIGINT UNSIGNED NULL
        COMMENT 'Instituição de custódia da conta, quando informada.',
    MODIFY COLUMN nome VARCHAR(120) NOT NULL
        COMMENT 'Nome da conta, único por titular.',
    MODIFY COLUMN tipo VARCHAR(10) NOT NULL
        COMMENT 'Tipo de conta: CORRETORA, CARTEIRA ou BANCO.',
    MODIFY COLUMN ativo BOOLEAN NOT NULL DEFAULT TRUE
        COMMENT 'Situação da conta: 1 = ativa; 0 = arquivada.',
    MODIFY COLUMN criado_em DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6)
        COMMENT 'Data e hora de criação do cadastro em UTC.';
