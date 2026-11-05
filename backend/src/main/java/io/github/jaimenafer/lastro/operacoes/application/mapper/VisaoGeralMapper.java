package io.github.jaimenafer.lastro.operacoes.application.mapper;

import io.github.jaimenafer.lastro.ativos.application.contract.AtivoResumo;
import io.github.jaimenafer.lastro.instituicoes.application.contract.InstituicaoResumo;
import io.github.jaimenafer.lastro.operacoes.application.dto.*;
import io.github.jaimenafer.lastro.operacoes.domain.exception.OperacaoInvalidaException;
import io.github.jaimenafer.lastro.operacoes.domain.model.*;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class VisaoGeralMapper {
  public static LocalDate data(String texto) {
    if (texto == null) return null;
    try {
      return LocalDate.parse(texto);
    } catch (DateTimeParseException erro) {
      throw new OperacaoInvalidaException(
          "Informe uma data existente no calendário: " + texto + ".");
    }
  }

  public static VisaoGeralResponse de(
      long revisao,
      LocalDate referencia,
      PeriodoVisaoGeral periodo,
      boolean possuiRegistros,
      ResumoVisaoGeral resumo,
      ResultadoLivro livro,
      Map<Long, AtivoResumo> ativos,
      Map<Long, InstituicaoResumo> instituicoes,
      List<ResultadoMensal> meses,
      List<ResultadoAtivo> resultados,
      long custoZero,
      long referenciasRepetidas) {
    List<PosicaoVisaoGeralResponse> posicoes =
        OperacoesMapper.posicoes(livro, ativos, instituicoes).stream()
            .map(p ->
                new PosicaoVisaoGeralResponse(
                    p.ativoId(),
                    p.ativoTicker(),
                    ativos.get(Long.valueOf(p.ativoId())).nome(),
                    p.quantidade(),
                    p.custoMedioBrl(),
                    p.custoTotalBrl(),
                    percentual(p.custoTotalBrl(), resumo.custoSaldosBrl()),
                    p.instituicoes()))
            .toList();
    return new VisaoGeralResponse(
        Long.toString(revisao),
        periodo.inicio(),
        periodo.fim(),
        referencia,
        possuiRegistros,
        new ResumoVisaoGeralResponse(
            numero(resumo.custoSaldosBrl()),
            numero(resumo.resultadoRealizadoBrl()),
            numero(resumo.taxasBrl()),
            numero(resumo.aportesBrl()),
            resumo.operacoesNoPeriodo()),
        new ParcelasResultadoResponse(
            numero(resumo.negociacaoBrl()), numero(resumo.moedaBrl()), numero(resumo.taxaBrl())),
        posicoes,
        periodo.fim(),
        posicoes.stream()
            .filter(p -> moedaDePagamento(ativos.get(Long.valueOf(p.ativoId()))))
            .map(p ->
                new SaldoMoedaResponse(
                    p.ativoId(), p.ativoTicker(), p.ativoNome(), p.quantidade(), p.instituicoes()))
            .toList(),
        new EvolucaoResultadoResponse(
            "0",
            meses.stream()
                .map(m ->
                    new ResultadoMensalResponse(
                        m.dataInicio(),
                        m.dataFim(),
                        numero(m.resultadoBrl()),
                        numero(m.acumuladoBrl()),
                        m.operacoes()))
                .toList()),
        resultados.stream()
            .sorted(
                Comparator.comparing(ResultadoAtivo::totalBrl)
                    .reversed()
                    .thenComparing(r -> ativos.get(r.ativoId()).ticker())
                    .thenComparing(ResultadoAtivo::ativoId))
            .map(r ->
                new ResultadoAtivoResponse(
                    r.ativoId().toString(),
                    ativos.get(r.ativoId()).ticker(),
                    ativos.get(r.ativoId()).nome(),
                    numero(r.negociacaoBrl()),
                    numero(r.moedaBrl()),
                    numero(r.taxaBrl()),
                    numero(r.totalBrl())))
            .toList(),
        new ConferenciaResponse(custoZero, referenciasRepetidas));
  }

  public static AvisosConferenciaResponse avisos(
      long revisao,
      List<Operacao> selecionadas,
      FiltroAvisosConferencia filtro,
      Map<Long, AtivoResumo> ativos,
      Map<Long, InstituicaoResumo> instituicoes) {
    List<AvisoConferenciaResponse> itens =
        selecionadas.stream()
            .sorted(
                Comparator.comparing(Operacao::getOcorridaEm)
                    .reversed()
                    .thenComparing(Operacao::getId))
            .skip((long) filtro.paginaEfetiva() * filtro.tamanhoEfetivo())
            .limit(filtro.tamanhoEfetivo())
            .map(o ->
                new AvisoConferenciaResponse(
                    o.getId().toString(),
                    o.getOcorridaEm().toString(),
                    instituicoes.get(o.getInstituicaoId()).nome(),
                    ativos.get(o.getAtivoId()).ticker(),
                    o.getReferencia(),
                    o.getJustificativaCustoZero()))
            .toList();
    return new AvisosConferenciaResponse(
        Long.toString(revisao),
        filtro.paginaEfetiva(),
        filtro.tamanhoEfetivo(),
        selecionadas.size(),
        itens);
  }

  private static boolean moedaDePagamento(AtivoResumo ativo) {
    return !ativo.cripto()
        || ativo.codigo().equals("cripto:USDT")
        || ativo.codigo().equals("cripto:USDC");
  }

  private static String percentual(String custo, BigDecimal total) {
    return total.signum() <= 0
        ? null
        : numero(
            new BigDecimal(custo)
                .multiply(BigDecimal.valueOf(100))
                .divide(total, 4, RoundingMode.HALF_EVEN));
  }

  private static String numero(BigDecimal valor) {
    return OperacoesMapper.numero(valor);
  }
}
