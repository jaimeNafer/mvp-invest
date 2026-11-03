package io.github.jaimenafer.lastro.operacoes;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import io.github.jaimenafer.lastro.operacoes.domain.exception.OperacaoConflitanteException;
import io.github.jaimenafer.lastro.operacoes.domain.model.CalculoOperacao;
import io.github.jaimenafer.lastro.operacoes.domain.model.FatoLivro;
import io.github.jaimenafer.lastro.operacoes.domain.model.PosicaoLivro;
import io.github.jaimenafer.lastro.operacoes.domain.model.ResultadoLivro;
import io.github.jaimenafer.lastro.operacoes.domain.model.TipoOperacao;
import io.github.jaimenafer.lastro.operacoes.domain.service.MotorLivro;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

class MotorLivroTests {
  private static final Long BRL = 1L;
  private static final Long BTC = 2L;
  private static final Long USDT = 3L;
  private static final Map<Long, String> CODIGOS =
      Map.of(BRL, "fiat:BRL", BTC, "BTC", USDT, "USDT");
  private static final Instant INICIO = Instant.parse("2026-11-01T12:00:00Z");

  @Test
  void aportePermiteCompraEVendaComCustosEResultados() {
    ResultadoLivro livro =
        calcular(
            aporte(1, "1000"),
            compra(2, BRL, "2", "600", "1", null, null),
            venda(3, BRL, "1", "400", "1", null, null));
    assertPosicao(livro, BTC, "1", "300");
    assertPosicao(livro, BRL, "800", "800");
    assertThat(calculo(livro, 3).custoBaixadoBrl()).isEqualByComparingTo("300");
    assertThat(calculo(livro, 3).resultadoNegociacaoBrl()).isEqualByComparingTo("100");
    assertThat(calculo(livro, 3).resultadoTotalBrl()).isEqualByComparingTo("100");
  }

  @Test
  void compraComTaxaNaCotacaoIncorporaTaxaAoCusto() {
    ResultadoLivro livro = calcular(aporte(1, "1000"), compra(2, BRL, "2", "600", "1", BRL, "10"));
    assertPosicao(livro, BTC, "2", "610");
    assertPosicao(livro, BRL, "390", "390");
    assertThat(calculo(livro, 2).taxaBrl()).isEqualByComparingTo("10");
    assertThat(calculo(livro, 2).resultadoTotalBrl()).isZero();
  }

  @Test
  void compraComTaxaNoAtivoCreditaQuantidadeLiquidaSemDuplicarCusto() {
    ResultadoLivro livro = calcular(aporte(1, "1000"), compra(2, BRL, "2", "600", "1", BTC, "0.1"));
    assertPosicao(livro, BTC, "1.9", "600");
    assertPosicao(livro, BRL, "400", "400");
    assertThat(calculo(livro, 2).taxaBrl()).isEqualByComparingTo("30");
    assertThat(calculo(livro, 2).resultadoTotalBrl()).isZero();
  }

  @Test
  void vendaComTaxaNaCotacaoCreditaReceitaLiquida() {
    ResultadoLivro livro =
        calcular(abertura(1, 1, BTC, "2", "600"), venda(2, BRL, "1", "400", "1", BRL, "10"));
    assertPosicao(livro, BTC, "1", "300");
    assertPosicao(livro, BRL, "390", "390");
    assertThat(calculo(livro, 2).resultadoTotalBrl()).isEqualByComparingTo("90");
  }

  @Test
  void vendaComTaxaNoAtivoBaixaQuantidadeETaxaPeloCustoMedio() {
    ResultadoLivro livro =
        calcular(abertura(1, 1, BTC, "2", "600"), venda(2, BRL, "1", "400", "1", BTC, "0.1"));
    assertPosicao(livro, BTC, "0.9", "270");
    assertPosicao(livro, BRL, "400", "400");
    assertThat(calculo(livro, 2).custoBaixadoBrl()).isEqualByComparingTo("330");
    assertThat(calculo(livro, 2).resultadoTotalBrl()).isEqualByComparingTo("70");
  }

