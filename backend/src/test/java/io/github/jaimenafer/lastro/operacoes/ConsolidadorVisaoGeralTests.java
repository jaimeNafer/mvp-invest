package io.github.jaimenafer.lastro.operacoes;

import static org.assertj.core.api.Assertions.assertThat;

import io.github.jaimenafer.lastro.operacoes.domain.model.*;
import io.github.jaimenafer.lastro.operacoes.domain.service.AnaliseResultado;
import io.github.jaimenafer.lastro.operacoes.domain.service.ConsolidadorVisaoGeral;
import io.github.jaimenafer.lastro.operacoes.domain.service.MotorLivro;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

class ConsolidadorVisaoGeralTests {
  @Test
  void preservaResultadoDaMoedaETaxaCalculadosPeloMotor() {
    var abertura =
        new FatoLivro(
            1L,
            TipoOperacao.SALDO_INICIAL,
            1L,
            3L,
            Instant.parse("2026-10-01T03:00:00Z"),
            1,
            new BigDecimal("100"),
            new BigDecimal("500"),
            null,
            null,
            null,
            null,
            null);
    var compra =
        new FatoLivro(
            2L,
            TipoOperacao.COMPRA,
            1L,
            2L,
            Instant.parse("2026-10-05T12:00:00Z"),
            2,
            BigDecimal.ONE,
            null,
            3L,
            new BigDecimal("50"),
            new BigDecimal("6"),
            3L,
            BigDecimal.ONE);
    var fatos = List.of(abertura, compra);
    var livro = MotorLivro.calcular(fatos, Map.of(2L, "cripto:BTC", 3L, "cripto:USDT"));
    var resumo =
        ConsolidadorVisaoGeral.resumir(
            fatos,
            livro,
            new PeriodoVisaoGeral(LocalDate.of(2026, 10, 5), LocalDate.of(2026, 10, 5)));
    assertThat(resumo.custoSaldosBrl()).isEqualByComparingTo("551");
    assertThat(resumo.resultadoRealizadoBrl()).isEqualByComparingTo("51");
    assertThat(resumo.negociacaoBrl()).isZero();
    assertThat(resumo.moedaBrl()).isEqualByComparingTo("50");
    assertThat(resumo.taxaBrl()).isEqualByComparingTo("1");
    assertThat(resumo.taxasBrl()).isEqualByComparingTo("6");
    assertThat(resumo.aportesBrl()).isZero();
    assertThat(resumo.operacoesNoPeriodo()).isEqualTo(1);
  }

  @Test
  void consolidaCincoMilFatosSemPerderPrecisao() {
    var fatos = new ArrayList<FatoLivro>();
    for (long i = 1; i <= 5000; i++) {
      fatos.add(
          new FatoLivro(
              i,
              TipoOperacao.APORTE,
              1L,
              1L,
              Instant.parse("2026-10-05T12:00:00Z").plusSeconds(i),
              i,
              new BigDecimal("1.01"),
              null,
              null,
              null,
              null,
              null,
              null));
    }
    var livro = MotorLivro.calcular(fatos, Map.of(1L, "fiat:BRL"));
    var resumo =
        ConsolidadorVisaoGeral.resumir(
            fatos,
            livro,
            new PeriodoVisaoGeral(LocalDate.of(2026, 10, 5), LocalDate.of(2026, 10, 5)));
    assertThat(resumo.custoSaldosBrl()).isEqualByComparingTo("5050");
    assertThat(resumo.aportesBrl()).isEqualByComparingTo("5050");
    assertThat(resumo.operacoesNoPeriodo()).isEqualTo(5000);
    assertThat(resumo.resultadoRealizadoBrl()).isZero();
    var periodo = new PeriodoVisaoGeral(LocalDate.of(2026, 10, 5), LocalDate.of(2026, 10, 5));
    assertThat(AnaliseResultado.mensal(fatos, livro, periodo).getFirst().operacoes())
        .isEqualTo(5000);
    assertThat(AnaliseResultado.porAtivo(fatos, livro, periodo, Map.of(1L, "fiat:BRL"))).isEmpty();
  }
}
