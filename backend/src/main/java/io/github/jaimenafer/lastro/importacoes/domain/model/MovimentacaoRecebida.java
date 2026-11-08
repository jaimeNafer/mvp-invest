package io.github.jaimenafer.lastro.importacoes.domain.model;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Map;

public record MovimentacaoRecebida(
    int linha,
    String aba,
    String conta,
    String operacao,
    String moeda,
    String instanteOriginal,
    Instant ocorridaEm,
    BigDecimal quantidade,
    String quantidadeOriginal,
    Map<String, String> camposOriginais,
    List<String> erros) {
  public MovimentacaoRecebida {
    camposOriginais = Map.copyOf(camposOriginais);
    erros = List.copyOf(erros);
  }
}
