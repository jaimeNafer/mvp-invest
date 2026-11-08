package io.github.jaimenafer.lastro.operacoes.application.dto;

import java.util.List;

public record PosicoesLivroResponse(
    String revisao, List<PosicaoLivroResponse> itens, boolean quantidadesCompletas) {}
