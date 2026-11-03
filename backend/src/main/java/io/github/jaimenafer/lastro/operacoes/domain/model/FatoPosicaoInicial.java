package io.github.jaimenafer.lastro.operacoes.domain.model;

import java.math.BigDecimal;

public record FatoPosicaoInicial(
    Long ativoId,
    String ticker,
    Long instituicaoId,
    String instituicaoNome,
    BigDecimal quantidade,
    BigDecimal custo) {}
