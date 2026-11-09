package io.github.jaimenafer.lastro.operacoes.application.dto;

public record DisponibilidadeVisaoGeralResponse(
    boolean custoCompleto,
    boolean resultadoCompleto,
    boolean taxasCompletas,
    boolean comAjustes,
    String subtotalCustoBrl,
    String subtotalResultadoBrl,
    String subtotalTaxasBrl,
    boolean quantidadesCompletas,
    CoberturaIndicadorResponse custo,
    CoberturaIndicadorResponse resultado,
    CoberturaIndicadorResponse taxas,
    CoberturaIndicadorResponse aportesLiquidos) {
  public DisponibilidadeVisaoGeralResponse(boolean custoCompleto, boolean resultadoCompleto,
      boolean taxasCompletas, boolean comAjustes, String subtotalCustoBrl,
      String subtotalResultadoBrl, String subtotalTaxasBrl, boolean quantidadesCompletas) {
    this(custoCompleto, resultadoCompleto, taxasCompletas, comAjustes, subtotalCustoBrl,
        subtotalResultadoBrl, subtotalTaxasBrl, quantidadesCompletas, null, null, null, null);
  }
}
