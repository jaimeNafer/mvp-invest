package io.github.jaimenafer.lastro;

import static org.assertj.core.api.Assertions.*;

import java.util.UUID;
import org.flywaydb.core.Flyway;
import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DriverManagerDataSource;
import org.testcontainers.junit.jupiter.*;
import org.testcontainers.mysql.MySQLContainer;

@Testcontainers
class NegociacoesMigrationTests {
  @Container static final MySQLContainer MYSQL = new MySQLContainer("mysql:8.0.42");

  @Test
  void atualizaV9PreservandoFatosECadeiaDeVersoes() {
    var fonte =
        new DriverManagerDataSource(MYSQL.getJdbcUrl(), MYSQL.getUsername(), MYSQL.getPassword());
    var jdbc = new JdbcTemplate(fonte);
    Flyway.configure().dataSource(fonte).cleanDisabled(false).load().clean();
    Flyway.configure().dataSource(fonte).target("9").load().migrate();
    jdbc.update(
        "INSERT INTO pontos_partida VALUES"
            + " ('a','2026-10-01',UTC_TIMESTAMP(6),'a',UTC_TIMESTAMP(6),'a')");
    long instituicao =
        jdbc.queryForObject("SELECT id FROM instituicoes WHERE codigo='binance'", Long.class);
    long ativo = jdbc.queryForObject("SELECT id FROM ativos WHERE ticker='BTC'", Long.class);
    String inserir =
        "INSERT INTO"
            + " operacoes(perfil_ref,instituicao_id,ativo_id,tipo,data_referencia,ocorrida_em,quantidade,custo_total_brl,situacao,substitui_id,chave_requisicao,fingerprint,criado_em,criado_por,alterado_em,alterado_por)"
            + " VALUES ('a',?,?,'SALDO_INICIAL','2026-10-01','2026-10-01"
            + " 03:00:00',1,100,?,?,?,REPEAT('a',64),UTC_TIMESTAMP(6),'a',UTC_TIMESTAMP(6),'a')";
    Long anterior = null;
    long raiz = 0;
    for (int i = 0; i < 3; i++) {
      jdbc.update(
          inserir,
          instituicao,
          ativo,
          i == 2 ? "CONFIRMADA" : "SUBSTITUIDA",
          anterior,
          UUID.randomUUID().toString());
      anterior = jdbc.queryForObject("SELECT MAX(id) FROM operacoes", Long.class);
      if (i == 0) raiz = anterior;
    }
    String colunas =
        "id,perfil_ref,instituicao_id,ativo_id,tipo,data_referencia,ocorrida_em,quantidade,custo_total_brl,situacao,substitui_id,chave_requisicao,fingerprint,criado_em,criado_por,alterado_em,alterado_por";
    var antes = jdbc.queryForList("SELECT " + colunas + " FROM operacoes ORDER BY id");
    var flyway = Flyway.configure().dataSource(fonte).load();
    assertThat(flyway.migrate().migrationsExecuted).isEqualTo(1);
    assertThat(jdbc.queryForList("SELECT " + colunas + " FROM operacoes ORDER BY id"))
        .isEqualTo(antes);
    assertThat(jdbc.queryForList("SELECT DISTINCT ordem_livro FROM operacoes", Long.class))
        .containsExactly(raiz);
    assertThat(jdbc.queryForObject("SELECT revisao FROM pontos_partida", Long.class)).isZero();
    assertThat(
            jdbc.queryForObject(
                "SELECT COUNT(*) FROM information_schema.columns WHERE table_schema=DATABASE() AND"
                    + " table_name IN ('operacoes','pontos_partida') AND TRIM(column_comment)=''",
                Integer.class))
        .isZero();
    assertThat(flyway.migrate().migrationsExecuted).isZero();
  }

  @Test
  void preservaCadeiaMaiorQueLimitePadraoDeRecursao() {
    var fonte =
        new DriverManagerDataSource(MYSQL.getJdbcUrl(), MYSQL.getUsername(), MYSQL.getPassword());
    var jdbc = new JdbcTemplate(fonte);
    Flyway.configure().dataSource(fonte).cleanDisabled(false).load().clean();
    Flyway.configure().dataSource(fonte).target("9").load().migrate();
    jdbc.update(
        "INSERT INTO pontos_partida VALUES"
            + " ('a','2026-10-01',UTC_TIMESTAMP(6),'a',UTC_TIMESTAMP(6),'a')");
    long instituicao =
        jdbc.queryForObject("SELECT id FROM instituicoes WHERE codigo='binance'", Long.class);
    long ativo = jdbc.queryForObject("SELECT id FROM ativos WHERE ticker='BTC'", Long.class);
    String sql =
        "INSERT INTO"
            + " operacoes(id,perfil_ref,instituicao_id,ativo_id,tipo,data_referencia,ocorrida_em,quantidade,custo_total_brl,situacao,substitui_id,chave_requisicao,fingerprint,criado_em,criado_por,alterado_em,alterado_por)"
            + " VALUES (?,'a',?,?,'SALDO_INICIAL','2026-10-01','2026-10-01"
            + " 03:00:00',1,100,?,?,?,REPEAT('a',64),UTC_TIMESTAMP(6),'a',UTC_TIMESTAMP(6),'a')";
    var registros = new java.util.ArrayList<Object[]>();
    for (int id = 1; id <= 1105; id++)
      registros.add(
          new Object[] {
            id,
            instituicao,
            ativo,
            id == 1105 ? "CONFIRMADA" : "SUBSTITUIDA",
            id == 1 ? null : id - 1,
            UUID.randomUUID().toString()
          });
    jdbc.batchUpdate(sql, registros);
    var flyway =
        Flyway.configure()
            .dataSource(fonte)
            .initSql("SET SESSION cte_max_recursion_depth = 1000")
            .load();
    assertThat(flyway.migrate().migrationsExecuted).isEqualTo(1);
    assertThat(jdbc.queryForList("SELECT DISTINCT ordem_livro FROM operacoes", Long.class))
        .containsExactly(1L);
    assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM operacoes", Integer.class))
        .isEqualTo(1105);
  }
}
