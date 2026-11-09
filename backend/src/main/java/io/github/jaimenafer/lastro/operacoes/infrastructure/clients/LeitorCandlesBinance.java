package io.github.jaimenafer.lastro.operacoes.infrastructure.clients;

import io.github.jaimenafer.lastro.operacoes.domain.exception.CotacaoInvalidaException;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.math.BigDecimal;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Instant;
import java.util.ArrayList;
import java.util.HexFormat;
import java.util.List;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVRecord;

public class LeitorCandlesBinance {
  private static final int MAXIMO_ZIP_BYTES = 100 * 1024 * 1024;
  private static final int MAXIMO_CSV_BYTES = 500 * 1024 * 1024;
  private static final int COLUNAS = 12;
  private static final Pattern CHECKSUM =
      Pattern.compile("^([a-fA-F0-9]{64})(?:\\s+[* ]?([^\\r\\n]+))?$", Pattern.MULTILINE);
  private static final List<String> CABECALHO =
      List.of(
          "open_time",
          "open",
          "high",
          "low",
          "close",
          "volume",
          "close_time",
          "quote_asset_volume",
          "number_of_trades",
          "taker_buy_base_asset_volume",
          "taker_buy_quote_asset_volume",
          "ignore");
  private static final CSVFormat FORMATO =
      CSVFormat.RFC4180.builder().setIgnoreEmptyLines(true).get();

  public LeituraCandlesBinance ler(byte[] zip, String checksumSha256) {
    validarEntrada(zip, checksumSha256);
    String sha256 = sha256(zip);
    validarChecksum(sha256, checksumSha256);
    String csv = extrairCsv(zip);
    return new LeituraCandlesBinance(sha256, interpretar(csv), true);
  }

  private void validarEntrada(byte[] zip, String checksumSha256) {
    if (zip == null || zip.length < 22 || zip.length > MAXIMO_ZIP_BYTES) {
      throw invalido("O arquivo ZIP de candles é inválido ou excede o limite permitido.");
    }
    if (checksumSha256 == null || !CHECKSUM.matcher(checksumSha256.trim()).matches()) {
      throw invalido("O checksum SHA-256 informado é inválido.");
    }
    if (!temFimZip(zip)) {
      throw invalido("O arquivo ZIP de candles está incompleto ou inválido.");
    }
  }

  private boolean temFimZip(byte[] zip) {
    int inicio = Math.max(0, zip.length - 65_557);
    for (int indice = zip.length - 22; indice >= inicio; indice--) {
      if (zip[indice] == 0x50
          && zip[indice + 1] == 0x4b
          && zip[indice + 2] == 0x05
          && zip[indice + 3] == 0x06) return true;
    }
    return false;
  }

  private void validarChecksum(String sha256, String checksumInformado) {
    Matcher matcher = CHECKSUM.matcher(checksumInformado.trim());
    if (!matcher.matches()) throw invalido("O checksum SHA-256 informado é inválido.");
    byte[] esperado = HexFormat.of().parseHex(matcher.group(1));
    byte[] recebido = HexFormat.of().parseHex(sha256);
    if (!MessageDigest.isEqual(esperado, recebido)) {
      throw invalido("O checksum SHA-256 do arquivo não confere.");
    }
  }

  private String extrairCsv(byte[] zip) {
    try (var entrada = new ZipInputStream(new ByteArrayInputStream(zip));
        var conteudo = new ByteArrayOutputStream()) {
      ZipEntry registro = entrada.getNextEntry();
      if (registro == null
          || registro.isDirectory()
          || !registro.getName().toLowerCase(Locale.ROOT).endsWith(".csv")) {
        throw invalido("O ZIP deve conter um arquivo CSV de candles.");
      }
      byte[] buffer = new byte[8192];
      int total = 0;
      int lidos;
      while ((lidos = entrada.read(buffer)) != -1) {
        total += lidos;
        if (total > MAXIMO_CSV_BYTES) {
          throw invalido("O CSV de candles excede o limite permitido.");
        }
        conteudo.write(buffer, 0, lidos);
      }
      entrada.closeEntry();
      if (entrada.getNextEntry() != null) {
        throw invalido("O ZIP deve conter somente um arquivo CSV de candles.");
      }
      String csv =
          StandardCharsets.UTF_8.newDecoder().decode(ByteBuffer.wrap(conteudo.toByteArray())).toString();
      return csv.startsWith("\ufeff") ? csv.substring(1) : csv;
    } catch (IOException erro) {
      throw invalido("O arquivo ZIP de candles está inválido ou ilegível.");
    }
  }

