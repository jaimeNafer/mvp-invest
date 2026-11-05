package io.github.jaimenafer.lastro.operacoes.application.dto;

import java.util.List;

public record EvolucaoResultadoResponse(
    String acumuladoInicialBrl, List<ResultadoMensalResponse> meses) {}
