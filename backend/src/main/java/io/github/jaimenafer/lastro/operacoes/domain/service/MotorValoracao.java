package io.github.jaimenafer.lastro.operacoes.domain.service;

import io.github.jaimenafer.lastro.operacoes.domain.model.CotacaoHistorica;
import io.github.jaimenafer.lastro.operacoes.domain.model.EstadoValoracao;
import io.github.jaimenafer.lastro.operacoes.domain.model.Valoracao;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.util.List;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class MotorValoracao {
  public static Valoracao valorar(BigDecimal quantidade, Long ativoId, Long brlId, Instant instante,
      BigDecimal valorManual, BigDecimal valorEfetivo, List<CotacaoHistorica> cotacoes) {
    if (valorEfetivo != null) return direto(EstadoValoracao.CONFIRMADO, valorEfetivo);
    if (valorManual != null) return direto(EstadoValoracao.MANUAL, valorManual);
    if (quantidade == null || quantidade.signum() <= 0 || ativoId == null || brlId == null || instante == null)
      return new Valoracao(EstadoValoracao.INDISPONIVEL, null, List.of());
    return ResolvedorCotacao.resolver(ativoId, brlId, instante, cotacoes)
        .map(c -> new Valoracao(c.aproximado() ? EstadoValoracao.ESTIMADO : EstadoValoracao.HISTORICO,
            quantidade.multiply(c.preco()).setScale(8, RoundingMode.HALF_EVEN), c.caminho()))
        .orElseGet(() -> new Valoracao(EstadoValoracao.INDISPONIVEL, null, List.of()));
  }

  private static Valoracao direto(EstadoValoracao estado, BigDecimal valor) {
    if (valor.signum() < 0) throw new IllegalArgumentException("O valor em reais não pode ser negativo.");
    return new Valoracao(estado, valor.setScale(8, RoundingMode.HALF_EVEN), List.of());
  }
}
