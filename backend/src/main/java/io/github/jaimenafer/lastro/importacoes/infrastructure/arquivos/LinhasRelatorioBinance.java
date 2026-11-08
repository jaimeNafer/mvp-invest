package io.github.jaimenafer.lastro.importacoes.infrastructure.arquivos;

import io.github.jaimenafer.lastro.importacoes.domain.model.MovimentacaoRecebida;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

final class LinhasRelatorioBinance {
  private LinhasRelatorioBinance() {}

  static List<MovimentacaoRecebida> expandir(
      int linha, String aba, Map<String, String> campos, List<String> erros) {
    var resultado = new ArrayList<MovimentacaoRecebida>();
    String data = campos.getOrDefault("datetime_tz_BRT", "");
    var instante = ValoresMovimentacao.instante(data, "uuuu-MM-dd-HH:mm:ss", erros);
    String tipo = campos.getOrDefault("type", "");
    if (tipo.isBlank()) erros.add("A operação de origem não foi informada.");
    boolean compra = tipo.equalsIgnoreCase("Buy");
    adicionar(
        resultado,
        linha,
        aba,
        campos,
        erros,
        instante,
        "sent",
        compra ? "Transaction Spend" : tipo,
        "sent_amount",
        "sent_currency",
        false);
    adicionar(
        resultado,
        linha,
        aba,
        campos,
        erros,
        instante,
        "received",
        compra ? "Transaction Buy" : tipo,
        "received_amount",
        "received_currency",
        true);
    adicionar(
        resultado,
        linha,
        aba,
        campos,
        erros,
        instante,
        "fee",
        "Transaction Fee",
        "fee_amount",
        "fee_currency",
        false);
    if (resultado.isEmpty()) {
      adicionarRegistroSemMovimento(resultado, linha, aba, campos, erros, instante);
    }
    return List.copyOf(resultado);
  }

  private static void adicionar(
      List<MovimentacaoRecebida> resultado,
      int linha,
      String aba,
      Map<String, String> campos,
      List<String> erros,
      java.time.Instant instante,
      String componente,
      String operacao,
      String campoValor,
      String campoMoeda,
      boolean recebimento) {
    String valorOriginal = campos.getOrDefault(campoValor, "");
    if (valorOriginal.isBlank()) return;
    var errosLinha = new ArrayList<>(erros);
    BigDecimal quantidade = ValoresMovimentacao.quantidade(valorOriginal, errosLinha);
    if (quantidade != null && quantidade.signum() == 0) return;
    if (quantidade != null && quantidade.signum() < 0) {
      errosLinha.add("O valor bruto deve ser positivo no arquivo de origem.");
      quantidade = quantidade.abs();
    }
    if (quantidade != null && "sent".equals(componente) && taxaIncluidaNoEnvio(campos)) {
      BigDecimal taxa = ValoresMovimentacao.quantidade(campos.get("fee_amount"), errosLinha);
      if (taxa != null && taxa.signum() >= 0 && quantidade.compareTo(taxa) > 0) {
        quantidade = quantidade.subtract(taxa);
      } else {
        errosLinha.add("A taxa em BRL deve ser menor que o total enviado.");
        quantidade = null;
      }
    }
    String moeda = campos.getOrDefault(campoMoeda, "");
    if (moeda.isBlank()) errosLinha.add("A moeda de origem não foi informada.");
    if (quantidade != null && !recebimento) quantidade = quantidade.negate();
    resultado.add(
        criar(
            linha,
            aba,
            campos,
            errosLinha,
            instante,
            componente,
            operacao,
            moeda,
            quantidade,
            valorOriginal));
  }

  private static boolean taxaIncluidaNoEnvio(Map<String, String> campos) {
    return "Buy".equalsIgnoreCase(campos.get("type"))
        && "OCBS".equalsIgnoreCase(campos.get("market_model_type"))
        && "BRL".equals(campos.get("sent_currency"))
        && "BRL".equals(campos.get("fee_currency"))
        && !campos.getOrDefault("fee_amount", "").isBlank();
  }

  private static void adicionarRegistroSemMovimento(
      List<MovimentacaoRecebida> resultado,
      int linha,
      String aba,
      Map<String, String> campos,
      List<String> erros,
      java.time.Instant instante) {
    resultado.add(
        criar(
            linha,
            aba,
            campos,
            erros,
            instante,
            "registro",
            campos.getOrDefault("type", ""),
            "",
            null,
            ""));
  }

  private static MovimentacaoRecebida criar(
      int linha,
      String aba,
      Map<String, String> campos,
      List<String> erros,
      java.time.Instant instante,
      String componente,
      String operacao,
      String moeda,
      BigDecimal quantidade,
      String quantidadeOriginal) {
    var originais = new LinkedHashMap<>(campos);
    originais.put("componente", componente);
    return new MovimentacaoRecebida(
        linha,
        aba,
        "",
        operacao,
        moeda,
        campos.getOrDefault("datetime_tz_BRT", ""),
        instante,
        quantidade,
        quantidadeOriginal,
        originais,
        erros);
  }
}
