package io.github.jaimenafer.lastro.operacoes.application.dto;

import java.time.LocalDate;

public record CoberturaQuantidades(
    boolean completa, boolean semData, LocalDate primeiraDataPendente) {}