  @Test
  void saidaDeStablecoinSeparaVariacaoDeMoedaEDaTaxa() {
    ResultadoLivro livro =
        calcular(abertura(1, 1, USDT, "200", "800"), compra(2, USDT, "2", "100", "5", USDT, "2"));
    assertPosicao(livro, USDT, "98", "392");
    assertPosicao(livro, BTC, "2", "510");
    CalculoOperacao calculo = calculo(livro, 2);
    assertThat(calculo.resultadoNegociacaoBrl()).isZero();
    assertThat(calculo.resultadoMoedaBrl()).isEqualByComparingTo("100");
    assertThat(calculo.resultadoTaxaBrl()).isEqualByComparingTo("2");
    assertThat(calculo.resultadoTotalBrl()).isEqualByComparingTo("102");
  }

  @Test
  void vendaEmStablecoinFormaCustoPelaCotacaoDaEntrada() {
    ResultadoLivro livro =
        calcular(abertura(1, 1, BTC, "2", "600"), venda(2, USDT, "1", "100", "5", USDT, "2"));
    assertPosicao(livro, USDT, "98", "490");
    assertThat(calculo(livro, 2).resultadoTotalBrl()).isEqualByComparingTo("190");
    assertThat(calculo(livro, 2).resultadoMoedaBrl()).isZero();
  }

  @Test
  void custoGlobalIndependeDaInstituicaoMasCustodiaLimitaSaida() {
    ResultadoLivro livro =
        calcular(
            abertura(1, 1, BTC, "1", "100"),
            abertura(2, 2, BTC, "1", "300"),
            venda(3, BRL, "1", "250", "1", null, null));
    assertPosicao(livro, BTC, "1", "200");
    assertThat(posicao(livro, BTC).instituicoes()).hasSize(1);
    assertThat(posicao(livro, BTC).instituicoes().getFirst().instituicaoId()).isEqualTo(2L);
    assertThat(calculo(livro, 3).resultadoTotalBrl()).isEqualByComparingTo("50");
    assertThatThrownBy(() ->
            calcular(abertura(1, 2, BTC, "2", "200"), venda(19, BRL, "1", "250", "1", null, null)))
        .isInstanceOf(OperacaoConflitanteException.class)
        .hasMessageContaining("19");
  }

  @Test
  void saidaIntegralConsomeResiduoAposBaixasParciais() {
    ResultadoLivro livro =
        calcular(
            abertura(1, 1, BTC, "3", "1"),
            venda(2, BRL, "1", "1", "1", null, null),
            venda(3, BRL, "1", "1", "1", null, null),
            venda(4, BRL, "1", "1", "1", null, null));
    assertThat(livro.posicoes()).extracting(PosicaoLivro::ativoId).containsExactly(BRL);
    assertThat(calculo(livro, 2).custoBaixadoBrl()).isEqualByComparingTo("0.33333333");
    assertThat(calculo(livro, 3).custoBaixadoBrl()).isEqualByComparingTo("0.33333334");
    assertThat(calculo(livro, 4).custoBaixadoBrl()).isEqualByComparingTo("0.33333333");
  }

  @Test
  void taxaNoAtivoConsomeTodoCustoResidualNaVendaIntegral() {
    ResultadoLivro livro =
        calcular(abertura(1, 1, BTC, "3", "1"), venda(2, BRL, "2", "1", "1", BTC, "1"));
    assertThat(livro.posicoes()).extracting(PosicaoLivro::ativoId).containsExactly(BRL);
    CalculoOperacao calculo = calculo(livro, 2);
    assertThat(calculo.custoBaixadoBrl()).isEqualByComparingTo("1");
    assertThat(calculo.resultadoNegociacaoBrl()).isEqualByComparingTo("0.33333333");
    assertThat(calculo.resultadoTaxaBrl()).isEqualByComparingTo("-0.33333333");
    assertThat(calculo.resultadoTotalBrl()).isZero();
  }

  @Test
  void baixaParcialArredondaMeioParaParEPreservaResiduoNaSaidaIntegral() {
    ResultadoLivro livro =
        calcular(
            abertura(1, 1, BTC, "2", "0.00000001"),
            venda(2, BRL, "1", "1", "1", null, null),
            venda(3, BRL, "1", "1", "1", null, null));
    assertThat(calculo(livro, 2).custoBaixadoBrl()).isZero();
    assertThat(calculo(livro, 3).custoBaixadoBrl()).isEqualByComparingTo("0.00000001");
  }

