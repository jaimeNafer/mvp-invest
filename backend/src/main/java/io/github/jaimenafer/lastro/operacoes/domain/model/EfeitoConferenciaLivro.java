package io.github.jaimenafer.lastro.operacoes.domain.model;

import java.util.List;

public record EfeitoConferenciaLivro(
    String id, EstadoConferencia estado, List<String> causas, CalculoOperacao calculo) {
  public EfeitoConferenciaLivro {
    causas = List.copyOf(causas);
  }
}
