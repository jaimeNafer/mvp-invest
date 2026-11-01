package io.github.jaimenafer.lastro;

import org.flywaydb.core.Flyway;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.support.TransactionTemplate;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.mysql.MySQLContainer;

import static org.assertj.core.api.Assertions.*;

@SpringBootTest(properties = "spring.config.import=")
@Testcontainers
class FoundationIntegrationTests {
    @Container @ServiceConnection
    static final MySQLContainer MYSQL = new MySQLContainer("mysql:8.0.42");

    @Autowired JdbcTemplate jdbc;
    @Autowired Flyway flyway;
    @Autowired TransactionTemplate transactions;

    @Test void migrationsCriamBancoSemDadosReaisEReexecucaoNaoAlteraSchema() {
        assertThat(flyway.info().applied()).hasSize(4);
        assertThat(flyway.migrate().migrationsExecuted).isZero();
        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM espacos", Integer.class)).isZero();
        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM contas", Integer.class)).isZero();
    }

    @Test void bancoRejeitaContaComTitularDeOutroEspacoETodaTransacaoReverte() {
        assertThatThrownBy(() -> transactions.executeWithoutResult(transaction -> {
            jdbc.update("INSERT INTO espacos(id, nome) VALUES (101, 'A'), (102, 'B')");
            jdbc.update("INSERT INTO titulares(id, espaco_id, nome) VALUES (101, 101, 'Titular A')");
            jdbc.update("INSERT INTO contas(espaco_id, titular_id, nome, tipo) VALUES (102, 101, 'Conta inválida', 'CORRETORA')");
        })).isInstanceOf(DataIntegrityViolationException.class);
        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM espacos WHERE id IN (101, 102)", Integer.class)).isZero();
    }

    @Test void bancoRejeitaPrecisaoDeAtivoAcimaDoLimite() {
        assertThatThrownBy(() -> jdbc.update("INSERT INTO ativos(codigo, ticker, nome, tipo, casas_decimais) VALUES ('teste:X', 'X', 'Inválido', 'CRIPTO', 19)"))
            .rootCause().isInstanceOf(java.sql.SQLException.class)
            .hasMessageContaining("ck_ativo_casas");
        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM ativos WHERE codigo = 'teste:X'", Integer.class)).isZero();
    }

    @Test void schemaNaoMantemControleDeUsuarios() {
        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM information_schema.tables WHERE table_schema = DATABASE() AND table_name IN ('usuarios', 'membros_espaco', 'credenciais_locais')", Integer.class)).isZero();
        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM information_schema.columns WHERE table_schema = DATABASE() AND table_name = 'espacos' AND column_name = 'criado_por'", Integer.class)).isZero();
    }
}
