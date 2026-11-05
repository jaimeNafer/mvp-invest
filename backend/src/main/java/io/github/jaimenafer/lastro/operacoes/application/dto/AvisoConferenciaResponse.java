package io.github.jaimenafer.lastro.operacoes.application.dto;

public record AvisoConferenciaResponse(
    String operacaoId,
    String ocorridaEm,
    String instituicaoNome,
    String ativoTicker,
    String referencia,
    String justificativa) {}
