package io.github.jaimenafer.lastro.operacoes.application.dto;

import java.util.List;

public record PreviaOperacaoResponse(
    String revisao,
    OperacaoResponse operacao,
    CalculoLivroResponse calculo,
    List<PosicaoLivroResponse> posicoes) {}
