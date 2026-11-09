package io.github.jaimenafer.lastro.importacoes.infrastructure.arquivos;

import io.github.jaimenafer.lastro.importacoes.domain.exception.ImportacaoInvalidaException;
import io.github.jaimenafer.lastro.importacoes.domain.model.MovimentacaoRecebida;
import java.io.IOException;
import java.io.StringReader;
import java.nio.ByteBuffer;
import java.nio.charset.CharacterCodingException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import org.apache.commons.csv.CSVFormat;

final class LeitorCsvMovimentacoes {
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

  List<MovimentacaoRecebida> ler(byte[] conteudo, int limite) {
    String texto = decodificar(conteudo);
    if (texto.startsWith("\ufeff")) texto = texto.substring(1);
    int fimCabecalho = texto.indexOf('\n');
    String cabecalho = fimCabecalho < 0 ? texto : texto.substring(0, fimCabecalho);
    if (!campos(cabecalho, new ArrayList<>()).equals(CABECALHO)) {
      throw new ImportacaoInvalidaException("O cabeçalho não corresponde ao relatório CSV aceito.");
    }
    if (fimCabecalho < 0) return List.of();
    var registros = new ArrayList<MovimentacaoRecebida>();
    String corpo = texto.substring(fimCabecalho + 1);
    int inicio = 0;
    int linha = 2;
    int linhaInicio = linha;
    boolean aspas = false;
    boolean inicioCampo = true;
    for (int i = 0; i < corpo.length(); i++) {
      char atual = corpo.charAt(i);
      if (atual == '"') {
        if (aspas && i + 1 < corpo.length() && corpo.charAt(i + 1) == '"') {
          i++;
          continue;
        }
        if (aspas || inicioCampo) aspas = !aspas;
      }
      if (atual == '\n') {
        if (!aspas) {
          adicionar(corpo.substring(inicio, i), linhaInicio, false, registros, limite);
          inicio = i + 1;
          linhaInicio = linha + 1;
        }
        linha++;
      }
      inicioCampo = !aspas && (atual == ',' || atual == '\n');
    }
    if (inicio < corpo.length()) {
      String resto = corpo.substring(inicio);
      if (aspas) {
        for (String original : resto.split("\\r?\\n", -1))
          adicionar(original, linhaInicio++, true, registros, limite);
      } else adicionar(resto, linhaInicio, false, registros, limite);
    }
    return registros;
  }

  private void adicionar(
      String texto,
      int linha,
      boolean malformado,
      List<MovimentacaoRecebida> registros,
      int limite) {
    if (texto.isBlank()) return;
    var erros = new ArrayList<String>();
    if (malformado) erros.add("O registro CSV contém aspas sem fechamento.");
    List<String> valores = campos(texto, erros);
    if (!malformado && erros.isEmpty()
        && (valores.stream().allMatch(String::isBlank)
            || ValoresMovimentacao.mensagemSemResultados(valores))) return;
    if (valores.size() != CABECALHO.size())
      erros.add("O registro não contém todas as colunas do cabeçalho.");
    var originais = new LinkedHashMap<String, String>();
    for (int i = 0; i < Math.max(CABECALHO.size(), valores.size()); i++) {
      originais.put(
          i < CABECALHO.size() ? CABECALHO.get(i) : "coluna_" + (i + 1),
          i < valores.size() ? valores.get(i) : "");
    }
    if (malformado) originais.put("registro_original", texto);
    registros.addAll(LinhasRelatorioBinance.expandir(linha, "", originais, erros));
    if (registros.size() > limite)
      throw new ImportacaoInvalidaException("O arquivo excede o limite de registros permitido.");
  }

  private List<String> campos(String texto, List<String> erros) {
    try (var parser = CSVFormat.RFC4180.parse(new StringReader(texto))) {
      var registros = parser.getRecords();
      if (registros.size() == 1) return registros.getFirst().toList();
      erros.add("O registro CSV não pôde ser delimitado.");
    } catch (IOException erro) {
      erros.add("O registro CSV está malformado.");
    }
    return Arrays.asList(texto.split(",", -1));
  }

  private String decodificar(byte[] conteudo) {
    try {
      return StandardCharsets.UTF_8.newDecoder().decode(ByteBuffer.wrap(conteudo)).toString();
    } catch (CharacterCodingException erro) {
      throw new ImportacaoInvalidaException("O arquivo CSV deve estar codificado em UTF-8.");
    }
  }
}
