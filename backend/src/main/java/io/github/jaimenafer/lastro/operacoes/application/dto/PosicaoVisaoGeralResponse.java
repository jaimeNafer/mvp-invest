package io.github.jaimenafer.lastro.operacoes.application.dto;

import java.util.List;

public record PosicaoVisaoGeralResponse(
    String ativoId,
    String ativoTicker,
    String ativoNome,
    String quantidade,
    String custoMedioBrl,
    String custoTotalBrl,
    String participacaoPercentual,
    List<CustodiaLivroResponse> instituicoes,
    boolean quantidadeDisponivel,
    boolean comAjustes,
    List<String> causas) {}
