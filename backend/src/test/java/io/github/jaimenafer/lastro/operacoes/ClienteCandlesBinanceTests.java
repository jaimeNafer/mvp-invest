package io.github.jaimenafer.lastro.operacoes;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import io.github.jaimenafer.lastro.operacoes.infrastructure.clients.ClienteCandlesBinance;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.net.http.HttpClient;
import java.net.http.HttpHeaders;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.HexFormat;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Function;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;
import org.junit.jupiter.api.Test;
import io.github.jaimenafer.lastro.operacoes.domain.exception.LimiteCotacaoException;

class ClienteCandlesBinanceTests {
  private static final String CANDLE =
      "1735689599999,1.0,2.0,0.5,1.5,3.0,1735689659999,4.0,7,2.0,3.0,0\n";
  private static final String JSON_KLINES =
      "[[1733011200000,\"1.0\",\"2.0\",\"0.5\",\"1.5\",\"3.0\",1733011259999,\"4.0\",7,\"2.0\",\"3.0\",\"0\"]]";

  @Test
  void baixaZipMensalComChecksumUsandoSomenteHostsOficiais() throws Exception {
    byte[] zip = zip(CANDLE);
    String checksum = sha256(zip) + "  BTCUSDT-1m-2024-12.zip\n";
    List<HttpRequest> requisicoes = new ArrayList<>();
    var cliente = new ClienteCandlesBinance(
        requisicao -> {
          requisicoes.add(requisicao);
          if (requisicao.uri().getPath().endsWith(".CHECKSUM")) {
            return resposta(requisicao, 200, "", checksum.getBytes(StandardCharsets.UTF_8));
          }
          return resposta(requisicao, 200, "", zip);
        },
        millis -> {});

    var leitura = cliente.ler("BTCUSDT", "1m", YearMonth.of(2024, 12));

    assertThat(leitura.candles()).hasSize(1);
    assertThat(leitura.arquivoValidado()).isTrue();
    assertThat(leitura.candles().getFirst().aberto()).isEqualByComparingTo("1.0");
    assertThat(requisicoes).hasSize(2);
    assertThat(requisicoes.getFirst().uri().toString())
        .isEqualTo("https://data.binance.vision/data/spot/monthly/klines/BTCUSDT/1m/BTCUSDT-1m-2024-12.zip");
    assertThat(requisicoes.get(1).uri().toString()).endsWith("BTCUSDT-1m-2024-12.zip.CHECKSUM");
    assertThat(requisicoes).allSatisfy(request -> {
      assertThat(request.uri().getScheme()).isEqualTo("https");
      assertThat(request.headers().firstValue("X-MBX-APIKEY")).isEmpty();
      assertThat(request.timeout()).contains(java.time.Duration.ofSeconds(30));
    });
  }

  @Test
  void usaKlinesRestQuandoArquivoMensalOuChecksumNaoEstaDisponivel() {
    List<HttpRequest> requisicoes = new ArrayList<>();
    var cliente = new ClienteCandlesBinance(
        requisicao -> {
          requisicoes.add(requisicao);
          if (requisicao.uri().getHost().equals("data.binance.vision")) {
            return resposta(requisicao, 404, "", new byte[0]);
          }
          return resposta(requisicao, 200, "", JSON_KLINES.getBytes(StandardCharsets.UTF_8));
        },
        millis -> {});

    var leitura = cliente.ler("BTCUSDT", "1m", YearMonth.of(2024, 12));

    assertThat(leitura.candles()).hasSize(1);
    assertThat(leitura.arquivoValidado()).isFalse();
    assertThat(leitura.candles().getFirst().abertoEm().toString())
        .isEqualTo("2024-12-01T00:00:00Z");
    assertThat(leitura.candles().getFirst().volume()).isEqualByComparingTo("3.0");
    assertThat(requisicoes).hasSize(2);
    assertThat(requisicoes.getLast().uri().toString())
        .startsWith("https://data-api.binance.vision/api/v3/klines?")
        .contains("symbol=BTCUSDT", "interval=1m", "startTime=", "endTime=", "limit=1000");
  }

  @Test
  void respeitaRetryAfterAoReceber429AntesDeRepetir() {
    AtomicInteger tentativas = new AtomicInteger();
    List<Long> pausas = new ArrayList<>();
    var cliente = new ClienteCandlesBinance(
        requisicao -> {
          if (requisicao.uri().getHost().equals("data.binance.vision")) {
            if (tentativas.getAndIncrement() == 0) return resposta(requisicao, 429, "2", new byte[0]);
            return resposta(requisicao, 404, "", new byte[0]);
          }
          return resposta(requisicao, 200, "", JSON_KLINES.getBytes(StandardCharsets.UTF_8));
        },
        pausas::add);

    cliente.ler("BTCUSDT", "1m", YearMonth.of(2024, 12));

    assertThat(pausas).containsExactly(2000L);
    assertThat(tentativas).hasValue(2);
  }

