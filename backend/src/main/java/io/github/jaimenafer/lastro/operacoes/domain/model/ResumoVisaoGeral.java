package io.github.jaimenafer.lastro.operacoes.domain.model;

import java.math.BigDecimal;

public record ResumoVisaoGeral(
    BigDecimal custoSaldosBrl,
    BigDecimal resultadoRealizadoBrl,
    BigDecimal taxasBrl,
    BigDecimal aportesBrl,
    long operacoesNoPeriodo,
    BigDecimal negociacaoBrl,
    BigDecimal moedaBrl,
    BigDecimal taxaBrl) {}
