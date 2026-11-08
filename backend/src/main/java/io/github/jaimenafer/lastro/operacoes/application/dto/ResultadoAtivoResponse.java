package io.github.jaimenafer.lastro.operacoes.application.dto;

public record ResultadoAtivoResponse(
    String ativoId,
    String ativoTicker,
    String ativoNome,
    String negociacaoBrl,
    String moedaBrl,
    String taxaBrl,
    String resultadoTotalBrl,
    String subtotalResultadoBrl) {}
