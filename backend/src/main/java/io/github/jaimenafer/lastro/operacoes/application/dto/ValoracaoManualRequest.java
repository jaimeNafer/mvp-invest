package io.github.jaimenafer.lastro.operacoes.application.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import tools.jackson.databind.JsonNode;

public record ValoracaoManualRequest(
    @NotBlank String revisao,
    @NotBlank String versao,
    @NotNull JsonNode valorTotalBrl) {}
