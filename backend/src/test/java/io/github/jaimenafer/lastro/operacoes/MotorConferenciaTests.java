package io.github.jaimenafer.lastro.operacoes;

import static org.assertj.core.api.Assertions.assertThat;

import io.github.jaimenafer.lastro.operacoes.domain.model.EstadoConferencia;
import io.github.jaimenafer.lastro.operacoes.domain.model.FatoLivro;
import io.github.jaimenafer.lastro.operacoes.domain.model.ItemConferenciaLivro;
import io.github.jaimenafer.lastro.operacoes.domain.model.LacunaLivro;
import io.github.jaimenafer.lastro.operacoes.domain.model.TipoOperacao;
import io.github.jaimenafer.lastro.operacoes.domain.service.MotorLivro;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

class MotorConferenciaTests {
  @Test
  void saidaCriptoSemFinalidadePreservaCustoRemanescenteSemInventarResultado() {
    var saida = new ItemConferenciaLivro(
        "b", 1L, 1L, 2L, DATA.plusSeconds(1), 2,
        decimal("-1"), null, null, false, null, null);
    var livro = MotorLivro.conferir(List.of(entrada("a", 1, 2, "2", "1000"), saida), CODIGOS);
    assertThat(livro.posicoes()).singleElement().satisfies(p -> {
      assertThat(p.quantidade()).isEqualByComparingTo("1");
      assertThat(p.custoTotalBrl()).isEqualByComparingTo("500");
    });
    assertThat(livro.efeitos().get(1).calculo()).isNull();
  }

  @Test
  void retiradaEmBrlReduzSaldoSemGerarResultado() {
    var fato = new FatoLivro(2L, TipoOperacao.RETIRADA, 1L, 1L, DATA.plusSeconds(1), 2,
        decimal("250"), null, null, null, null, null, null);
    var retirada = new ItemConferenciaLivro("b", 1L, 1L, 1L, fato.ocorridaEm(), 2,
        decimal("-250"), null, fato, false, null, null);
    var livro = MotorLivro.conferir(List.of(entrada("a", 1, 1, "1000", null), retirada), CODIGOS);
    assertThat(livro.posicoes()).singleElement()
        .satisfies(p -> assertThat(p.quantidade()).isEqualByComparingTo("750"));
    assertThat(livro.efeitos().get(1).estado()).isEqualTo(EstadoConferencia.COERENTE);
    assertThat(livro.efeitos().get(1).calculo().resultadoTotalBrl()).isEqualByComparingTo("0");
    var semSaldo = MotorLivro.conferir(List.of(retirada), CODIGOS);
    assertThat(semSaldo.efeitos().getFirst().estado())
        .isEqualTo(EstadoConferencia.CORRECAO_NECESSARIA);
    assertThat(semSaldo.lacunas()).singleElement()
        .satisfies(l -> assertThat(l.quantidade()).isEqualByComparingTo("250"));
  }

  @Test
  void saldoAnteriorSemCustoPermanecePendente() {
    Instant instante = Instant.parse("2024-01-01T12:00:00Z");
    FatoLivro fato =
        new FatoLivro(
            1L,
            TipoOperacao.SALDO_INICIAL,
            1L,
            2L,
            instante,
            1,
            BigDecimal.ONE,
            null,
            null,
            null,
            null,
            null,
            null);
    var item =
        new ItemConferenciaLivro(
            "saldo", 1L, 1L, 2L, instante, 1, BigDecimal.ONE, null, fato, false, null, null);
    var resultado = MotorLivro.conferir(List.of(item), CODIGOS);
    assertThat(resultado.efeitos())
        .singleElement()
        .satisfies(e -> assertThat(e.estado()).isEqualTo(EstadoConferencia.PENDENTE));
    assertThat(resultado.posicoes())
        .singleElement()
        .satisfies(p -> assertThat(p.custoTotalBrl()).isNull());
  }

