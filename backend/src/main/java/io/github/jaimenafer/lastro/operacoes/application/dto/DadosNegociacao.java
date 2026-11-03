package io.github.jaimenafer.lastro.operacoes.application.dto;

import io.github.jaimenafer.lastro.operacoes.domain.model.TipoOperacao;
import java.math.BigDecimal;
import java.time.Instant;

public record DadosNegociacao(
    TipoOperacao tipo,
    Long instituicaoId,
    Long ativoId,
    Instant ocorridaEm,
    BigDecimal quantidade,
    Long moedaCotacaoId,
    BigDecimal totalBruto,
    BigDecimal cotacaoBrl,
    Long taxaAtivoId,
    BigDecimal taxaQuantidade,
    String referencia,
    String motivo) {}
