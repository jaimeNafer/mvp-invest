package io.github.jaimenafer.lastro;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

import io.github.jaimenafer.lastro.operacoes.application.port.IProvedorCotacoes;
import io.github.jaimenafer.lastro.operacoes.application.usecase.IProcessamentoCotacoes;
import io.github.jaimenafer.lastro.operacoes.domain.model.LotePrecos;
import io.github.jaimenafer.lastro.operacoes.domain.model.PrecoCandle;
import io.github.jaimenafer.lastro.operacoes.domain.repository.ICargaCotacaoRepository;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.YearMonth;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.transaction.support.TransactionTemplate;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.mysql.MySQLContainer;

@SpringBootTest(properties = {"spring.config.import=", "lastro.valoracao.processamento-ativo=false"})
@Testcontainers
class ProcessamentoCotacoesIntegrationTests {
  @Container @ServiceConnection static final MySQLContainer MYSQL = new MySQLContainer("mysql:8.0.42");
  @Autowired JdbcTemplate jdbc;
  @Autowired ICargaCotacaoRepository cargas;
  @Autowired IProcessamentoCotacoes processamento;
  @Autowired TransactionTemplate transacao;
  @MockitoBean IProvedorCotacoes provedor;

  @Test
  void baixaForaDaTransacaoEGravaUmaUnicaVez() {
    Long base = jdbc.queryForObject("SELECT id FROM ativos WHERE codigo='cripto:BTC'", Long.class);
    Long moeda = jdbc.queryForObject("SELECT id FROM ativos WHERE codigo='cripto:USDT'", Long.class);
    jdbc.update("INSERT INTO pares_cotacao(ativo_base_id,ativo_cotacao_id,fonte,simbolo) VALUES (?,?,'BINANCE','BTCUSDT')", base, moeda);
    Long par = jdbc.queryForObject("SELECT id FROM pares_cotacao WHERE simbolo='BTCUSDT'", Long.class);
    Instant inicio = Instant.parse("2024-01-01T00:00:00Z"), fim = Instant.parse("2024-02-01T00:00:00Z");
    transacao.executeWithoutResult(status -> cargas.agendar(par, "HORA", inicio, fim, Instant.now()));
    when(provedor.consultar("BTCUSDT", "HORA", YearMonth.of(2024, 1)))
        .thenAnswer(chamada -> {
          assertThat(org.springframework.transaction.support.TransactionSynchronizationManager.isActualTransactionActive()).isFalse();
          return new LotePrecos(List.of(new PrecoCandle(inicio, new BigDecimal("42000"))), "a".repeat(64));
        });
    assertThat(processamento.processarProxima()).isTrue();
    assertThat(processamento.processarProxima()).isFalse();
    assertThat(jdbc.queryForObject("SELECT preco FROM cotacoes_historicas WHERE par_id=?", BigDecimal.class, par))
        .isEqualByComparingTo("42000");
    assertThat(jdbc.queryForObject("SELECT estado FROM cargas_cotacao WHERE par_id=?", String.class, par))
        .isEqualTo("CONCLUIDA");
    verify(provedor, times(1)).consultar("BTCUSDT", "HORA", YearMonth.of(2024, 1));
  }

  @Test
  void loteInvalidoNaoGravaPrecosEAgendaNovaTentativa() {
    Long base = jdbc.queryForObject("SELECT id FROM ativos WHERE codigo='cripto:ETH'", Long.class);
    Long moeda = jdbc.queryForObject("SELECT id FROM ativos WHERE codigo='cripto:USDT'", Long.class);
    jdbc.update("INSERT INTO pares_cotacao(ativo_base_id,ativo_cotacao_id,fonte,simbolo) VALUES (?,?,'BINANCE','ETHUSDT')", base, moeda);
    Long par = jdbc.queryForObject("SELECT id FROM pares_cotacao WHERE simbolo='ETHUSDT'", Long.class);
    Instant inicio = Instant.parse("2024-01-01T00:00:00Z"), fim = Instant.parse("2024-02-01T00:00:00Z");
    transacao.executeWithoutResult(status -> cargas.agendar(par, "HORA", inicio, fim, Instant.now()));
    when(provedor.consultar("ETHUSDT", "HORA", YearMonth.of(2024, 1))).thenReturn(
        new LotePrecos(List.of(new PrecoCandle(inicio, BigDecimal.ONE),
            new PrecoCandle(inicio.plusSeconds(3600), BigDecimal.ZERO)), "b".repeat(64)));
    assertThat(processamento.processarProxima()).isTrue();
    assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM cotacoes_historicas WHERE par_id=?", Integer.class, par)).isZero();
    assertThat(jdbc.queryForObject("SELECT estado FROM cargas_cotacao WHERE par_id=?", String.class, par)).isEqualTo("FALHA");
    assertThat(processamento.processarProxima()).isFalse();
  }

  @Test
  void resultadoVazioEhRegistradoESemRepeticaoImediata() {
    Long base = jdbc.queryForObject("SELECT id FROM ativos WHERE codigo='cripto:SOL'", Long.class);
    Long moeda = jdbc.queryForObject("SELECT id FROM ativos WHERE codigo='cripto:USDT'", Long.class);
    jdbc.update("INSERT INTO pares_cotacao(ativo_base_id,ativo_cotacao_id,fonte,simbolo) VALUES (?,?,'BINANCE','SOLUSDT')", base, moeda);
    Long par = jdbc.queryForObject("SELECT id FROM pares_cotacao WHERE simbolo='SOLUSDT'", Long.class);
    Instant inicio = Instant.parse("2024-01-01T00:00:00Z"), fim = Instant.parse("2024-02-01T00:00:00Z");
    transacao.executeWithoutResult(status -> cargas.agendar(par, "HORA", inicio, fim, Instant.now()));
    when(provedor.consultar("SOLUSDT", "HORA", YearMonth.of(2024, 1)))
        .thenReturn(new LotePrecos(List.of(), null));
    assertThat(processamento.processarProxima()).isTrue();
    assertThat(jdbc.queryForObject("SELECT estado FROM cargas_cotacao WHERE par_id=?", String.class, par)).isEqualTo("SEM_DADOS");
    assertThat(processamento.processarProxima()).isFalse();
  }
}
