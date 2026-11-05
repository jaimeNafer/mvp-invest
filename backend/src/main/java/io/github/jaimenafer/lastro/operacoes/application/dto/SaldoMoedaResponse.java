package io.github.jaimenafer.lastro.operacoes.application.dto;

import java.util.List;

public record SaldoMoedaResponse(
    String ativoId,
    String ativoTicker,
    String ativoNome,
    String quantidade,
    List<CustodiaLivroResponse> instituicoes) {}
