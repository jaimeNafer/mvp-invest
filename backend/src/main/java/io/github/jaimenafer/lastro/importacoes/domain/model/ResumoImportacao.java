package io.github.jaimenafer.lastro.importacoes.domain.model;

import java.time.Instant;

public record ResumoImportacao(
    Long id, String hash, String nomeArquivo, Instant criadoEm, int novos, int reconhecidos) {}
