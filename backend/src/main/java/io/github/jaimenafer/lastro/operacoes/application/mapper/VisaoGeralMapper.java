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
      Map<Long, InstituicaoResumo> instituicoes) {
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
        posicoes);
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
