package io.github.jaimenafer.lastro.operacoes.domain.service;

import io.github.jaimenafer.lastro.operacoes.domain.model.*;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.ZoneId;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class AnaliseResultado {
  public static List<ResultadoMensal> mensal(
      List<FatoLivro> fatos, ResultadoLivro livro, PeriodoVisaoGeral periodo) {
    Map<Long, CalculoOperacao> calculos = calculosPorId(livro);
    Map<YearMonth, BigDecimal> resultados = new HashMap<>();
    Map<YearMonth, Long> quantidades = new HashMap<>();
    for (FatoLivro fato : fatos) {
      if (!periodo.contem(fato.ocorridaEm())) continue;
      YearMonth mes = YearMonth.from(fato.ocorridaEm().atZone(ZoneId.of("America/Fortaleza")));
      resultados.merge(mes, calculos.get(fato.id()).resultadoTotalBrl(), BigDecimal::add);
      quantidades.merge(mes, 1L, Long::sum);
    }
    List<ResultadoMensal> serie = new ArrayList<>();
    BigDecimal acumulado = BigDecimal.ZERO;
    YearMonth ultimo = YearMonth.from(periodo.fim());
    for (YearMonth mes = YearMonth.from(periodo.inicio());
        !mes.isAfter(ultimo);
        mes = mes.plusMonths(1)) {
      BigDecimal resultado = resultados.getOrDefault(mes, BigDecimal.ZERO);
      acumulado = acumulado.add(resultado);
      LocalDate inicio = mes.atDay(1).isBefore(periodo.inicio()) ? periodo.inicio() : mes.atDay(1);
      LocalDate fim =
          mes.atEndOfMonth().isAfter(periodo.fim()) ? periodo.fim() : mes.atEndOfMonth();
      serie.add(
          new ResultadoMensal(
              inicio, fim, resultado, acumulado, quantidades.getOrDefault(mes, 0L)));
    }
    return List.copyOf(serie);
  }

  public static List<ResultadoAtivo> porAtivo(
      List<FatoLivro> fatos,
      ResultadoLivro livro,
      PeriodoVisaoGeral periodo,
      Map<Long, String> codigos) {
    Map<Long, CalculoOperacao> calculos = calculosPorId(livro);
    Map<Long, ResultadoAtivo> resultados = new HashMap<>();
    for (FatoLivro fato : fatos) {
      if (!periodo.contem(fato.ocorridaEm())) continue;
      CalculoOperacao calculo = calculos.get(fato.id());
      if (fato.tipo() == TipoOperacao.VENDA) {
        resultados.merge(
            fato.ativoId(),
            new ResultadoAtivo(
                fato.ativoId(),
                calculo.resultadoNegociacaoBrl(),
                BigDecimal.ZERO,
                calculo.resultadoTaxaBrl()),
            ResultadoAtivo::somar);
      } else if (compraRealizaMoeda(fato, codigos)) {
        resultados.merge(
            fato.moedaCotacaoId(),
            new ResultadoAtivo(
                fato.moedaCotacaoId(),
                BigDecimal.ZERO,
                calculo.resultadoMoedaBrl(),
                calculo.resultadoTaxaBrl()),
            ResultadoAtivo::somar);
      }
    }
    return List.copyOf(resultados.values());
  }

  private static boolean compraRealizaMoeda(FatoLivro fato, Map<Long, String> codigos) {
    return fato.tipo() == TipoOperacao.COMPRA
        && !"fiat:BRL".equals(codigos.get(fato.moedaCotacaoId()));
  }

  private static Map<Long, CalculoOperacao> calculosPorId(ResultadoLivro livro) {
    return livro.calculos().stream()
        .collect(Collectors.toMap(CalculoOperacao::operacaoId, Function.identity()));
  }
}
