package io.github.jaimenafer.lastro.operacoes.domain.model;

import java.math.BigDecimal;
import java.time.Instant;

public record PrecoCandle(Instant instante, BigDecimal preco) {}
