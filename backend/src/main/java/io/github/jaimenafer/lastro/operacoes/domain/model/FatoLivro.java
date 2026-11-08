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
    BigDecimal taxaQuantidade,
    BigDecimal valorTotalValoradoBrl,
    BigDecimal taxaExternaBrl) {
  public FatoLivro(Long id, TipoOperacao tipo, Long instituicaoId, Long ativoId, Instant ocorridaEm,
      long ordemLivro, BigDecimal quantidade, BigDecimal custoTotalBrl, Long moedaCotacaoId,
      BigDecimal totalBruto, BigDecimal cotacaoBrl, Long taxaAtivoId, BigDecimal taxaQuantidade,
      BigDecimal valorTotalValoradoBrl) {
    this(id, tipo, instituicaoId, ativoId, ocorridaEm, ordemLivro, quantidade, custoTotalBrl,
        moedaCotacaoId, totalBruto, cotacaoBrl, taxaAtivoId, taxaQuantidade, valorTotalValoradoBrl, null);
  }
  public FatoLivro(Long id, TipoOperacao tipo, Long instituicaoId, Long ativoId, Instant ocorridaEm,
      long ordemLivro, BigDecimal quantidade, BigDecimal custoTotalBrl, Long moedaCotacaoId,
      BigDecimal totalBruto, BigDecimal cotacaoBrl, Long taxaAtivoId, BigDecimal taxaQuantidade) {
    this(id, tipo, instituicaoId, ativoId, ocorridaEm, ordemLivro, quantidade, custoTotalBrl,
        moedaCotacaoId, totalBruto, cotacaoBrl, taxaAtivoId, taxaQuantidade, null, null);
  }
}
