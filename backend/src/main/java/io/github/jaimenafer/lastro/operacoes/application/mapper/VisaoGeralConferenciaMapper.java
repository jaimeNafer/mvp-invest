package io.github.jaimenafer.lastro.operacoes.application.mapper;

import io.github.jaimenafer.lastro.operacoes.application.dto.*;
import io.github.jaimenafer.lastro.operacoes.domain.model.*;
import io.github.jaimenafer.lastro.operacoes.domain.service.AnaliseResultado;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.ZoneId;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class VisaoGeralConferenciaMapper {
  public static VisaoGeralResponse de(
      long revisao,
      LocalDate referencia,
      PeriodoVisaoGeral periodo,
      RetratoConferencia retrato,
      long custoZero,
      long referenciasRepetidas) {
    ResultadoLivro livro = LivroConferenciaMapper.livro(retrato);
    List<FatoLivro> fatos =
        retrato.itens().stream()
            .filter(i -> i.operacao() != null)
            .map(ItemConferenciaLivro::operacao)
            .toList();
    Map<Long, CalculoOperacao> calculos =
        livro.calculos().stream()
            .collect(Collectors.toMap(CalculoOperacao::operacaoId, Function.identity()));
    List<FatoLivro> noPeriodo = fatos.stream().filter(f -> periodo.contem(f.ocorridaEm())).toList();
    boolean semClassificacao =
        !retrato.quantidadesCompletas()
            || retrato.itens().stream()
                .anyMatch(i -> semClassificacao(i) && periodo.contem(i.ocorridaEm()));
    List<CalculoOperacao> calculosPeriodo =
        noPeriodo.stream().map(f -> calculos.get(f.id())).toList();
    BigDecimal subtotalCusto =
        retrato.livro().posicoes().stream()
            .filter(p -> p.quantidadeDisponivel() && p.custoTotalBrl() != null)
            .map(PosicaoConferenciaLivro::custoTotalBrl)
            .reduce(BigDecimal.ZERO, BigDecimal::add);
    boolean custoCompleto =
        retrato.quantidadesCompletas()
            && retrato.livro().posicoes().stream()
                .allMatch(p -> p.quantidadeDisponivel() && p.custoTotalBrl() != null)
            && retrato.itens().stream()
                .noneMatch(i -> semClassificacao(i) && i.quantidade().signum() < 0
                    && retrato.ativos().get(i.ativoId()) != null
                    && retrato.ativos().get(i.ativoId()).cripto());
    BigDecimal subtotalResultado = subtotal(calculosPeriodo, CalculoOperacao::resultadoTotalBrl);
    BigDecimal subtotalTaxas = subtotal(calculosPeriodo, CalculoOperacao::taxaBrl);
    boolean resultadoCompleto =
        !semClassificacao
            && calculosPeriodo.stream().allMatch(c -> c != null && c.resultadoTotalBrl() != null);
    boolean taxasCompletas =
        !semClassificacao
            && calculosPeriodo.stream().allMatch(c -> c != null && c.taxaBrl() != null);
    BigDecimal aportes =
        noPeriodo.stream()
            .filter(f -> f.tipo() == TipoOperacao.APORTE)
            .map(FatoLivro::quantidade)
            .reduce(BigDecimal.ZERO, BigDecimal::add);
    var resumo =
        new ResumoVisaoGeral(
            custoCompleto ? subtotalCusto : null,
            resultadoCompleto ? subtotalResultado : null,
            taxasCompletas ? subtotalTaxas : null,
            semClassificacao ? null : aportes,
            noPeriodo.size(),
            total(calculosPeriodo, CalculoOperacao::resultadoNegociacaoBrl, semClassificacao),
            total(calculosPeriodo, CalculoOperacao::resultadoMoedaBrl, semClassificacao),
            total(calculosPeriodo, CalculoOperacao::resultadoTaxaBrl, semClassificacao));
    List<FatoLivro> calculaveis =
        fatos.stream()
            .filter(f ->
                calculos.get(f.id()) != null && calculos.get(f.id()).resultadoTotalBrl() != null)
            .toList();
    Map<Long, String> codigos =
        retrato.ativos().values().stream().collect(Collectors.toMap(a -> a.id(), a -> a.codigo()));
    List<ResultadoMensal> meses = AnaliseResultado.mensal(calculaveis, livro, periodo);
    List<ResultadoAtivo> resultados =
        AnaliseResultado.porAtivo(calculaveis, livro, periodo, codigos);
    VisaoGeralResponse base =
        VisaoGeralMapper.de(
            revisao,
            referencia,
            periodo,
            !retrato.itens().isEmpty() || !retrato.quantidadesCompletas(),
            resumo,
            livro,
            retrato.ativos(),
            retrato.instituicoes(),
            meses,
            resultados,
            custoZero,
            referenciasRepetidas);
    List<PosicaoVisaoGeralResponse> posicoes =
        posicoes(retrato, custoCompleto ? subtotalCusto : null);
    return new VisaoGeralResponse(
        base.revisao(),
        base.dataInicio(),
        base.dataFim(),
        base.dataReferencia(),
        base.possuiRegistrosAteFim(),
        base.resumo(),
        base.resultado(),
        posicoes,
        base.dataSaldos(),
        base.saldosMoedas().stream()
            .map(m -> {
              var p =
                  posicoes.stream()
                      .filter(v -> v.ativoId().equals(m.ativoId()))
                      .findFirst()
                      .orElseThrow();
              return new SaldoMoedaResponse(
                  m.ativoId(),
                  m.ativoTicker(),
                  m.ativoNome(),
                  m.quantidade(),
                  m.instituicoes(),
                  p.quantidadeDisponivel(),
                  p.comAjustes());
            })
            .toList(),
        new EvolucaoResultadoResponse("0", meses(meses, retrato, noPeriodo, calculos, periodo)),
        resultados(noPeriodo, calculos, retrato, codigos),
        base.conferencia(),
        retrato.resumo(),
        new DisponibilidadeVisaoGeralResponse(
            custoCompleto,
            resultadoCompleto,
            taxasCompletas,
            retrato.itens().stream().anyMatch(ItemConferenciaLivro::ajuste),
            numero(subtotalCusto),
            numero(subtotalResultado),
            numero(subtotalTaxas),
            retrato.quantidadesCompletas()));
  }

  private static boolean semClassificacao(ItemConferenciaLivro item) {
    return !item.ajuste() && !item.creditoAutomatico() && item.operacao() == null
        && item.transferenciaRef() == null;
  }

  private static BigDecimal subtotal(
      List<CalculoOperacao> calculos, Function<CalculoOperacao, BigDecimal> parcela) {
    return calculos.stream()
        .filter(Objects::nonNull)
        .map(parcela)
        .filter(Objects::nonNull)
        .reduce(BigDecimal.ZERO, BigDecimal::add);
  }

  private static BigDecimal total(
      List<CalculoOperacao> calculos,
      Function<CalculoOperacao, BigDecimal> parcela,
      boolean incompleto) {
    return incompleto || calculos.stream().anyMatch(c -> c == null || parcela.apply(c) == null)
        ? null
        : subtotal(calculos, parcela);
  }

  private static List<PosicaoVisaoGeralResponse> posicoes(
      RetratoConferencia retrato, BigDecimal subtotal) {
    return LivroConferenciaMapper.posicoes(retrato).stream()
        .map(p ->
            new PosicaoVisaoGeralResponse(
                p.ativoId(),
                p.ativoTicker(),
                retrato.ativos().get(Long.valueOf(p.ativoId())).nome(),
                p.quantidade(),
                p.custoMedioBrl(),
                p.custoTotalBrl(),
                percentual(p, subtotal),
                p.instituicoes(),
                p.quantidadeDisponivel(),
                p.comAjustes(),
                p.causas()))
        .toList();
  }

  private static String percentual(PosicaoLivroResponse posicao, BigDecimal subtotal) {
    if (!posicao.quantidadeDisponivel()
        || posicao.custoTotalBrl() == null
        || (subtotal == null || subtotal.signum() <= 0)) return null;
    return numero(
        new BigDecimal(posicao.custoTotalBrl())
            .multiply(BigDecimal.valueOf(100))
            .divide(subtotal, 4, RoundingMode.HALF_EVEN));
  }

  private static List<ResultadoMensalResponse> meses(
      List<ResultadoMensal> calculaveis,
      RetratoConferencia retrato,
      List<FatoLivro> fatos,
      Map<Long, CalculoOperacao> calculos,
      PeriodoVisaoGeral periodo) {
    Set<YearMonth> incompletos = new HashSet<>();
    fatos.stream()
        .filter(
            f -> calculos.get(f.id()) == null || calculos.get(f.id()).resultadoTotalBrl() == null)
        .map(f -> mes(f.ocorridaEm()))
        .forEach(incompletos::add);
    retrato.itens().stream()
        .filter(VisaoGeralConferenciaMapper::semClassificacao)
        .filter(i -> periodo.contem(i.ocorridaEm()))
        .map(i -> mes(i.ocorridaEm()))
        .forEach(incompletos::add);
    Map<YearMonth, Long> quantidades =
        fatos.stream()
            .collect(Collectors.groupingBy(f -> mes(f.ocorridaEm()), Collectors.counting()));
    boolean acumuladoCompleto = !retrato.incertezaSemData();
    List<ResultadoMensalResponse> resultado = new ArrayList<>();
    for (ResultadoMensal m : calculaveis) {
      boolean mesCompleto =
          !retrato.incertezaSemData()
              && (retrato.primeiraDataPendente() == null
                  || YearMonth.from(m.dataInicio())
                      .isBefore(YearMonth.from(retrato.primeiraDataPendente())))
              && !incompletos.contains(YearMonth.from(m.dataInicio()));
      acumuladoCompleto &= mesCompleto;
      resultado.add(
          new ResultadoMensalResponse(
              m.dataInicio(),
              m.dataFim(),
              mesCompleto ? numero(m.resultadoBrl()) : null,
              acumuladoCompleto ? numero(m.acumuladoBrl()) : null,
              quantidades.getOrDefault(YearMonth.from(m.dataInicio()), 0L),
              numero(m.resultadoBrl()),
              numero(m.acumuladoBrl())));
    }
    return List.copyOf(resultado);
  }

  private static YearMonth mes(java.time.Instant instante) {
    return YearMonth.from(instante.atZone(ZoneId.of("America/Fortaleza")));
  }

  private static List<ResultadoAtivoResponse> resultados(
      List<FatoLivro> fatos,
      Map<Long, CalculoOperacao> calculos,
      RetratoConferencia retrato,
      Map<Long, String> codigos) {
    Map<Long, List<ParcelaResultadoAtivo>> porAtivo = fatos.stream()
        .flatMap(fato -> ParcelaResultadoAtivo.de(fato, calculos.get(fato.id()),
            fato.moedaCotacaoId() != null && "fiat:BRL".equals(codigos.get(fato.moedaCotacaoId()))).stream())
        .collect(Collectors.groupingBy(ParcelaResultadoAtivo::ativoId));
    List<ResultadoAtivoResponse> resultados = new ArrayList<>();
    for (var entrada : porAtivo.entrySet()) {
      Long id = entrada.getKey();
      List<ParcelaResultadoAtivo> parcelas = entrada.getValue();
      var ativo = retrato.ativos().get(id);
      boolean incompleto =
          !retrato.quantidadesCompletas()
              || retrato.itens().stream()
                  .anyMatch(i -> semClassificacao(i) && i.ativoId().equals(id));
      BigDecimal negociacao = totalParcelas(parcelas, ParcelaResultadoAtivo::negociacao, incompleto);
      BigDecimal moeda = totalParcelas(parcelas, ParcelaResultadoAtivo::moeda, incompleto);
      BigDecimal taxa = totalParcelas(parcelas, ParcelaResultadoAtivo::taxa, incompleto);
      BigDecimal total = negociacao == null || moeda == null || taxa == null
          ? null : negociacao.add(moeda).add(taxa);
      BigDecimal conhecido = parcelas.stream()
          .flatMap(p -> java.util.stream.Stream.of(p.negociacao(), p.moeda(), p.taxa()))
          .filter(Objects::nonNull).reduce(BigDecimal.ZERO, BigDecimal::add);
      resultados.add(
          new ResultadoAtivoResponse(
              id.toString(),
              ativo.ticker(),
              ativo.nome(),
              numero(negociacao), numero(moeda), numero(taxa), numero(total), numero(conhecido)));
    }
    return resultados.stream()
        .sorted(
            Comparator.comparing(
                    ResultadoAtivoResponse::resultadoTotalBrl,
                    Comparator.nullsLast(
                        Comparator.comparing((String valor) -> new BigDecimal(valor)).reversed()))
                .thenComparing(ResultadoAtivoResponse::ativoTicker)
                .thenComparing(ResultadoAtivoResponse::ativoId))
        .toList();
  }

  private static BigDecimal totalParcelas(List<ParcelaResultadoAtivo> parcelas,
      Function<ParcelaResultadoAtivo, BigDecimal> campo, boolean incompleto) {
    if (incompleto || parcelas.stream().map(campo).anyMatch(Objects::isNull)) return null;
    return parcelas.stream().map(campo).reduce(BigDecimal.ZERO, BigDecimal::add);
  }

  private static String numero(BigDecimal valor) {
    return OperacoesMapper.numero(valor);
  }
}
