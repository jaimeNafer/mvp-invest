package io.github.jaimenafer.lastro.operacoes.domain.model;

import java.math.BigDecimal;
import java.util.List;

public record PosicaoConferenciaLivro(
    Long ativoId,
    BigDecimal quantidade,
    BigDecimal custoTotalBrl,
    BigDecimal custoMedioBrl,
    boolean quantidadeDisponivel,
    List<CustodiaConferenciaLivro> contas,
    List<String> causas) {
  public PosicaoConferenciaLivro {
    contas = List.copyOf(contas);
    causas = List.copyOf(causas);
  }
}
