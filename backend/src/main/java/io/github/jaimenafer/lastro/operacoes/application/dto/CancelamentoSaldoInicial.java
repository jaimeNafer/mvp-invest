package io.github.jaimenafer.lastro.operacoes.application.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record CancelamentoSaldoInicial(
    @NotBlank(message = "Informe o motivo do cancelamento. Limite: 300 caracteres.")
    @Size(max = 300, message = "Informe o motivo do cancelamento. Limite: 300 caracteres.") String motivo) {
  public CancelamentoSaldoInicial {
    motivo = motivo == null ? null : motivo.strip();
  }
}
