package io.github.jaimenafer.lastro;

import static org.assertj.core.api.Assertions.*;

import io.github.jaimenafer.lastro.operacoes.domain.repository.ICargaCotacaoRepository;
import java.time.Instant;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.mysql.MySQLContainer;

@SpringBootTest(properties = {"spring.config.import=", "lastro.valoracao.processamento-ativo=false"})
@Testcontainers
class CargaCotacaoIntegrationTests {
  @Container @ServiceConnection static final MySQLContainer MYSQL = new MySQLContainer("mysql:8.0.42");
  @Autowired JdbcTemplate jdbc;
  @Autowired ICargaCotacaoRepository cargas;
  @Autowired PlatformTransactionManager transacoes;

  @Test
  @Transactional
  void agendaUmaVezERetomaReservaExpiradaSemAceitarTrabalhadorAntigo() {
    Long base = jdbc.queryForObject("SELECT id FROM ativos WHERE codigo='cripto:BTC'", Long.class);
    Long moeda = jdbc.queryForObject("SELECT id FROM ativos WHERE codigo='cripto:USDT'", Long.class);
    jdbc.update("INSERT INTO pares_cotacao(ativo_base_id,ativo_cotacao_id,fonte,simbolo) VALUES (?,?,'BINANCE','BTCUSDT')",base,moeda);
    Long par = jdbc.queryForObject("SELECT id FROM pares_cotacao WHERE simbolo='BTCUSDT'",Long.class);
    Instant inicio=Instant.parse("2024-01-01T00:00:00Z"), fim=Instant.parse("2024-02-01T00:00:00Z"), agora=Instant.parse("2024-03-01T00:00:00Z");
    cargas.agendar(par,"HORA",inicio,fim,agora);
    cargas.agendar(par,"HORA",inicio,fim,agora);
    assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM cargas_cotacao",Integer.class)).isEqualTo(1);
    var primeira = cargas.reservar(agora,agora.plusSeconds(60),"primeiro").orElseThrow();
    assertThat(cargas.reservar(agora,agora.plusSeconds(60),"outro")).isEmpty();
    var retomada = cargas.reservar(agora.plusSeconds(61),agora.plusSeconds(120),"segundo").orElseThrow();
    assertThat(retomada.id()).isEqualTo(primeira.id());
    assertThat(cargas.concluir(primeira.id(),"primeiro",false,agora.plusSeconds(62))).isFalse();
    assertThat(cargas.concluir(retomada.id(),"segundo",true,agora.plusSeconds(62))).isTrue();
    assertThat(jdbc.queryForObject("SELECT estado FROM cargas_cotacao WHERE id=?",String.class,retomada.id())).isEqualTo("SEM_DADOS");
  }

  @Test
  @Transactional
  void respeitaPrazoDeRetentativaERejeitaFalhaDeReservaVencida() {
    Long base = jdbc.queryForObject("SELECT id FROM ativos WHERE codigo='cripto:ETH'", Long.class);
    Long moeda = jdbc.queryForObject("SELECT id FROM ativos WHERE codigo='cripto:USDT'", Long.class);
    jdbc.update("INSERT INTO pares_cotacao(ativo_base_id,ativo_cotacao_id,fonte,simbolo) VALUES (?,?,'BINANCE','ETHUSDT')", base, moeda);
    Long par = jdbc.queryForObject("SELECT id FROM pares_cotacao WHERE simbolo='ETHUSDT'", Long.class);
    Instant agora = Instant.parse("2024-03-01T00:00:00Z");
    cargas.agendar(par, "HORA", agora.minusSeconds(3600), agora, agora);
    var carga = cargas.reservar(agora, agora.plusSeconds(60), "reserva").orElseThrow();
    assertThat(cargas.falhar(carga.id(), "outra", agora.plusSeconds(1), agora.plusSeconds(30))).isFalse();
    assertThat(cargas.falhar(carga.id(), "reserva", agora.plusSeconds(1), agora.plusSeconds(30))).isTrue();
    assertThat(cargas.reservar(agora.plusSeconds(29), agora.plusSeconds(90), "nova")).isEmpty();
    assertThat(cargas.reservar(agora.plusSeconds(30), agora.plusSeconds(90), "nova")).isPresent();
  }

