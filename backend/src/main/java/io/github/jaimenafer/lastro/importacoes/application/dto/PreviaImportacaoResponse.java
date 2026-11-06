package io.github.jaimenafer.lastro.importacoes.application.dto;

public record PreviaImportacaoResponse(
    String hash,
    String revisao,
    String previaId,
    int novos,
    int reconhecidos,
    java.util.List<ErroImportacaoResponse> erros,
    java.util.List<EventoPreviaImportacao> eventos) {}
