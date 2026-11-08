package io.github.jaimenafer.lastro.importacoes.infrastructure.arquivos;

import io.github.jaimenafer.lastro.importacoes.domain.exception.ImportacaoInvalidaException;
import io.github.jaimenafer.lastro.importacoes.domain.model.MovimentacaoRecebida;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.apache.poi.ooxml.POIXMLException;
import org.apache.poi.ss.usermodel.CellType;
import org.apache.poi.ss.usermodel.DateUtil;
import org.apache.poi.xssf.usermodel.XSSFCell;
import org.apache.poi.xssf.usermodel.XSSFRow;
import org.apache.poi.xssf.usermodel.XSSFSheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

final class LeitorPlanilhaMovimentacoes {
  private static final Set<String> OBRIGATORIOS =
      Set.of("tempo", "conta", "operacao", "moeda", "alterar");

  List<MovimentacaoRecebida> ler(byte[] conteudo, int limite) {
    try (var workbook = new XSSFWorkbook(new ByteArrayInputStream(conteudo))) {
      var registros = new ArrayList<MovimentacaoRecebida>();
      if (!workbook.getExternalLinksTables().isEmpty() || workbook.isMacroEnabled()) {
        throw new ImportacaoInvalidaException("A planilha contém recursos não permitidos.");
      }
      for (var sheet : workbook)
        lerAba((XSSFSheet) sheet, workbook.isDate1904(), registros, limite);
      return registros;
    } catch (IOException | POIXMLException | IllegalArgumentException erro) {
      throw new ImportacaoInvalidaException("Não foi possível interpretar a planilha XLSX.");
    }
  }

  private void lerAba(
      XSSFSheet sheet, boolean data1904, List<MovimentacaoRecebida> registros, int limite) {
    Map<String, Integer> colunas = Map.of();
    Map<Integer, String> nomes = Map.of();
    boolean fusoIdentificado = false;
    for (var row : sheet) {
      Map<Integer, String> valores = valores((XSSFRow) row);
      if (valores.values().stream().allMatch(String::isBlank)) continue;
      if (colunas.isEmpty()) {
        fusoIdentificado |=
            valores.values().stream()
                .map(ValoresMovimentacao::normalizar)
                .anyMatch(v -> v.contains("periodo") && v.matches(".*utc\\s*-\\s*-?\\s*0?3.*"));
        var candidato = new LinkedHashMap<String, Integer>();
        valores.forEach(
            (indice, texto) -> candidato.put(ValoresMovimentacao.normalizar(texto), indice));
        if (candidato.keySet().containsAll(OBRIGATORIOS)) {
          colunas = candidato;
          nomes = valores;
        }
        continue;
      }
      if (registros.size() >= limite)
        throw new ImportacaoInvalidaException("O arquivo excede o limite de registros permitido.");
      registros.add(registro((XSSFRow) row, colunas, nomes, valores, fusoIdentificado, data1904));
    }
    if (colunas.isEmpty())
      throw new ImportacaoInvalidaException(
          "O cabeçalho da planilha não corresponde ao relatório de movimentações aceito.");
  }

