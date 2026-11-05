package io.github.jaimenafer.lastro.operacoes;

import static org.assertj.core.api.Assertions.assertThat;

import io.github.jaimenafer.lastro.operacoes.domain.model.*;
import io.github.jaimenafer.lastro.operacoes.domain.service.AnaliseResultado;
import io.github.jaimenafer.lastro.operacoes.domain.service.MotorLivro;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

class AnaliseResultadoTests {
  @Test
  void acumulaMesesParciaisComPrejuizoEMesSemOperacoes() {
    var abertura =
        fato(1, TipoOperacao.SALDO_INICIAL, "2026-06-01T03:00:00Z", "2", "200", null, null, null);
    var venda = fato(2, TipoOperacao.VENDA, "2026-07-02T03:00:00Z", "1", null, "80", 1L, "2");
    var fatos = List.of(abertura, venda);
    var livro = MotorLivro.calcular(fatos, Map.of(1L, "fiat:BRL", 2L, "cripto:BTC"));
    var periodo =
        new PeriodoVisaoGeral(LocalDate.parse("2026-07-02"), LocalDate.parse("2026-09-03"));
    var meses = AnaliseResultado.mensal(fatos, livro, periodo);
    assertThat(meses).hasSize(3);
    assertThat(meses.getFirst().dataInicio()).isEqualTo(periodo.inicio());
    assertThat(meses.getLast().dataFim()).isEqualTo(periodo.fim());
    assertThat(meses.getFirst().resultadoBrl()).isEqualByComparingTo("-22");
    assertThat(meses.get(1).operacoes()).isZero();
    assertThat(meses.get(1).resultadoBrl()).isZero();
    assertThat(meses.getLast().acumuladoBrl()).isEqualByComparingTo("-22");
    var ativos =
        AnaliseResultado.porAtivo(
            fatos, livro, periodo, Map.of(1L, "fiat:BRL", 2L, "cripto:BTC", 3L, "cripto:USDT"));
    assertThat(ativos).hasSize(1);
    assertThat(ativos.getFirst().ativoId()).isEqualTo(2L);
    assertThat(ativos.getFirst().negociacaoBrl()).isEqualByComparingTo("-20");
    assertThat(ativos.getFirst().taxaBrl()).isEqualByComparingTo("-2");
    assertThat(ativos.getFirst().totalBrl()).isEqualByComparingTo("-22");
  }

