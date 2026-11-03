package io.github.jaimenafer.lastro.operacoes.application.dto;

public record CustodiaInicialResponse(
    String instituicaoId,
    String instituicaoNome,
    String quantidade,
    String custoInicialInformadoBrl) {}
