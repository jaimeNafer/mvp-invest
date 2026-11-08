package io.github.jaimenafer.lastro.operacoes.domain.model;

import java.math.BigDecimal;
import java.time.Instant;

public record ItemConferenciaLivro(
    String id,
    Long contaId,
    Long instituicaoId,
    Long ativoId,
    Instant ocorridaEm,
    long ordem,
    BigDecimal quantidade,
    BigDecimal custoEntradaBrl,
    FatoLivro operacao,
    boolean ajuste,
    String ancoraId,
    String transferenciaRef,
    boolean creditoAutomatico) {
  public ItemConferenciaLivro(
      String id, Long contaId, Long instituicaoId, Long ativoId, Instant ocorridaEm, long ordem,
      BigDecimal quantidade, BigDecimal custoEntradaBrl, FatoLivro operacao, boolean ajuste,
      String ancoraId, String transferenciaRef) {
    this(id, contaId, instituicaoId, ativoId, ocorridaEm, ordem, quantidade, custoEntradaBrl,
        operacao, ajuste, ancoraId, transferenciaRef, false);
  }
}
