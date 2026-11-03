package io.github.jaimenafer.lastro.carteiras.application.dto;

import jakarta.validation.constraints.NotNull;

public record SituacaoCarteira(
    @NotNull(message = "Informe a situação da carteira.") Boolean ativo) {}
