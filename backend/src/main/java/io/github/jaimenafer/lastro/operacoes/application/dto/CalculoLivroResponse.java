package io.github.jaimenafer.lastro.operacoes.application.dto;

public record CalculoLivroResponse(
    String valorBrutoBrl,
    String taxaBrl,
    String custoBaixadoBrl,
    String resultadoNegociacaoBrl,
    String resultadoMoedaBrl,
    String resultadoTaxaBrl,
    String resultadoTotalBrl) {}
