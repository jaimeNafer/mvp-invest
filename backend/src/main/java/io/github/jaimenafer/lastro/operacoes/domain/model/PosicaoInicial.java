package io.github.jaimenafer.lastro.operacoes.domain.model;

import java.math.BigDecimal;
import java.util.List;

public record PosicaoInicial(
    Long ativoId,
    String ticker,
    BigDecimal quantidade,
    BigDecimal custo,
    BigDecimal medio,
    List<FatoPosicaoInicial> instituicoes) {}
