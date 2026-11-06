package io.github.jaimenafer.lastro.importacoes.application.dto;

public record PaginaImportacoesResponse(
    java.util.List<ResumoImportacaoResponse> itens, long total, int pagina, int tamanho) {}
