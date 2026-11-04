package io.github.jaimenafer.lastro.operacoes.application.dto;

import jakarta.validation.constraints.Pattern;

public record FiltroVisaoGeral(
    @Pattern(
            regexp = "\\d{4}-\\d{2}-\\d{2}",
            message = "Informe a data inicial no formato AAAA-MM-DD.")
        String dataInicio,
    @Pattern(
            regexp = "\\d{4}-\\d{2}-\\d{2}",
            message = "Informe a data final no formato AAAA-MM-DD.")
        String dataFim) {}