  private MovimentacaoRecebida registro(
      XSSFRow row,
      Map<String, Integer> colunas,
      Map<Integer, String> nomes,
      Map<Integer, String> valores,
      boolean fuso,
      boolean data1904) {
    var erros = new ArrayList<String>();
    var originais = new LinkedHashMap<String, String>();
    valores.forEach((indice, valor) ->
        originais.put(nomes.getOrDefault(indice, "coluna_" + (indice + 1)), valor));
    String data = campo("tempo", colunas, valores);
    String quantidade = campo("alterar", colunas, valores);
    String conta = campo("conta", colunas, valores);
    String operacao = campo("operacao", colunas, valores);
    String moeda = campo("moeda", colunas, valores);
    boolean formulaData = formula(row, colunas.get("tempo"));
    boolean formulaQuantidade = formula(row, colunas.get("alterar"));
    for (String nome : OBRIGATORIOS) {
      if (formula(row, colunas.get(nome)))
        erros.add("O campo " + nome + " contém fórmula e exige conferência.");
    }
    if (conta.isBlank()) erros.add("A conta de origem não foi informada.");
    if (operacao.isBlank()) erros.add("A operação de origem não foi informada.");
    if (moeda.isBlank()) erros.add("A moeda de origem não foi informada.");
    boolean tipoQuantidadeLegivel = quantidadeLegivel(row.getCell(colunas.get("alterar")));
    if (!tipoQuantidadeLegivel && !formulaQuantidade)
      erros.add("A quantidade deve ser informada como texto decimal ou número.");
    BigDecimal valor =
        tipoQuantidadeLegivel ? ValoresMovimentacao.quantidade(quantidade, erros) : null;
    Instant instante = null;
    if (!fuso) erros.add("O fuso horário do relatório não foi identificado.");
    if (fuso && !formulaData)
      instante = instante(row.getCell(colunas.get("tempo")), data, data1904, erros);
    return new MovimentacaoRecebida(
        row.getRowNum() + 1,
        row.getSheet().getSheetName(),
        conta,
        operacao,
        moeda,
        data,
        instante,
        valor,
        quantidade,
        originais,
        erros);
  }

  private Instant instante(XSSFCell cell, String texto, boolean data1904, List<String> erros) {
    if (cell != null
        && cell.getCellType() == CellType.NUMERIC
        && DateUtil.isADateFormat(
            cell.getCellStyle().getDataFormat(), cell.getCellStyle().getDataFormatString())) {
      try {
        BigDecimal serial = new BigDecimal(texto);
        long dias = serial.setScale(0, RoundingMode.FLOOR).longValueExact();
        if (dias < 0 || dias > 2958465 || (!data1904 && dias == 60))
          throw new ArithmeticException();
        long nanos =
            serial
                .subtract(BigDecimal.valueOf(dias))
                .multiply(new BigDecimal("86400000000000"))
                .setScale(0, RoundingMode.HALF_EVEN)
                .longValueExact();
        var base = data1904 ? LocalDate.of(1904, 1, 1) : LocalDate.of(1899, 12, 31);
        long correcao = !data1904 && dias > 60 ? 1 : 0;
        return base.plusDays(dias - correcao)
            .atStartOfDay()
            .plusNanos(nanos)
            .toInstant(ZoneOffset.ofHours(-3));
      } catch (ArithmeticException erro) {
        erros.add("A data numérica da planilha não é válida.");
        return null;
      }
    }
    return ValoresMovimentacao.instante(texto, "uuuu-MM-dd HH:mm:ss", erros);
  }

  private Map<Integer, String> valores(XSSFRow row) {
    var valores = new LinkedHashMap<Integer, String>();
    for (var cell : row) {
      XSSFCell celula = (XSSFCell) cell;
      valores.put(
          celula.getColumnIndex(),
          switch (celula.getCellType()) {
            case STRING -> celula.getStringCellValue();
            case NUMERIC, ERROR, BOOLEAN ->
                celula.getRawValue() == null ? "" : celula.getRawValue();
            case FORMULA -> "=" + celula.getCellFormula();
            default -> "";
          });
    }
    return valores;
  }

  private String campo(String nome, Map<String, Integer> colunas, Map<Integer, String> valores) {
    return valores.getOrDefault(colunas.get(nome), "");
  }

  private boolean formula(XSSFRow row, int indice) {
    return row.getCell(indice) != null && row.getCell(indice).getCellType() == CellType.FORMULA;
  }

  private boolean quantidadeLegivel(XSSFCell cell) {
    return cell != null
        && (cell.getCellType() == CellType.STRING || cell.getCellType() == CellType.NUMERIC);
  }
}
