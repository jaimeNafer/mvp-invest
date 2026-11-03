package io.github.jaimenafer.lastro.operacoes.domain.model;

import java.util.List;

public record ResultadoLivro(List<PosicaoLivro> posicoes, List<CalculoOperacao> calculos) {
  public ResultadoLivro {
    posicoes = List.copyOf(posicoes);
    calculos = List.copyOf(calculos);
  }
}
