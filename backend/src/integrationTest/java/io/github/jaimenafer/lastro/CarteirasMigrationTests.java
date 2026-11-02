package io.github.jaimenafer.lastro;

import org.flywaydb.core.Flyway;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DriverManagerDataSource;
import org.springframework.dao.DataIntegrityViolationException;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.mysql.MySQLContainer;

import static org.assertj.core.api.Assertions.*;

@Testcontainers
class CarteirasMigrationTests {
    @Container
    static final MySQLContainer MYSQL = new MySQLContainer("mysql:8.0.42");
    private DriverManagerDataSource fonte;
    private JdbcTemplate jdbc;

    @BeforeEach void prepararV7() {
        fonte = new DriverManagerDataSource(MYSQL.getJdbcUrl(), MYSQL.getUsername(), MYSQL.getPassword());
        jdbc = new JdbcTemplate(fonte);
        Flyway.configure().dataSource(fonte).cleanDisabled(false).load().clean();
        Flyway.configure().dataSource(fonte).target("7").load().migrate();
    }

    @Test void atualizaV7PreservaAtivosECriaCatalogoContasEVinculos() {
        jdbc.update("INSERT INTO ativos(codigo, ticker, nome, tipo, casas_decimais) VALUES ('fiat:BRL', 'BRL', 'Real', 'FIAT', 2)");
        var ativos = jdbc.queryForList("SELECT * FROM ativos");
        var flyway = Flyway.configure().dataSource(fonte).load();
        assertThat(flyway.migrate().migrationsExecuted).isEqualTo(1);
        assertThat(jdbc.queryForList("SELECT * FROM ativos")).isEqualTo(ativos);
        assertThat(jdbc.queryForList("SELECT codigo FROM instituicoes ORDER BY codigo", String.class)).containsExactly("binance", "bitget");
        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM instituicoes WHERE ativo = TRUE AND pais_sede IS NULL", Integer.class)).isEqualTo(2);
        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM information_schema.tables WHERE table_schema = DATABASE() AND table_name = 'titulares'", Integer.class)).isZero();
        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM information_schema.columns WHERE table_schema = DATABASE() AND column_name = 'titular_id'", Integer.class)).isZero();
        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM information_schema.columns WHERE table_schema = DATABASE() AND table_name <> 'flyway_schema_history' AND TRIM(column_comment) <> ''", Integer.class)).isEqualTo(23);
        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM information_schema.tables WHERE table_schema = DATABASE() AND table_name <> 'flyway_schema_history' AND TRIM(table_comment) <> ''", Integer.class)).isEqualTo(5);
        var instituicao = jdbc.queryForObject("SELECT id FROM instituicoes WHERE codigo = 'binance'", Long.class);
        jdbc.update("INSERT INTO carteiras(id, perfil_ref, nome) VALUES (11, 'a', 'Principal'), (22, 'a', 'Recorte')");
        jdbc.update("INSERT INTO contas(id, perfil_ref, instituicao_id) VALUES (33, 'a', ?)", instituicao);
        jdbc.update("INSERT INTO carteira_contas(carteira_id, conta_id) VALUES (11, 33), (22, 33)");
        assertThatThrownBy(() -> jdbc.update("INSERT INTO contas(perfil_ref, instituicao_id) VALUES ('a', ?)", instituicao)).isInstanceOf(DataIntegrityViolationException.class);
        assertThatThrownBy(() -> jdbc.update("INSERT INTO carteira_contas(carteira_id, conta_id) VALUES (11, 33)")).isInstanceOf(DataIntegrityViolationException.class);
        assertThatThrownBy(() -> jdbc.update("DELETE FROM contas WHERE id = 33")).isInstanceOf(DataIntegrityViolationException.class);
        assertThatThrownBy(() -> jdbc.update("INSERT INTO carteira_contas(carteira_id, conta_id) VALUES (11, 99999)")).isInstanceOf(DataIntegrityViolationException.class);
        assertThat(flyway.migrate().migrationsExecuted).isZero();
    }

    @Test void recusaConversaoSeExistiremCadastrosSemApagarDados() {
        jdbc.update("INSERT INTO titulares(perfil_ref, nome) VALUES ('a', 'Preservar')");
        assertThatThrownBy(() -> Flyway.configure().dataSource(fonte).load().migrate())
            .isInstanceOf(org.flywaydb.core.api.FlywayException.class).hasStackTraceContaining("ck_v8_exige_cadastros_vazios");
        assertThat(jdbc.queryForObject("SELECT nome FROM titulares", String.class)).isEqualTo("Preservar");
        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM information_schema.tables WHERE table_schema = DATABASE() AND table_name = 'carteiras'", Integer.class)).isZero();
    }
}
