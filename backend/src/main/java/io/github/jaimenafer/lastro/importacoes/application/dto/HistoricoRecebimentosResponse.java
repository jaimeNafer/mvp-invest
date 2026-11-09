package io.github.jaimenafer.lastro.importacoes.application.dto;

import java.util.List;

public record HistoricoRecebimentosResponse(
    List<ArquivoNoHistoricoResponse> itens, int pagina, int tamanho, long total) {}
