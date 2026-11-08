package io.github.jaimenafer.lastro.importacoes;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import io.github.jaimenafer.lastro.importacoes.domain.exception.ImportacaoInvalidaException;
import io.github.jaimenafer.lastro.importacoes.infrastructure.arquivos.LeitorMovimentacoesBinance;
import java.io.ByteArrayOutputStream;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.junit.jupiter.api.Test;

class LeitorMovimentacoesBinanceTests {
  private final LeitorMovimentacoesBinance leitor = new LeitorMovimentacoesBinance();
  private static final String CABECALHO =
      "id,datetime_tz_BRT,type,label,market_model_type,order_type,sent_amount,sent_currency,sent_value_BRL,sent_address,received_amount,received_currency,received_value_BRL,received_address,fee_amount,fee_currency,fee_value_BRL";

  @Test
  void preservaLinhasInvalidasEOperacoesDesconhecidasSemDescartarValidas() throws Exception {
    byte[] bytes = planilha(false);
    var documento = leitor.ler("extrato.csv", bytes);
    assertThat(documento.hash()).matches("[a-f0-9]{64}");
    assertThat(documento.registros()).hasSize(3);
    var primeira = documento.registros().getFirst();
    assertThat(primeira.linha()).isEqualTo(3);
    assertThat(primeira.aba()).isEqualTo("Movimentos");
    assertThat(primeira.ocorridaEm()).isEqualTo(Instant.parse("2024-05-01T13:20:30Z"));
    assertThat(primeira.quantidade()).isEqualByComparingTo("-0.123456789012345678");
    assertThat(primeira.quantidadeOriginal()).isEqualTo("-0.123456789012345678");
    assertThat(primeira.erros()).isEmpty();
    assertThat(documento.registros().get(1).erros()).hasSize(2);
    assertThat(documento.registros().get(1).camposOriginais())
        .containsEntry("Tempo", "data inválida");
    assertThat(documento.registros().get(2).operacao()).isEqualTo("Operação desconhecida");
    assertThat(documento.registros().get(2).erros()).isEmpty();
  }

  @Test
  void naoConfiaEmFormulaMesmoComResultadoEmCache() throws Exception {
    var documento = leitor.ler("extrato.xlsx", planilha(true));
    assertThat(documento.registros().getFirst().quantidade()).isNull();
    assertThat(documento.registros().getFirst().erros()).anyMatch(m -> m.contains("fórmula"));
  }

  @Test
  void interpretaCsvEnriquecidoPreservandoTodasAsColunasEPrecisao() {
    String deposito = "d1,2024-05-01-10:20:30,Deposit,,FIAT,,0,,,,100.00000001,BRL,,,0,BRL,";
    String compra =
        "c1,2024-05-02-10:20:30,Buy,,OCBS,,100.12,BRL,,,0.000000000000000001,BTC,,,0.12,BRL,";
    String invalida = "i1,errada,Buy,,OCBS,,100,BRL,,,inválida,BTC,,,0,BRL,";
    var documento =
        leitor.ler(
            "extrato.xlsx",
            (CABECALHO + "\n" + deposito + "\n" + compra + "\n" + invalida)
                .getBytes(StandardCharsets.UTF_8));
    assertThat(documento.registros()).hasSize(6);
    assertThat(documento.registros().getFirst().quantidade()).isEqualByComparingTo("100.00000001");
    assertThat(documento.registros().getFirst().erros()).isEmpty();
    assertThat(documento.registros().get(1).camposOriginais())
        .containsEntry("sent_amount", "100.12")
        .containsEntry("componente", "sent");
    assertThat(documento.registros().get(1).quantidade()).isEqualByComparingTo("-100");
    assertThat(documento.registros().get(2).quantidade())
        .isEqualByComparingTo("0.000000000000000001");
    assertThat(documento.registros().get(3).quantidade()).isEqualByComparingTo("-0.12");
    assertThat(documento.registros().get(4).erros()).hasSize(1);
    assertThat(documento.registros().get(5).erros()).hasSize(2);
  }

