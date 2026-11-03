package io.github.jaimenafer.lastro.operacoes.domain.model;

import java.math.BigDecimal;
import java.util.List;

public record PosicaoLivro(
    Long ativoId,
    BigDecimal quantidade,
    BigDecimal custoTotalBrl,
    BigDecimal custoMedioBrl,
    List<CustodiaLivro> instituicoes) {
  public PosicaoLivro {
    instituicoes = List.copyOf(instituicoes);
  }
}