  @Test
  void creditoCoerenteMantemCustoDesconhecidoERepercuteNaVendaPosterior() {
    var credito = new ItemConferenciaLivro(
        "b", 1L, 1L, 2L, DATA.plusSeconds(1), 2, decimal("1"), null,
        null, false, null, null, true);
    var livro = MotorLivro.conferir(
        List.of(entrada("a", 1, 2, "2", "1000"), credito, venda("c", 1, "1", "400")),
        CODIGOS);
    assertThat(livro.efeitos().get(1).estado()).isEqualTo(EstadoConferencia.COERENTE);
    assertThat(livro.efeitos().get(1).calculo()).isNull();
    assertThat(livro.efeitos().get(2).calculo().resultadoTotalBrl()).isNull();
    assertThat(livro.posicoes()).filteredOn(p -> p.ativoId().equals(2L))
        .singleElement().satisfies(p -> {
          assertThat(p.quantidade()).isEqualByComparingTo("2");
          assertThat(p.custoTotalBrl()).isNull();
          assertThat(p.custoMedioBrl()).isNull();
        });
  }

  @Test
  void entradaSemCustoPreservaQuantidadeEReceitaSemInventarResultado() {
    var livro =
        MotorLivro.conferir(
            List.of(entrada("a", 1, 2, "2", null), venda("b", 1, "1", "400")), CODIGOS);
    assertThat(livro.posicoes())
        .filteredOn(p -> p.ativoId().equals(2L))
        .singleElement()
        .satisfies(p -> {
          assertThat(p.quantidade()).isEqualByComparingTo("1");
          assertThat(p.custoTotalBrl()).isNull();
          assertThat(p.custoMedioBrl()).isNull();
          assertThat(p.quantidadeDisponivel()).isTrue();
        });
    assertThat(livro.efeitos().get(1).calculo().valorBrutoBrl()).isEqualByComparingTo("400");
    assertThat(livro.efeitos().get(1).calculo().custoBaixadoBrl()).isNull();
    assertThat(livro.efeitos().get(1).calculo().resultadoTotalBrl()).isNull();
    assertThat(livro.posicoes())
        .filteredOn(p -> p.ativoId().equals(1L))
        .singleElement()
        .satisfies(p -> assertThat(p.custoTotalBrl()).isEqualByComparingTo("400"));
  }

  @Test
  void deficitNaoConsomeOutraContaNemCompletaSaldoSilenciosamente() {
    var livro =
        MotorLivro.conferir(
            List.of(entrada("a", 2, 2, "5", "500"), venda("b", 1, "2", "800")), CODIGOS);
    assertThat(livro.lacunas())
        .singleElement()
        .satisfies(l -> {
          assertThat(l.contaId()).isEqualTo(1L);
          assertThat(l.quantidade()).isEqualByComparingTo("2");
          assertThat(l.ancoraId()).isEqualTo("b");
        });
    assertThat(livro.efeitos().get(1).estado()).isEqualTo(EstadoConferencia.CORRECAO_NECESSARIA);
    assertThat(livro.posicoes())
        .filteredOn(p -> p.ativoId().equals(2L))
        .singleElement()
        .satisfies(p -> {
          assertThat(p.quantidadeDisponivel()).isFalse();
          assertThat(p.contas())
              .filteredOn(c -> c.contaId().equals(1L))
              .singleElement()
              .satisfies(c -> assertThat(c.quantidade()).isEqualByComparingTo("-2"));
        });
  }

  @Test
  void ajusteProvisorioAncoraAntesDaVendaSemDataEconomica() {
    var ajuste =
        new ItemConferenciaLivro(
            "aj", 1L, 1L, 2L, null, 0, decimal("2"), null, null, true, "b", null);
    var livro = MotorLivro.conferir(List.of(venda("b", 1, "1", "400"), ajuste), CODIGOS);
    assertThat(livro.lacunas()).isEmpty();
    assertThat(livro.efeitos())
        .allSatisfy(e -> assertThat(e.estado()).isEqualTo(EstadoConferencia.PENDENTE));
    assertThat(livro.efeitos())
        .filteredOn(e -> e.id().equals("aj"))
        .singleElement()
        .satisfies(e -> assertThat(e.calculo()).isNull());
    assertThat(livro.efeitos())
        .filteredOn(e -> e.id().equals("b"))
        .singleElement()
        .satisfies(e -> assertThat(e.calculo().resultadoTotalBrl()).isNull());
  }

