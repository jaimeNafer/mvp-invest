package io.github.jaimenafer.lastro.importacoes.infrastructure.arquivos;

import io.github.jaimenafer.lastro.importacoes.domain.exception.ImportacaoInvalidaException;
import io.github.jaimenafer.lastro.importacoes.domain.model.MovimentacaoRecebida;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.apache.poi.ooxml.POIXMLException;
import org.apache.poi.ss.usermodel.CellType;
import org.apache.poi.ss.usermodel.DateUtil;
import org.apache.poi.xssf.usermodel.XSSFRow;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

final class LeitorPlanilhaRelatorioBinance {
  private static final List<String> CABECALHO =
      List.of(
          "id",
          "datetime_tz_BRT",
          "type",
          "label",
          "market_model_type",
          "order_type",
          "sent_amount",
          "sent_currency",
          "sent_value_BRL",
          "sent_address",
          "received_amount",
          "received_currency",
          "received_value_BRL",
          "received_address",
          "fee_amount",
          "fee_currency",
          "fee_value_BRL");
  private static final DateTimeFormatter DATA = DateTimeFormatter.ofPattern("uuuu-MM-dd-HH:mm:ss");

  List<MovimentacaoRecebida> ler(byte[] conteudo, int limite) {
    try (var workbook = new XSSFWorkbook(new ByteArrayInputStream(conteudo))) {
      var registros = new ArrayList<MovimentacaoRecebida>();
      boolean encontrado = false;
      for (var planilha : workbook) {
        var sheet = (org.apache.poi.xssf.usermodel.XSSFSheet) planilha;
        int cabecalho = linhaCabecalho(sheet);
        if (cabecalho < 0) continue;
        encontrado = true;
        for (int indice = cabecalho + 1; indice <= sheet.getLastRowNum(); indice++) {
          XSSFRow row = sheet.getRow(indice);
          if (row == null) continue;
          Map<String, String> campos = campos(row);
          if (campos.values().stream().allMatch(String::isBlank)) continue;
          var erros = new ArrayList<String>();
          if (row.getLastCellNum() != CABECALHO.size())
            erros.add("O registro não contém todas as colunas do cabeçalho.");
          campos.put("datetime_tz_BRT", data(row, campos.get("datetime_tz_BRT"), erros));
          if (row.cellIterator().hasNext()
              && java.util.stream.IntStream.range(0, CABECALHO.size())
                  .anyMatch(coluna ->
                      row.getCell(coluna) != null
                          && row.getCell(coluna).getCellType() == CellType.FORMULA))
            erros.add("O registro contém fórmula e exige conferência.");
          registros.addAll(
              LinhasRelatorioBinance.expandir(
                  indice + 1, sheet.getSheetName(), campos, erros));
          if (registros.size() > limite)
            throw new ImportacaoInvalidaException(
                "O arquivo excede o limite de registros permitido.");
        }
      }
      return encontrado ? List.copyOf(registros) : null;
    } catch (IOException | POIXMLException | IllegalArgumentException erro) {
      throw new ImportacaoInvalidaException("Não foi possível interpretar a planilha XLSX.");
    }
  }

  private int linhaCabecalho(org.apache.poi.xssf.usermodel.XSSFSheet sheet) {
    for (int indice = 0; indice <= Math.min(sheet.getLastRowNum(), 20); indice++) {
      XSSFRow row = sheet.getRow(indice);
      if (row == null || row.getLastCellNum() != CABECALHO.size()) continue;
      var valores = new ArrayList<String>();
      for (int coluna = 0; coluna < CABECALHO.size(); coluna++)
        valores.add(texto(row, coluna));
      if (valores.equals(CABECALHO)) return indice;
    }
    return -1;
  }

  private Map<String, String> campos(XSSFRow row) {
    var valores = new LinkedHashMap<String, String>();
    for (int coluna = 0; coluna < CABECALHO.size(); coluna++)
      valores.put(CABECALHO.get(coluna), texto(row, coluna));
    return valores;
  }

  private String texto(XSSFRow row, int coluna) {
    var cell = row.getCell(coluna);
    if (cell == null) return "";
    return switch (cell.getCellType()) {
      case STRING -> cell.getStringCellValue();
      case NUMERIC, ERROR, BOOLEAN -> cell.getRawValue() == null ? "" : cell.getRawValue();
      case FORMULA -> "=" + cell.getCellFormula();
      default -> "";
    };
  }

  private String data(XSSFRow row, String valor, List<String> erros) {
    var cell = row.getCell(1);
    if (cell == null
        || cell.getCellType() != CellType.NUMERIC
        || !DateUtil.isADateFormat(
            cell.getCellStyle().getDataFormat(), cell.getCellStyle().getDataFormatString()))
      return valor;
    try {
      BigDecimal serial = new BigDecimal(cell.getRawValue());
      long dias = serial.setScale(0, RoundingMode.FLOOR).longValueExact();
      if (dias < 0 || dias > 2958465 || dias == 60) throw new ArithmeticException();
      long nanos =
          serial
              .subtract(BigDecimal.valueOf(dias))
              .multiply(new BigDecimal("86400000000000"))
              .setScale(0, RoundingMode.HALF_EVEN)
              .longValueExact();
      return LocalDate.of(1899, 12, 31)
          .plusDays(dias - (dias > 60 ? 1 : 0))
          .atStartOfDay()
          .plusNanos(nanos)
          .format(DATA);
    } catch (ArithmeticException erro) {
      erros.add("A data numérica da planilha não é válida.");
      return valor;
    }
  }
}
