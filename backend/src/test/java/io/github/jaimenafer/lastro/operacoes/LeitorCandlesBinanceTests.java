package io.github.jaimenafer.lastro.operacoes;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import io.github.jaimenafer.lastro.operacoes.domain.exception.CotacaoInvalidaException;
import io.github.jaimenafer.lastro.operacoes.infrastructure.clients.LeitorCandlesBinance;
import java.io.ByteArrayOutputStream;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.HexFormat;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;
import org.junit.jupiter.api.Test;

class LeitorCandlesBinanceTests {
  private final LeitorCandlesBinance leitor = new LeitorCandlesBinance();

  @Test
  void leFormatoAntigoSemCabecalhoComTimestampEmMilissegundos() throws Exception {
    byte[] zip = zip("BTCUSDT-1m-2024-12.csv", linha("1735689599999", "1735689659999"));

    var leitura = leitor.ler(zip, checksum(zip));

    assertThat(leitura.sha256()).isEqualTo(checksum(zip));
    assertThat(leitura.candles()).hasSize(1);
    assertThat(leitura.candles().getFirst().abertoEm().toString())
        .isEqualTo("2024-12-31T23:59:59.999Z");
    assertThat(leitura.candles().getFirst().aberto()).isEqualByComparingTo("1.000000000000000001");
    assertThat(leitura.candles().getFirst().maximo()).isEqualByComparingTo("2.0");
    assertThat(leitura.candles().getFirst().minimo()).isEqualByComparingTo("0.5");
    assertThat(leitura.candles().getFirst().fechado()).isEqualByComparingTo("1.5");
  }

  @Test
  void leFormatoComCabecalhoETimestampEmMicrossegundos() throws Exception {
    String csv = "open_time,open,high,low,close,volume,close_time,quote_volume,count,taker_buy_volume,taker_buy_quote_volume,ignore\n"
        + linha("1735689599999999", "1735689659999999");
    byte[] zip = zip("BTCUSDT-1m-2025-01.csv", csv);

    var candle = leitor.ler(zip, checksum(zip)).candles().getFirst();

    assertThat(candle.abertoEm().toString()).isEqualTo("2024-12-31T23:59:59.999999Z");
    assertThat(candle.fechadoEm().toString()).isEqualTo("2025-01-01T00:00:59.999999Z");
    assertThat(candle.volume()).isEqualByComparingTo("3.000000000000000001");
    assertThat(candle.numeroNegociacoes()).isEqualTo(7L);
  }

  @Test
  void rejeitaChecksumZipEValoresOhlcInvalidos() throws Exception {
    byte[] zip = zip("candles.csv", linha("1735689599999", "1735689659999"));
    assertThatThrownBy(() -> leitor.ler(zip, "0".repeat(64)))
        .isInstanceOf(CotacaoInvalidaException.class);
    assertThatThrownBy(() -> leitor.ler(new byte[] {80, 75, 3, 4}, checksum(new byte[] {80, 75, 3, 4})))
        .isInstanceOf(CotacaoInvalidaException.class);

    byte[] ohlcInvalido = zip("candles.csv", linha("1735689599999", "1735689659999", "1", "2", "0.5", "3"));
    assertThatThrownBy(() -> leitor.ler(ohlcInvalido, checksum(ohlcInvalido)))
        .isInstanceOf(CotacaoInvalidaException.class);
  }

  @Test
  void rejeitaMisturaDePrecisaoDeTimestampNoMesmoArquivo() throws Exception {
    String csv = linha("1735689599999", "1735689659999")
        + linha("1735689659999999", "1735689719999999");
    byte[] zip = zip("candles.csv", csv);

    assertThatThrownBy(() -> leitor.ler(zip, checksum(zip)))
        .isInstanceOf(CotacaoInvalidaException.class);
  }
  @Test
  void rejeitaDadosNumericamenteInvalidos() throws Exception {
    String[] valores = linha("1735689599999", "1735689659999").split(",");
    valores[1] = "NaN";
    byte[] zip = zip("candles.csv", String.join(",", valores));

    assertThatThrownBy(() -> leitor.ler(zip, checksum(zip)))
        .isInstanceOf(CotacaoInvalidaException.class);
  }

  private static String linha(String abertura, String fechamento) {
    return linha(abertura, fechamento, "1.000000000000000001", "2.0", "0.5", "1.5");
  }

  private static String linha(
      String abertura, String fechamento, String aberto, String maximo, String minimo, String fechado) {
    return String.join(",", abertura, aberto, maximo, minimo, fechado, "3.000000000000000001",
        fechamento, "4.000000000000000001", "7", "2.000000000000000001", "3.000000000000000001", "0")
        + "\n";
  }

  private static byte[] zip(String nome, String csv) throws Exception {
    try (var bytes = new ByteArrayOutputStream(); var output = new ZipOutputStream(bytes)) {
      output.putNextEntry(new ZipEntry(nome));
      output.write(csv.getBytes(StandardCharsets.UTF_8));
      output.closeEntry();
      output.finish();
      return bytes.toByteArray();
    }
  }

  private static String checksum(byte[] conteudo) throws Exception {
    return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(conteudo));
  }
}



