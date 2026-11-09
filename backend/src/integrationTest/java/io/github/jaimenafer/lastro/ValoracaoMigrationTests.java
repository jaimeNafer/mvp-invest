package io.github.jaimenafer.lastro;

import static org.assertj.core.api.Assertions.*;

import org.flywaydb.core.Flyway;
import org.junit.jupiter.api.Test;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DriverManagerDataSource;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.mysql.MySQLContainer;

@Testcontainers
class ValoracaoMigrationTests {
  @Container static final MySQLContainer MYSQL = new MySQLContainer("mysql:8.0.42");

  @Test
  void criaCacheGlobalSemDuplicarCandlesEPreservaCatalogo() {
    var fonte = new DriverManagerDataSource(MYSQL.getJdbcUrl(), MYSQL.getUsername(), MYSQL.getPassword());
    var jdbc = new JdbcTemplate(fonte);
    Flyway.configure().dataSource(fonte).target("15").load().migrate();
    var catalogo = jdbc.queryForList("SELECT * FROM ativos ORDER BY id");
    var flyway = Flyway.configure().dataSource(fonte).target("16").load();
    assertThat(flyway.migrate().migrationsExecuted).isEqualTo(1);
    assertThat(jdbc.queryForList("SELECT * FROM ativos ORDER BY id")).isEqualTo(catalogo);
    Long base = jdbc.queryForObject("SELECT id FROM ativos WHERE codigo='cripto:BTC'", Long.class);
    Long moeda = jdbc.queryForObject("SELECT id FROM ativos WHERE codigo='cripto:USDT'", Long.class);
    jdbc.update("INSERT INTO pares_cotacao(ativo_base_id,ativo_cotacao_id,fonte,simbolo) VALUES (?,?,'BINANCE','BTCUSDT')", base, moeda);
    Long par = jdbc.queryForObject("SELECT id FROM pares_cotacao WHERE simbolo='BTCUSDT'", Long.class);
    String inserir = "INSERT INTO cotacoes_historicas(par_id,granularidade,instante,preco,obtido_em) VALUES (?,'HORA','2024-01-01 00:00:00',10,UTC_TIMESTAMP(6))";
    jdbc.update(inserir, par);
    assertThatThrownBy(() -> jdbc.update(inserir, par)).isInstanceOf(DataIntegrityViolationException.class);
    assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM information_schema.columns WHERE table_schema=DATABASE() AND table_name IN ('pares_cotacao','cotacoes_historicas','cargas_cotacao') AND column_comment=''", Integer.class)).isZero();
    Long cotacao = jdbc.queryForObject("SELECT id FROM cotacoes_historicas WHERE par_id=?", Long.class, par);
    jdbc.update("INSERT INTO historico_cotacoes(cotacao_id,versao,preco,checksum,obtido_em) VALUES (?,1,10,NULL,UTC_TIMESTAMP(6))", cotacao);
    assertThatThrownBy(() -> jdbc.update("INSERT INTO historico_cotacoes(cotacao_id,versao,preco,checksum,obtido_em) VALUES (?,1,10,NULL,UTC_TIMESTAMP(6))", cotacao)).isInstanceOf(DataIntegrityViolationException.class);
    assertThat(flyway.migrate().migrationsExecuted).isZero();
    jdbc.update("""
        INSERT INTO cargas_cotacao(par_id,granularidade,inicio_em,fim_em,estado,atualizada_em)
        VALUES (?,'HORA','2024-01-01','2024-02-01','CONCLUIDA','2024-01-31 23:59:59'),
               (?,'DIA','2024-01-01','2024-02-01','FALHA','2024-02-02')
        """, par, par);
    var atual = Flyway.configure().dataSource(fonte).load();
    assertThat(atual.migrate().migrationsExecuted).isEqualTo(1);
    assertThat(jdbc.queryForList("SELECT estado FROM cargas_cotacao ORDER BY id", String.class))
        .containsExactly("PARCIAL", "FALHA_ESGOTADA");
    assertThat(jdbc.queryForObject("SELECT preco FROM cotacoes_historicas WHERE id=?",
        java.math.BigDecimal.class, cotacao)).isEqualByComparingTo("10");
    assertThat(atual.migrate().migrationsExecuted).isZero();
  }
}
