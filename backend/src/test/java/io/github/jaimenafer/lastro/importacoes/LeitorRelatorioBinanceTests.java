package io.github.jaimenafer.lastro.importacoes;

import static org.assertj.core.api.Assertions.assertThat;

import io.github.jaimenafer.lastro.importacoes.infrastructure.csv.LeitorRelatorioBinance;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.stream.IntStream;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

class LeitorRelatorioBinanceTests {

  private static final String CABECALHO =
      "id,datetime_tz_BRT,type,label,market_model_type,order_type,sent_amount,sent_currency,sent_value_BRL,sent_address,received_amount,received_currency,received_value_BRL,received_address,fee_amount,fee_currency,fee_value_BRL";
  private final LeitorRelatorioBinance leitor = new LeitorRelatorioBinance();

  @Test
  void aceitaDepositoSemSaidaRepresentadaPorZeroExplicito() {
    String registro = aporte("deposito-zero", "2026-01-01-10:00:00");
    String[] campos = registro.split(",", -1);
    campos[6] = "0";
    var resultado = leitor.ler(csv(String.join(",", campos)));
    assertThat(resultado.erros()).isEmpty();
    assertThat(resultado.eventos()).hasSize(1);
  }

  @Test
  void interpretaAporteEmBrlComHorarioFixoDeBrasilia() {
    var arquivo = leitor.ler(csv(aporte("deposito", "2026-02-28-23:00:00")));

    assertThat(arquivo.erros()).isEmpty();
    assertThat(arquivo.eventos())
        .singleElement()
        .satisfies(evento -> {
          assertThat(evento.linha()).isEqualTo(2);
          assertThat(evento.referencia()).isEqualTo("deposito");
          assertThat(evento.tipo()).isEqualTo("APORTE");
          assertThat(evento.codigoAtivo()).isEqualTo("BRL");
          assertThat(evento.ocorridaEm()).isEqualTo(Instant.parse("2026-03-01T02:00:00Z"));
          assertThat(evento.quantidade()).isEqualByComparingTo("1000.00000001");
          assertThat(evento.principal()).isEqualByComparingTo("1000.00000001");
          assertThat(evento.taxa()).isZero();
        });
    assertThat(arquivo.hash()).matches("[a-f0-9]{64}");
  }

  @Test
  void separaTaxaIncluidaNoTotalOcbsSemPerderPrecisao() {
    var arquivo = leitor.ler(csv(compra("ocbs", "OCBS", "100.12", "0.12", "BRL")));

    assertThat(arquivo.erros()).isEmpty();
    assertThat(arquivo.eventos())
        .singleElement()
        .satisfies(evento -> {
          assertThat(evento.tipo()).isEqualTo("COMPRA");
          assertThat(evento.codigoAtivo()).isEqualTo("BTC");
          assertThat(evento.quantidade()).isEqualByComparingTo("0.000000000000000001");
          assertThat(evento.principal()).isEqualByComparingTo("100.00");
          assertThat(evento.taxa()).isEqualByComparingTo("0.12");
        });
  }

  @Test
  void aceitaConvertComTaxaExplicitamenteZeroSemUsarValoracaoAuxiliar() {
    var arquivo = leitor.ler(csv(compra("convert", "CONVERT", "100.12", "0", "")));
    assertThat(arquivo.erros()).isEmpty();
    assertThat(arquivo.eventos())
        .singleElement()
        .satisfies(evento -> {
          assertThat(evento.principal()).isEqualByComparingTo("100.12");
          assertThat(evento.taxa()).isZero();
        });
  }

  @Test
  void aceitaBomAspasEscapadasVirgulasENovaLinhaDentroDoRegistro() {
    String registro =
        aporte("\"deposito,\"\"externo\"\"\"", "2026-01-01-10:00:00")
            .replace(",,FIAT,", ",\"rótulo\nexterno\",FIAT,");
    var arquivo =
        leitor.ler(
            ("\ufeff" + CABECALHO + "\r\n" + registro + "\r\n").getBytes(StandardCharsets.UTF_8));
    assertThat(arquivo.erros()).isEmpty();
    assertThat(arquivo.eventos())
        .singleElement()
        .satisfies(evento -> assertThat(evento.referencia()).isEqualTo("deposito,\"externo\""));
  }

  @Test
  void ordenaCronologicamentePreservandoDesempateEProveniencia() {
    var arquivo =
        leitor.ler(
            csv(
                aporte("ultimo", "2026-02-01-10:00:00"),
                aporte("primeiro", "2026-01-01-10:00:00"),
                aporte("segundo", "2026-01-01-10:00:00")));
    assertThat(arquivo.eventos())
        .extracting("referencia")
        .containsExactly("primeiro", "segundo", "ultimo");
    assertThat(arquivo.eventos()).extracting("linha").containsExactly(3, 4, 2);
  }

  @Test
  void rejeitaArquivoInteiroComDiagnosticosPorRegistro() {
    var arquivo =
        leitor.ler(
            csv(
                aporte("valido", "2026-01-01-10:00:00"),
                compra("invalido", "CONVERT", "100", "0.1", "BRL"),
                aporte("valido", "2026-01-01-10:00:00")));
    assertThat(arquivo.eventos()).isEmpty();
    assertThat(arquivo.erros()).extracting("linha").containsExactly(3, 4);
    assertThat(arquivo.erros()).allSatisfy(erro -> assertThat(erro.mensagem()).isNotBlank());
  }

