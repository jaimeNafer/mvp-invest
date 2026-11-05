package io.github.jaimenafer.lastro.operacoes.application.dto;

import java.util.List;

public record AvisosConferenciaResponse(
    String revisao, int pagina, int tamanho, long total, List<AvisoConferenciaResponse> itens) {}