  @Test
  void atribuiRealizacaoDaCompraAMoedaGastaSemDuplicarTaxa() {
    var abertura =
        new FatoLivro(
            1L,
            TipoOperacao.SALDO_INICIAL,
            1L,
            3L,
            Instant.parse("2026-06-01T03:00:00Z"),
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
            Instant.parse("2026-07-02T03:00:00Z"),
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
    var periodo =
        new PeriodoVisaoGeral(LocalDate.parse("2026-07-02"), LocalDate.parse("2026-07-02"));
    var resultados =
        AnaliseResultado.porAtivo(
            fatos, livro, periodo, Map.of(1L, "fiat:BRL", 2L, "cripto:BTC", 3L, "cripto:USDT"));
    assertThat(resultados).hasSize(1);
    assertThat(resultados.getFirst().ativoId()).isEqualTo(3L);
    assertThat(resultados.getFirst().moedaBrl()).isEqualByComparingTo("50");
    assertThat(resultados.getFirst().taxaBrl()).isEqualByComparingTo("1");
    assertThat(resultados.getFirst().totalBrl()).isEqualByComparingTo("51");
    assertThat(AnaliseResultado.mensal(fatos, livro, periodo).getFirst().acumuladoBrl())
        .isEqualByComparingTo("51");
  }

  @Test
  void aportesEAberturasNaoCriamResultadoPorAtivo() {
    var aporte =
        new FatoLivro(
            1L,
            TipoOperacao.APORTE,
            1L,
            1L,
            Instant.parse("2026-07-02T03:00:00Z"),
            1,
            BigDecimal.TEN,
            null,
            null,
            null,
            null,
            null,
            null);
    var fatos = List.of(aporte);
    var livro = MotorLivro.calcular(fatos, Map.of(1L, "fiat:BRL"));
    var periodo =
        new PeriodoVisaoGeral(LocalDate.parse("2026-07-02"), LocalDate.parse("2026-07-02"));
    assertThat(
            AnaliseResultado.porAtivo(
                fatos, livro, periodo, Map.of(1L, "fiat:BRL", 2L, "cripto:BTC", 3L, "cripto:USDT")))
        .isEmpty();
    assertThat(AnaliseResultado.mensal(fatos, livro, periodo).getFirst().acumuladoBrl()).isZero();
  }

  @ParameterizedTest
  @CsvSource({"cripto:USDT,4,-51", "cripto:USDC,6,51", "cripto:USDT,5,0"})
  void preservaResultadoNegativoPositivoOuZeroDaMoeda(
      String codigo, String cotacao, String esperado) {
    var abertura =
        new FatoLivro(
            1L,
            TipoOperacao.SALDO_INICIAL,
            1L,
            3L,
            Instant.parse("2026-06-01T03:00:00Z"),
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
            Instant.parse("2026-07-02T03:00:00Z"),
            2,
            BigDecimal.ONE,
            null,
            3L,
            new BigDecimal("50"),
            new BigDecimal(cotacao),
            3L,
            BigDecimal.ONE);
    var fatos = List.of(abertura, compra);
    var codigos = Map.of(2L, "cripto:BTC", 3L, codigo);
    var livro = MotorLivro.calcular(fatos, codigos);
    var periodo =
        new PeriodoVisaoGeral(LocalDate.parse("2026-07-01"), LocalDate.parse("2026-07-31"));
    var resultados = AnaliseResultado.porAtivo(fatos, livro, periodo, codigos);
    assertThat(resultados).hasSize(1);
    assertThat(resultados.getFirst().totalBrl()).isEqualByComparingTo(esperado);
    assertThat(AnaliseResultado.mensal(fatos, livro, periodo).getLast().acumuladoBrl())
        .isEqualByComparingTo(resultados.getFirst().totalBrl());
  }

  @Test
  void compraEmBrlNaoInventaRealizacaoEAtivoEsgotadoMantemResultado() {
    var aporte =
        new FatoLivro(
            1L,
            TipoOperacao.APORTE,
            1L,
            1L,
            Instant.parse("2026-06-01T03:00:00Z"),
            1,
            new BigDecimal("1000"),
            null,
            null,
            null,
            null,
            null,
            null);
    var compra = fato(2, TipoOperacao.COMPRA, "2026-06-02T03:00:00Z", "1", null, "100", 1L, "2");
    var venda = fato(3, TipoOperacao.VENDA, "2026-07-01T02:59:59Z", "1", null, "150", 1L, "1");
    var fatos = List.of(aporte, compra, venda);
    var codigos = Map.of(1L, "fiat:BRL", 2L, "cripto:BTC");
    var livro = MotorLivro.calcular(fatos, codigos);
    var periodo =
        new PeriodoVisaoGeral(LocalDate.parse("2026-06-01"), LocalDate.parse("2026-07-31"));
    assertThat(livro.posicoes()).noneMatch(p -> p.ativoId().equals(2L));
    assertThat(AnaliseResultado.porAtivo(fatos, livro, periodo, codigos)).hasSize(1);
    assertThat(AnaliseResultado.porAtivo(fatos, livro, periodo, codigos).getFirst().totalBrl())
        .isEqualByComparingTo("47");
    assertThat(AnaliseResultado.mensal(fatos, livro, periodo).getFirst().resultadoBrl())
        .isEqualByComparingTo("47");
    assertThat(AnaliseResultado.mensal(fatos, livro, periodo).getLast().operacoes()).isZero();
  }

  private FatoLivro fato(
      long id,
      TipoOperacao tipo,
      String instante,
      String quantidade,
      String custo,
      String bruto,
      Long taxaAtivo,
      String taxa) {
    return new FatoLivro(
        id,
        tipo,
        1L,
        2L,
        Instant.parse(instante),
        id,
        new BigDecimal(quantidade),
        custo == null ? null : new BigDecimal(custo),
        bruto == null ? null : 1L,
        bruto == null ? null : new BigDecimal(bruto),
        bruto == null ? null : BigDecimal.ONE,
        taxaAtivo,
        taxa == null ? null : new BigDecimal(taxa));
  }
}
