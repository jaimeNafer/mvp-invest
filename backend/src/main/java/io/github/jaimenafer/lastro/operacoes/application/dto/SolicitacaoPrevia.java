package io.github.jaimenafer.lastro.operacoes.application.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.*;

public record SolicitacaoPrevia(
    @NotNull(message = "Informe a ação da prévia.") AcaoPrevia acao,
    @Positive(message = "Informe uma operação válida.") Long operacaoId,
    @Valid CadastroOperacao operacao,
    @Size(max = 300, message = "O motivo aceita até 300 caracteres.") String motivo) {
  public SolicitacaoPrevia {
    motivo = motivo == null ? "" : motivo.strip();
  }
}
