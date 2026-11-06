package io.github.jaimenafer.lastro.importacoes.application.dto;

public record ReciboImportacaoResponse(
    String id,
    String hash,
    String nomeArquivo,
    java.time.Instant criadoEm,
    int novos,
    int reconhecidos,
    java.util.List<EventoReciboResponse> eventos) {}
