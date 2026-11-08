package io.github.jaimenafer.lastro.operacoes.application.contract;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Map;

public record LinhaConferenciaRecebida(
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
  public LinhaConferenciaRecebida {
    camposOriginais = Map.copyOf(camposOriginais);
    erros = List.copyOf(erros);
  }
}
