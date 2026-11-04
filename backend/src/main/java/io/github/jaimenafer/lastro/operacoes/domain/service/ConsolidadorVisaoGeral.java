package io.github.jaimenafer.lastro.operacoes.domain.service;

import io.github.jaimenafer.lastro.operacoes.domain.model.*;
import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class ConsolidadorVisaoGeral {
  public static ResumoVisaoGeral resumir(
      List<FatoLivro> fatos, ResultadoLivro livro, PeriodoVisaoGeral periodo) {
    Map<Long, CalculoOperacao> calculos =
        livro.calculos().stream()
            .collect(Collectors.toMap(CalculoOperacao::operacaoId, Function.identity()));
    BigDecimal taxas = BigDecimal.ZERO;
    BigDecimal aportes = BigDecimal.ZERO;
    BigDecimal negociacao = BigDecimal.ZERO;
    BigDecimal moeda = BigDecimal.ZERO;
    BigDecimal taxa = BigDecimal.ZERO;
    long quantidade = 0;
    for (FatoLivro fato : fatos) {
      if (!periodo.contem(fato.ocorridaEm())) continue;
      CalculoOperacao calculo = calculos.get(fato.id());
      taxas = taxas.add(calculo.taxaBrl());
      negociacao = negociacao.add(calculo.resultadoNegociacaoBrl());
      moeda = moeda.add(calculo.resultadoMoedaBrl());
      taxa = taxa.add(calculo.resultadoTaxaBrl());
      if (fato.tipo() == TipoOperacao.APORTE) aportes = aportes.add(fato.quantidade());
      quantidade++;
    }
    BigDecimal custo =
        livro.posicoes().stream()
            .map(PosicaoLivro::custoTotalBrl)
            .reduce(BigDecimal.ZERO, BigDecimal::add);
    return new ResumoVisaoGeral(
        custo,
        negociacao.add(moeda).add(taxa),
        taxas,
        aportes,
        quantidade,
        negociacao,
        moeda,
        taxa);
  }
}
