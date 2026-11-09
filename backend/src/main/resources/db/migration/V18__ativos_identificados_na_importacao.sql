ALTER TABLE ativos
    MODIFY nome VARCHAR(160) NULL COMMENT 'Nome do ativo.',
    MODIFY tipo VARCHAR(6) NULL COMMENT 'Tipo do ativo: FIAT ou CRIPTO.';
