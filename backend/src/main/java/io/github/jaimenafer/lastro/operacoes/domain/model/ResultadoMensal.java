package io.github.jaimenafer.lastro.operacoes.domain.model;

import java.math.BigDecimal;
import java.time.LocalDate;

public record ResultadoMensal(
    LocalDate dataInicio,
    LocalDate dataFim,
    BigDecimal resultadoBrl,
    BigDecimal acumuladoBrl,
    long operacoes) {}
