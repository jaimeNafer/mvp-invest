package io.github.jaimenafer.lastro.operacoes.application.dto;

import java.util.List;

public record PosicaoInicialResponse(
    String ativoId,
    String ativoTicker,
    String quantidade,
    String custoTotalBrl,
    String custoMedioBrl,
    List<CustodiaInicialResponse> instituicoes) {}
