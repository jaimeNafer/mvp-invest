package io.github.jaimenafer.lastro.acesso.domain.exception;

public class AcessoInvalidoException extends RuntimeException {
  public AcessoInvalidoException() {
    super("Token de acesso ausente ou inválido. Entre novamente.");
  }
}
