package io.github.jaimenafer.lastro.operacoes.application.dto;

import java.util.List;

public record PosicaoLivroResponse(
    String ativoId,
    String ativoTicker,
    String quantidade,
    String custoTotalBrl,
    String custoMedioBrl,
    List<CustodiaLivroResponse> instituicoes) {}
