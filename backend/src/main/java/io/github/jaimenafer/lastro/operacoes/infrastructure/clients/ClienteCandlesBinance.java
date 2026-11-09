package io.github.jaimenafer.lastro.operacoes.infrastructure.clients;

import io.github.jaimenafer.lastro.operacoes.domain.exception.LimiteCotacaoException;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.math.BigDecimal;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.CharacterCodingException;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Duration;
import java.time.Instant;
import java.time.YearMonth;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HexFormat;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.Set;
import java.util.function.Function;
import java.util.function.LongConsumer;
import java.util.regex.Pattern;

public class ClienteCandlesBinance {
  private static final String HOST_ARQUIVOS = "data.binance.vision";
  private static final String HOST_API = "data-api.binance.vision";
  private static final Set<String> HOSTS_OFICIAIS = Set.of(HOST_ARQUIVOS, HOST_API);
  private static final Duration TIMEOUT_CONEXAO = Duration.ofSeconds(10);
  private static final Duration TIMEOUT_REQUISICAO = Duration.ofSeconds(30);
  private static final int MAXIMO_ZIP_BYTES = 100 * 1024 * 1024;
  private static final int MAXIMO_CHECKSUM_BYTES = 4096;
  private static final int MAXIMO_PAGINA_REST_BYTES = 8 * 1024 * 1024;
  private static final int MAXIMO_CANDLES_REST = 250_000;
  private static final int LIMITE_REST = 1000;
  private static final int MAXIMO_REPETICOES_RATE_LIMIT = 1;
  private static final long MAXIMO_ESPERA_NA_THREAD_MILLIS = 30_000;
  private static final Pattern SIMBOLO = Pattern.compile("[A-Z0-9]{2,20}");
  private static final Set<String> INTERVALOS =
      Set.of("1s", "1m", "3m", "5m", "15m", "30m", "1h", "2h", "4h", "6h", "8h", "12h", "1d", "3d", "1w", "1M", "1mo");
  private static final JsonMapper JSON = JsonMapper.builder().build();
  private static final HttpClient HTTP =
      HttpClient.newBuilder()
          .connectTimeout(TIMEOUT_CONEXAO)
          .followRedirects(HttpClient.Redirect.NEVER)
          .build();

  private final Function<HttpRequest, HttpResponse<InputStream>> transporte;
  private final LongConsumer aguardar;
  private final LeitorCandlesBinance leitorZip = new LeitorCandlesBinance();

  public ClienteCandlesBinance() {
    this(ClienteCandlesBinance::enviarHttp, ClienteCandlesBinance::pausar);
  }

  public ClienteCandlesBinance(
      Function<HttpRequest, HttpResponse<InputStream>> transporte, LongConsumer aguardar) {
    this.transporte = Objects.requireNonNull(transporte);
    this.aguardar = Objects.requireNonNull(aguardar);
  }

  public LeituraCandlesBinance ler(String simbolo, String intervalo, YearMonth mes) {
    validarParametros(simbolo, intervalo, mes);
    URI arquivo = uriArquivo(simbolo, intervalo, mes);
    byte[] conteudoZip = baixar(arquivo, MAXIMO_ZIP_BYTES, "application/zip");
    if (conteudoZip != null) {
      byte[] conteudoChecksum = baixar(uriOficial(arquivo + ".CHECKSUM"), MAXIMO_CHECKSUM_BYTES, "text/plain");
      if (conteudoChecksum != null) {
        return leitorZip.ler(conteudoZip, textoUtf8(conteudoChecksum));
      }
    }
    return lerKlinesRest(simbolo, intervaloRest(intervalo), mes);
  }

  private void validarParametros(String simbolo, String intervalo, YearMonth mes) {
    if (simbolo == null || !SIMBOLO.matcher(simbolo).matches()) {
      throw new IllegalArgumentException("O símbolo de mercado é inválido.");
    }
    if (intervalo == null || !INTERVALOS.contains(intervalo)) {
      throw new IllegalArgumentException("O intervalo de candles não é suportado.");
    }
    Objects.requireNonNull(mes, "O mês de candles é obrigatório.");
  }

