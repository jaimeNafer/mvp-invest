package io.github.jaimenafer.lastro.operacoes.application.dto;

import java.time.LocalDate;
import java.util.List;

public record ListaSaldosIniciais(
    List<SaldoInicialResponse> itens,
    int pagina,
    int tamanho,
    long total,
    LocalDate dataReferencia,
    boolean possuiSaldosConfirmados) {}
