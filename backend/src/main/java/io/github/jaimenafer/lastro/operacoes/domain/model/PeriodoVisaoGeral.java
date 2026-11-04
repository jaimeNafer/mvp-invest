package io.github.jaimenafer.lastro.operacoes.domain.model;

import io.github.jaimenafer.lastro.operacoes.domain.exception.OperacaoInvalidaException;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;

public record PeriodoVisaoGeral(LocalDate inicio, LocalDate fim) {
  private static final ZoneId FUSO = ZoneId.of("America/Fortaleza");

  public PeriodoVisaoGeral {
    if (inicio == null || fim == null)
      throw new OperacaoInvalidaException("Informe as duas datas do período.");
    if (inicio.getYear() < 1000 || fim.getYear() < 1000)
      throw new OperacaoInvalidaException("Informe datas a partir do ano 1000.");
    if (inicio.isAfter(fim))
      throw new OperacaoInvalidaException("A data inicial não pode ser posterior à data final.");
  }

  public static PeriodoVisaoGeral definir(
      LocalDate inicio, LocalDate fim, LocalDate referencia, LocalDate hoje) {
    if ((inicio == null) != (fim == null))
      throw new OperacaoInvalidaException("Informe as datas inicial e final juntas.");
    PeriodoVisaoGeral periodo =
        inicio == null
            ? new PeriodoVisaoGeral(referencia == null ? hoje : referencia, hoje)
            : new PeriodoVisaoGeral(inicio, fim);
    if (periodo.fim().isAfter(hoje))
      throw new OperacaoInvalidaException("O período não pode incluir datas futuras.");
    return periodo;
  }

  public boolean ateFim(Instant instante) {
    return instante.isBefore(fim.plusDays(1).atStartOfDay(FUSO).toInstant());
  }

  public boolean contem(Instant instante) {
    return !instante.isBefore(inicio.atStartOfDay(FUSO).toInstant()) && ateFim(instante);
  }
}