  @Test
  void csvEPastaXlsxDeRelatorioGeramAsMesmasParcelasEPreservamIdsRepetidos() throws Exception {
    String compra =
        "123456789,2024-05-26-19:05:32,Buy,,OCBS,,100.12,BRL,,,0.12345678,SOL,,,0.12,BRL,";
    var csv =
        leitor.ler(
            "relatorio.csv",
            (CABECALHO + "\n" + compra + "\n" + compra).getBytes(StandardCharsets.UTF_8));
    var xlsx = leitor.ler("relatorio.xlsx", planilhaRelatorio(compra));

    assertThat(csv.registros()).hasSize(6);
    assertThat(xlsx.registros()).hasSize(3);
    assertThat(xlsx.registros())
        .extracting(r -> r.camposOriginais().get("componente"))
        .containsExactly("sent", "received", "fee");
    assertThat(xlsx.registros()).extracting("quantidade")
        .containsExactly(new java.math.BigDecimal("-100.00"), new java.math.BigDecimal("0.12345678"),
            new java.math.BigDecimal("-0.12"));
    assertThat(csv.registros().get(0).camposOriginais().get("id"))
        .isEqualTo(csv.registros().get(3).camposOriginais().get("id"));
  }

  @Test
  void rejeitaConteudoIlegivelELimiteSemExporValores() {
    assertThatThrownBy(() -> leitor.ler("arquivo.xlsx", new byte[] {80, 75, 3, 4, 0}))
        .isInstanceOf(ImportacaoInvalidaException.class);
    assertThatThrownBy(() -> leitor.ler("arquivo.csv", new byte[10 * 1024 * 1024 + 1]))
        .isInstanceOf(ImportacaoInvalidaException.class);
  }

  @Test
  void preservaValorNumericoXmlSemPassagemPorDouble() throws Exception {
    byte[] bytes;
    try (var workbook = new XSSFWorkbook(new java.io.ByteArrayInputStream(planilha(false)));
        var output = new ByteArrayOutputStream()) {
      var cell = workbook.getSheetAt(0).getRow(2).getCell(4);
      cell.setCellValue(1);
      cell.getCTCell().setV("-123456789012345678.123456789012345678");
      workbook.write(output);
      bytes = output.toByteArray();
    }
    var registro = leitor.ler("exato.xlsx", bytes).registros().getFirst();
    assertThat(registro.quantidade())
        .isEqualByComparingTo("-123456789012345678.123456789012345678");
    assertThat(registro.quantidadeOriginal()).isEqualTo("-123456789012345678.123456789012345678");
  }

  @Test
  void rejeitaMacrosELimiteDescompactado() throws Exception {
    try (var output = new ByteArrayOutputStream();
        var zip = new ZipOutputStream(output)) {
      zip.putNextEntry(new ZipEntry("xl/vbaProject.bin"));
      zip.write(new byte[] {1});
      zip.closeEntry();
      zip.finish();
      assertThatThrownBy(() -> leitor.ler("macro.xlsx", output.toByteArray()))
          .isInstanceOf(ImportacaoInvalidaException.class);
    }
  }

  @Test
  void preservaLinhasCsvMalformadasSemPerderAsDemais() {
    String boa = "d1,2024-05-01-10:20:30,Deposit,,FIAT,,,,,,100,BRL,,,0,,";
    var documento =
        leitor.ler(
            "extrato.csv",
            (CABECALHO + "\n" + boa + "\nregistro,curto\n" + boa).getBytes(StandardCharsets.UTF_8));
    assertThat(documento.registros()).hasSize(3);
    assertThat(documento.registros().get(1).erros()).isNotEmpty();
    assertThat(documento.registros().get(2).erros()).isEmpty();
    assertThat(documento.registros()).extracting("linha").containsExactly(2, 3, 4);
  }

  @Test
  void preservaTextoCsvComQuebraDeLinhaEIdentidadeOriginal() {
    String boa =
        "\"id,\"\"externo\"\"\",2024-05-01-10:20:30,Deposit,\"rótulo\n"
            + "original\",FIAT,,,,,,100,BRL,,,0,,";
    var documento =
        leitor.ler(
            "extrato.csv", ("\ufeff" + CABECALHO + "\n" + boa).getBytes(StandardCharsets.UTF_8));
    assertThat(documento.registros())
        .singleElement()
        .satisfies(registro -> {
          assertThat(registro.erros()).isEmpty();
          assertThat(registro.camposOriginais())
              .containsEntry("id", "id,\"externo\"")
              .containsEntry("label", "rótulo\noriginal");
        });
  }