  private List<CandleBinance> interpretar(String conteudo) {
    List<CandleBinance> candles = new ArrayList<>();
    try (CSVParser parser = FORMATO.parse(new java.io.StringReader(conteudo))) {
      boolean primeiraLinha = true;
      int precisaoArquivo = 0;
      for (CSVRecord linha : parser) {
        if (primeiraLinha && ehCabecalho(linha)) {
          primeiraLinha = false;
          continue;
        }
        primeiraLinha = false;
        int precisaoLinha = precisaoTimestamp(linha.get(0));
        if (precisaoArquivo != 0 && precisaoArquivo != precisaoLinha) {
          throw invalido("O CSV não pode misturar timestamps em milissegundos e microssegundos.");
        }
        precisaoArquivo = precisaoLinha;
        candles.add(converter(linha));
      }
    } catch (IOException erro) {
      throw invalido("O CSV de candles contém dados inválidos.");
    }
    if (candles.isEmpty()) throw invalido("O CSV de candles não contém registros.");
    return candles;
  }

  private boolean ehCabecalho(CSVRecord linha) {
    if (linha.size() != COLUNAS) return false;
    for (int indice = 0; indice < COLUNAS; indice++) {
      String coluna = linha.get(indice).trim().toLowerCase(Locale.ROOT).replace(' ', '_');
      if (!cabecalhoEquivalente(indice, coluna)) return false;
    }
    return true;
  }

  private boolean cabecalhoEquivalente(int indice, String coluna) {
    if (CABECALHO.get(indice).equals(coluna)) return true;
    return switch (indice) {
      case 7 -> coluna.equals("quote_volume");
      case 8 -> coluna.equals("count");
      case 9 -> coluna.equals("taker_buy_volume");
      case 10 -> coluna.equals("taker_buy_quote_volume");
      default -> false;
    };
  }

  private CandleBinance converter(CSVRecord linha) {
    if (linha.size() != COLUNAS) {
      throw invalido("Uma linha do CSV não contém as 12 colunas esperadas.");
    }
    String timestampAbertura = linha.get(0);
    String timestampFechamento = linha.get(6);
    if (precisaoTimestamp(timestampAbertura) != precisaoTimestamp(timestampFechamento)) {
      throw invalido("Uma linha do CSV mistura unidades de timestamp.");
    }
    Instant abertura = instante(timestampAbertura);
    BigDecimal aberto = decimal(linha.get(1));
    BigDecimal maximo = decimal(linha.get(2));
    BigDecimal minimo = decimal(linha.get(3));
    BigDecimal fechado = decimal(linha.get(4));
    BigDecimal volume = decimal(linha.get(5));
    Instant fechamento = instante(timestampFechamento);
    BigDecimal volumeCotacao = decimal(linha.get(7));
    long negociacoes = inteiro(linha.get(8));
    BigDecimal volumeTaker = decimal(linha.get(9));
    BigDecimal volumeCotacaoTaker = decimal(linha.get(10));
    decimal(linha.get(11));
    validarOhlc(
        abertura,
        fechamento,
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
        abertura,
        aberto,
        maximo,
        minimo,
        fechado,
        volume,
        fechamento,
        volumeCotacao,
        negociacoes,
        volumeTaker,
        volumeCotacaoTaker);
  }

  private void validarOhlc(
      Instant abertura,
      Instant fechamento,
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
        || fechamento.isBefore(abertura)
        || aberto.signum() < 0
        || maximo.signum() < 0
        || minimo.signum() < 0
        || fechado.signum() < 0
        || volume.signum() < 0
        || volumeCotacao.signum() < 0
        || negociacoes < 0
        || volumeTaker.signum() < 0
        || cotacaoTaker.signum() < 0) {
      throw invalido("Uma linha do CSV contém valores OHLC ou de volume inconsistentes.");
    }
  }

  private Instant instante(String valor) {
    String timestamp = valor.trim();
    int precisao = precisaoTimestamp(timestamp);
    try {
      if (precisao == 13) return Instant.ofEpochMilli(Long.parseLong(timestamp));
      long microssegundos = Long.parseLong(timestamp);
      return Instant.ofEpochSecond(microssegundos / 1_000_000, (microssegundos % 1_000_000) * 1_000);
    } catch (RuntimeException erro) {
      throw invalido("Um timestamp do CSV de candles é inválido.");
    }
  }

  private int precisaoTimestamp(String valor) {
    String timestamp = valor.trim();
    if ((timestamp.length() == 13 || timestamp.length() == 16)
        && timestamp.chars().allMatch(Character::isDigit)) return timestamp.length();
    throw invalido("Um timestamp do CSV deve estar em milissegundos ou microssegundos.");
  }

  private BigDecimal decimal(String valor) {
    try {
      return new BigDecimal(valor.trim());
    } catch (NumberFormatException erro) {
      throw invalido("Uma coluna numérica do CSV de candles é inválida.");
    }
  }

  private long inteiro(String valor) {
    try {
      return Long.parseLong(valor.trim());
    } catch (NumberFormatException erro) {
      throw invalido("A contagem de negociações do CSV é inválida.");
    }
  }

  private String sha256(byte[] conteudo) {
    try {
      return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(conteudo));
    } catch (NoSuchAlgorithmException erro) {
      throw new IllegalStateException(erro);
    }
  }

  private CotacaoInvalidaException invalido(String mensagem) {
    return new CotacaoInvalidaException(mensagem);
  }
}

