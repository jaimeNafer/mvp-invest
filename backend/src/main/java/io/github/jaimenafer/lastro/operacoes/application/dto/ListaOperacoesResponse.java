package io.github.jaimenafer.lastro.operacoes.application.dto;

import java.time.LocalDate;
import java.util.List;

public record ListaOperacoesResponse(
    List<OperacaoResponse> itens,
    long total,
    int pagina,
    int tamanho,
    String revisao,
    LocalDate dataReferencia) {}
