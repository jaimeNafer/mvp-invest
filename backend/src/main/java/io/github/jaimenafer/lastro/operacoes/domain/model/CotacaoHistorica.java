package io.github.jaimenafer.lastro.operacoes.domain.model;

import java.math.BigDecimal;
import java.time.Instant;

public record CotacaoHistorica(Long baseId, Long moedaId, Instant instante, BigDecimal preco,
    String fonte, String granularidade, long versao, String checksum, Instant obtidoEm) {
  public CotacaoHistorica(Long baseId, Long moedaId, Instant instante, BigDecimal preco) {
    this(baseId, moedaId, instante, preco, null, null, 0, null, null);
  }
}