  @ParameterizedTest
  @ValueSource(strings = {"", "-0.1", "abc", "1e2", "0,12", "NaN"})
  void rejeitaTaxaAusenteNegativaOuDecimalInvalido(String taxa) {
    var arquivo = leitor.ler(csv(compra("compra", "OCBS", "100", taxa, "BRL")));
    assertThat(arquivo.eventos()).isEmpty();
    assertThat(arquivo.erros()).isNotEmpty();
  }

  @ParameterizedTest
  @ValueSource(
      strings = {
        "2026-02-29-10:00:00",
        "2026-01-01-24:00:00",
        "2026-1-01-10:00:00",
        "2026-01-01T10:00:00",
        "+10000-01-01-10:00:00",
        "-0001-01-01-10:00:00",
        "0000-01-01-10:00:00",
        ""
      })
  void rejeitaDataInvalidaSemNormalizar(String data) {
    var arquivo = leitor.ler(csv(aporte("deposito", data)));
    assertThat(arquivo.eventos()).isEmpty();
    assertThat(arquivo.erros()).extracting("linha").containsExactly(2);
  }

  @Test
  void rejeitaTiposMoedasModelosEValoresIncompativeis() {
    String base = compra("compra", "OCBS", "100", "0.1", "BRL");
    for (String registro :
        new String[] {
          base.replace(",Buy,", ",Sell,"),
          base.replace(",OCBS,", ",SPOT,"),
          base.replace(",100,BRL,", ",100,USDT,"),
          base.replace(",0.1,BRL,", ",0.1,BNB,"),
          compra("compra", "OCBS", "0.1", "0.1", "BRL"),
          base.replace("0.000000000000000001", "0"),
          base.replace("compra,", ",")
        }) {
      assertThat(leitor.ler(csv(registro)).erros()).as(registro).isNotEmpty();
    }
  }

  @Test
  void rejeitaCabecalhoAlteradoArquivoVazioECsvMalformado() {
    assertThat(
            leitor
                .ler(CABECALHO.replace("fee_amount", "taxa").getBytes(StandardCharsets.UTF_8))
                .erros())
        .isNotEmpty();
    assertThat(
            leitor
                .ler(CABECALHO.replace("fee_amount", "").getBytes(StandardCharsets.UTF_8))
                .erros())
        .isNotEmpty();
    assertThat(leitor.ler(csv()).erros()).isNotEmpty();
    assertThat(leitor.ler(csv("\"aspas sem fechamento")).erros()).isNotEmpty();
    assertThat(leitor.ler(new byte[] {(byte) 0xc3, (byte) 0x28}).erros()).isNotEmpty();
  }

  @Test
  void limitaBytesEQuantidadeDeEventos() {
    assertThat(leitor.ler(new byte[10 * 1024 * 1024 + 1]).erros()).isNotEmpty();
    String[] registros =
        IntStream.range(0, 5001)
            .mapToObj(i -> aporte("id-" + i, "2026-01-01-10:00:00"))
            .toArray(String[]::new);
    assertThat(leitor.ler(csv(registros)).erros()).isNotEmpty();
  }

  @Test
  void rejeitaIdentificadorAcimaDaCapacidadeDoRegistro() {
    assertThat(leitor.ler(csv(aporte("i".repeat(161), "2026-01-01-10:00:00"))).erros())
        .isNotEmpty();
  }

  @Test
  void rejeitaDecimalAcimaDaCapacidadeDoRegistro() {
    assertThat(leitor.ler(csv(compra("compra", "OCBS", "9".repeat(161), "0", "BRL"))).erros())
        .isNotEmpty();
  }

  @Test
  void preservaNumeroDoRegistroMalformadoAposRegistroValido() {
    var arquivo =
        leitor.ler(csv(aporte("valido", "2026-01-01-10:00:00"), "\"aspas sem fechamento"));
    assertThat(arquivo.eventos()).isEmpty();
    assertThat(arquivo.erros()).extracting("linha").containsExactly(3);
  }

  @Test
  void hashIdentificaBytesOriginaisEListasSaoImutaveis() {
    byte[] bytes = csv(aporte("deposito", "2026-01-01-10:00:00"));
    assertThat(leitor.ler(bytes).hash()).isEqualTo(leitor.ler(bytes).hash());
    assertThat(leitor.ler(bytes).hash())
        .isNotEqualTo(leitor.ler(csv(aporte("outro", "2026-01-01-10:00:00"))).hash());
    org.assertj.core.api.Assertions.assertThatThrownBy(() -> leitor.ler(bytes).eventos().clear())
        .isInstanceOf(UnsupportedOperationException.class);
  }

  private byte[] csv(String... registros) {
    return (CABECALHO + "\n" + String.join("\n", registros)).getBytes(StandardCharsets.UTF_8);
  }

  private String aporte(String id, String data) {
    return String.join(
        ",",
        id,
        data,
        "Deposit",
        "",
        "FIAT",
        "",
        "",
        "",
        "",
        "",
        "1000.00000001",
        "BRL",
        "ignorado",
        "",
        "0",
        "",
        "ignorado");
  }

  private String compra(String id, String modelo, String total, String taxa, String moedaTaxa) {
    return String.join(
        ",",
        id,
        "2026-01-01-10:00:00",
        "Buy",
        "",
        modelo,
        "",
        total,
        "BRL",
        "ignorado",
        "",
        "0.000000000000000001",
        "BTC",
        "ignorado",
        "",
        taxa,
        moedaTaxa,
        "ignorado");
  }
}
