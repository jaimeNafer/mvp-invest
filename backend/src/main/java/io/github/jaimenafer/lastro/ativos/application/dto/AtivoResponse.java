package io.github.jaimenafer.lastro.ativos.application.dto;

import io.github.jaimenafer.lastro.ativos.domain.model.TipoAtivo;

public record AtivoResponse(
    String id,
    String codigo,
    String ticker,
    String nome,
    TipoAtivo tipo,
    int casasDecimais,
    boolean ativo) {}
