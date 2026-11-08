package io.github.jaimenafer.lastro.operacoes;

import static org.assertj.core.api.Assertions.*;

import io.github.jaimenafer.lastro.operacoes.domain.model.CotacaoHistorica;
import io.github.jaimenafer.lastro.operacoes.domain.model.EstadoValoracao;
import io.github.jaimenafer.lastro.operacoes.domain.service.MotorValoracao;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import org.junit.jupiter.api.Test;

class MotorValoracaoTests {
  private static final Instant DATA = Instant.parse("2024-06-11T10:30:00Z");

  @Test
  void valorEfetivoTemPrioridadeSobreValorManual() {
    var r = MotorValoracao.valorar(new BigDecimal("2"), 1L, 3L, DATA,
        new BigDecimal("12"), new BigDecimal("10"), List.of());
    assertThat(r.estado()).isEqualTo(EstadoValoracao.CONFIRMADO);
    assertThat(r.valorTotalBrl()).isEqualByComparingTo("10");
  }

  @Test
  void valorEfetivoPrevaleceSobreMercado() {
    var r = MotorValoracao.valorar(new BigDecimal("2"), 1L, 3L, DATA,
        null, new BigDecimal("10"), List.of(new CotacaoHistorica(1L, 3L, DATA, new BigDecimal("9"))));
    assertThat(r.estado()).isEqualTo(EstadoValoracao.CONFIRMADO);
    assertThat(r.valorTotalBrl()).isEqualByComparingTo("10");
  }

  @Test
  void valoraComAberturaAnteriorSemInventarParidade() {
    var cotacoes = List.of(
        new CotacaoHistorica(1L, 2L, DATA.minusSeconds(1800), new BigDecimal("150")),
        new CotacaoHistorica(2L, 3L, DATA.minusSeconds(1800), new BigDecimal("5.2")));
    var r = MotorValoracao.valorar(new BigDecimal("2"), 1L, 3L, DATA, null, null, cotacoes);
    assertThat(r.estado()).isEqualTo(EstadoValoracao.ESTIMADO);
    assertThat(r.valorTotalBrl()).isEqualByComparingTo("1560");
    assertThat(r.caminho()).hasSize(2);
  }

  @Test
  void ausenciaDeCotacaoPermaneceIndisponivel() {
    var r = MotorValoracao.valorar(new BigDecimal("2"), 1L, 3L, DATA, null, null, List.of());
    assertThat(r.estado()).isEqualTo(EstadoValoracao.INDISPONIVEL);
    assertThat(r.valorTotalBrl()).isNull();
  }
}