  @Test
  void naoPresumeFusoAusenteNemDescartaLinhasSemData() throws Exception {
    byte[] bytes;
    try (var workbook = new XSSFWorkbook(new java.io.ByteArrayInputStream(planilha(false)));
        var output = new ByteArrayOutputStream()) {
      workbook.getSheetAt(0).getRow(0).getCell(0).setCellValue("Período");
      workbook.getSheetAt(0).getRow(2).getCell(0).setCellValue("");
      workbook.write(output);
      bytes = output.toByteArray();
    }
    var documento = leitor.ler("sem-fuso.xlsx", bytes);
    assertThat(documento.registros()).hasSize(3);
    assertThat(documento.registros().getFirst().ocorridaEm()).isNull();
    assertThat(documento.registros().get(2).erros()).anyMatch(m -> m.contains("fuso"));
  }

  @Test
  void recusaPlanilhaSemCabecalhoMesmoQuandoVazia() throws Exception {
    try (var workbook = new XSSFWorkbook();
        var output = new ByteArrayOutputStream()) {
      workbook.createSheet("Vazia");
      workbook.write(output);
      assertThatThrownBy(() -> leitor.ler("vazia.xlsx", output.toByteArray()))
          .isInstanceOf(ImportacaoInvalidaException.class);
    }
  }

  @Test
  void rejeitaVinculoExternoComNamespaceELimiteDeInflacao() throws Exception {
    byte[] bytes;
    try (var output = new ByteArrayOutputStream();
        var zip = new ZipOutputStream(output)) {
      zip.putNextEntry(new ZipEntry("_rels/.rels"));
      zip.write(
          ("<r:Relationships"
               + " xmlns:r=\"http://schemas.openxmlformats.org/package/2006/relationships\"><r:Relationship"
               + " Id=\"r1\" Type=\"external\" Target=\"https://example.invalid\""
               + " TargetMode=\"External\"/></r:Relationships>")
              .getBytes(StandardCharsets.UTF_8));
      zip.closeEntry();
      zip.putNextEntry(new ZipEntry("xl/workbook.xml"));
      zip.write(new byte[2048]);
      zip.closeEntry();
      zip.putNextEntry(new ZipEntry("[Content_Types].xml"));
      zip.write(new byte[1]);
      zip.closeEntry();
      zip.finish();
      bytes = output.toByteArray();
    }
    assertThatThrownBy(() -> leitor.ler("externo.xlsx", bytes))
        .isInstanceOf(ImportacaoInvalidaException.class)
        .hasMessageContaining("externos");
    var field = LeitorMovimentacoesBinance.class.getDeclaredField("maximoDescompactados");
    field.setAccessible(true);
    field.setLong(leitor, 1024);
    assertThatThrownBy(() -> leitor.ler("limite.xlsx", planilha(false)))
        .isInstanceOf(ImportacaoInvalidaException.class)
        .hasMessageContaining("descompactação");
  }

  @Test
  void aceitaDataSerialSomenteComFormatoDeDataEPreservaOriginal() throws Exception {
    byte[] bytes;
    try (var workbook = new XSSFWorkbook(new java.io.ByteArrayInputStream(planilha(false)));
        var output = new ByteArrayOutputStream()) {
      var cell = workbook.getSheetAt(0).getRow(2).getCell(0);
      cell.setCellValue(45413.5);
      var style = workbook.createCellStyle();
      style.setDataFormat(workbook.createDataFormat().getFormat("yyyy-mm-dd hh:mm:ss"));
      cell.setCellStyle(style);
      workbook.write(output);
      bytes = output.toByteArray();
    }
    var registro = leitor.ler("serial.xlsx", bytes).registros().getFirst();
    assertThat(registro.ocorridaEm()).isEqualTo(Instant.parse("2024-05-01T15:00:00Z"));
    assertThat(registro.instanteOriginal()).isEqualTo("45413.5");
  }

  @Test
  void aceitaDezMilRegistrosSemDeduplicarERecusaExcedente() {
    String linha = "igual,2024-05-01-10:20:30,Deposit,,FIAT,,,,,,100,BRL,,,0,,\n";
    assertThat(
            leitor
                .ler(
                    "limite.csv",
                    (CABECALHO + "\n" + linha.repeat(10000)).getBytes(StandardCharsets.UTF_8))
                .registros())
        .hasSize(10000);
    assertThatThrownBy(() ->
            leitor.ler(
                "excedente.csv",
                (CABECALHO + "\n" + linha.repeat(10001)).getBytes(StandardCharsets.UTF_8)))
        .isInstanceOf(ImportacaoInvalidaException.class);
  }

