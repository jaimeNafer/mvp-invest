package io.github.jaimenafer.lastro.operacoes.application.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import tools.jackson.databind.JsonNode;

public record CadastroSaldoInicial(
    @NotNull(message = "Informe identificadores válidos.")
    @Positive(message = "Informe identificadores válidos.") Long instituicaoId,
    @NotNull(message = "Informe identificadores válidos.")
    @Positive(message = "Informe identificadores válidos.") Long ativoId,
    @NotNull(message = "Informe uma data válida.") String dataReferencia,
    @NotNull(message = "Envie quantidades e custos como textos decimais.") JsonNode quantidade,
    @NotNull(message = "Envie quantidades e custos como textos decimais.") JsonNode custoTotalBrl,
    Boolean custoZeroConfirmado,
    @Size(max = 300, message = "Informe a justificativa do custo zero. Limite: 300 caracteres.")
    String justificativaCustoZero,
    @Size(max = 300, message = "Informe o motivo da correção. Limite: 300 caracteres.") String motivo) {
  public CadastroSaldoInicial {
    justificativaCustoZero = justificativaCustoZero == null ? "" : justificativaCustoZero.strip();
    motivo = motivo == null ? "" : motivo.strip();
  }
}