  @Test
  void propostasSomamDeficitMinimoSemInflarASegundaSaida() {
    var livro =
        MotorLivro.conferir(
            List.of(
                venda("a", 1, "2", "800"),
                entrada("b", 1, 2, "1", null),
                venda("c", 1, "3", "1200")),
            CODIGOS);
    assertThat(livro.lacunas())
        .extracting(LacunaLivro::quantidade)
        .usingComparatorForType(BigDecimal::compareTo, BigDecimal.class)
        .containsExactly(decimal("2"), decimal("2"));
  }

  @Test
  void zerarBaseDesconhecidaPermiteNovaBaseConhecida() {
    var livro =
        MotorLivro.conferir(
            List.of(
                entrada("a", 1, 2, "1", null),
                venda("b", 1, "1", "400"),
                entrada("c", 1, 2, "2", "600")),
            CODIGOS);
    assertThat(livro.posicoes())
        .filteredOn(p -> p.ativoId().equals(2L))
        .singleElement()
        .satisfies(p -> {
          assertThat(p.custoTotalBrl()).isEqualByComparingTo("600");
          assertThat(p.custoMedioBrl()).isEqualByComparingTo("300");
        });
    assertThat(livro.efeitos().get(1).calculo().resultadoTotalBrl()).isNull();
  }

  @Test
  void transferenciaConfirmadaPreservaCustoGlobalSemReceitaOuLucro() {
    var saida =
        new ItemConferenciaLivro(
            "b", 2L, 1L, 2L, DATA.plusSeconds(1), 1, decimal("-2"), null, null, false, null, "t");
    var entrada =
        new ItemConferenciaLivro(
            "c", 1L, 1L, 2L, DATA.plusSeconds(2), 2, decimal("2"), null, null, false, null, "t");
    var livro =
        MotorLivro.conferir(
            List.of(entrada("a", 2, 2, "5", "500"), saida, entrada, venda("d", 1, "1", "400")),
            CODIGOS);
    assertThat(livro.lacunas()).isEmpty();
    assertThat(livro.efeitos().get(3).calculo().resultadoTotalBrl()).isEqualByComparingTo("300");
    assertThat(livro.posicoes())
        .filteredOn(p -> p.ativoId().equals(2L))
        .singleElement()
        .satisfies(p -> assertThat(p.custoTotalBrl()).isEqualByComparingTo("400"));
    assertThat(livro.efeitos().get(1).calculo()).isNull();
  }

  @Test
  void receitaConhecidaNaoHerdaPendenciaDoCustoDaVenda() {
    var livro =
        MotorLivro.conferir(
            List.of(entrada("a", 1, 2, "1", null), venda("b", 1, "1", "400")), CODIGOS);
    assertThat(livro.posicoes())
        .singleElement()
        .satisfies(p -> {
          assertThat(p.ativoId()).isEqualTo(1L);
          assertThat(p.causas()).isEmpty();
          assertThat(p.custoTotalBrl()).isEqualByComparingTo("400");
        });
  }

  @Test
  void ajusteSemAncoraExistenteNaoDesapareceDaConferencia() {
    var ajuste =
        new ItemConferenciaLivro(
            "aj", 1L, 1L, 2L, null, 0, decimal("2"), null, null, true, "inexistente", null);
    var livro = MotorLivro.conferir(List.of(ajuste), CODIGOS);
    assertThat(livro.efeitos())
        .singleElement()
        .satisfies(e -> {
          assertThat(e.id()).isEqualTo("aj");
          assertThat(e.estado()).isEqualTo(EstadoConferencia.CORRECAO_NECESSARIA);
        });
    assertThat(livro.posicoes()).isEmpty();
  }