  @Test
  void saldoDeCotacaoPrecisaCobrirTambemTaxaDaCompra() {
    assertThatThrownBy(
            () -> calcular(aporte(1, "600"), compra(2, BRL, "2", "600", "1", BRL, "0.00000001")))
        .isInstanceOf(OperacaoConflitanteException.class)
        .hasMessageContaining("2");
  }

  @Test
  void ordenaPorInstanteOrdemEstavelEIdSemModificarEntrada() {
    FatoLivro primeiro =
        new FatoLivro(
            9L,
            TipoOperacao.APORTE,
            1L,
            BRL,
            INICIO,
            1,
            numero("1000"),
            null,
            null,
            null,
            null,
            null,
            null);
    FatoLivro segundo =
        new FatoLivro(
            10L,
            TipoOperacao.COMPRA,
            1L,
            BTC,
            INICIO,
            1,
            numero("2"),
            null,
            BRL,
            numero("600"),
            numero("1"),
            null,
            null);
    FatoLivro terceiro =
        new FatoLivro(
            -1L,
            TipoOperacao.VENDA,
            1L,
            BTC,
            INICIO,
            2,
            numero("1"),
            null,
            BRL,
            numero("400"),
            numero("1"),
            null,
            null);
    List<FatoLivro> fatos = new ArrayList<>(List.of(terceiro, segundo, primeiro));
    ResultadoLivro livro = MotorLivro.calcular(fatos, CODIGOS);
    assertThat(livro.calculos())
        .extracting(CalculoOperacao::operacaoId)
        .containsExactly(9L, 10L, -1L);
    assertThat(fatos).containsExactly(terceiro, segundo, primeiro);
    assertPosicao(livro, BTC, "1", "300");
  }

  @Test
  void rejeitaFaltaExataDeUmaFracaoSemTolerancia() {
    assertThatThrownBy(() ->
            calcular(
                abertura(1, 1, BTC, "1", "1"),
                venda(2, BRL, "1.000000000000000001", "1", "1", null, null)))
        .isInstanceOf(OperacaoConflitanteException.class)
        .hasMessageContaining("2");
  }

  @Test
  void quantidadesECustosGrandesNaoSofremLimiteGlobalDePrecisao() {
    ResultadoLivro livro =
        calcular(
            abertura(1, 1, BTC, "0.000000000000000003", "999999999999999999999999999999.99999999"));
    assertThat(posicao(livro, BTC).custoTotalBrl())
        .isEqualByComparingTo("999999999999999999999999999999.99999999");
    assertThat(posicao(livro, BTC).custoMedioBrl().toPlainString())
        .isEqualTo(
            "333333333333333333333333333333333333330000000000.000000000000000000000000000000000000");
  }

  @Test
  void custoMedioPeriodicoMantem36CasasECustoZeroPreservaQuantidade() {
    ResultadoLivro livro = calcular(abertura(1, 1, BTC, "3", "1"), abertura(2, 1, USDT, "2", "0"));
    assertThat(posicao(livro, BTC).custoMedioBrl().toPlainString())
        .isEqualTo("0.333333333333333333333333333333333333");
    assertPosicao(livro, USDT, "2", "0");
  }

  @Test
  void taxaZeroEquivaleAAusenciaDeTaxa() {
    ResultadoLivro semTaxa =
        calcular(aporte(1, "1000"), compra(2, BRL, "2", "600", "1", null, null));
    ResultadoLivro taxaZero =
        calcular(aporte(1, "1000"), compra(2, BRL, "2", "600", "1", BRL, "0"));
    assertThat(taxaZero).isEqualTo(semTaxa);
  }

  @Test
  void resultadoExpõeColecoesImutaveisEOrdenadas() {
    ResultadoLivro livro =
        calcular(abertura(1, 9, BTC, "1", "100"), abertura(2, 2, BTC, "1", "300"), aporte(3, "10"));
    assertThat(livro.posicoes()).extracting(PosicaoLivro::ativoId).containsExactly(BRL, BTC);
    assertThat(posicao(livro, BTC).instituicoes())
        .extracting(c -> c.instituicaoId())
        .containsExactly(2L, 9L);
    assertThatThrownBy(() -> livro.posicoes().clear())
        .isInstanceOf(UnsupportedOperationException.class);
    assertThatThrownBy(() -> livro.calculos().clear())
        .isInstanceOf(UnsupportedOperationException.class);
    assertThatThrownBy(() -> posicao(livro, BTC).instituicoes().clear())
        .isInstanceOf(UnsupportedOperationException.class);
  }

