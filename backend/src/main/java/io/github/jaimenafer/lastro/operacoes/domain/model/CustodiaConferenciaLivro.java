package io.github.jaimenafer.lastro.operacoes.domain.model;

import java.math.BigDecimal;

public record CustodiaConferenciaLivro(
    Long contaId, Long instituicaoId, BigDecimal quantidade, boolean quantidadeDisponivel) {}
