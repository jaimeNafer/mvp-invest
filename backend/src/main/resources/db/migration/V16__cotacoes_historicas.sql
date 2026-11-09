CREATE TABLE pares_cotacao (
    id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT PRIMARY KEY COMMENT 'Identificador do par de cotação.',
    ativo_base_id BIGINT UNSIGNED NOT NULL COMMENT 'Ativo precificado.',
    ativo_cotacao_id BIGINT UNSIGNED NOT NULL COMMENT 'Ativo usado como unidade de preço.',
    fonte VARCHAR(32) NOT NULL COMMENT 'Fonte do par: BINANCE.',
    simbolo VARCHAR(80) COLLATE utf8mb4_bin NOT NULL COMMENT 'Símbolo do par na fonte.',
    CONSTRAINT uk_par_fonte_simbolo UNIQUE (fonte, simbolo),
    CONSTRAINT uk_par_fonte_ativos UNIQUE (fonte, ativo_base_id, ativo_cotacao_id),
    CONSTRAINT fk_par_ativo_base FOREIGN KEY (ativo_base_id) REFERENCES ativos(id),
    CONSTRAINT fk_par_ativo_cotacao FOREIGN KEY (ativo_cotacao_id) REFERENCES ativos(id),
    CONSTRAINT ck_par_ativos_distintos CHECK (ativo_base_id <> ativo_cotacao_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='Pares de ativos identificados nas fontes de preço.';

CREATE TABLE cotacoes_historicas (
    id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT PRIMARY KEY COMMENT 'Identificador da cotação histórica.',
    par_id BIGINT UNSIGNED NOT NULL COMMENT 'Par de ativos da cotação.',
    granularidade VARCHAR(8) NOT NULL COMMENT 'Intervalo do candle: HORA ou DIA.',
    instante DATETIME(6) NOT NULL COMMENT 'Início do candle em UTC.',
    preco DECIMAL(38,18) NOT NULL COMMENT 'Preço de abertura na unidade de cotação.',
    versao BIGINT UNSIGNED NOT NULL DEFAULT 1 COMMENT 'Versão atual da cotação.',
    checksum CHAR(64) COLLATE utf8mb4_bin NULL COMMENT 'SHA-256 do arquivo de origem.',
    obtido_em DATETIME(6) NOT NULL COMMENT 'Instante de obtenção em UTC.',
    CONSTRAINT uk_cotacao_candle UNIQUE (par_id, granularidade, instante),
    CONSTRAINT fk_cotacao_par FOREIGN KEY (par_id) REFERENCES pares_cotacao(id),
    CONSTRAINT ck_cotacao_granularidade CHECK (granularidade IN ('HORA', 'DIA')),
    CONSTRAINT ck_cotacao_preco CHECK (preco > 0),
    INDEX ix_cotacao_instante (granularidade, instante, par_id),
    INDEX ix_cotacao_obtido (obtido_em)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='Preços históricos globais de pares de ativos.';

CREATE TABLE cargas_cotacao (
    id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT PRIMARY KEY COMMENT 'Identificador da carga histórica.',
    par_id BIGINT UNSIGNED NOT NULL COMMENT 'Par de ativos consultado.',
    granularidade VARCHAR(8) NOT NULL COMMENT 'Intervalo do candle: HORA ou DIA.',
    inicio_em DATETIME(6) NOT NULL COMMENT 'Início do período consultado em UTC.',
    fim_em DATETIME(6) NOT NULL COMMENT 'Fim exclusivo do período consultado em UTC.',
    estado VARCHAR(16) NOT NULL COMMENT 'Situação da carga: PENDENTE, EXECUTANDO, CONCLUIDA, SEM_DADOS ou FALHA.',
    tentativas INT NOT NULL DEFAULT 0 COMMENT 'Número de tentativas de obtenção.',
    proxima_tentativa_em DATETIME(6) NULL COMMENT 'Instante permitido para nova tentativa em UTC.',
    reserva_ate DATETIME(6) NULL COMMENT 'Prazo da reserva da carga em UTC.',
    reserva_token VARCHAR(64) COLLATE utf8mb4_bin NULL COMMENT 'Identificador da reserva da carga.',
    atualizada_em DATETIME(6) NOT NULL COMMENT 'Última atualização em UTC.',
    CONSTRAINT uk_carga_periodo UNIQUE (par_id, granularidade, inicio_em, fim_em),
    CONSTRAINT fk_carga_par FOREIGN KEY (par_id) REFERENCES pares_cotacao(id),
    CONSTRAINT ck_carga_granularidade CHECK (granularidade IN ('HORA', 'DIA')),
    CONSTRAINT ck_carga_periodo CHECK (fim_em > inicio_em),
    CONSTRAINT ck_carga_estado CHECK (estado IN ('PENDENTE', 'EXECUTANDO', 'CONCLUIDA', 'SEM_DADOS', 'FALHA')),
    CONSTRAINT ck_carga_tentativas CHECK (tentativas >= 0),
    INDEX ix_carga_execucao (estado, proxima_tentativa_em, reserva_ate)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='Períodos de cotações consultados e pendentes.';

CREATE TABLE historico_cotacoes (
    cotacao_id BIGINT UNSIGNED NOT NULL COMMENT 'Cotação histórica alterada.',
    versao BIGINT UNSIGNED NOT NULL COMMENT 'Versão preservada da cotação.',
    preco DECIMAL(38,18) NOT NULL COMMENT 'Preço de abertura preservado.',
    checksum CHAR(64) COLLATE utf8mb4_bin NULL COMMENT 'SHA-256 do arquivo de origem.',
    obtido_em DATETIME(6) NOT NULL COMMENT 'Instante de obtenção em UTC.',
    PRIMARY KEY (cotacao_id, versao),
    CONSTRAINT fk_historico_cotacao FOREIGN KEY (cotacao_id) REFERENCES cotacoes_historicas(id),
    CONSTRAINT ck_historico_cotacao_preco CHECK (preco > 0),
    CONSTRAINT ck_historico_cotacao_versao CHECK (versao > 0)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='Versões anteriores de cotações históricas.';

CREATE TABLE valoracoes_manuais (
    id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT PRIMARY KEY COMMENT 'Identificador da valoração manual.',
    perfil_ref VARCHAR(120) COLLATE utf8mb4_bin NOT NULL COMMENT 'Usuário proprietário.',
    registro_id BIGINT UNSIGNED NULL COMMENT 'Registro valorado.',
    grupo_id BIGINT UNSIGNED NULL COMMENT 'Operação agrupada valorada.',
    identidade VARCHAR(64) COLLATE utf8mb4_bin NULL COMMENT 'Identidade econômica da operação valorada.',
    valor_brl DECIMAL(38,8) NOT NULL COMMENT 'Valor total informado em reais.',
    versao BIGINT UNSIGNED NOT NULL COMMENT 'Versão atual da valoração.',
    alterado_por VARCHAR(120) COLLATE utf8mb4_bin NOT NULL COMMENT 'Autor da última alteração.',
    alterado_em DATETIME(6) NOT NULL COMMENT 'Última alteração em UTC.',
    UNIQUE KEY uk_valoracao_registro (registro_id),
    UNIQUE KEY uk_valoracao_grupo (grupo_id),
    INDEX ix_valoracao_perfil (perfil_ref),
    CONSTRAINT fk_valoracao_registro FOREIGN KEY (registro_id) REFERENCES registros_conferencia(id),
    CONSTRAINT fk_valoracao_grupo FOREIGN KEY (grupo_id) REFERENCES grupos_associacao(id),
    CONSTRAINT ck_valoracao_referencia CHECK ((registro_id IS NULL) <> (grupo_id IS NULL)),
    CONSTRAINT ck_valoracao_valor CHECK (valor_brl > 0),
    CONSTRAINT ck_valoracao_versao CHECK (versao > 0)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='Valores em reais informados para operações.';

CREATE TABLE historico_valoracoes_manuais (
    valoracao_id BIGINT UNSIGNED NOT NULL COMMENT 'Valoração manual alterada.',
    versao BIGINT UNSIGNED NOT NULL COMMENT 'Versão preservada.',
    valor_brl DECIMAL(38,8) NOT NULL COMMENT 'Valor em reais preservado.',
    identidade VARCHAR(64) COLLATE utf8mb4_bin NULL COMMENT 'Identidade econômica da operação valorada.',
    alterado_por VARCHAR(120) COLLATE utf8mb4_bin NOT NULL COMMENT 'Autor da versão preservada.',
    alterado_em DATETIME(6) NOT NULL COMMENT 'Alteração em UTC.',
    PRIMARY KEY (valoracao_id, versao),
    CONSTRAINT fk_historico_valoracao FOREIGN KEY (valoracao_id) REFERENCES valoracoes_manuais(id),
    CONSTRAINT ck_historico_valoracao_valor CHECK (valor_brl > 0),
    CONSTRAINT ck_historico_valoracao_versao CHECK (versao > 0)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='Versões anteriores de valores informados para operações.';