  @Test
  void transferenciaDesbalanceadaNaoProduzEfeitoParcial() {
    var saida =
        new ItemConferenciaLivro(
            "b", 2L, 1L, 2L, DATA.plusSeconds(1), 1, decimal("-2"), null, null, false, null, "t");
    var entrada =
        new ItemConferenciaLivro(
            "c", 1L, 1L, 2L, DATA.plusSeconds(2), 2, decimal("1"), null, null, false, null, "t");
    var livro =
        MotorLivro.conferir(List.of(entrada("a", 2, 2, "5", "500"), saida, entrada), CODIGOS);
    assertThat(livro.efeitos().subList(1, 3))
        .allSatisfy(e -> assertThat(e.estado()).isEqualTo(EstadoConferencia.CORRECAO_NECESSARIA));
    assertThat(livro.posicoes())
        .singleElement()
        .satisfies(p -> {
          assertThat(p.quantidade()).isEqualByComparingTo("5");
          assertThat(p.custoTotalBrl()).isEqualByComparingTo("500");
        });
  }

  @Test
  void transferenciaComOrigemDeficitariaNaoFabricaCustoConhecidoNaContaDestino() {
    var saida =
        new ItemConferenciaLivro(
            "b", 2L, 1L, 2L, DATA.plusSeconds(1), 1, decimal("-2"), null, null, false, null, "t");
    var entrada =
        new ItemConferenciaLivro(
            "c", 1L, 1L, 2L, DATA.plusSeconds(2), 2, decimal("2"), null, null, false, null, "t");
    var livro =
        MotorLivro.conferir(
            List.of(entrada("a", 3, 2, "5", "500"), saida, entrada, venda("d", 1, "1", "400")),
            CODIGOS);
    assertThat(livro.efeitos().get(3).calculo().custoBaixadoBrl()).isNull();
    assertThat(livro.efeitos().get(3).calculo().resultadoTotalBrl()).isNull();
  }

  @Test
  void custoZeroDeclaradoContinuaConhecido() {
    var livro =
        MotorLivro.conferir(
            List.of(entrada("a", 1, 2, "2", "0"), venda("b", 1, "1", "400")), CODIGOS);
    assertThat(livro.efeitos().get(1).calculo().resultadoTotalBrl()).isEqualByComparingTo("400");
    assertThat(livro.posicoes())
        .filteredOn(p -> p.ativoId().equals(2L))
        .singleElement()
        .satisfies(p -> assertThat(p.custoTotalBrl()).isZero());
  }

  @Test
  void ajusteBrlPermiteCompraComCustoNominalSemSeTornarAporte() {
    var compra =
        new FatoLivro(
            1L,
            TipoOperacao.COMPRA,
            1L,
            2L,
            DATA,
            1,
            decimal("2"),
            null,
            1L,
            decimal("600"),
            null,
            null,
            null);
    var item =
        new ItemConferenciaLivro(
            "b", 1L, 1L, 2L, DATA, 1, decimal("2"), null, compra, false, null, null);
    var ajuste =
        new ItemConferenciaLivro(
            "aj", 1L, 1L, 1L, null, 0, decimal("600"), null, null, true, "b", null);
    var livro = MotorLivro.conferir(List.of(item, ajuste), CODIGOS);
    assertThat(livro.lacunas()).isEmpty();
    assertThat(livro.efeitos().get(0).calculo()).isNull();
    assertThat(livro.efeitos().get(1).calculo().resultadoTotalBrl()).isZero();
    assertThat(livro.posicoes())
        .singleElement()
        .satisfies(p -> {
          assertThat(p.custoTotalBrl()).isEqualByComparingTo("600");
          assertThat(p.custoMedioBrl()).isEqualByComparingTo("300");
          assertThat(p.causas()).contains("1:1:b");
        });
  }

