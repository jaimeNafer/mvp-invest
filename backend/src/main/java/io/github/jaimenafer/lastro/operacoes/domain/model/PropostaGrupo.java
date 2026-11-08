package io.github.jaimenafer.lastro.operacoes.domain.model;

import java.math.BigDecimal;
import java.util.List;

public record PropostaGrupo(
    List<LinhaAssociacao> linhas,
    boolean taxaZeroConfirmada,
    BigDecimal cotacaoBrl,
    boolean desfazer) {
  public PropostaGrupo {
    linhas = List.copyOf(linhas);
  }
}