  @Test
  void csvAceitaBomCrLfECamposComAspasSemAlterarValores() {
    String[] campos = new String[17];
    java.util.Arrays.fill(campos, "");
    campos[0] = "saida";
    campos[1] = "2024-05-01-10:20:30";
    campos[2] = "Withdraw";
    campos[3] = "\"texto com \"\"aspas\"\" e\nlinha\"";
    campos[6] = "0.2";
    campos[7] = "BTC";
    var doc =
        leitor.ler(
            "sintetico.csv",
            ("\ufeff" + CABECALHO + "\r\n\r\n" + String.join(",", campos))
                .getBytes(StandardCharsets.UTF_8));
    assertThat(doc.registros())
        .singleElement()
        .satisfies(r -> {
          assertThat(r.quantidade()).isEqualByComparingTo("-0.2");
          assertThat(r.camposOriginais().get("label")).contains("aspas", "\nlinha");
        });
    assertThat(leitor.ler("cabecalho.csv", CABECALHO.getBytes(StandardCharsets.UTF_8)).registros())
        .isEmpty();
  }

  @Test
  void recusaArquivoVazioCodificacaoInvalidaECabecalhoDesconhecido() {
    for (byte[] bytes :
        new byte[][] {
          new byte[0],
          null,
          new byte[] {(byte) 0xc3, 0x28},
          "cabecalho estranho".getBytes(StandardCharsets.UTF_8),
          new byte[] {80, 75, 0}
        })
      assertThatThrownBy(() -> leitor.ler("arquivo", bytes))
          .isInstanceOf(ImportacaoInvalidaException.class);
  }

  @Test
  void preservaCamposObrigatoriosVaziosSemCriarQuantidade() {
    String linha = "id,0000-01-01-10:20:30,,,,,,,,,invalido,,,,,,";
    var registro =
        leitor
            .ler("incompleto.csv", (CABECALHO + "\n" + linha).getBytes(StandardCharsets.UTF_8))
            .registros()
            .getFirst();
    assertThat(registro.quantidade()).isNull();
    assertThat(registro.ocorridaEm()).isNull();
    assertThat(registro.erros()).hasSizeGreaterThanOrEqualTo(3);
  }

  @Test
  void recusaPacoteComCaminhosDuplicadosOuAusentesEVinculosIlegiveis() throws Exception {
    for (String[] nomes :
        new String[][] {
          {"XL/workbook.xml", "xl/workbook.xml"},
          {"../conteudo"},
          {"xl/externalLinks/link.xml"},
          {"[Content_Types].xml"},
          {"xl/workbook.xml"},
          {"_rels/.rels"}
        }) {
      try (var output = new ByteArrayOutputStream();
          var zip = new ZipOutputStream(output)) {
        for (String nome : nomes) {
          zip.putNextEntry(new ZipEntry(nome));
          zip.write("XML inválido".getBytes(StandardCharsets.UTF_8));
          zip.closeEntry();
        }
        zip.finish();
        assertThatThrownBy(() -> leitor.ler("pacote.xlsx", output.toByteArray()))
            .isInstanceOf(ImportacaoInvalidaException.class);
      }
    }
  }

  @Test
  void quantidadeBooleanaNaoRepresentaUmAtivoInteiro() throws Exception {
    try (var workbook = new XSSFWorkbook(new java.io.ByteArrayInputStream(planilha(false)));
        var output = new ByteArrayOutputStream()) {
      workbook.getSheetAt(0).getRow(2).getCell(4).setCellValue(true);
      workbook.write(output);
      var registro = leitor.ler("booleano.xlsx", output.toByteArray()).registros().getFirst();
      assertThat(registro.quantidade()).isNull();
      assertThat(registro.erros()).isNotEmpty();
    }
  }

  @Test
  void ausenciaDeFusoCamposEFormulaDeDataFicamExplicitas() throws Exception {
    try (var workbook = new XSSFWorkbook(new java.io.ByteArrayInputStream(planilha(false)));
        var output = new ByteArrayOutputStream()) {
      var aba = workbook.getSheetAt(0);
      aba.getRow(0).getCell(0).setCellValue("Período sem fuso");
      aba.createRow(8).createCell(0).setCellValue("");
      aba.getRow(2).getCell(1).setCellValue("");
      aba.getRow(2).getCell(2).setCellValue("");
      aba.getRow(2).getCell(3).setCellValue("");
      aba.getRow(2).getCell(0).setCellFormula("1+1");
      aba.getRow(2).createCell(7).setBlank();
      workbook.write(output);
      var registro = leitor.ler("campos.xlsx", output.toByteArray()).registros().getFirst();
      assertThat(registro.ocorridaEm()).isNull();
      assertThat(registro.erros())
          .anyMatch(e -> e.contains("fuso"))
          .anyMatch(e -> e.contains("fórmula"))
          .anyMatch(e -> e.contains("conta"));
    }
  }

