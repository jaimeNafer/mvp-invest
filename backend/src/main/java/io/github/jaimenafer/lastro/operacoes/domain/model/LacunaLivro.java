package io.github.jaimenafer.lastro.operacoes.domain.model;

import java.math.BigDecimal;

public record LacunaLivro(
    String id,
    String ancoraId,
    Long contaId,
    Long instituicaoId,
    Long ativoId,
    BigDecimal quantidade) {}
