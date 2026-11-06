package io.github.jaimenafer.lastro.operacoes.application.contract;

import java.math.BigDecimal;
import java.time.Instant;

public record MovimentoImportado(
    String chave,
    String chaveLegada,
    String tipo,
    long instituicaoId,
    long ativoId,
    Instant ocorridaEm,
    BigDecimal quantidade,
    BigDecimal principal,
    BigDecimal taxa,
    String referencia,
    Long operacaoOriginalId) {}
