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
        assertThat(flyway.info().applied()).hasSize(8);
        assertThat(flyway.migrate().migrationsExecuted).isZero();
        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM carteiras", Integer.class)).isZero();
        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM contas", Integer.class)).isZero();
        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM instituicoes", Integer.class)).isEqualTo(2);
    }

    @Test void bancoRejeitaContaSemInstituicaoETodaTransacaoReverte() {
        assertThatThrownBy(() -> transactions.executeWithoutResult(transaction -> {
            jdbc.update("INSERT INTO carteiras(id, perfil_ref, nome) VALUES (101, 'perfil-a', 'Carteira A')");
            jdbc.update("INSERT INTO contas(carteira_id, instituicao_id) VALUES (101, 999999)");
        })).isInstanceOf(DataIntegrityViolationException.class);
        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM carteiras WHERE id = 101", Integer.class)).isZero();
    }

    @Test void bancoRejeitaPrecisaoDeAtivoAcimaDoLimite() {
        assertThatThrownBy(() -> jdbc.update("INSERT INTO ativos(codigo, ticker, nome, tipo, casas_decimais) VALUES ('teste:X', 'X', 'Inválido', 'CRIPTO', 19)"))
            .rootCause().isInstanceOf(java.sql.SQLException.class)
            .hasMessageContaining("ck_ativo_casas");
        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM ativos WHERE codigo = 'teste:X'", Integer.class)).isZero();
    }

    @Test void schemaNaoMantemEspacosOuControleDeUsuarios() {
        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM information_schema.tables WHERE table_schema = DATABASE() AND table_name IN ('titulares', 'espacos', 'usuarios', 'membros_espaco', 'credenciais_locais')", Integer.class)).isZero();
        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM information_schema.columns WHERE table_schema = DATABASE() AND column_name IN ('espaco_id', 'revisao_livro')", Integer.class)).isZero();
    }
}
