package io.github.jaimenafer.lastro.operacoes.domain.model;

import java.math.BigDecimal;
import java.time.Instant;

public record LinhaAssociacao(
    long registroId,
    long versao,
    Long instituicaoId,
    Long contaId,
    Long ativoId,
    String codigo,
    String ticker,
    Instant data,
    BigDecimal quantidade,
    String operacao,
    String referencia,
    PapelLinha papel,
    Boolean taxaZeroDeclarada,
    String origemFisica) {
  public LinhaAssociacao {
    taxaZeroDeclarada = Boolean.TRUE.equals(taxaZeroDeclarada);
  }

  public LinhaAssociacao(
      long registroId, long versao, Long instituicaoId, Long contaId, Long ativoId,
      String codigo, String ticker, Instant data, BigDecimal quantidade, String operacao,
      String referencia, PapelLinha papel, Boolean taxaZeroDeclarada) {
    this(registroId, versao, instituicaoId, contaId, ativoId, codigo, ticker, data, quantidade,
        operacao, referencia, papel, taxaZeroDeclarada, null);
  }

  public LinhaAssociacao(
      long registroId,
      long versao,
      Long instituicaoId,
      Long contaId,
      Long ativoId,
      String codigo,
      String ticker,
      Instant data,
      BigDecimal quantidade,
      String operacao,
      String referencia,
      PapelLinha papel) {
    this(
        registroId,
        versao,
        instituicaoId,
        contaId,
        ativoId,
        codigo,
        ticker,
        data,
        quantidade,
        operacao,
        referencia,
        papel,
        false,
        null);
  }
}