  @Test
  void reproduzCincoMilEventosSemAcumularErro() {
    List<FatoLivro> fatos = new ArrayList<>();
    fatos.add(aporte(1, "10000"));
    for (long id = 2; id <= 5000; id++) {
      fatos.add(
          id % 2 == 0
              ? compra(id, BRL, "0.00000001", "1", "1", null, null)
              : venda(id, BRL, "0.00000001", "1.5", "1", null, null));
    }
    ResultadoLivro livro = MotorLivro.calcular(fatos, CODIGOS);
    assertThat(livro.calculos()).hasSize(5000);
    assertPosicao(livro, BTC, "0.00000001", "1");
    assertPosicao(livro, BRL, "11248.5", "11248.5");
  }

  @Test
  void rejeitaResultadoMonetarioAcimaDaCapacidadeSemTruncar() {
    assertThatThrownBy(() ->
            calcular(
                abertura(1, 1, USDT, "99999999999999999999", "1"),
                compra(2, USDT, "1", "99999999999999999999", "99999999999999999999", null, null)))
        .isInstanceOf(
            io.github.jaimenafer.lastro.operacoes.domain.exception.OperacaoInvalidaException.class)
        .hasMessageContaining("capacidade");
  }

  private ResultadoLivro calcular(FatoLivro... fatos) {
    return MotorLivro.calcular(List.of(fatos), CODIGOS);
  }

  private FatoLivro aporte(long id, String quantidade) {
    return fato(id, TipoOperacao.APORTE, 1L, BRL, quantidade, null, null, null, null, null, null);
  }

  private FatoLivro abertura(
      long id, long instituicao, Long ativo, String quantidade, String custo) {
    return fato(
        id,
        TipoOperacao.SALDO_INICIAL,
        instituicao,
        ativo,
        quantidade,
        custo,
        null,
        null,
        null,
        null,
        null);
  }

  private FatoLivro compra(
      long id,
      Long moeda,
      String quantidade,
      String bruto,
      String cambio,
      Long taxaAtivo,
      String taxa) {
    return fato(
        id, TipoOperacao.COMPRA, 1L, BTC, quantidade, null, moeda, bruto, cambio, taxaAtivo, taxa);
  }

  private FatoLivro venda(
      long id,
      Long moeda,
      String quantidade,
      String bruto,
      String cambio,
      Long taxaAtivo,
      String taxa) {
    return fato(
        id, TipoOperacao.VENDA, 1L, BTC, quantidade, null, moeda, bruto, cambio, taxaAtivo, taxa);
  }

  private FatoLivro fato(
      long id,
      TipoOperacao tipo,
      long instituicao,
      Long ativo,
      String quantidade,
      String custo,
      Long moeda,
      String bruto,
      String cambio,
      Long taxaAtivo,
      String taxa) {
    return new FatoLivro(
        id,
        tipo,
        instituicao,
        ativo,
        INICIO.plusSeconds(id),
        id,
        numero(quantidade),
        numero(custo),
        moeda,
        numero(bruto),
        numero(cambio),
        taxaAtivo,
        numero(taxa));
  }

  private BigDecimal numero(String valor) {
    return valor == null ? null : new BigDecimal(valor);
  }

  private PosicaoLivro posicao(ResultadoLivro livro, Long ativo) {
    return livro.posicoes().stream()
        .filter(p -> p.ativoId().equals(ativo))
        .findFirst()
        .orElseThrow();
  }

  private CalculoOperacao calculo(ResultadoLivro livro, long id) {
    return livro.calculos().stream().filter(c -> c.operacaoId() == id).findFirst().orElseThrow();
  }

  private void assertPosicao(ResultadoLivro livro, Long ativo, String quantidade, String custo) {
    assertThat(posicao(livro, ativo).quantidade()).isEqualByComparingTo(quantidade);
    assertThat(posicao(livro, ativo).custoTotalBrl()).isEqualByComparingTo(custo);
  }
}
