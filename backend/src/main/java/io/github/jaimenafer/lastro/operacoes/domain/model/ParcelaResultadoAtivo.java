package io.github.jaimenafer.lastro.operacoes.domain.model;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

public record ParcelaResultadoAtivo(Long ativoId, BigDecimal negociacao, BigDecimal moeda,
    BigDecimal taxa) {
  public static List<ParcelaResultadoAtivo> de(FatoLivro fato, CalculoOperacao calculo,
      boolean cotacaoEmReais) {
    BigDecimal zero = BigDecimal.ZERO;
    BigDecimal resultadoTaxa = calculo == null ? null : calculo.resultadoTaxaBrl();
    boolean taxaExterna = fato.taxaExternaBrl() != null;
    boolean venda = fato.tipo() == TipoOperacao.VENDA;
    List<ParcelaResultadoAtivo> parcelas = new ArrayList<>();
    if (venda)
      parcelas.add(new ParcelaResultadoAtivo(fato.ativoId(),
          calculo == null ? null : calculo.resultadoNegociacaoBrl(), zero,
          taxaExterna ? fato.taxaExternaBrl().negate() : resultadoTaxa));
    else if (fato.tipo() == TipoOperacao.COMPRA && !cotacaoEmReais)
      parcelas.add(new ParcelaResultadoAtivo(fato.moedaCotacaoId(), zero,
          calculo == null ? null : calculo.resultadoMoedaBrl(), taxaExterna ? zero : resultadoTaxa));
    if (taxaExterna) {
      BigDecimal resultadoBaixa = venda && resultadoTaxa != null
          ? resultadoTaxa.add(fato.taxaExternaBrl()) : resultadoTaxa;
      parcelas.add(new ParcelaResultadoAtivo(fato.taxaAtivoId(), zero, zero, resultadoBaixa));
    }
    return List.copyOf(parcelas);
  }
}
