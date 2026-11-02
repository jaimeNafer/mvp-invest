ALTER TABLE titulares
    ADD COLUMN perfil_ref VARCHAR(120) COLLATE utf8mb4_bin NULL AFTER id;

UPDATE titulares SET perfil_ref = CAST(espaco_id AS CHAR);

ALTER TABLE contas DROP FOREIGN KEY fk_conta_titular;

ALTER TABLE contas
    DROP INDEX uk_conta_titular,
    DROP INDEX uk_conta_nome,
    DROP COLUMN espaco_id,
    ADD CONSTRAINT uk_conta_nome UNIQUE (titular_id, nome),
    ADD CONSTRAINT fk_conta_titular FOREIGN KEY (titular_id) REFERENCES titulares(id);

ALTER TABLE titulares
    DROP FOREIGN KEY fk_titular_espaco,
    DROP INDEX uk_titular_espaco,
    DROP COLUMN espaco_id,
    MODIFY COLUMN perfil_ref VARCHAR(120) COLLATE utf8mb4_bin NOT NULL,
    ADD INDEX ix_titular_perfil_nome (perfil_ref, nome, id);

DROP TABLE espacos;
