package io.github.jaimenafer.lastro.importacoes.application.dto;

import java.time.Instant;

public record ArquivoNoHistoricoResponse(String id, String nomeArquivo, Instant criadoEm, int registros) {}
