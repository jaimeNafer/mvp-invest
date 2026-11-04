package io.github.jaimenafer.lastro.ativos.application.contract;

public record AtivoResumo(
    Long id,
    String codigo,
    String ticker,
    int casasDecimais,
    boolean ativo,
    boolean permiteSaldoInicial,
    boolean cripto) {}
