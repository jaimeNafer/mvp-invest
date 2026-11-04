package io.github.jaimenafer.lastro;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.UUID;
import org.flywaydb.core.Flyway;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DriverManagerDataSource;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.mysql.MySQLContainer;

@Testcontainers
class CatalogoAtivosMigrationTests {
  @Container static final MySQLContainer MYSQL = new MySQLContainer("mysql:8.0.42");
  private DriverManagerDataSource fonte;
  private JdbcTemplate jdbc;

  @BeforeEach
  void prepararV10() {
    fonte =
        new DriverManagerDataSource(MYSQL.getJdbcUrl(), MYSQL.getUsername(), MYSQL.getPassword());
    jdbc = new JdbcTemplate(fonte);
    Flyway.configure().dataSource(fonte).cleanDisabled(false).load().clean();
    Flyway.configure().dataSource(fonte).target("10").load().migrate();
  }

  @Test
  void adicionaCatalogoSemAlterarFatosMetadadosOuAtivosAnteriores() {
    jdbc.update(
        "INSERT INTO"
            + " pontos_partida(perfil_ref,data_referencia,criado_em,criado_por,alterado_em,alterado_por,revisao)"
            + " VALUES ('a','2026-10-01',UTC_TIMESTAMP(6),'a',UTC_TIMESTAMP(6),'a',7)");
    jdbc.update(
        "INSERT INTO"
            + " operacoes(perfil_ref,instituicao_id,ativo_id,tipo,data_referencia,ocorrida_em,quantidade,custo_total_brl,situacao,chave_requisicao,fingerprint,criado_em,criado_por,alterado_em,alterado_por,ordem_livro,versao_politica)"
            + " SELECT 'a',i.id,a.id,'SALDO_INICIAL','2026-10-01','2026-10-01"
            + " 03:00:00',1,100,'CONFIRMADA',?,REPEAT('a',64),UTC_TIMESTAMP(6),'a',UTC_TIMESTAMP(6),'a',1,1"
            + " FROM instituicoes i JOIN ativos a ON a.codigo='cripto:BTC' WHERE"
            + " i.codigo='binance'",
        UUID.randomUUID().toString());
    var fatos = jdbc.queryForList("SELECT * FROM operacoes ORDER BY id");
    var pontos = jdbc.queryForList("SELECT * FROM pontos_partida ORDER BY perfil_ref");
    var catalogo = jdbc.queryForList("SELECT * FROM ativos ORDER BY id");
    var comentarios =
        jdbc.queryForList(
            "SELECT TABLE_NAME,COLUMN_NAME,COLUMN_COMMENT FROM information_schema.columns WHERE"
                + " TABLE_SCHEMA=DATABASE() ORDER BY TABLE_NAME,ORDINAL_POSITION");
    var flyway = Flyway.configure().dataSource(fonte).target("11").load();
    assertThat(flyway.migrate().migrationsExecuted).isEqualTo(1);
    assertThat(jdbc.queryForList("SELECT * FROM operacoes ORDER BY id")).isEqualTo(fatos);
    assertThat(jdbc.queryForList("SELECT * FROM pontos_partida ORDER BY perfil_ref"))
        .isEqualTo(pontos);
    assertThat(jdbc.queryForList("SELECT * FROM ativos ORDER BY id"))
        .containsAll(catalogo)
        .hasSize(14);
    assertThat(
            jdbc.queryForList(
                "SELECT TABLE_NAME,COLUMN_NAME,COLUMN_COMMENT FROM information_schema.columns WHERE"
                    + " TABLE_SCHEMA=DATABASE() ORDER BY TABLE_NAME,ORDINAL_POSITION"))
        .isEqualTo(comentarios);
    assertThat(
            jdbc.queryForList(
                "SELECT ticker FROM ativos WHERE codigo IN"
                    + " ('cripto:SOL','cripto:LINK','cripto:NEAR','cripto:LDO','cripto:ARKM','cripto:RONIN','cripto:PYTH','cripto:TIA')"
                    + " AND tipo='CRIPTO' AND casas_decimais=18 AND ativo=TRUE",
                String.class))
        .hasSize(8);
    assertThat(flyway.migrate().migrationsExecuted).isZero();
  }

  @Test
  void preservaAtivoCompativelInativoComNomeEIdExistentes() {
    jdbc.update(
        "INSERT INTO ativos(id,codigo,ticker,nome,tipo,casas_decimais,ativo) VALUES"
            + " (80,'cripto:SOL','SOL','Nome preservado','CRIPTO',18,FALSE)");
    var anterior = jdbc.queryForMap("SELECT * FROM ativos WHERE id=80");
    Flyway.configure().dataSource(fonte).target("11").load().migrate();
    assertThat(jdbc.queryForMap("SELECT * FROM ativos WHERE id=80")).isEqualTo(anterior);
    assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM ativos", Integer.class)).isEqualTo(14);
  }

  @ParameterizedTest
  @CsvSource({
    "outro:SOL,SOL,CRIPTO,18",
    "cripto:SOL,SOLX,CRIPTO,18",
    "cripto:SOL,SOL,FIAT,18",
    "cripto:SOL,SOL,cripto,18",
    "cripto:SOL,SOL,CRIPTO,8",
    "cripto:sol,SOL,CRIPTO,18"
  })
  void recusaCatalogoIncompativelSemInsercaoParcial(
      String codigo, String ticker, String tipo, int casas) {
    jdbc.update(
        "INSERT INTO ativos(codigo,ticker,nome,tipo,casas_decimais,ativo) VALUES"
            + " (?,?,'Existente',?,?,TRUE)",
        codigo,
        ticker,
        tipo,
        casas);
    var antes = jdbc.queryForList("SELECT * FROM ativos ORDER BY id");
    assertThatThrownBy(() -> Flyway.configure().dataSource(fonte).target("11").load().migrate())
        .hasStackTraceContaining("ck_catalogo_cripto_v11_compativel");
    assertThat(jdbc.queryForList("SELECT * FROM ativos ORDER BY id")).isEqualTo(antes);
  }
}
