package io.github.jaimenafer.lastro.operacoes.application.dto;

import java.math.BigDecimal;
import java.time.LocalDate;

public record DadosSaldoInicial(
    Long instituicaoId,
    Long ativoId,
    LocalDate data,
    BigDecimal quantidade,
    BigDecimal custo,
    String justificativa,
    String motivo) {}
