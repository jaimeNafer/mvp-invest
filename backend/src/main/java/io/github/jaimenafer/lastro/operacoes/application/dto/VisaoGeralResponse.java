package io.github.jaimenafer.lastro.operacoes.application.dto;

import java.time.LocalDate;
import java.util.List;

public record VisaoGeralResponse(
    String revisao,
    LocalDate dataInicio,
    LocalDate dataFim,
    LocalDate dataReferencia,
    boolean possuiRegistrosAteFim,
    ResumoVisaoGeralResponse resumo,
    ParcelasResultadoResponse resultado,
    List<PosicaoVisaoGeralResponse> posicoes) {}
