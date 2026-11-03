package io.github.jaimenafer.lastro.operacoes.application.dto;

public record EntradaSaldoInicial(
    Long instituicaoId,
    Long ativoId,
    String dataReferencia,
    String quantidade,
    String custoTotalBrl,
    Boolean custoZeroConfirmado,
    String justificativaCustoZero,
    String motivo) {}
