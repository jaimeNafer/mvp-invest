ALTER TABLE cargas_cotacao
    DROP CHECK ck_carga_estado,
    MODIFY estado VARCHAR(16) NOT NULL COMMENT 'Situação: PENDENTE, EXECUTANDO, PARCIAL, CONCLUIDA, SEM_DADOS, FALHA ou FALHA_ESGOTADA.',
    ADD CONSTRAINT ck_carga_estado CHECK (estado IN
        ('PENDENTE', 'EXECUTANDO', 'PARCIAL', 'CONCLUIDA', 'SEM_DADOS', 'FALHA', 'FALHA_ESGOTADA'));

UPDATE cargas_cotacao SET estado='PARCIAL'
WHERE estado IN ('CONCLUIDA','SEM_DADOS') AND atualizada_em<fim_em;

UPDATE cargas_cotacao SET estado='FALHA_ESGOTADA'
WHERE estado='FALHA' AND proxima_tentativa_em IS NULL;
