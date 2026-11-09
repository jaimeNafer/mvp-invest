package io.github.jaimenafer.lastro.operacoes.domain.model;

import java.math.BigDecimal;
import java.time.Instant;

public record ValoracaoManual(Long registroId, Long grupoId, BigDecimal valorBrl, String identidade, long versao,
    String alteradoPor, Instant alteradoEm) {}