  private URI uriArquivo(String simbolo, String intervalo, YearMonth mes) {
    String nome = simbolo + "-" + intervalo + "-" + mes + ".zip";
    return uriOficial(
        "https://" + HOST_ARQUIVOS + "/data/spot/monthly/klines/" + simbolo + "/" + intervalo + "/" + nome);
  }

  private URI uriOficial(String endereco) {
    URI uri = URI.create(endereco);
    if (!"https".equals(uri.getScheme())
        || !HOSTS_OFICIAIS.contains(uri.getHost())
        || uri.getUserInfo() != null
        || uri.getPort() != -1
        || uri.getFragment() != null) {
      throw new IllegalArgumentException("O endereço da fonte de candles não é oficial.");
    }
    return uri;
  }

  private byte[] baixar(URI uri, int limiteBytes, String accept) {
    validarUriOficial(uri);
    HttpRequest request =
        HttpRequest.newBuilder(uri)
            .timeout(TIMEOUT_REQUISICAO)
            .header("Accept", accept)
            .GET()
            .build();
    HttpResponse<InputStream> resposta = enviar(request);
    if (resposta.statusCode() == 404) {
      fechar(resposta.body());
      return null;
    }
    if (resposta.statusCode() != 200) {
      fechar(resposta.body());
      throw new IllegalStateException(
          "A fonte pública de candles respondeu HTTP " + resposta.statusCode() + ".");
    }
    return lerLimitado(resposta, limiteBytes);
  }

  private HttpResponse<InputStream> enviar(HttpRequest request) {
    for (int tentativa = 0; ; tentativa++) {
      HttpResponse<InputStream> resposta;
      try {
        resposta = transporte.apply(request);
      } catch (RuntimeException erro) {
        throw new IllegalStateException("Falha ao consultar a fonte pública de candles.", erro);
      }
      int status = resposta.statusCode();
      if (!rateLimited(status)) return resposta;
      String retryAfter = resposta.headers().firstValue("Retry-After").orElse("");
      fechar(resposta.body());
      long espera = retryAfter.isBlank() ? 60_000 : tempoDeEsperaMillis(retryAfter);
      if (tentativa >= MAXIMO_REPETICOES_RATE_LIMIT || espera > MAXIMO_ESPERA_NA_THREAD_MILLIS)
        throw new LimiteCotacaoException(status, espera);
      aguardar.accept(espera);
    }
  }

  private boolean rateLimited(int status) {
    return status == 429 || status == 418;
  }

  private long tempoDeEsperaMillis(String retryAfter) {
    try {
      long segundos = Long.parseLong(retryAfter.trim());
      if (segundos < 0) throw new NumberFormatException();
      return Math.multiplyExact(segundos, 1000);
    } catch (NumberFormatException | ArithmeticException erro) {
      try {
        Instant liberarEm =
            ZonedDateTime.parse(retryAfter, DateTimeFormatter.RFC_1123_DATE_TIME).toInstant();
        return Math.max(0, Duration.between(Instant.now(), liberarEm).toMillis());
      } catch (RuntimeException dataInvalida) {
        throw new IllegalStateException("A fonte pública informou Retry-After inválido.");
      }
    }
  }

  private byte[] lerLimitado(HttpResponse<InputStream> resposta, int limiteBytes) {
    try (InputStream corpo = resposta.body()) {
      long tamanhoInformado =
          resposta.headers().firstValueAsLong("Content-Length").orElse(-1);
      if (tamanhoInformado > limiteBytes) {
        throw new IllegalStateException("A resposta da fonte pública excede o limite de bytes.");
      }
      ByteArrayOutputStream bytes = new ByteArrayOutputStream(Math.min(limiteBytes, 16 * 1024));
      byte[] buffer = new byte[8192];
      int total = 0;
      int lidos;
      while ((lidos = corpo.read(buffer)) != -1) {
        total += lidos;
        if (total > limiteBytes) {
          throw new IllegalStateException("A resposta da fonte pública excede o limite de bytes.");
        }
        bytes.write(buffer, 0, lidos);
      }
      return bytes.toByteArray();
    } catch (IOException erro) {
      throw new IllegalStateException("Não foi possível ler a resposta da fonte pública.", erro);
    }
  }