  @Test
  @Transactional
  void reabreCargaEsgotadaEFechaPeriodoQueFoiConsultadoAntesDaVirada() {
    cargas.prepararParesConhecidos();
    Long par = jdbc.queryForObject("SELECT id FROM pares_cotacao WHERE simbolo='BTCUSDT'", Long.class);
    Instant fim = Instant.parse("2024-07-01T00:00:00Z");
    Instant inicio = Instant.parse("2024-06-01T00:00:00Z");
    Instant antes = fim.minusSeconds(60);
    cargas.agendar(par, "DIA", inicio, fim, antes);
    var primeira = cargas.reservar(antes, fim.plusSeconds(60), "primeira").orElseThrow();
    assertThat(cargas.falhar(primeira.id(), "primeira", antes.plusSeconds(1), null)).isTrue();
    assertThat(cargas.reservar(antes.plusSeconds(2), fim, "sem-retentativa")).isEmpty();
    cargas.agendar(par, "DIA", inicio, fim, antes.plusSeconds(3));
    cargas.agendar(par, "DIA", inicio, fim, antes.plusSeconds(3));
    var retomada = cargas.reservar(antes.plusSeconds(4), fim.plusSeconds(60), "retomada").orElseThrow();
    assertThat(retomada.id()).isEqualTo(primeira.id());
    assertThat(retomada.tentativas()).isEqualTo(1);
    assertThat(cargas.concluir(retomada.id(), "retomada", false, antes, fim.plusSeconds(1))).isTrue();
    assertThat(jdbc.queryForObject("SELECT estado FROM cargas_cotacao WHERE id=?", String.class, retomada.id()))
        .isEqualTo("PARCIAL");
    var fechada = cargas.reservar(fim.plusSeconds(2), fim.plusSeconds(60), "fechamento").orElseThrow();
    assertThat(cargas.concluir(fechada.id(), "fechamento", false, fim.plusSeconds(3))).isTrue();
    cargas.agendar(par, "DIA", inicio, fim, fim.plusSeconds(4));
    assertThat(cargas.reservar(fim.plusSeconds(4), fim.plusSeconds(60), "duplicada")).isEmpty();
  }

  @Test
  void doisTrabalhadoresNaoReservamOMesmoPeriodo() throws Exception {
    Long base = jdbc.queryForObject("SELECT id FROM ativos WHERE codigo='cripto:SOL'", Long.class);
    Long moeda = jdbc.queryForObject("SELECT id FROM ativos WHERE codigo='cripto:USDC'", Long.class);
    jdbc.update("INSERT INTO pares_cotacao(ativo_base_id,ativo_cotacao_id,fonte,simbolo) VALUES (?,?,'BINANCE','SOLUSDC')", base, moeda);
    Long par = jdbc.queryForObject("SELECT id FROM pares_cotacao WHERE simbolo='SOLUSDC'", Long.class);
    Instant agora = Instant.parse("2024-03-01T00:00:00Z");
    TransactionTemplate transacao = new TransactionTemplate(transacoes);
    transacao.executeWithoutResult(status -> cargas.agendar(par, "HORA", agora.minusSeconds(3600), agora, agora));
    CountDownLatch preparados = new CountDownLatch(2);
    CountDownLatch iniciar = new CountDownLatch(1);
    try (var executor = Executors.newFixedThreadPool(2)) {
      var primeiro = executor.submit(() -> transacao.execute(status -> {
        preparados.countDown();
        try { iniciar.await(); } catch (InterruptedException e) { Thread.currentThread().interrupt(); throw new IllegalStateException(e); }
        return cargas.reservar(agora, agora.plusSeconds(60), "primeiro");
      }));
      var segundo = executor.submit(() -> transacao.execute(status -> {
        preparados.countDown();
        try { iniciar.await(); } catch (InterruptedException e) { Thread.currentThread().interrupt(); throw new IllegalStateException(e); }
        return cargas.reservar(agora, agora.plusSeconds(60), "segundo");
      }));
      boolean ambosPreparados = preparados.await(10, TimeUnit.SECONDS);
      iniciar.countDown();
      assertThat(ambosPreparados).isTrue();
      assertThat(java.util.stream.Stream.of(primeiro.get(15, TimeUnit.SECONDS), segundo.get(15, TimeUnit.SECONDS))
          .filter(java.util.Optional::isPresent)).hasSize(1);
    }
  }
}
