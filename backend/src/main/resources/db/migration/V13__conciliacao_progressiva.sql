CREATE TABLE contas_custodia (
    id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT PRIMARY KEY COMMENT 'Identificador do registro.',
    perfil_ref VARCHAR(120) COLLATE utf8mb4_bin NOT NULL COMMENT 'Usuário proprietário.',
    instituicao_id BIGINT UNSIGNED NOT NULL COMMENT 'Instituição de custódia.',
    codigo VARCHAR(120) COLLATE utf8mb4_bin NOT NULL COMMENT 'Código da conta na instituição.',
    nome VARCHAR(160) NOT NULL COMMENT 'Nome da conta de custódia.',
    criado_em DATETIME(6) NOT NULL COMMENT 'Criação em UTC.',
    UNIQUE KEY uk_custodia_codigo (perfil_ref, instituicao_id, codigo),
    CONSTRAINT fk_custodia_instituicao FOREIGN KEY (instituicao_id) REFERENCES instituicoes(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='Contas de custódia por usuário e instituição.';

CREATE TABLE registros_conferencia (
    id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT PRIMARY KEY COMMENT 'Identificador do registro.',
    perfil_ref VARCHAR(120) COLLATE utf8mb4_bin NOT NULL COMMENT 'Usuário proprietário.',
    arquivo_ref VARCHAR(120) COLLATE utf8mb4_bin NULL COMMENT 'Referência do arquivo de origem.',
    origem_ref VARCHAR(240) COLLATE utf8mb4_bin NOT NULL COMMENT 'Identidade estável do registro de origem.',
    instituicao_id BIGINT UNSIGNED NOT NULL COMMENT 'Instituição de origem.',
    original_json JSON NOT NULL COMMENT 'Valores originais recebidos.',
    erros_json JSON NOT NULL COMMENT 'Problemas identificados na recepção.',
    proposta_json JSON NOT NULL COMMENT 'Dados propostos para conferência.',
    aplicada_json JSON NULL COMMENT 'Última proposta com efeitos aplicados.',
    versao BIGINT NOT NULL COMMENT 'Versão dos dados propostos.',
    revisado_por VARCHAR(120) COLLATE utf8mb4_bin NULL COMMENT 'Autor da revisão manual.',
    revisado_em DATETIME(6) NULL COMMENT 'Revisão manual em UTC.',
    criado_em DATETIME(6) NOT NULL COMMENT 'Recepção em UTC.',
    UNIQUE KEY uk_conferencia_origem (perfil_ref, origem_ref),
    CONSTRAINT fk_conferencia_instituicao FOREIGN KEY (instituicao_id) REFERENCES instituicoes(id),
    CONSTRAINT ck_conferencia_versao CHECK (versao > 0)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='Registros originais e versões de conferência.';

CREATE TABLE ajustes_conciliatorios (
    id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT PRIMARY KEY COMMENT 'Identificador do registro.',
    perfil_ref VARCHAR(120) COLLATE utf8mb4_bin NOT NULL COMMENT 'Usuário proprietário.',
    lacuna_ref VARCHAR(240) COLLATE utf8mb4_bin NOT NULL COMMENT 'Identidade estável da lacuna.',
    ancora_ref VARCHAR(240) COLLATE utf8mb4_bin NOT NULL COMMENT 'Registro que determina a posição do ajuste.',
    conta_custodia_id BIGINT UNSIGNED NOT NULL COMMENT 'Conta de custódia afetada.',
    instituicao_id BIGINT UNSIGNED NOT NULL COMMENT 'Instituição de custódia.',
    ativo_id BIGINT UNSIGNED NOT NULL COMMENT 'Ativo com quantidade ausente.',
    quantidade DECIMAL(38,18) NOT NULL COMMENT 'Quantidade provisória aceita.',
    custo_total_brl DECIMAL(38,8) NULL COMMENT 'Custo em reais, quando conhecido.',
    versao BIGINT NOT NULL COMMENT 'Versão do ajuste da lacuna.',
    ativo BOOLEAN NOT NULL COMMENT 'Indica efeito vigente.',
    vigente TINYINT GENERATED ALWAYS AS (IF(ativo, 1, NULL)) STORED COMMENT 'Marcador de unicidade do ajuste ativo.',
    anterior_id BIGINT UNSIGNED NULL COMMENT 'Versão anterior do ajuste.',
    aceito_por VARCHAR(120) COLLATE utf8mb4_bin NOT NULL COMMENT 'Autor do aceite.',
    criado_em DATETIME(6) NOT NULL COMMENT 'Criação e aceite em UTC.',
    encerrado_em DATETIME(6) NULL COMMENT 'Encerramento em UTC.',
    UNIQUE KEY uk_ajuste_vigente (perfil_ref, lacuna_ref, vigente),
    UNIQUE KEY uk_ajuste_versao (perfil_ref, lacuna_ref, versao),
    CONSTRAINT fk_ajuste_custodia FOREIGN KEY (conta_custodia_id) REFERENCES contas_custodia(id),
    CONSTRAINT fk_ajuste_instituicao FOREIGN KEY (instituicao_id) REFERENCES instituicoes(id),
    CONSTRAINT fk_ajuste_ativo FOREIGN KEY (ativo_id) REFERENCES ativos(id),
    CONSTRAINT fk_ajuste_anterior FOREIGN KEY (anterior_id) REFERENCES ajustes_conciliatorios(id),
    CONSTRAINT ck_ajuste_quantidade CHECK (quantidade > 0),
    CONSTRAINT ck_ajuste_custo CHECK (custo_total_brl IS NULL OR custo_total_brl >= 0),
    CONSTRAINT ck_ajuste_versao CHECK (versao > 0)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='Versões aceitas de ajustes para lacunas.';

CREATE TABLE comandos_conciliacao (
    id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT PRIMARY KEY COMMENT 'Identificador do registro.',
    perfil_ref VARCHAR(120) COLLATE utf8mb4_bin NOT NULL COMMENT 'Usuário proprietário.',
    chave CHAR(36) CHARACTER SET ascii COLLATE ascii_bin NOT NULL COMMENT 'Chave de repetição segura.',
    fingerprint CHAR(64) CHARACTER SET ascii COLLATE ascii_bin NOT NULL COMMENT 'Resumo do comando normalizado.',
    resposta_json JSON NOT NULL COMMENT 'Resposta original do comando.',
    criado_em DATETIME(6) NOT NULL COMMENT 'Confirmação em UTC.',
    UNIQUE KEY uk_comando_conciliacao (perfil_ref, chave)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='Comandos de conciliação e respostas idempotentes.';

INSERT INTO contas_custodia (perfil_ref, instituicao_id, codigo, nome, criado_em)
SELECT DISTINCT perfil_ref, instituicao_id, 'nao_especificada', 'Não especificada', UTC_TIMESTAMP(6)
FROM operacoes;

CREATE TABLE arquivos_recebidos (
    id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT PRIMARY KEY COMMENT 'Identificador do arquivo recebido.',
    perfil_ref VARCHAR(120) COLLATE utf8mb4_bin NOT NULL COMMENT 'Usuário proprietário.',
    hash CHAR(64) CHARACTER SET ascii COLLATE ascii_bin NOT NULL COMMENT 'Resumo criptográfico do arquivo.',
    nome_arquivo VARCHAR(255) NOT NULL COMMENT 'Nome do arquivo recebido.',
    versao_leitor VARCHAR(80) NOT NULL COMMENT 'Versão do leitor do arquivo.',
    registros INT NOT NULL COMMENT 'Quantidade de registros recebidos.',
    criado_em DATETIME(6) NOT NULL COMMENT 'Recepção em UTC.',
    conteudo MEDIUMBLOB NOT NULL COMMENT 'Conteúdo original do arquivo.',
    UNIQUE KEY uk_arquivo_recebido_hash (perfil_ref, hash)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='Arquivos originais recebidos para conferência.';

CREATE TABLE historico_propostas (
 registro_id BIGINT UNSIGNED NOT NULL COMMENT 'Registro de conferência alterado.',
 ordem INT NOT NULL COMMENT 'Ordem da versão anterior.',
 versao BIGINT NOT NULL COMMENT 'Versão dos dados preservados.',
 proposta_json JSON NOT NULL COMMENT 'Dados anteriores à alteração.',
 alterado_por VARCHAR(120) COLLATE utf8mb4_bin NOT NULL COMMENT 'Autor da alteração.',
 alterado_em DATETIME(6) NOT NULL COMMENT 'Alteração em UTC.',
 PRIMARY KEY (registro_id,ordem),
 CONSTRAINT fk_proposta_registro FOREIGN KEY(registro_id) REFERENCES registros_conferencia(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='Dados anteriores às correções de conferência.';