  private void validarUriOficial(URI uri) {
    if (!"https".equals(uri.getScheme()) || !HOSTS_OFICIAIS.contains(uri.getHost())) {
      throw new IllegalArgumentException("O endereço da fonte de candles não é oficial.");
    }
  }

  private String textoUtf8(byte[] bytes) {
    try {
      return StandardCharsets.UTF_8.newDecoder().decode(java.nio.ByteBuffer.wrap(bytes)).toString();
    } catch (CharacterCodingException erro) {
      throw new IllegalStateException("O checksum da fonte pública não está em UTF-8.");
    }
  }

  private LeituraCandlesBinance lerKlinesRest(String simbolo, String intervalo, YearMonth mes) {
    long inicio = mes.atDay(1).atStartOfDay(ZoneOffset.UTC).toInstant().toEpochMilli();
    long fim = mes.plusMonths(1).atDay(1).atStartOfDay(ZoneOffset.UTC).toInstant().toEpochMilli() - 1;
    long cursor = inicio;
    List<CandleBinance> candles = new ArrayList<>();
    MessageDigest digest = sha256Digest();
    long ultimaAbertura = -1;
    while (cursor <= fim) {
      URI uri = uriKlinesRest(simbolo, intervalo, cursor, fim);
      byte[] pagina = baixar(uri, MAXIMO_PAGINA_REST_BYTES, "application/json");
      if (pagina == null) {
        throw new IllegalStateException("A consulta pública de klines não está disponível.");
      }
      digest.update(pagina);
      List<CandleBinance> candlesPagina = interpretarKlines(pagina, inicio, fim, ultimaAbertura);
      if (candlesPagina.isEmpty()) break;
      candles.addAll(candlesPagina);
      if (candles.size() > MAXIMO_CANDLES_REST) {
        throw new IllegalStateException("A consulta REST excedeu o limite de candles permitido.");
      }
      ultimaAbertura = candlesPagina.getLast().abertoEm().toEpochMilli();
      if (candlesPagina.size() < LIMITE_REST || ultimaAbertura >= fim) break;
      cursor = ultimaAbertura + 1;
    }
    return new LeituraCandlesBinance(HexFormat.of().formatHex(digest.digest()), candles);
  }

  private URI uriKlinesRest(String simbolo, String intervalo, long inicio, long fim) {
    String query =
        "symbol=" + encode(simbolo)
            + "&interval=" + encode(intervalo)
            + "&startTime=" + inicio
            + "&endTime=" + fim
            + "&limit=" + LIMITE_REST;
    return uriOficial("https://" + HOST_API + "/api/v3/klines?" + query);
  }

  private String intervaloRest(String intervalo) {
    return intervalo.equals("1mo") ? "1M" : intervalo;
  }

  private String encode(String valor) {
    return URLEncoder.encode(valor, StandardCharsets.UTF_8);
  }

  private List<CandleBinance> interpretarKlines(byte[] conteudo, long inicio, long fim, long ultimaAbertura) {
    try {
      JsonNode linhas = JSON.readTree(conteudo);
      if (!linhas.isArray() || linhas.size() > LIMITE_REST) {
        throw new IllegalStateException("A resposta pública de klines tem formato inválido.");
      }
      List<CandleBinance> candles = new ArrayList<>();
      long ultimaLinha = ultimaAbertura;
      for (JsonNode linha : linhas) {
        if (!linha.isArray() || linha.size() != 12) {
          throw new IllegalStateException("Uma linha REST de klines não contém 12 valores.");
        }
        CandleBinance candle = candleRest(linha);
        long abertura = candle.abertoEm().toEpochMilli();
        if (abertura < inicio || abertura > fim || abertura <= ultimaLinha) {
          throw new IllegalStateException("A resposta REST contém timestamps fora de ordem ou do período.");
        }
        ultimaLinha = abertura;
        candles.add(candle);
      }
      return candles;
    } catch (JacksonException erro) {
      throw new IllegalStateException("A resposta pública de klines contém JSON inválido.", erro);
    }
  }

