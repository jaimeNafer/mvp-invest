package io.github.jaimenafer.lastro.operacoes.application.dto;

import java.util.List;

public record HistoricoOperacoesResponse(
    List<ItemHistoricoOperacaoResponse> itens, int total, int pagina, int tamanho) {}
