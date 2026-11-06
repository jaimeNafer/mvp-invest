package io.github.jaimenafer.lastro.importacoes.application.dto;

import java.time.Instant;

public record ResumoImportacaoResponse(
    String id, String hash, String nomeArquivo, Instant criadoEm, int novos, int reconhecidos) {}
