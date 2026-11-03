package io.github.jaimenafer.lastro.operacoes.application.dto;

import io.github.jaimenafer.lastro.operacoes.domain.model.SituacaoOperacao;
import java.time.LocalDate;

public record SaldoInicialResponse(
    String id,
    String instituicaoId,
    String instituicaoNome,
    String ativoId,
    String ativoTicker,
    LocalDate dataReferencia,
    String quantidade,
    String custoTotalBrl,
    SituacaoOperacao situacao,
    String substituiId,
    String justificativaCustoZero,
    String motivoAlteracao,
    String criadoEm,
    String alteradoEm) {}
