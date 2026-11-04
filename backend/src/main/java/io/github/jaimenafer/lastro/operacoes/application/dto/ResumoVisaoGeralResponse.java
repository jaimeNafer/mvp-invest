package io.github.jaimenafer.lastro.operacoes.application.dto;

public record ResumoVisaoGeralResponse(
    String custoSaldosBrl,
    String resultadoRealizadoBrl,
    String taxasBrl,
    String aportesBrl,
    long operacoesNoPeriodo) {}
