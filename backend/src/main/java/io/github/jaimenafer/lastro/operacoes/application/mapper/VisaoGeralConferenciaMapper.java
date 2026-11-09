package io.github.jaimenafer.lastro.operacoes.application.mapper;

import io.github.jaimenafer.lastro.operacoes.application.dto.*;
import io.github.jaimenafer.lastro.operacoes.domain.model.*;
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
    List<GrupoEmConferencia> gruposPendentes = gruposPendentes(retrato, periodo);
    Set<String> linhasAgrupadas = linhasAgrupadas(retrato);
    long movimentosAmbiguos = retrato.itens().stream()
        .filter(i -> semClassificacao(i) && periodo.contem(i.ocorridaEm()))
        .filter(i -> !linhasAgrupadas.contains(idOrigem(i.id()))).count();
    boolean coberturaIncerta = retrato.incertezaSemData()
        || (retrato.primeiraDataPendente() != null
            && !retrato.primeiraDataPendente().isAfter(periodo.fim()));
    long pendenciasSemCalculo = movimentosAmbiguos + gruposPendentes.size();
    long incertos = coberturaIncerta ? 1 : 0;
    boolean semClassificacao = pendenciasSemCalculo + incertos > 0;
    List<CalculoOperacao> calculosPeriodo =
        noPeriodo.stream().map(f -> calculos.get(f.id())).toList();
    var custos = retrato.livro().posicoes();
    long custosConhecidos = custos.stream()
        .filter(p -> p.quantidadeDisponivel() && p.custoTotalBrl() != null).count();
    BigDecimal subtotalCusto = custos.stream()
        .filter(p -> p.quantidadeDisponivel() && p.custoTotalBrl() != null)
        .map(PosicaoConferenciaLivro::custoTotalBrl).reduce(BigDecimal.ZERO, BigDecimal::add);
    long custosDesconhecidos = custos.size() - custosConhecidos;
    boolean custoCompleto = retrato.quantidadesCompletas()
        && custosDesconhecidos == 0
        && retrato.itens().stream().noneMatch(i -> semClassificacao(i)
            && i.quantidade().signum() < 0 && retrato.ativos().get(i.ativoId()) != null
            && retrato.ativos().get(i.ativoId()).cripto());

    Map<Long, String> codigos =
        retrato.ativos().values().stream().collect(Collectors.toMap(a -> a.id(), a -> a.codigo()));
    Map<Long, List<ParcelaResultadoAtivo>> parcelasPorFato = noPeriodo.stream()
        .collect(Collectors.toMap(FatoLivro::id, f -> ParcelaResultadoAtivo.de(f, calculos.get(f.id()),
            f.moedaCotacaoId() != null && "fiat:BRL".equals(codigos.get(f.moedaCotacaoId())))));
    List<FatoLivro> negociacoes = noPeriodo.stream().filter(VisaoGeralConferenciaMapper::negociacao).toList();
    long resultadosConhecidos = negociacoes.stream().filter(f -> resultadoCalculavel(calculos.get(f.id()))).count();
    long resultadosDesconhecidos = negociacoes.size() - resultadosConhecidos + pendenciasSemCalculo + incertos;
    BigDecimal subtotalResultado = parcelasPorFato.values().stream().flatMap(List::stream)
        .map(VisaoGeralConferenciaMapper::subtotalParcela).reduce(BigDecimal.ZERO, BigDecimal::add);
    boolean resultadoParcialConhecido = parcelasPorFato.values().stream().flatMap(List::stream)
        .anyMatch(p -> valores(p).anyMatch(v -> v != null && v.signum() != 0));
    BigDecimal subtotalTaxasGrupos = gruposPendentes.stream()
        .flatMap(g -> g.resumo().taxas().stream())
        .filter(t -> "fiat:BRL".equals(t.codigo()))
        .map(ParcelaGrupo::quantidade).reduce(BigDecimal.ZERO, BigDecimal::add);
    long taxasConhecidas = negociacoes.stream().map(f -> calculos.get(f.id()))
        .filter(c -> c != null && c.taxaBrl() != null).count()
        + gruposPendentes.stream().filter(VisaoGeralConferenciaMapper::taxasDoGrupoCompletas).count();
    long taxasDesconhecidas = negociacoes.size() + gruposPendentes.size() - taxasConhecidas
        + movimentosAmbiguos + incertos;
    BigDecimal subtotalTaxas = negociacoes.stream().map(f -> calculos.get(f.id()))
        .filter(Objects::nonNull).map(CalculoOperacao::taxaBrl)
        .filter(Objects::nonNull).reduce(BigDecimal.ZERO, BigDecimal::add)
        .add(subtotalTaxasGrupos);
    var capital = capitalLiquido(retrato, periodo, linhasAgrupadas);
    boolean resultadoCompleto = resultadosDesconhecidos == 0;
    boolean taxasCompletas = taxasDesconhecidas == 0;
    BigDecimal aportes = noPeriodo.stream().filter(f -> f.tipo() == TipoOperacao.APORTE)
        .map(FatoLivro::quantidade).reduce(BigDecimal.ZERO, BigDecimal::add);
    Map<YearMonth, Long> operacoesPorMes = new HashMap<>();
    noPeriodo.forEach(f -> operacoesPorMes.merge(mes(f.ocorridaEm()), 1L, Long::sum));
    gruposPendentes.forEach(g -> dataGrupo(g)
        .ifPresent(data -> operacoesPorMes.merge(mes(data), 1L, Long::sum)));
    retrato.itens().stream().filter(i -> i.creditoAutomatico() && periodo.contem(i.ocorridaEm()))
        .forEach(i -> operacoesPorMes.merge(mes(i.ocorridaEm()), 1L, Long::sum));
    var resumo = new ResumoVisaoGeral(
        custoCompleto ? subtotalCusto : null,
        resultadoCompleto ? subtotalResultado : null,
        taxasCompletas ? subtotalTaxas : null,
        capital.status() == EstadoAgregadoVisaoGeral.COMPLETO ? aportes : null,
        operacoesPorMes.values().stream().mapToLong(Long::longValue).sum(),
        total(calculosPeriodo, CalculoOperacao::resultadoNegociacaoBrl, semClassificacao),
        total(calculosPeriodo, CalculoOperacao::resultadoMoedaBrl, semClassificacao),
        total(calculosPeriodo, CalculoOperacao::resultadoTaxaBrl, semClassificacao));
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
            List.of(),
            List.of(),
            custoZero,
            referenciasRepetidas);
    List<PosicaoVisaoGeralResponse> posicoes =
        posicoes(retrato, subtotalCusto);
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
        new EvolucaoResultadoResponse("0", meses(retrato, noPeriodo, calculos, parcelasPorFato,
            periodo, operacoesPorMes, gruposPendentes, linhasAgrupadas)),
        resultados(parcelasPorFato, retrato, periodo),
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
            retrato.quantidadesCompletas(),
            indicador(subtotalCusto, custosConhecidos, custosDesconhecidos, custoCompleto, false,
                custosDesconhecidos > 0 ? "Há posições sem custo identificado." : "Há posições pendentes de conferência."),
            indicador(subtotalResultado, resultadosConhecidos, resultadosDesconhecidos, resultadoCompleto,
                resultadoParcialConhecido, "Há operações ou parcelas sem resultado calculável."),
            indicador(subtotalTaxas, taxasConhecidas, taxasDesconhecidas, taxasCompletas,
                subtotalTaxasGrupos.signum() != 0,
                "Há operações com taxas sem valor ou informação suficiente."),
            capital));
  }

  private static boolean semClassificacao(ItemConferenciaLivro item) {
    return !item.ajuste() && !item.creditoAutomatico() && item.operacao() == null
        && item.transferenciaRef() == null;
  }

  private static boolean negociacao(FatoLivro fato) {
    return fato.tipo() == TipoOperacao.COMPRA || fato.tipo() == TipoOperacao.VENDA;
  }

  private static boolean resultadoCalculavel(CalculoOperacao calculo) {
    return calculo != null && calculo.resultadoTotalBrl() != null;
  }

  private static java.util.stream.Stream<BigDecimal> valores(ParcelaResultadoAtivo p) {
    return java.util.stream.Stream.of(p.negociacao(), p.moeda(), p.taxa());
  }

  private static BigDecimal subtotalParcela(ParcelaResultadoAtivo parcela) {
    return valores(parcela).filter(Objects::nonNull).reduce(BigDecimal.ZERO, BigDecimal::add);
  }

  private static boolean temResultadoConhecido(ParcelaResultadoAtivo parcela) {
    return valores(parcela).allMatch(Objects::nonNull)
        || valores(parcela).anyMatch(v -> v != null && v.signum() != 0);
  }

  private static CoberturaIndicadorResponse indicador(BigDecimal subtotal, long conhecidos,
      long desconhecidos, boolean completo, boolean parcelaConhecida, String motivo) {
    boolean temValor = completo || conhecidos > 0 || parcelaConhecida;
    return new CoberturaIndicadorResponse(temValor ? numero(subtotal) : null,
        completo ? EstadoAgregadoVisaoGeral.COMPLETO
            : temValor ? EstadoAgregadoVisaoGeral.PARCIAL : EstadoAgregadoVisaoGeral.INDISPONIVEL,
        conhecidos, desconhecidos, completo ? null : motivo);
  }

  private static List<GrupoEmConferencia> gruposPendentes(RetratoConferencia retrato,
      PeriodoVisaoGeral periodo) {
    Set<String> gruposCalculados = retrato.itens().stream().filter(i -> i.operacao() != null)
        .map(ItemConferenciaLivro::id).collect(Collectors.toSet());
    return retrato.gruposAplicados().stream()
        .filter(g -> !g.proposta().desfazer() && g.resumo().natureza() != null)
        .filter(g -> !gruposCalculados.contains(g.identificador()))
        .filter(g -> dataGrupo(g).filter(periodo::contem).isPresent())
        .toList();
  }

  private static Optional<java.time.Instant> dataGrupo(GrupoEmConferencia grupo) {
    return grupo.proposta().linhas().stream().map(LinhaAssociacao::data).min(Comparator.naturalOrder());
  }

  private static boolean taxasDoGrupoCompletas(GrupoEmConferencia grupo) {
    var taxas = grupo.resumo().taxas();
    return taxas.isEmpty() ? grupo.proposta().taxaZeroConfirmada()
        : taxas.stream().allMatch(t -> "fiat:BRL".equals(t.codigo()));
  }

  public static Set<String> linhasAgrupadas(RetratoConferencia retrato) {
    return retrato.gruposAplicados().stream()
        .filter(g -> !g.proposta().desfazer() && g.resumo().natureza() != null)
        .flatMap(g -> g.proposta().linhas().stream()).map(l -> "registro:" + l.registroId())
        .collect(Collectors.toSet());
  }

  public static boolean capitalIndeterminado(ItemConferenciaLivro item, Set<String> linhasAgrupadas) {
    return semClassificacao(item) && !linhasAgrupadas.contains(idOrigem(item.id()));
  }

  private static String idOrigem(String id) {
    int separador = id.indexOf(':', "registro:".length());
    return id.startsWith("registro:") && separador >= 0 ? id.substring(0, separador) : id;
  }

  private static CoberturaIndicadorResponse capitalLiquido(RetratoConferencia retrato,
      PeriodoVisaoGeral periodo, Set<String> linhasAgrupadas) {
    List<BigDecimal> conhecidos = retrato.itens().stream()
        .filter(i -> i.ocorridaEm() != null && periodo.contem(i.ocorridaEm()))
        .map(ItemConferenciaLivro::operacao).filter(Objects::nonNull)
        .map(FatoLivro::fluxoExternoBrl).filter(Objects::nonNull).toList();
    long desconhecidos = retrato.itens().stream()
        .filter(i -> i.ocorridaEm() != null && periodo.contem(i.ocorridaEm()))
        .filter(i -> capitalIndeterminado(i, linhasAgrupadas)).count()
        + (retrato.incertezaSemData() || (retrato.primeiraDataPendente() != null
            && !retrato.primeiraDataPendente().isAfter(periodo.fim())) ? 1 : 0);
    return indicador(conhecidos.stream().reduce(BigDecimal.ZERO, BigDecimal::add),
        conhecidos.size(), desconhecidos, desconhecidos == 0, false,
        "Há movimentações cuja origem ou destino externo ainda não foi identificado.");
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
        .sorted(Comparator
            .comparing((PosicaoVisaoGeralResponse p) -> p.custoTotalBrl() == null
                ? null : new BigDecimal(p.custoTotalBrl()), Comparator.nullsLast(Comparator.reverseOrder()))
            .thenComparing(PosicaoVisaoGeralResponse::ativoTicker))
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
      RetratoConferencia retrato,
      List<FatoLivro> fatos,
      Map<Long, CalculoOperacao> calculos,
      Map<Long, List<ParcelaResultadoAtivo>> parcelasPorFato,
      PeriodoVisaoGeral periodo, Map<YearMonth, Long> quantidades,
      List<GrupoEmConferencia> gruposPendentes, Set<String> linhasAgrupadas) {
    Set<YearMonth> incompletos = new HashSet<>();
    fatos.stream()
        .filter(
            f -> calculos.get(f.id()) == null || calculos.get(f.id()).resultadoTotalBrl() == null)
        .map(f -> mes(f.ocorridaEm()))
        .forEach(incompletos::add);
    retrato.itens().stream()
        .filter(i -> capitalIndeterminado(i, linhasAgrupadas))
        .filter(i -> periodo.contem(i.ocorridaEm()))
        .map(i -> mes(i.ocorridaEm()))
        .forEach(incompletos::add);
    gruposPendentes.forEach(g -> dataGrupo(g).ifPresent(data -> incompletos.add(mes(data))));
    boolean acumuladoCompleto = !retrato.incertezaSemData();
    List<ResultadoMensalResponse> resultado = new ArrayList<>();
    Map<YearMonth, BigDecimal> conhecidos = new HashMap<>();
    Set<YearMonth> mesesComParcela = new HashSet<>();
    for (FatoLivro fato : fatos)
      if (negociacao(fato)) {
        var parcelas = parcelasPorFato.get(fato.id());
        YearMonth mes = mes(fato.ocorridaEm());
        if (resultadoCalculavel(calculos.get(fato.id()))
            || parcelas.stream().anyMatch(VisaoGeralConferenciaMapper::temResultadoConhecido))
          mesesComParcela.add(mes);
        conhecidos.merge(mes, parcelas.stream().map(VisaoGeralConferenciaMapper::subtotalParcela)
            .reduce(BigDecimal.ZERO, BigDecimal::add), BigDecimal::add);
      }
    BigDecimal acumulado = BigDecimal.ZERO;
    boolean temAcumuladoConhecido = false;
    for (YearMonth mes = YearMonth.from(periodo.inicio()); !mes.isAfter(YearMonth.from(periodo.fim()));
        mes = mes.plusMonths(1)) {
      LocalDate inicio = mes.atDay(1).isBefore(periodo.inicio()) ? periodo.inicio() : mes.atDay(1);
      LocalDate fim = mes.atEndOfMonth().isAfter(periodo.fim()) ? periodo.fim() : mes.atEndOfMonth();
      boolean mesCompleto = !retrato.incertezaSemData()
          && (retrato.primeiraDataPendente() == null
              || mes.isBefore(YearMonth.from(retrato.primeiraDataPendente())))
          && !incompletos.contains(mes);
      acumuladoCompleto &= mesCompleto;
      BigDecimal conhecido = conhecidos.getOrDefault(mes, BigDecimal.ZERO);
      acumulado = acumulado.add(conhecido);
      boolean temMesConhecido = mesCompleto || mesesComParcela.contains(mes);
      temAcumuladoConhecido |= temMesConhecido;
      resultado.add(new ResultadoMensalResponse(inicio, fim,
          mesCompleto ? numero(conhecido) : null, acumuladoCompleto ? numero(acumulado) : null,
          quantidades.getOrDefault(mes, 0L), temMesConhecido ? numero(conhecido) : null,
          temAcumuladoConhecido ? numero(acumulado) : null));
    }
    return List.copyOf(resultado);
  }

  private static YearMonth mes(java.time.Instant instante) {
    return YearMonth.from(instante.atZone(ZoneId.of("America/Fortaleza")));
  }

  private static List<ResultadoAtivoResponse> resultados(
      Map<Long, List<ParcelaResultadoAtivo>> parcelasPorFato, RetratoConferencia retrato,
      PeriodoVisaoGeral periodo) {
    Map<Long, List<ParcelaResultadoAtivo>> porAtivo = parcelasPorFato.values().stream()
        .flatMap(List::stream).collect(Collectors.groupingBy(ParcelaResultadoAtivo::ativoId));
    List<ResultadoAtivoResponse> resultados = new ArrayList<>();
    for (var entrada : porAtivo.entrySet()) {
      Long id = entrada.getKey();
      List<ParcelaResultadoAtivo> parcelas = entrada.getValue();
      var ativo = retrato.ativos().get(id);
      boolean incompleto =
          retrato.incertezaSemData() || retrato.itens().stream()
              .anyMatch(i -> semClassificacao(i) && i.ativoId().equals(id) && periodo.contem(i.ocorridaEm()));
      BigDecimal negociacao = totalParcelas(parcelas, ParcelaResultadoAtivo::negociacao, incompleto);
      BigDecimal moeda = totalParcelas(parcelas, ParcelaResultadoAtivo::moeda, incompleto);
      BigDecimal taxa = totalParcelas(parcelas, ParcelaResultadoAtivo::taxa, incompleto);
      BigDecimal total = negociacao == null || moeda == null || taxa == null
          ? null : negociacao.add(moeda).add(taxa);
      BigDecimal conhecido = parcelas.stream().map(VisaoGeralConferenciaMapper::subtotalParcela)
          .reduce(BigDecimal.ZERO, BigDecimal::add);
      boolean temConhecido = total != null
          || parcelas.stream().anyMatch(VisaoGeralConferenciaMapper::temResultadoConhecido);
      resultados.add(
          new ResultadoAtivoResponse(
              id.toString(),
              ativo.ticker(),
              ativo.nome(),
              numero(negociacao), numero(moeda), numero(taxa), numero(total),
              temConhecido ? numero(conhecido) : null));
    }
    return resultados.stream()
        .sorted(
            Comparator.comparing(
                    (ResultadoAtivoResponse r) -> r.subtotalResultadoBrl() == null
                        ? null : new BigDecimal(r.subtotalResultadoBrl()).abs(),
                    Comparator.nullsLast(Comparator.reverseOrder()))
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
