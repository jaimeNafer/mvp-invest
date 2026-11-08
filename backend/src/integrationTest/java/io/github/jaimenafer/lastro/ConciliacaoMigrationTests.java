package io.github.jaimenafer.lastro;

import static org.assertj.core.api.Assertions.*;

import org.flywaydb.core.Flyway;
import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DriverManagerDataSource;
import org.testcontainers.junit.jupiter.*;
import org.testcontainers.mysql.MySQLContainer;

@Testcontainers
class ConciliacaoMigrationTests {
  @Container static final MySQLContainer MYSQL = new MySQLContainer("mysql:8.0.42");

  @Test
  void atualizaV12PreservandoFatosECriandoCustodiaNaoEspecificada() {
    var fonte =
        new DriverManagerDataSource(MYSQL.getJdbcUrl(), MYSQL.getUsername(), MYSQL.getPassword());
    var jdbc = new JdbcTemplate(fonte);
    Flyway.configure().dataSource(fonte).target("12").load().migrate();
    jdbc.update(
        "INSERT INTO"
            + " pontos_partida(perfil_ref,data_referencia,criado_em,criado_por,alterado_em,alterado_por)"
            + " VALUES"
            + " ('migracao','2024-01-01',UTC_TIMESTAMP(6),'migracao',UTC_TIMESTAMP(6),'migracao')");
    long inst =
        jdbc.queryForObject("SELECT id FROM instituicoes WHERE codigo='binance'", Long.class);
    long ativo = jdbc.queryForObject("SELECT id FROM ativos WHERE codigo='cripto:BTC'", Long.class);
    jdbc.update(
        "INSERT INTO"
            + " operacoes(perfil_ref,instituicao_id,ativo_id,tipo,data_referencia,ocorrida_em,quantidade,custo_total_brl,situacao,chave_requisicao,fingerprint,criado_em,criado_por,alterado_em,alterado_por,ordem_livro)"
            + " VALUES ('migracao',?,?,'SALDO_INICIAL','2024-01-01','2024-01-01"
            + " 03:00:00',1,100,'CONFIRMADA',UUID(),REPEAT('a',64),UTC_TIMESTAMP(6),'migracao',UTC_TIMESTAMP(6),'migracao',1)",
        inst,
        ativo);
    var antes = jdbc.queryForList("SELECT * FROM operacoes");
    var flyway = Flyway.configure().dataSource(fonte).target("13").load();
    assertThat(flyway.migrate().migrationsExecuted).isEqualTo(1);
    assertThat(jdbc.queryForList("SELECT * FROM operacoes")).isEqualTo(antes);
    assertThat(jdbc.queryForList("SELECT nome FROM contas_custodia", String.class))
        .containsExactly("Não especificada");
    assertThat(flyway.migrate().migrationsExecuted).isZero();
    String tabelas =
        "('contas_custodia','registros_conferencia','ajustes_conciliatorios','comandos_conciliacao','arquivos_recebidos','historico_propostas')";
    assertThat(
            jdbc.queryForObject(
                "SELECT COUNT(*) FROM information_schema.tables WHERE table_schema=DATABASE() AND"
                    + " table_name IN "
                    + tabelas
                    + " AND table_comment<>''",
                Integer.class))
        .isEqualTo(6);
    assertThat(
            jdbc.queryForObject(
                "SELECT COUNT(*) FROM information_schema.columns WHERE table_schema=DATABASE() AND"
                    + " table_name IN "
                    + tabelas
                    + " AND column_comment=''",
                Integer.class))
        .isZero();
  }
}
