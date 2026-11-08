package io.github.jaimenafer.lastro.operacoes.application.dto;

import io.github.jaimenafer.lastro.operacoes.domain.model.PapelLinha;

public record LinhaAssociacaoResponse(
    String id,
    String versao,
    String data,
    String instituicaoId,
    String contaId,
    String ativoTicker,
    String quantidade,
    String operacao,
    String referencia,
    PapelLinha papel,
    String grupoNumero,
    String arquivo,
    Integer linha,
    String aba) {}
