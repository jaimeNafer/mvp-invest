package io.github.jaimenafer.lastro.operacoes.domain.model;

import java.math.BigDecimal;

public record CalculoOperacao(
    Long operacaoId,
    BigDecimal valorBrutoBrl,
    BigDecimal taxaBrl,
    BigDecimal custoBaixadoBrl,
    BigDecimal resultadoNegociacaoBrl,
    BigDecimal resultadoMoedaBrl,
    BigDecimal resultadoTaxaBrl,
    BigDecimal resultadoTotalBrl) {}
