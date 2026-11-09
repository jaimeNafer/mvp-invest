package io.github.jaimenafer.lastro.operacoes.domain.model;

import java.time.Instant;

public record CargaCotacao(Long id, Long parId, String simbolo, String granularidade,
    Instant inicio, Instant fim, String reservaToken, int tentativas) {}
