package io.github.jaimenafer.lastro.operacoes.domain.model;

import java.math.BigDecimal;
import java.time.Instant;

public record FatoLivro(
    Long id,
    TipoOperacao tipo,
    Long instituicaoId,
    Long ativoId,
    Instant ocorridaEm,
    long ordemLivro,
    BigDecimal quantidade,
    BigDecimal custoTotalBrl,
    Long moedaCotacaoId,
    BigDecimal totalBruto,
    BigDecimal cotacaoBrl,
    Long taxaAtivoId,
    BigDecimal taxaQuantidade) {}