  private CandleBinance candleRest(JsonNode linha) {
    long aberturaMillis = inteiroJson(linha.get(0));
    long fechamentoMillis = inteiroJson(linha.get(6));
    BigDecimal aberto = decimalJson(linha.get(1));
    BigDecimal maximo = decimalJson(linha.get(2));
    BigDecimal minimo = decimalJson(linha.get(3));
    BigDecimal fechado = decimalJson(linha.get(4));
    BigDecimal volume = decimalJson(linha.get(5));
    BigDecimal volumeCotacao = decimalJson(linha.get(7));
    long negociacoes = inteiroJson(linha.get(8));
    BigDecimal volumeTaker = decimalJson(linha.get(9));
    BigDecimal volumeCotacaoTaker = decimalJson(linha.get(10));
    validarOhlc(
        aberturaMillis,
        fechamentoMillis,
        aberto,
        maximo,
        minimo,
        fechado,
        volume,
        volumeCotacao,
        negociacoes,
        volumeTaker,
        volumeCotacaoTaker);
    return new CandleBinance(
        Instant.ofEpochMilli(aberturaMillis),
        aberto,
        maximo,
        minimo,
        fechado,
        volume,
        Instant.ofEpochMilli(fechamentoMillis),
        volumeCotacao,
        negociacoes,
        volumeTaker,
        volumeCotacaoTaker);
  }

  private void validarOhlc(
      long abertura,
      long fechamento,
      BigDecimal aberto,
      BigDecimal maximo,
      BigDecimal minimo,
      BigDecimal fechado,
      BigDecimal volume,
      BigDecimal volumeCotacao,
      long negociacoes,
      BigDecimal volumeTaker,
      BigDecimal cotacaoTaker) {
    if (maximo.compareTo(minimo) < 0
        || maximo.compareTo(aberto) < 0
        || maximo.compareTo(fechado) < 0
        || minimo.compareTo(aberto) > 0
        || minimo.compareTo(fechado) > 0
        || fechamento < abertura
        || aberto.signum() < 0
        || maximo.signum() < 0
        || minimo.signum() < 0
        || fechado.signum() < 0
        || volume.signum() < 0
        || volumeCotacao.signum() < 0
        || negociacoes < 0
        || volumeTaker.signum() < 0
        || cotacaoTaker.signum() < 0) {
      throw new IllegalStateException("A resposta pública contém valores OHLC ou de volume inconsistentes.");
    }
  }

  private long inteiroJson(JsonNode valor) {
    if (valor == null || valor.isNull()) {
      throw new IllegalStateException("A resposta pública de klines contém um inteiro ausente.");
    }
    try {
      return Long.parseLong(valor.asText());
    } catch (NumberFormatException erro) {
      throw new IllegalStateException("A resposta pública de klines contém um inteiro inválido.");
    }
  }

  private BigDecimal decimalJson(JsonNode valor) {
    if (valor == null || valor.isNull() || (!valor.isTextual() && !valor.isNumber())) {
      throw new IllegalStateException("A resposta pública de klines contém um decimal ausente.");
    }
    try {
      return new BigDecimal(valor.asText());
    } catch (NumberFormatException erro) {
      throw new IllegalStateException("A resposta pública de klines contém um decimal inválido.");
    }
  }

  private MessageDigest sha256Digest() {
    try {
      return MessageDigest.getInstance("SHA-256");
    } catch (NoSuchAlgorithmException erro) {
      throw new IllegalStateException(erro);
    }
  }

  private void fechar(InputStream corpo) {
    try {
      corpo.close();
    } catch (IOException erro) {
      throw new IllegalStateException("Não foi possível fechar a resposta da fonte pública.", erro);
    }
  }

  private static HttpResponse<InputStream> enviarHttp(HttpRequest request) {
    try {
      return HTTP.send(request, HttpResponse.BodyHandlers.ofInputStream());
    } catch (IOException erro) {
      throw new IllegalStateException("Falha ao consultar a fonte pública de candles.", erro);
    } catch (InterruptedException erro) {
      Thread.currentThread().interrupt();
      throw new IllegalStateException("A consulta pública de candles foi interrompida.", erro);
    }
  }

  private static void pausar(long millis) {
    try {
      Thread.sleep(millis);
    } catch (InterruptedException erro) {
      Thread.currentThread().interrupt();
      throw new IllegalStateException("A espera indicada pela fonte pública foi interrompida.", erro);
    }
  }
}