  @Test
  void historicoCompletoMantemAsMesmasParcelasDecimaisDoMotorEstrito() {
    var aporte =
        new FatoLivro(
            1L,
            TipoOperacao.APORTE,
            1L,
            1L,
            DATA,
            1,
            decimal("1000"),
            null,
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
            DATA,
            2,
            decimal("2"),
            null,
            1L,
            decimal("600"),
            null,
            1L,
            decimal("10"));
    var venda =
        new FatoLivro(
            3L,
            TipoOperacao.VENDA,
            1L,
            2L,
            DATA,
            3,
            decimal("0.3"),
            null,
            1L,
            decimal("120"),
            null,
            2L,
            decimal("0.01"));
    var fatos = List.of(aporte, compra, venda);
    var itens =
        fatos.stream()
            .map(f ->
                new ItemConferenciaLivro(
                    "op:" + f.id(),
                    1L,
                    1L,
                    f.ativoId(),
                    f.ocorridaEm(),
                    f.ordemLivro(),
                    f.quantidade(),
                    f.custoTotalBrl(),
                    f,
                    false,
                    null,
                    null))
            .toList();
    var estrito = MotorLivro.calcular(fatos, CODIGOS);
    var conferencia = MotorLivro.conferir(itens, CODIGOS);
    assertThat(conferencia.efeitos())
        .extracting(e -> e.calculo())
        .containsExactlyElementsOf(estrito.calculos());
    assertThat(conferencia.lacunas()).isEmpty();
    conferencia
        .posicoes()
        .forEach(p ->
            assertThat(estrito.posicoes())
                .filteredOn(e -> e.ativoId().equals(p.ativoId()))
                .singleElement()
                .satisfies(e -> {
                  assertThat(p.quantidade()).isEqualTo(e.quantidade());
                  assertThat(p.custoTotalBrl()).isEqualTo(e.custoTotalBrl());
                  assertThat(p.custoMedioBrl()).isEqualTo(e.custoMedioBrl());
                }));
  }

  @Test
  void entradaComCustoConhecidoNaoDependeDoCustoDeEntradasAnteriores() {
    var livro =
        MotorLivro.conferir(
            List.of(entrada("a", 1, 2, "1", null), entrada("b", 1, 2, "1", "300")), CODIGOS);
    assertThat(livro.efeitos().get(1).estado()).isEqualTo(EstadoConferencia.COERENTE);
    assertThat(livro.efeitos().get(1).causas()).isEmpty();
    assertThat(livro.posicoes())
        .singleElement()
        .satisfies(p -> assertThat(p.custoTotalBrl()).isNull());
  }

  @Test
  void dezMilEntradasSemCustoNaoCriamDependenciasEntreEntradasIndependentes() {
    var itens =
        java.util.stream.IntStream.range(0, 10000)
            .mapToObj(i ->
                new ItemConferenciaLivro(
                    "r" + i,
                    1L,
                    1L,
                    2L,
                    DATA.plusSeconds(i),
                    i,
                    BigDecimal.ONE,
                    null,
                    null,
                    false,
                    null,
                    null))
            .toList();
    var livro = MotorLivro.conferir(itens, CODIGOS);
    assertThat(livro.efeitos()).hasSize(10000);
    assertThat(livro.efeitos().stream().mapToInt(e -> e.causas().size()).max().orElse(0))
        .isEqualTo(1);
    assertThat(livro.posicoes())
        .singleElement()
        .satisfies(p -> {
          assertThat(p.quantidade()).isEqualByComparingTo("10000");
          assertThat(p.custoTotalBrl()).isNull();
        });
  }

  private static final Instant DATA = Instant.parse("2026-11-01T00:00:00Z");
  private static final Map<Long, String> CODIGOS = Map.of(1L, "fiat:BRL", 2L, "cripto:BTC");

  private static ItemConferenciaLivro entrada(
      String id, long conta, long ativo, String quantidade, String custo) {
    return new ItemConferenciaLivro(
        id,
        conta,
        1L,
        ativo,
        DATA.plusSeconds(id.charAt(0) - 'a'),
        id.charAt(0),
        decimal(quantidade),
        decimal(custo),
        null,
        false,
        null,
        null);
  }

  private static ItemConferenciaLivro venda(
      String id, long conta, String quantidade, String total) {
    var fato =
        new FatoLivro(
            (long) id.charAt(0),
            TipoOperacao.VENDA,
            1L,
            2L,
            DATA.plusSeconds(id.charAt(0) - 'a'),
            id.charAt(0),
            decimal(quantidade),
            null,
            1L,
            decimal(total),
            null,
            null,
            null);
    return new ItemConferenciaLivro(
        id,
        conta,
        1L,
        2L,
        fato.ocorridaEm(),
        fato.ordemLivro(),
        fato.quantidade(),
        null,
        fato,
        false,
        null,
        null);
  }

  private static BigDecimal decimal(String valor) {
    return valor == null ? null : new BigDecimal(valor);
  }
}
