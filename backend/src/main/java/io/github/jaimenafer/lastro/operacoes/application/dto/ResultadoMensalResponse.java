package io.github.jaimenafer.lastro.operacoes.application.dto;

import java.time.LocalDate;

public record ResultadoMensalResponse(
    LocalDate dataInicio,
    LocalDate dataFim,
    String resultadoBrl,
    String acumuladoBrl,
    long operacoes,
    String subtotalResultadoBrl,
    String subtotalAcumuladoBrl) {}
