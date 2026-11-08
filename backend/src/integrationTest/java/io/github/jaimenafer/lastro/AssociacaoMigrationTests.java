package io.github.jaimenafer.lastro;

import static org.assertj.core.api.Assertions.*;

import org.flywaydb.core.Flyway;
import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DriverManagerDataSource;
import org.testcontainers.junit.jupiter.*;
import org.testcontainers.mysql.MySQLContainer;

@Testcontainers
class AssociacaoMigrationTests {
  @Container static final MySQLContainer MYSQL = new MySQLContainer("mysql:8.0.42");

  @Test
  void atualizaV13SemAlterarDadosEProtegeUnicidadeDaLinha() {
    var fonte =
        new DriverManagerDataSource(MYSQL.getJdbcUrl(), MYSQL.getUsername(), MYSQL.getPassword());
    var jdbc = new JdbcTemplate(fonte);
    Flyway.configure().dataSource(fonte).target("13").load().migrate();
    long instituicao =
        jdbc.queryForObject("SELECT id FROM instituicoes WHERE codigo='binance'", Long.class);
    jdbc.update(
        "INSERT INTO"
            + " registros_conferencia(perfil_ref,origem_ref,instituicao_id,original_json,erros_json,proposta_json,versao,criado_em)"
            + " VALUES ('migracao','linha:1',?,'{}','[]','{}',1,UTC_TIMESTAMP(6))",
        instituicao);
    var antes = jdbc.queryForList("SELECT * FROM registros_conferencia");
    var catalogo = jdbc.queryForList("SELECT * FROM ativos");
    var flyway = Flyway.configure().dataSource(fonte).target("14").load();
    assertThat(flyway.migrate().migrationsExecuted).isEqualTo(1);
    assertThat(jdbc.queryForList("SELECT * FROM registros_conferencia")).isEqualTo(antes);
    assertThat(jdbc.queryForList("SELECT * FROM ativos")).isEqualTo(catalogo);
    assertThat(flyway.migrate().migrationsExecuted).isZero();
    String tabelas =
        "('grupos_associacao','linhas_grupos_associacao','historico_grupos_associacao')";
    assertThat(
            jdbc.queryForObject(
                "SELECT COUNT(*) FROM information_schema.columns WHERE table_schema=DATABASE() AND"
                    + " table_name IN "
                    + tabelas
                    + " AND column_comment=''",
                Integer.class))
        .isZero();
    assertThat(
            jdbc.queryForObject(
                "SELECT COUNT(*) FROM information_schema.tables WHERE table_schema=DATABASE() AND"
                    + " table_name IN "
                    + tabelas
                    + " AND table_comment<>''",
                Integer.class))
        .isEqualTo(3);
    jdbc.update(
        "INSERT INTO"
            + " grupos_associacao(perfil_ref,proposta_json,versao,alterado_por,alterado_em,criado_em)"
            + " VALUES"
            + " ('migracao','{}',1,'migracao',UTC_TIMESTAMP(6),UTC_TIMESTAMP(6)),('migracao','{}',1,'migracao',UTC_TIMESTAMP(6),UTC_TIMESTAMP(6))");
    long linha =
        jdbc.queryForObject(
            "SELECT id FROM registros_conferencia WHERE perfil_ref='migracao'", Long.class);
    var grupos = jdbc.queryForList("SELECT id FROM grupos_associacao ORDER BY id", Long.class);
    jdbc.update(
        "INSERT INTO linhas_grupos_associacao(registro_id,grupo_id) VALUES (?,?)",
        linha,
        grupos.getFirst());
    assertThatThrownBy(() ->
            jdbc.update(
                "INSERT INTO linhas_grupos_associacao(registro_id,grupo_id) VALUES (?,?)",
                linha,
                grupos.getLast()))
        .isInstanceOf(org.springframework.dao.DataIntegrityViolationException.class);
    jdbc.update("INSERT INTO ativos(codigo,ticker,nome,tipo,casas_decimais,ativo) VALUES ('cripto:BNB','BNB','Nome preservado','CRIPTO',18,FALSE)");
    var antesV15 = jdbc.queryForMap("SELECT * FROM ativos WHERE codigo='cripto:BNB'");
    var atual = Flyway.configure().dataSource(fonte).load();
    assertThat(atual.migrate().migrationsExecuted).isEqualTo(1);
    assertThat(jdbc.queryForMap("SELECT * FROM ativos WHERE codigo='cripto:BNB'")).isEqualTo(antesV15);
    assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM ativos WHERE codigo IN ('cripto:LISTA','cripto:IO')", Integer.class)).isEqualTo(2);
    assertThat(atual.migrate().migrationsExecuted).isZero();
  }
}
