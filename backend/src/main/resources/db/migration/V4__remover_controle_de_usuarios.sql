ALTER TABLE espacos
    DROP FOREIGN KEY fk_espaco_usuario,
    DROP COLUMN criado_por;

DROP TABLE membros_espaco;
DROP TABLE usuarios;
