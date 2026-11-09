package io.github.jaimenafer.lastro.operacoes.application.dto;

public record CoberturaIndicadorResponse(String valorConhecido, EstadoAgregadoVisaoGeral status,
    long componentesConhecidos, long componentesDesconhecidos, String motivo) {}
