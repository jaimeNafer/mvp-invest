package io.github.jaimenafer.lastro.operacoes.application.dto;

import java.time.LocalDate;

public record PatrimonioDiarioResponse(LocalDate data, String valorTotalBrl, String valorConhecidoBrl,
    int ativosComPreco, int ativosSemPreco, boolean quantidadesCompletas) {}
