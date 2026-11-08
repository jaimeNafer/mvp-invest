CREATE TABLE grupos_associacao (
 id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT PRIMARY KEY COMMENT 'Identificador da operação agrupada.',
 perfil_ref VARCHAR(120) COLLATE utf8mb4_bin NOT NULL COMMENT 'Usuário proprietário.',
 proposta_json JSON NOT NULL COMMENT 'Linhas e decisões propostas para a operação.',
 aplicada_json JSON NULL COMMENT 'Versão da operação incorporada ao histórico.',
 versao BIGINT NOT NULL COMMENT 'Versão da proposta.',
 alterado_por VARCHAR(120) COLLATE utf8mb4_bin NOT NULL COMMENT 'Autor da última decisão.',
 alterado_em DATETIME(6) NOT NULL COMMENT 'Última decisão em UTC.',
 criado_em DATETIME(6) NOT NULL COMMENT 'Criação em UTC.',
 INDEX ix_grupo_perfil (perfil_ref),
 CONSTRAINT ck_grupo_versao CHECK (versao > 0)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='Operações compostas de linhas complementares.';
CREATE TABLE linhas_grupos_associacao (
 registro_id BIGINT UNSIGNED NOT NULL PRIMARY KEY COMMENT 'Linha reservada à associação.',
 grupo_id BIGINT UNSIGNED NOT NULL COMMENT 'Operação agrupada responsável pela linha.',
 CONSTRAINT fk_linha_grupo_registro FOREIGN KEY (registro_id) REFERENCES registros_conferencia(id),
 CONSTRAINT fk_linha_grupo_grupo FOREIGN KEY (grupo_id) REFERENCES grupos_associacao(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='Linhas reservadas pelas versões propostas ou aplicadas dos grupos.';
CREATE TABLE historico_grupos_associacao (
 grupo_id BIGINT UNSIGNED NOT NULL COMMENT 'Operação agrupada alterada.',
 ordem INT NOT NULL COMMENT 'Ordem da versão anterior.',
 versao BIGINT NOT NULL COMMENT 'Versão preservada.',
 proposta_json JSON NOT NULL COMMENT 'Linhas e decisões da versão anterior.',
 alterado_por VARCHAR(120) COLLATE utf8mb4_bin NOT NULL COMMENT 'Autor da versão preservada.',
 alterado_em DATETIME(6) NOT NULL COMMENT 'Decisão em UTC.',
 PRIMARY KEY (grupo_id,ordem),
 CONSTRAINT fk_historico_grupo FOREIGN KEY (grupo_id) REFERENCES grupos_associacao(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='Histórico das decisões de associação de linhas.';
