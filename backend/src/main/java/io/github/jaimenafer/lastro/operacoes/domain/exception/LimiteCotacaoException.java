package io.github.jaimenafer.lastro.operacoes.domain.exception;

import lombok.Getter;

@Getter
public class LimiteCotacaoException extends IllegalStateException {
  private final long esperaMillis;

  public LimiteCotacaoException(int status, long esperaMillis) {
    super("A fonte pública de candles respondeu HTTP " + status + " e limitou a consulta.");
    this.esperaMillis = esperaMillis;
  }
}
