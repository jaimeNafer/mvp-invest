CREATE TABLE importacoes (
    id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT PRIMARY KEY COMMENT 'Identificador do recibo de importação.',
    perfil_ref VARCHAR(120) COLLATE utf8mb4_bin NOT NULL COMMENT 'Referência do usuário proprietário da importação.',
    chave_requisicao CHAR(36) CHARACTER SET ascii COLLATE ascii_bin NOT NULL COMMENT 'Chave de repetição segura da confirmação.',
    hash CHAR(64) CHARACTER SET ascii COLLATE ascii_bin NOT NULL COMMENT 'Resumo SHA-256 do arquivo original.',
    decisoes VARCHAR(64) NOT NULL COMMENT 'Confirmações de aportes próprios, taxas incluídas e posição anterior zerada.',
    nome_arquivo VARCHAR(255) NOT NULL COMMENT 'Nome do arquivo importado.',
    versao_leitor VARCHAR(40) NOT NULL COMMENT 'Versão do formato interpretado.',
    arquivo MEDIUMBLOB NOT NULL COMMENT 'Conteúdo original do arquivo confirmado.',
    criado_em DATETIME(6) NOT NULL COMMENT 'Instante da confirmação em UTC.',
    novos INT NOT NULL COMMENT 'Quantidade de operações registradas nesta confirmação.',
    reconhecidos INT NOT NULL COMMENT 'Quantidade de operações anteriormente registradas.',
    CONSTRAINT uk_importacao_chave UNIQUE (perfil_ref, chave_requisicao),
    CONSTRAINT uk_importacao_arquivo UNIQUE (perfil_ref, hash),
    INDEX ix_importacao_historico (perfil_ref, criado_em, id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='Recibos e arquivos confirmados de importação por usuário.';

CREATE TABLE importacoes_eventos (
    id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT PRIMARY KEY COMMENT 'Identificador do vínculo do evento importado.',
    importacao_id BIGINT UNSIGNED NOT NULL COMMENT 'Recibo que contém o evento.',
    linha INT NOT NULL COMMENT 'Linha do evento no arquivo de origem.',
    referencia VARCHAR(160) COLLATE utf8mb4_bin NOT NULL COMMENT 'Referência do evento na instituição.',
    operacao_id BIGINT UNSIGNED NOT NULL COMMENT 'Operação financeira registrada ou reconhecida.',
    CONSTRAINT uk_importacao_evento UNIQUE (importacao_id, referencia),
    CONSTRAINT fk_importacao_evento_recibo FOREIGN KEY (importacao_id) REFERENCES importacoes(id),
    CONSTRAINT fk_importacao_evento_operacao FOREIGN KEY (operacao_id) REFERENCES operacoes(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='Vínculos entre eventos de arquivos confirmados e operações financeiras.';
