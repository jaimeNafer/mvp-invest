package io.github.jaimenafer.lastro.operacoes.application.dto;

import io.github.jaimenafer.lastro.operacoes.domain.model.*;
import jakarta.validation.constraints.*;
import tools.jackson.databind.JsonNode;

public record SaldoAnteriorConferenciaRequest(
    @NotBlank(message = "Informe a revisão do histórico.") String revisao,
    @NotBlank(message = "Informe o registro de referência.") String ancoraId,
    @NotBlank(message = "Informe a conta de custódia.") String contaId,
    @NotBlank(message = "Informe o ativo.") String ativoId,
    @NotBlank(message = "Informe data e hora com fuso horário.") String data,
    @NotNull(message = "Informe a quantidade como texto decimal.") JsonNode quantidade,
    JsonNode custoTotalBrl,
    @NotBlank @Size(max = 500) String motivo) {}
