package io.github.jaimenafer.lastro.operacoes.domain.model;

import java.math.BigDecimal;

public record ResultadoAtivo(
    Long ativoId, BigDecimal negociacaoBrl, BigDecimal moedaBrl, BigDecimal taxaBrl) {
  public BigDecimal totalBrl() {
    return negociacaoBrl.add(moedaBrl).add(taxaBrl);
  }

  public ResultadoAtivo somar(ResultadoAtivo outro) {
    return new ResultadoAtivo(
        ativoId,
        negociacaoBrl.add(outro.negociacaoBrl),
        moedaBrl.add(outro.moedaBrl),
        taxaBrl.add(outro.taxaBrl));
  }
}
