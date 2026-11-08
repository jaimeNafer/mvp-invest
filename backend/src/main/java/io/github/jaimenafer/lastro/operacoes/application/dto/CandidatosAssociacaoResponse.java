package io.github.jaimenafer.lastro.operacoes.application.dto;

import java.util.List;

public record CandidatosAssociacaoResponse(
    String revisao,
    List<LinhaAssociacaoResponse> linhas,
    GrupoAssociacaoResponse grupo,
    List<String> avisos,
    long janelaMilissegundos) {}
