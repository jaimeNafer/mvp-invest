package io.github.jaimenafer.lastro.operacoes.application.dto;

import io.github.jaimenafer.lastro.operacoes.domain.model.EstadoValoracao;

public record ValoracaoTimelineResponse(EstadoValoracao statusValoracao, String valorTotalBrl,
    String cotacaoUtilizada, String dataHoraCotacao, String fonteCotacao, String metodoConversao,
    boolean aproximado, String motivoIndisponibilidade, boolean permiteCorrecaoManual) {}
