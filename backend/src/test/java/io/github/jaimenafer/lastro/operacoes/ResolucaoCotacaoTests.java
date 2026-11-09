package io.github.jaimenafer.lastro.operacoes;

import static org.assertj.core.api.Assertions.assertThat;

import io.github.jaimenafer.lastro.operacoes.domain.model.CotacaoHistorica;
import io.github.jaimenafer.lastro.operacoes.domain.service.ResolvedorCotacao;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import org.junit.jupiter.api.Test;

class ResolucaoCotacaoTests {
  private static final Instant INSTANTE = Instant.parse("2024-06-11T10:30:00Z");

  @Test
  void resolveCaminhoIndiretoComPrecosReaisDeStablecoin() {
    var cotacoes = List.of(
        new CotacaoHistorica(1L, 2L, Instant.parse("2024-06-11T10:00:00Z"), new BigDecimal("150")),
        new CotacaoHistorica(2L, 3L, Instant.parse("2024-06-11T10:00:00Z"), new BigDecimal("5.20")));
    var resultado = ResolvedorCotacao.resolver(1L, 3L, INSTANTE, cotacoes);
    assertThat(resultado).isPresent();
    assertThat(resultado.orElseThrow().preco()).isEqualByComparingTo("780");
    assertThat(resultado.orElseThrow().aproximado()).isTrue();
  }
  @Test
  void inverteParERejeitaCotacoesFuturasOuAntigas() {
    var cotacoes = List.of(
        new CotacaoHistorica(2L, 1L, INSTANTE.minusSeconds(60), new BigDecimal("2")),
        new CotacaoHistorica(1L, 3L, INSTANTE.plusSeconds(1), new BigDecimal("10")),
        new CotacaoHistorica(1L, 4L, INSTANTE.minusSeconds(86401), new BigDecimal("20")));
    assertThat(ResolvedorCotacao.resolver(1L, 2L, INSTANTE, cotacoes).orElseThrow().preco())
        .isEqualByComparingTo("0.5");
    assertThat(ResolvedorCotacao.resolver(1L, 3L, INSTANTE, cotacoes)).isEmpty();
    assertThat(ResolvedorCotacao.resolver(1L, 4L, INSTANTE, cotacoes)).isEmpty();
  }

  @Test
  void prefereCaminhoContemporaneoAoDiretoAntigo() {
    var cotacoes = List.of(
        new CotacaoHistorica(1L, 3L, INSTANTE.minusSeconds(86400), new BigDecimal("9")),
        new CotacaoHistorica(1L, 2L, INSTANTE, new BigDecimal("2")),
        new CotacaoHistorica(2L, 3L, INSTANTE, new BigDecimal("3")));
    assertThat(ResolvedorCotacao.resolver(1L, 3L, INSTANTE, cotacoes).orElseThrow().preco())
        .isEqualByComparingTo("6");
  }

  @Test
  void prefereCaminhoDiretoQuandoAQualidadeTemporalEmpata() {
    var direto = new CotacaoHistorica(1L, 3L, INSTANTE, new BigDecimal("9"));
    var cotacoes = List.of(
        new CotacaoHistorica(1L, 2L, INSTANTE, new BigDecimal("2")),
        new CotacaoHistorica(2L, 3L, INSTANTE, new BigDecimal("3")),
        direto);
    assertThat(ResolvedorCotacao.resolver(1L, 3L, INSTANTE, cotacoes).orElseThrow().caminho())
        .containsExactly(direto);
    var limite = new CotacaoHistorica(1L, 3L, INSTANTE.minusSeconds(86400), new BigDecimal("9"));
    assertThat(ResolvedorCotacao.resolver(1L, 3L, INSTANTE, List.of(limite))).isPresent();
  }

  @Test
  void rejeitaCaminhoComHorariosIncompativeis() {
    var cotacoes = List.of(
        new CotacaoHistorica(1L, 2L, INSTANTE.minusSeconds(3601), new BigDecimal("2")),
        new CotacaoHistorica(2L, 3L, INSTANTE, new BigDecimal("3")));
    assertThat(ResolvedorCotacao.resolver(1L, 3L, INSTANTE, cotacoes)).isEmpty();
  }

  @Test
  void naoConverteStablecoinsPorParidadeSemCotacao() {
    assertThat(ResolvedorCotacao.resolver(1L, 2L, INSTANTE, List.of())).isEmpty();
  }
}
