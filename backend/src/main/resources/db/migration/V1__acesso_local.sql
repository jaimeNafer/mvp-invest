CREATE TABLE usuarios (
    id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT PRIMARY KEY,
    nome VARCHAR(160) NOT NULL,
    login VARCHAR(120) NOT NULL,
    ativo BOOLEAN NOT NULL DEFAULT TRUE,
    criado_em DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    CONSTRAINT uk_usuario_login UNIQUE (login)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- A identidade interna não exige dados OIDC. Nunca armazenar senha em texto.
CREATE TABLE credenciais_locais (
    usuario_id BIGINT UNSIGNED NOT NULL PRIMARY KEY,
    senha_hash VARCHAR(255) NOT NULL,
    versao_credencial BIGINT UNSIGNED NOT NULL DEFAULT 0,
    atualizada_em DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    CONSTRAINT fk_credencial_usuario FOREIGN KEY (usuario_id) REFERENCES usuarios(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE espacos (
    id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT PRIMARY KEY,
    nome VARCHAR(120) NOT NULL,
    criado_por BIGINT UNSIGNED NOT NULL,
    revisao_livro BIGINT UNSIGNED NOT NULL DEFAULT 0,
    criado_em DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    CONSTRAINT fk_espaco_usuario FOREIGN KEY (criado_por) REFERENCES usuarios(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE membros_espaco (
    espaco_id BIGINT UNSIGNED NOT NULL,
    usuario_id BIGINT UNSIGNED NOT NULL,
    papel VARCHAR(10) NOT NULL DEFAULT 'ADMIN',
    ativo BOOLEAN NOT NULL DEFAULT TRUE,
    PRIMARY KEY (espaco_id, usuario_id),
    CONSTRAINT uk_membro_usuario UNIQUE (usuario_id),
    CONSTRAINT ck_membro_papel CHECK (papel IN ('ADMIN', 'EDITOR', 'LEITOR')),
    CONSTRAINT fk_membro_espaco FOREIGN KEY (espaco_id) REFERENCES espacos(id),
    CONSTRAINT fk_membro_usuario FOREIGN KEY (usuario_id) REFERENCES usuarios(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
