package io.github.jaimenafer.lastro.importacoes.application.dto;

public record EventoPreviaImportacao(
    int linha,
    String referencia,
    String tipo,
    java.time.Instant ocorridaEm,
    String ativo,
    String quantidade,
    String principal,
    String taxa,
    String situacao) {}
