package io.github.jaimenafer.lastro;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.flywaydb.core.Flyway;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DriverManagerDataSource;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.mysql.MySQLContainer;

@Testcontainers
class SaldoInicialMigrationTests {
  @Container static final MySQLContainer MYSQL = new MySQLContainer("mysql:8.0.42");
  DriverManagerDataSource fonte;
  JdbcTemplate jdbc;

  @BeforeEach
  void preparar() {
    fonte =
        new DriverManagerDataSource(MYSQL.getJdbcUrl(), MYSQL.getUsername(), MYSQL.getPassword());
    jdbc = new JdbcTemplate(fonte);
    Flyway.configure().dataSource(fonte).cleanDisabled(false).load().clean();
    Flyway.configure().dataSource(fonte).target("8").load().migrate();
  }

  @Test
  void preservaCadastrosCatalogaAtivosEReaplicaSemAlterar() {
    jdbc.update("INSERT INTO carteiras(perfil_ref,nome) VALUES ('a','Preservar')");
    jdbc.update(
        "INSERT INTO contas(carteira_id,instituicao_id) SELECT c.id,i.id FROM carteiras c JOIN"
            + " instituicoes i ON i.codigo='binance'");
    jdbc.update(
        "INSERT INTO ativos(codigo,ticker,nome,tipo,casas_decimais) VALUES ('fiat:BRL','BRL','Nome"
            + " preservado','FIAT',2)");
    var carteiras = jdbc.queryForList("SELECT * FROM carteiras");
    var contas = jdbc.queryForList("SELECT * FROM contas");
    var flyway = Flyway.configure().dataSource(fonte).target("9").load();
    assertThat(flyway.migrate().migrationsExecuted).isEqualTo(1);
    assertThat(jdbc.queryForList("SELECT * FROM carteiras")).isEqualTo(carteiras);
    assertThat(jdbc.queryForList("SELECT * FROM contas")).isEqualTo(contas);
    assertThat(jdbc.queryForObject("SELECT nome FROM ativos WHERE codigo='fiat:BRL'", String.class))
        .isEqualTo("Nome preservado");
    assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM ativos", Integer.class)).isEqualTo(6);
    assertThat(
            jdbc.queryForObject(
                "SELECT COUNT(*) FROM information_schema.columns WHERE table_schema=DATABASE() AND"
                    + " table_name IN ('operacoes','pontos_partida') AND TRIM(column_comment)=''",
                Integer.class))
        .isZero();
    assertThat(
            jdbc.queryForObject(
                "SELECT COUNT(*) FROM information_schema.key_column_usage WHERE"
                    + " table_schema=DATABASE() AND table_name='operacoes' AND"
                    + " referenced_table_name IN ('carteiras','contas')",
                Integer.class))
        .isZero();
    assertThat(flyway.migrate().migrationsExecuted).isZero();
  }

  @Test
  void catalogoIncompativelInterrompeAntesDeCriarEstruturas() {
    jdbc.update(
        "INSERT INTO ativos(codigo,ticker,nome,tipo,casas_decimais) VALUES"
            + " ('outro:BTC','BTC','Preservar','CRIPTO',8)");
    assertThatThrownBy(() -> Flyway.configure().dataSource(fonte).target("9").load().migrate())
        .hasStackTraceContaining("ck_catalogo_saldo_compativel");
    assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM ativos", Integer.class)).isEqualTo(1);
    assertThat(
            jdbc.queryForObject(
                "SELECT COUNT(*) FROM information_schema.tables WHERE table_schema=DATABASE() AND"
                    + " table_name='operacoes'",
                Integer.class))
        .isZero();
  }

  @Test
  void bancoProtegeConfirmacaoHistoricoEJustificativaZero() {
    Flyway.configure().dataSource(fonte).target("9").load().migrate();
    jdbc.update(
        "INSERT INTO pontos_partida VALUES"
            + " ('a','2026-10-01',UTC_TIMESTAMP(6),'a',UTC_TIMESTAMP(6),'a')");
    var instituicao =
        jdbc.queryForObject("SELECT id FROM instituicoes WHERE codigo='binance'", Long.class);
    var ativo = jdbc.queryForObject("SELECT id FROM ativos WHERE ticker='BTC'", Long.class);
    var sql =
        "INSERT INTO"
            + " operacoes(perfil_ref,instituicao_id,ativo_id,tipo,data_referencia,ocorrida_em,quantidade,custo_total_brl,situacao,chave_requisicao,fingerprint,criado_em,criado_por,alterado_em,alterado_por)"
            + " VALUES ('a',?,?,'SALDO_INICIAL','2026-10-01','2026-10-01"
            + " 03:00:00',1,?,'CONFIRMADA',?,REPEAT('a',64),UTC_TIMESTAMP(6),'a',UTC_TIMESTAMP(6),'a')";
    jdbc.update(sql, instituicao, ativo, 10, java.util.UUID.randomUUID().toString());
    assertThatThrownBy(
            () -> jdbc.update(sql, instituicao, ativo, 20, java.util.UUID.randomUUID().toString()))
        .isInstanceOf(DataIntegrityViolationException.class);
    jdbc.update("UPDATE operacoes SET situacao='CANCELADA'");
    jdbc.update(sql, instituicao, ativo, 20, java.util.UUID.randomUUID().toString());
    jdbc.update("UPDATE operacoes SET situacao='CANCELADA'");
    assertThatThrownBy(
            () -> jdbc.update(sql, instituicao, ativo, 0, java.util.UUID.randomUUID().toString()))
        .rootCause()
        .isInstanceOf(java.sql.SQLException.class)
        .hasMessageContaining("ck_abertura_zero");
    assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM operacoes", Integer.class)).isEqualTo(2);
  }
}
