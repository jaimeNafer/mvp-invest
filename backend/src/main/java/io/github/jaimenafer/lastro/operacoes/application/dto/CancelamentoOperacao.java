package io.github.jaimenafer.lastro.operacoes.application.dto;

import jakarta.validation.constraints.*;

public record CancelamentoOperacao(
    @NotNull(message = "Atualize a prévia antes de confirmar.")
        @PositiveOrZero(message = "Informe uma revisão válida.")
        Long revisao,
    @NotBlank(message = "Informe o motivo do cancelamento.")
        @Size(max = 300, message = "O motivo aceita até 300 caracteres.")
        String motivo) {
  public CancelamentoOperacao {
    motivo = motivo == null ? null : motivo.strip();
  }
}