  @Test
  void datasSeriaisInvalidasNaoInventamInstanteE1904EhRespeitado() throws Exception {
    for (String serial : java.util.List.of("60", "-1", "2958466")) {
      try (var workbook = new XSSFWorkbook(new java.io.ByteArrayInputStream(planilha(false)));
          var output = new ByteArrayOutputStream()) {
        var celula = workbook.getSheetAt(0).getRow(2).getCell(0);
        celula.setCellValue(1);
        celula.getCTCell().setV(serial);
        var style = workbook.createCellStyle();
        style.setDataFormat(workbook.createDataFormat().getFormat("yyyy-mm-dd hh:mm:ss"));
        celula.setCellStyle(style);
        workbook.write(output);
        assertThat(
                leitor.ler("serial.xlsx", output.toByteArray()).registros().getFirst().ocorridaEm())
            .isNull();
      }
    }
    try (var workbook = new XSSFWorkbook(new java.io.ByteArrayInputStream(planilha(false)));
        var output = new ByteArrayOutputStream()) {
      workbook.getCTWorkbook().getWorkbookPr().setDate1904(true);
      var celula = workbook.getSheetAt(0).getRow(2).getCell(0);
      celula.setCellValue(1);
      var style = workbook.createCellStyle();
      style.setDataFormat(workbook.createDataFormat().getFormat("yyyy-mm-dd hh:mm:ss"));
      celula.setCellStyle(style);
      workbook.write(output);
      assertThat(leitor.ler("1904.xlsx", output.toByteArray()).registros().getFirst().ocorridaEm())
          .isEqualTo(Instant.parse("1904-01-02T03:00:00Z"));
    }
  }

  private byte[] planilha(boolean formula) throws Exception {
    try (var workbook = new XSSFWorkbook();
        var output = new ByteArrayOutputStream()) {
      var sheet = workbook.createSheet("Movimentos");
      sheet.createRow(0).createCell(0).setCellValue("Período(UTC--3)");
      String[] headers = {"Tempo", "Conta", "Operação", "Moeda", "Alterar", "Observação"};
      var header = sheet.createRow(1);
      for (int i = 0; i < headers.length; i++) header.createCell(i).setCellValue(headers[i]);
      String[][] rows = {
        {
          "2024-05-01 10:20:30",
          "Spot",
          "Withdraw",
          "BTC",
          "-0.123456789012345678",
          "origem sintética"
        },
        {"data inválida", "Funding", "Deposit", "BRL", "inválida", ""},
        {"2024-05-02 10:20:30", "Earn", "Operação desconhecida", "BTC", "1", ""}
      };
      for (int r = 0; r < rows.length; r++) {
        var row = sheet.createRow(r + 2);
        for (int c = 0; c < rows[r].length; c++) row.createCell(c).setCellValue(rows[r][c]);
      }
      if (formula) sheet.getRow(2).getCell(4).setCellFormula("1+1");
      var empty = workbook.createSheet("Vazia");
      empty.createRow(0).createCell(0).setCellValue("Período(UTC--3)");
      var emptyHeader = empty.createRow(1);
      for (int i = 0; i < headers.length; i++) emptyHeader.createCell(i).setCellValue(headers[i]);
      workbook.write(output);
      return output.toByteArray();
    }
  }

  private byte[] planilhaRelatorio(String linha) throws Exception {
    try (var workbook = new XSSFWorkbook();
        var output = new ByteArrayOutputStream()) {
      var sheet = workbook.createSheet("Relatório");
      var cabecalho = sheet.createRow(0);
      var colunas = CABECALHO.split(",");
      for (int coluna = 0; coluna < colunas.length; coluna++)
        cabecalho.createCell(coluna).setCellValue(colunas[coluna]);
      var valores = sheet.createRow(1);
      var campos = linha.split(",", -1);
      for (int coluna = 0; coluna < campos.length; coluna++)
        valores.createCell(coluna).setCellValue(campos[coluna]);
      workbook.write(output);
      return output.toByteArray();
    }
  }
}
