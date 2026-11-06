package io.github.jaimenafer.lastro.importacoes.domain.model;

import java.math.BigDecimal;
import java.time.Instant;

public record EventoImportacao(
    int linha,
    String referencia,
    String tipo,
    Instant ocorridaEm,
    String codigoAtivo,
    BigDecimal quantidade,
    BigDecimal principal,
    BigDecimal taxa) {}
