package io.github.jaimenafer.lastro;

import static org.assertj.core.api.Assertions.*;

import io.github.jaimenafer.lastro.operacoes.domain.repository.IHistoricoCotacaoRepository;
import java.math.BigDecimal;
import java.time.Instant;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.annotation.Transactional;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.mysql.MySQLContainer;

@SpringBootTest(properties = "spring.config.import=")
@Testcontainers
class CacheCotacaoIntegrationTests {
  @Container @ServiceConnection static final MySQLContainer MYSQL = new MySQLContainer("mysql:8.0.42");
  @Autowired JdbcTemplate jdbc;
  @Autowired IHistoricoCotacaoRepository cotacoes;

  @Test
  @Transactional
  void registraGlobalmenteComIdempotenciaEPreservaVersaoAnterior() {
    Long base = jdbc.queryForObject("SELECT id FROM ativos WHERE codigo='cripto:BTC'", Long.class);
    Long moeda = jdbc.queryForObject("SELECT id FROM ativos WHERE codigo='cripto:USDT'", Long.class);
    jdbc.update("INSERT INTO pares_cotacao(ativo_base_id,ativo_cotacao_id,fonte,simbolo) VALUES (?,?,'BINANCE','BTCUSDT')", base, moeda);
    Long par = jdbc.queryForObject("SELECT id FROM pares_cotacao WHERE simbolo='BTCUSDT'", Long.class);
    Instant instante = Instant.parse("2024-01-01T12:00:00Z");
    Instant obtida = Instant.parse("2024-02-01T00:00:00Z");
    cotacoes.registrar(par, "HORA", instante, new BigDecimal("12.34"), "a".repeat(64), obtida);
    cotacoes.registrar(par, "HORA", instante, new BigDecimal("12.340"), "a".repeat(64), obtida.plusSeconds(2));
    assertThat(jdbc.queryForObject("SELECT versao FROM cotacoes_historicas WHERE par_id=?", Long.class, par)).isEqualTo(1);
    cotacoes.registrar(par, "HORA", instante, new BigDecimal("13"), "b".repeat(64), obtida.plusSeconds(3));
    assertThat(jdbc.queryForObject("SELECT versao FROM cotacoes_historicas WHERE par_id=?", Long.class, par)).isEqualTo(2);
    assertThat(jdbc.queryForObject("SELECT preco FROM historico_cotacoes", BigDecimal.class)).isEqualByComparingTo("12.34");
    assertThat(cotacoes.buscar(instante, instante.plusSeconds(1), "HORA"))
        .singleElement().satisfies(c -> { assertThat(c.preco()).isEqualByComparingTo("13"); assertThat(c.fonte()).isEqualTo("BINANCE"); assertThat(c.versao()).isEqualTo(2); assertThat(c.checksum()).isEqualTo("b".repeat(64)); });
  }
}