  @Test
  void interrompeComRetryAfterEmResposta418Persistente() {
    AtomicInteger tentativas = new AtomicInteger();
    List<Long> pausas = new ArrayList<>();
    var cliente = new ClienteCandlesBinance(
        requisicao -> {
          tentativas.incrementAndGet();
          return resposta(requisicao, 418, "15", new byte[0]);
        },
        pausas::add);

    assertThatThrownBy(() -> cliente.ler("BTCUSDT", "1m", YearMonth.of(2024, 12)))
        .isInstanceOf(IllegalStateException.class)
        .hasMessageContaining("418");
    assertThat(pausas).containsExactly(15_000L);
    assertThat(tentativas).hasValue(2);
  }

  @Test
  void limiteLongoEhDevolvidoParaAgendamentoSemAguardarNaThread() {
    List<Long> pausas = new ArrayList<>();
    var cliente = new ClienteCandlesBinance(
        requisicao -> resposta(requisicao, 429, "3600", new byte[0]), pausas::add);
    assertThatThrownBy(() -> cliente.ler("BTCUSDT", "1h", YearMonth.of(2024, 12)))
        .isInstanceOfSatisfying(LimiteCotacaoException.class,
            erro -> assertThat(erro.getEsperaMillis()).isEqualTo(3_600_000));
    assertThat(pausas).isEmpty();
  }

  @Test
  void rejeitaRespostaMaiorQueOLimiteAntesDeLerOBody() {
    AtomicInteger leituras = new AtomicInteger();
    var cliente = new ClienteCandlesBinance(
        requisicao -> {
          leituras.incrementAndGet();
          return resposta(requisicao, 200, "", new byte[1], "Content-Length", "104857601");
        },
        millis -> {});

    assertThatThrownBy(() -> cliente.ler("BTCUSDT", "1m", YearMonth.of(2024, 12)))
        .isInstanceOf(IllegalStateException.class)
        .hasMessageContaining("limite");
    assertThat(leituras).hasValue(1);
  }

  @Test
  void rejeitaParametroQueNaoPodeSerInseridoEmUrl() {
    var cliente = new ClienteCandlesBinance(requisicao -> {
      throw new AssertionError("Não deve enviar requisição para parâmetros inválidos.");
    }, millis -> {});

    assertThatThrownBy(() -> cliente.ler("BTCUSDT?x=1", "1m", YearMonth.of(2024, 12)))
        .isInstanceOf(IllegalArgumentException.class);
  }

  private static HttpResponse<java.io.InputStream> resposta(
      HttpRequest requisicao, int status, String retryAfter, byte[] corpo, String... cabecalho) {
    var valores = new java.util.HashMap<String, List<String>>();
    if (!retryAfter.isEmpty()) valores.put("Retry-After", List.of(retryAfter));
    if (cabecalho.length == 2) valores.put(cabecalho[0], List.of(cabecalho[1]));
    return new HttpResponse<>() {
      @Override
      public int statusCode() {
        return status;
      }

      @Override
      public HttpRequest request() {
        return requisicao;
      }

      @Override
      public Optional<HttpResponse<java.io.InputStream>> previousResponse() {
        return Optional.empty();
      }

      @Override
      public HttpHeaders headers() {
        return HttpHeaders.of(valores, (nome, valor) -> true);
      }

      @Override
      public java.io.InputStream body() {
        return new ByteArrayInputStream(corpo);
      }

      @Override
      public Optional<javax.net.ssl.SSLSession> sslSession() {
        return Optional.empty();
      }

      @Override
      public java.net.URI uri() {
        return requisicao.uri();
      }

      @Override
      public HttpClient.Version version() {
        return HttpClient.Version.HTTP_2;
      }
    };
  }

  private static byte[] zip(String csv) throws Exception {
    try (var bytes = new ByteArrayOutputStream(); var output = new ZipOutputStream(bytes)) {
      output.putNextEntry(new ZipEntry("BTCUSDT-1m-2024-12.csv"));
      output.write(csv.getBytes(StandardCharsets.UTF_8));
      output.closeEntry();
      output.finish();
      return bytes.toByteArray();
    }
  }

  private static String sha256(byte[] bytes) throws Exception {
    return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(bytes));
  }
}



