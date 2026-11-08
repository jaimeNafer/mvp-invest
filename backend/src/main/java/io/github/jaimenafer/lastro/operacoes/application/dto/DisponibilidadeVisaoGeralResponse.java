package io.github.jaimenafer.lastro.operacoes.application.dto;

public record DisponibilidadeVisaoGeralResponse(
    boolean custoCompleto,
    boolean resultadoCompleto,
    boolean taxasCompletas,
    boolean comAjustes,
    String subtotalCustoBrl,
    String subtotalResultadoBrl,
    String subtotalTaxasBrl,
    boolean quantidadesCompletas) {}
