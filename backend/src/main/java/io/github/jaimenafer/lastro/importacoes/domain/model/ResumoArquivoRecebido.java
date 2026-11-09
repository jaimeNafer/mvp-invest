package io.github.jaimenafer.lastro.importacoes.domain.model;

import java.time.Instant;

public record ResumoArquivoRecebido(Long id, String nomeArquivo, Instant criadoEm, int registros) {}
