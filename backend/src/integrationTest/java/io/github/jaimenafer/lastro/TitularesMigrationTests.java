package io.github.jaimenafer.lastro;

import org.flywaydb.core.Flyway;
import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DriverManagerDataSource;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.mysql.MySQLContainer;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@Testcontainers
class TitularesMigrationTests {
    @Container
    static final MySQLContainer MYSQL = new MySQLContainer("mysql:8.0.42");

    @Test void atualizaV4PreservandoCadastrosEDocumentaCamposSemAlterarEstrutura() {
        var fonte = new DriverManagerDataSource(MYSQL.getJdbcUrl(), MYSQL.getUsername(), MYSQL.getPassword());
        var jdbc = new JdbcTemplate(fonte);
        Flyway.configure().dataSource(fonte).target("4").load().migrate();
        jdbc.update("INSERT INTO espacos(id, nome, revisao_livro) VALUES (1, 'A', 7), (2, 'B', 3)");
        jdbc.update("INSERT INTO titulares(id, espaco_id, nome, ativo) VALUES (11, 1, 'Titular A', FALSE), (22, 2, 'Titular B', TRUE)");
        jdbc.update("INSERT INTO instituicoes(id, codigo, nome) VALUES (33, 'teste:banco', 'Instituição')");
        jdbc.update("INSERT INTO ativos(id, codigo, ticker, nome, tipo, casas_decimais, ativo) VALUES (66, 'fiat:BRL', 'BRL', 'Real brasileiro', 'FIAT', 2, TRUE), (77, 'crypto:BTC', 'BTC', 'Bitcoin', 'CRIPTO', 8, FALSE)");
        jdbc.update("INSERT INTO contas(id, espaco_id, titular_id, instituicao_id, nome, tipo) VALUES (44, 1, 11, 33, 'Conta A', 'BANCO'), (55, 2, 22, 33, 'Conta A', 'BANCO')");
        var titularesAntes = jdbc.queryForList("SELECT id, nome, ativo, criado_em FROM titulares ORDER BY id");
        var contasAntes = jdbc.queryForList("SELECT id, titular_id, instituicao_id, nome, tipo, ativo, criado_em FROM contas ORDER BY id");

        var flyway = Flyway.configure().dataSource(fonte).target("5").load();
        assertThat(flyway.migrate().migrationsExecuted).isEqualTo(1);
        assertThat(jdbc.queryForList("SELECT id, nome, ativo, criado_em FROM titulares ORDER BY id")).isEqualTo(titularesAntes);
        assertThat(jdbc.queryForList("SELECT id, titular_id, instituicao_id, nome, tipo, ativo, criado_em FROM contas ORDER BY id")).isEqualTo(contasAntes);
        assertThat(jdbc.queryForObject("SELECT perfil_ref FROM titulares WHERE id = 11", String.class)).isEqualTo("1");
        assertThat(jdbc.queryForObject("SELECT perfil_ref FROM titulares WHERE id = 22", String.class)).isEqualTo("2");
        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM information_schema.tables WHERE table_schema = DATABASE() AND table_name = 'espacos'", Integer.class)).isZero();
        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM information_schema.columns WHERE table_schema = DATABASE() AND column_name IN ('espaco_id', 'revisao_livro')", Integer.class)).isZero();
        assertThatThrownBy(() -> jdbc.update("INSERT INTO contas(titular_id, nome, tipo) VALUES (11, 'Conta A', 'BANCO')"))
            .isInstanceOf(org.springframework.dao.DuplicateKeyException.class);
        assertThatThrownBy(() -> jdbc.update("INSERT INTO contas(titular_id, nome, tipo) VALUES (999, 'Inválida', 'BANCO')"))
            .isInstanceOf(org.springframework.dao.DataIntegrityViolationException.class);
        jdbc.update("INSERT INTO titulares(perfil_ref, nome) VALUES ('identidade-externa', 'Novo titular')");
        assertThat(jdbc.queryForObject("SELECT id FROM titulares WHERE perfil_ref = 'identidade-externa'", Long.class)).isGreaterThan(22L);
        assertThat(flyway.migrate().migrationsExecuted).isZero();

        var consultaColunas = """
            SELECT table_name, column_name, ordinal_position, column_type, is_nullable,
                   column_default, extra, character_set_name, collation_name
            FROM information_schema.columns
            WHERE table_schema = DATABASE() AND table_name <> 'flyway_schema_history'
            ORDER BY table_name, ordinal_position
            """;
        var consultaIndices = """
            SELECT table_name, index_name, non_unique, seq_in_index, column_name, collation, sub_part, index_type
            FROM information_schema.statistics
            WHERE table_schema = DATABASE() AND table_name <> 'flyway_schema_history'
            ORDER BY table_name, index_name, seq_in_index
            """;
        var consultaVinculos = """
            SELECT table_name, constraint_name, column_name, ordinal_position, referenced_table_name, referenced_column_name
            FROM information_schema.key_column_usage
            WHERE table_schema = DATABASE() AND table_name <> 'flyway_schema_history'
            ORDER BY table_name, constraint_name, ordinal_position
            """;
        var consultaTabelas = """
            SELECT table_name, engine, table_collation, auto_increment
            FROM information_schema.tables
            WHERE table_schema = DATABASE() AND table_name <> 'flyway_schema_history'
            ORDER BY table_name
            """;
        var colunasAntes = jdbc.queryForList(consultaColunas);
        var indicesAntes = jdbc.queryForList(consultaIndices);
        var vinculosAntes = jdbc.queryForList(consultaVinculos);
        var tabelasAntes = jdbc.queryForList(consultaTabelas);
        var titularesV5 = jdbc.queryForList("SELECT * FROM titulares ORDER BY id");
        var instituicoesV5 = jdbc.queryForList("SELECT * FROM instituicoes ORDER BY id");
        var ativosV5 = jdbc.queryForList("SELECT * FROM ativos ORDER BY id");
        var documentado = Flyway.configure().dataSource(fonte).target("7").load();
        assertThat(documentado.migrate().migrationsExecuted).isEqualTo(2);
        assertThat(jdbc.queryForList(consultaColunas)).isEqualTo(colunasAntes);
        assertThat(jdbc.queryForList(consultaIndices)).isEqualTo(indicesAntes);
        assertThat(jdbc.queryForList(consultaVinculos)).isEqualTo(vinculosAntes);
        assertThat(jdbc.queryForList(consultaTabelas)).isEqualTo(tabelasAntes);
        assertThat(jdbc.queryForList("SELECT * FROM titulares ORDER BY id")).isEqualTo(titularesV5);
        assertThat(jdbc.queryForList("SELECT * FROM instituicoes ORDER BY id")).isEqualTo(instituicoesV5);
        assertThat(jdbc.queryForList("SELECT * FROM ativos ORDER BY id")).isEqualTo(ativosV5);
        assertThat(jdbc.queryForList("SELECT id, titular_id, instituicao_id, nome, tipo, ativo, criado_em FROM contas ORDER BY id")).isEqualTo(contasAntes);
        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM information_schema.columns WHERE table_schema = DATABASE() AND table_name <> 'flyway_schema_history' AND TRIM(column_comment) <> ''", Integer.class)).isEqualTo(24);
        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM information_schema.tables WHERE table_schema = DATABASE() AND table_name <> 'flyway_schema_history' AND TRIM(table_comment) <> ''", Integer.class)).isEqualTo(4);
        assertThat(documentado.migrate().migrationsExecuted).isZero();
    }
}
