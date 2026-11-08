package io.github.jaimenafer.lastro.operacoes;

import static org.assertj.core.api.Assertions.*;

import io.github.jaimenafer.lastro.operacoes.domain.exception.OperacaoInvalidaException;
import io.github.jaimenafer.lastro.operacoes.domain.model.*;
import io.github.jaimenafer.lastro.operacoes.domain.service.AgrupamentoNegociacoes;
import io.github.jaimenafer.lastro.operacoes.domain.service.MotorLivro;
import io.github.jaimenafer.lastro.operacoes.domain.service.ProjecaoGrupo;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

class AgrupamentoNegociacoesTests {
  private LinhaAssociacao linha(
      long id, String codigo, String quantidade, String ref, long nanos, PapelLinha papel) {
    return new LinhaAssociacao(
        id,
        1,
        1L,
        1L,
        codigo.equals("fiat:BRL") ? 1L : codigo.equals("cripto:USDT") ? 4L : 2L,
        codigo,
        codigo,
        Instant.parse("2024-05-26T22:05:32Z").plusNanos(nanos),
        new BigDecimal(quantidade),
        "Buy Crypto With Fiat",
        ref,
        papel);
  }

  private LinhaAssociacao conversao(long id, long ativo, String codigo, String quantidade,
      String ref, long nanos, PapelLinha papel) {
    return new LinhaAssociacao(id, 1, 1L, 1L, ativo, codigo, codigo,
        Instant.parse("2024-06-11T10:54:00Z").plusNanos(nanos),
        new BigDecimal(quantidade), "Binance Convert", ref, papel);
  }

  @Test
  void reconheceConversaoCriptoSemInventarValorOuPendenciaDeAssociacao() {
    var saida = conversao(1, 20L, "cripto:BNB", "-0.14536437", "", 0, PapelLinha.PAGAMENTO);
    var entrada = conversao(2, 21L, "cripto:SOL", "0.57296862", "", 2000000000L, PapelLinha.RECEBIMENTO);
    var grupos = AgrupamentoNegociacoes.automaticos(List.of(saida, entrada), Duration.ofSeconds(2));
    assertThat(grupos).hasSize(1);
    var resumo = AgrupamentoNegociacoes.resumir(grupos.getFirst(), false, null);
    assertThat(resumo.natureza().name()).isEqualTo("CONVERSAO");
    assertThat(resumo.principalBrl()).isNull();
    assertThat(resumo.pendencias()).isEmpty();
    assertThat(resumo.completo()).isFalse();
  }

  @Test
  void conversaoNaoEscolheParQuandoOutraLinhaTambemPodeSerContrapartida() {
    var saida = conversao(1, 20L, "cripto:BNB", "-1", "", 0, PapelLinha.PAGAMENTO);
    var entrada = conversao(2, 21L, "cripto:SOL", "2", "", 2000000000L, PapelLinha.RECEBIMENTO);
    var outraSaida = conversao(3, 20L, "cripto:BNB", "-3", "", 3000000000L, PapelLinha.PAGAMENTO);
    assertThat(AgrupamentoNegociacoes.automaticos(List.of(saida, entrada, outraSaida), Duration.ofSeconds(2)))
        .isEmpty();
  }

  @Test
  void conversoesNoMesmoInstanteComReferenciasFortesDistintasPermanecemSeparadas() {
    var linhas = List.of(
        conversao(1, 20L, "cripto:BNB", "-1", "order/alpha", 0, PapelLinha.PAGAMENTO),
        conversao(2, 21L, "cripto:SOL", "2", "order/alpha", 0, PapelLinha.RECEBIMENTO),
        conversao(3, 20L, "cripto:BNB", "-3", "order/beta", 0, PapelLinha.PAGAMENTO),
        conversao(4, 21L, "cripto:SOL", "4", "order/beta", 0, PapelLinha.RECEBIMENTO));
    assertThat(AgrupamentoNegociacoes.automaticos(linhas, Duration.ofSeconds(2)))
        .containsExactly(List.of(linhas.get(0), linhas.get(1)), List.of(linhas.get(2), linhas.get(3)));
  }

  @Test
  void multiplasExecucoesDosDoisLadosExigemReferenciaForteComum() {
    var linhas = List.of(
        conversao(1, 20L, "cripto:BNB", "-0.1", "order/alpha", 0, PapelLinha.PAGAMENTO),
        conversao(2, 20L, "cripto:BNB", "-0.2", "order/alpha", 0, PapelLinha.PAGAMENTO),
        conversao(3, 21L, "cripto:SOL", "0.5", "order/alpha", 0, PapelLinha.RECEBIMENTO),
        conversao(4, 21L, "cripto:SOL", "0.8", "order/alpha", 0, PapelLinha.RECEBIMENTO));
    assertThat(AgrupamentoNegociacoes.automaticos(linhas, Duration.ofSeconds(2))).hasSize(1);
    var resumo = AgrupamentoNegociacoes.resumir(linhas, false, null);
    assertThat(resumo.pagamentos().getFirst().quantidade()).isEqualByComparingTo("0.3");
    assertThat(resumo.recebimentos().getFirst().quantidade()).isEqualByComparingTo("1.3");
    var semReferencia = linhas.stream().map(l -> new LinhaAssociacao(l.registroId(), l.versao(),
        l.instituicaoId(), l.contaId(), l.ativoId(), l.codigo(), l.ticker(), l.data(),
        l.quantidade(), l.operacao(), "12345", l.papel())).toList();
    assertThat(AgrupamentoNegociacoes.automaticos(semReferencia, Duration.ofSeconds(2))).isEmpty();
  }

  @Test
  void taxaNoAtivoRecebidoNaoCriaDeficitQuandoLinhaDeTaxaVemPrimeiro() {
    var taxa = new LinhaAssociacao(1, 1, 1L, 1L, 21L, "cripto:SOL", "SOL",
        Instant.parse("2024-06-11T10:54:00Z"), new BigDecimal("-0.01"),
        "Transaction Fee", "order/abc", PapelLinha.TAXA);
    var saida = conversao(2, 20L, "cripto:BNB", "-1", "order/abc", 0, PapelLinha.PAGAMENTO);
    var entrada = conversao(3, 21L, "cripto:SOL", "2", "order/abc", 0, PapelLinha.RECEBIMENTO);
    var linhas = List.of(taxa, saida, entrada);
    var resumo = AgrupamentoNegociacoes.resumir(linhas, false, null);
    var inicial = new ItemConferenciaLivro("inicial", 1L, 1L, 20L, saida.data().minusSeconds(1),
        0, BigDecimal.ONE, new BigDecimal("1000"), null, false, null, null);
    var itens = new ArrayList<ItemConferenciaLivro>();
    itens.add(inicial);
    itens.addAll(ProjecaoGrupo.projetar("grupo", new PropostaGrupo(linhas, false, null, false), resumo));
    var livro = MotorLivro.conferir(itens, Map.of(20L, "cripto:BNB", 21L, "cripto:SOL"));
    assertThat(livro.lacunas()).isEmpty();
    assertThat(livro.posicoes()).filteredOn(p -> p.ativoId().equals(21L)).singleElement()
        .satisfies(p -> assertThat(p.quantidade()).isEqualByComparingTo("1.99"));
  }

  @Test
  void conversaoComExecucoesParciaisBaixaCustoUmaVezSobreQuantidadeTotal() {
    var saida1 = conversao(1, 20L, "cripto:BNB", "-1", "order/abc", 0, PapelLinha.PAGAMENTO);
    var saida2 = conversao(2, 20L, "cripto:BNB", "-1", "order/abc", 0, PapelLinha.PAGAMENTO);
    var entrada = conversao(3, 21L, "cripto:SOL", "2", "order/abc", 0, PapelLinha.RECEBIMENTO);
    var linhas = List.of(saida1, saida2, entrada);
    var resumo = AgrupamentoNegociacoes.resumir(linhas, false, null);
    var inic = new ItemConferenciaLivro("inicial", 1L, 1L, 20L, saida1.data().minusSeconds(1),
        0, new BigDecimal("3"), new BigDecimal("0.00000001"), null, false, null, null);
    var parcelas = ProjecaoGrupo.projetar("grupo", new PropostaGrupo(linhas, false, null, false), resumo);
    assertThat(parcelas).hasSize(2);
    var itens = new ArrayList<ItemConferenciaLivro>();
    itens.add(inic);
    itens.addAll(parcelas);
    var livro = MotorLivro.conferir(itens, Map.of(20L, "cripto:BNB", 21L, "cripto:SOL"));
    assertThat(livro.posicoes()).filteredOn(p -> p.ativoId().equals(20L)).singleElement()
        .satisfies(p -> {
          assertThat(p.quantidade()).isEqualByComparingTo("1");
          assertThat(p.custoTotalBrl()).isZero();
        });
  }

  @Test
  void conversaoComTaxaNoAtivoEnviadoNaoDuplicaDebito() {
    var saida = conversao(1, 20L, "cripto:BNB", "-0.5", "order/abc", 0, PapelLinha.PAGAMENTO);
    var entrada = conversao(2, 21L, "cripto:SOL", "2", "order/abc", 0, PapelLinha.RECEBIMENTO);
    var taxa = new LinhaAssociacao(3, 1, 1L, 1L, 20L, "cripto:BNB", "BNB", saida.data(),
        new BigDecimal("-0.01"), "Transaction Fee", "order/abc", PapelLinha.TAXA);
    var linhas = List.of(saida, entrada, taxa);
    var cotacao = new BigDecimal("2000");
    var resumo = AgrupamentoNegociacoes.resumir(linhas, false, cotacao);
    assertThat(resumo.completo()).isTrue();
    assertThat(AgrupamentoNegociacoes.custoFinal(resumo, cotacao)).isEqualByComparingTo("1020");
    var inicial = new ItemConferenciaLivro("inicial", 1L, 1L, 20L, saida.data().minusSeconds(1),
        0, BigDecimal.ONE, new BigDecimal("1000"), null, false, null, null);
    var fato = ProjecaoGrupo.projetar(
        "grupo", new PropostaGrupo(linhas, false, cotacao, false), resumo).getFirst();
    var livro = MotorLivro.conferir(
        List.of(inicial, fato), Map.of(20L, "cripto:BNB", 21L, "cripto:SOL"));
    assertThat(livro.posicoes()).filteredOn(p -> p.ativoId().equals(20L)).singleElement()
        .satisfies(p -> assertThat(p.quantidade()).isEqualByComparingTo("0.49"));
    assertThat(livro.posicoes()).filteredOn(p -> p.ativoId().equals(21L)).singleElement()
        .satisfies(p -> assertThat(p.custoTotalBrl()).isEqualByComparingTo("1020"));
    assertThat(livro.efeitos().get(1).calculo().resultadoTotalBrl()).isEqualByComparingTo("510");
  }

  @Test
  void conversaoCriptoComCotacaoDeclaradaTemValorEmReais() {
    var saida = conversao(1, 20L, "cripto:BNB", "-0.5", "ref/abc", 0, PapelLinha.PAGAMENTO);
    var entrada = conversao(2, 21L, "cripto:SOL", "2", "ref/abc", 0, PapelLinha.RECEBIMENTO);
    var resumo = AgrupamentoNegociacoes.resumir(List.of(saida, entrada), false, new BigDecimal("2000"));
    assertThat(resumo.natureza().name()).isEqualTo("CONVERSAO");
    assertThat(resumo.principalBrl()).isEqualByComparingTo("1000");
    assertThat(resumo.completo()).isTrue();
  }

  @Test
  void conversaoBrlParaBnbTemValorEconomicoConhecido() {
    var saida = conversao(1, 1L, "fiat:BRL", "-1000", "", 0, PapelLinha.PAGAMENTO);
    var entrada = conversao(2, 20L, "cripto:BNB", "0.27536352", "", 0, PapelLinha.RECEBIMENTO);
    var grupos = AgrupamentoNegociacoes.automaticos(List.of(saida, entrada), Duration.ofSeconds(2));
    assertThat(grupos).hasSize(1);
    var resumo = AgrupamentoNegociacoes.resumir(grupos.getFirst(), false, null);
    assertThat(resumo.natureza()).isEqualTo(NaturezaRegistro.COMPRA);
    assertThat(resumo.principalBrl()).isEqualByComparingTo("1000");
    assertThat(resumo.completo()).isTrue();
  }

  @Test
  void referenciasDistintasNoMesmoInstanteFormamOperacoesSeparadas() {
    var linhas = List.of(
        linha(1, "cripto:NEAR", "7.28", "Wallet/abc", 0, PapelLinha.RECEBIMENTO),
        linha(2, "fiat:BRL", "-300", "Wallet/abc", 0, PapelLinha.PAGAMENTO),
        linha(3, "cripto:SOL", "2", "Wallet/def", 0, PapelLinha.RECEBIMENTO),
        linha(4, "fiat:BRL", "-200", "Wallet/def", 0, PapelLinha.PAGAMENTO));
    assertThat(AgrupamentoNegociacoes.automaticos(linhas, Duration.ofSeconds(2)))
        .containsExactly(List.of(linhas.get(0), linhas.get(1)), List.of(linhas.get(2), linhas.get(3)));
  }

  @Test
  void taxaProximaSemReferenciaImpedePresumirZero() {
    var recebimento = linha(1, "cripto:SOL", "1", "Wallet/abc", 0, PapelLinha.RECEBIMENTO);
    var pagamento = linha(2, "fiat:BRL", "-100", "Wallet/abc", 0, PapelLinha.PAGAMENTO);
    var taxa = linha(3, "cripto:SOL", "-0.01", "", 0, PapelLinha.TAXA);
    assertThat(AgrupamentoNegociacoes.automaticos(List.of(recebimento, pagamento, taxa), Duration.ofSeconds(2)))
        .isEmpty();
  }

  @Test
  void reconheceCompraComTaxaNaoInformadaComoZero() {
    var linhas =
        List.of(
            linha(
                1,
                "cripto:SOL",
                "0.12345678",
                "Via CashBalance - Wallet/abc",
                0,
                PapelLinha.RECEBIMENTO),
            linha(2, "fiat:BRL", "-100", "Via CashBalance - Wallet/abc", 0, PapelLinha.PAGAMENTO));
    var r = AgrupamentoNegociacoes.resumir(linhas, false, null);
    assertThat(r.natureza()).isEqualTo(NaturezaRegistro.COMPRA);
    assertThat(r.principalBrl()).isEqualByComparingTo("100");
    assertThat(r.completo()).isTrue();
    assertThat(AgrupamentoNegociacoes.automaticos(linhas)).hasSize(1);
  }

  @Test
  void naoAgrupaDuasNegociacoesSemReferencia() {
    assertThat(
            AgrupamentoNegociacoes.automaticos(
                List.of(
                    linha(1, "cripto:SOL", "1", "", 0, PapelLinha.RECEBIMENTO),
                    linha(2, "fiat:BRL", "-100", "", 0, PapelLinha.PAGAMENTO),
                    linha(3, "cripto:SOL", "2", "", 0, PapelLinha.RECEBIMENTO),
                    linha(4, "fiat:BRL", "-200", "", 0, PapelLinha.PAGAMENTO))))
        .isEmpty();
  }

  @Test
  void aceitaDoisSegundosMasNaoUmNanoAlem() {
    var a = linha(1, "cripto:SOL", "1", "a", 0, PapelLinha.RECEBIMENTO);
    assertThatCode(() ->
            AgrupamentoNegociacoes.resumir(
                List.of(a, linha(2, "fiat:BRL", "-100", "a", 2000000000L, PapelLinha.PAGAMENTO)),
                false,
                null))
        .doesNotThrowAnyException();
    assertThatThrownBy(() ->
            AgrupamentoNegociacoes.resumir(
                List.of(a, linha(2, "fiat:BRL", "-100", "a", 2000000001L, PapelLinha.PAGAMENTO)),
                false,
                null))
        .isInstanceOf(OperacaoInvalidaException.class);
  }

  @Test
  void janelaConfiguradaTambemLimitaAssociacaoManual() {
    var recebimento = linha(1, "cripto:SOL", "1", "", 0, PapelLinha.RECEBIMENTO);
    var pagamento = linha(2, "fiat:BRL", "-100", "", 1500000000L, PapelLinha.PAGAMENTO);

    assertThatThrownBy(() -> AgrupamentoNegociacoes.resumir(
            List.of(recebimento, pagamento), true, null, Duration.ofSeconds(1)))
        .isInstanceOf(OperacaoInvalidaException.class)
        .hasMessageContaining("janela permitida");
    assertThatCode(() -> AgrupamentoNegociacoes.resumir(
            List.of(recebimento, pagamento), true, null, Duration.ofSeconds(2)))
        .doesNotThrowAnyException();
  }

  @Test
  void automaticoExigeInstanteExato() {
    assertThat(
            AgrupamentoNegociacoes.automaticos(
                List.of(
                    linha(1, "cripto:SOL", "1", "Wallet/abc", 0, PapelLinha.RECEBIMENTO),
                    linha(2, "fiat:BRL", "-100", "Wallet/abc", 1, PapelLinha.PAGAMENTO))))
        .isEmpty();
  }

  @Test
  void agrupaExecucoesParciaisDentroDaJanelaConfigurada() {
    var primeiro = linha(1, "cripto:SOL", "0.1", "Wallet/abc", 0, PapelLinha.RECEBIMENTO);
    var segundo = linha(2, "cripto:SOL", "0.02345678", "Wallet/abc", 1000000000L, PapelLinha.RECEBIMENTO);
    var pagamento = linha(3, "fiat:BRL", "-100", "Wallet/abc", 1000000000L, PapelLinha.PAGAMENTO);

    var grupos = AgrupamentoNegociacoes.automaticos(List.of(primeiro, segundo, pagamento), Duration.ofSeconds(2));

    assertThat(grupos).hasSize(1);
    assertThat(AgrupamentoNegociacoes.resumir(grupos.getFirst(), false, null).recebimentos())
        .singleElement()
        .extracting(ParcelaGrupo::quantidade)
        .isEqualTo(new BigDecimal("0.12345678"));
  }

  @Test
  void janelaAutomaticaUsaDistanciaEntreExtremosESemEncadeamento() {
    var linhas = List.of(
        linha(1, "cripto:SOL", "1", "Wallet/abc", 0, PapelLinha.RECEBIMENTO),
        linha(2, "fiat:BRL", "-100", "Wallet/abc", 2000000000L, PapelLinha.PAGAMENTO),
        linha(3, "cripto:SOL", "2", "Wallet/abc", 3000000000L, PapelLinha.RECEBIMENTO));

    assertThat(AgrupamentoNegociacoes.automaticos(linhas, Duration.ofSeconds(2)))
        .hasSize(1)
        .allSatisfy(grupo -> assertThat(grupo).hasSize(2));
  }

  @Test
  void agrupamentoAmbiguoNoMesmoIntervaloContinuaPendente() {
    var linhas = List.of(
        linha(1, "cripto:SOL", "1", "Wallet/abc", 0, PapelLinha.RECEBIMENTO),
        linha(2, "fiat:BRL", "-100", "Wallet/abc", 0, PapelLinha.PAGAMENTO),
        linha(3, "cripto:SOL", "2", "Wallet/abc", 1000000000L, PapelLinha.RECEBIMENTO),
        linha(4, "fiat:BRL", "-200", "Wallet/abc", 1000000000L, PapelLinha.PAGAMENTO));

    assertThat(AgrupamentoNegociacoes.automaticos(linhas, Duration.ofSeconds(2))).isEmpty();
  }

  @Test
  void associaTaxaNegativaNaMesmaJanelaSemAgruparIdentificadorRecorrenteComoProva() {
    var recebimento = linha(1, "cripto:SOL", "1", "", 0, PapelLinha.RECEBIMENTO);
    var pagamento = linha(2, "fiat:BRL", "-100", "", 1000000000L, PapelLinha.PAGAMENTO);
    var taxa = new LinhaAssociacao(
        3, 1, 1L, 1L, 3L, "cripto:BNB", "BNB", recebimento.data().plusNanos(1000000000L),
        new BigDecimal("-0.01"), "Transaction Fee", "", PapelLinha.TAXA);

    var grupos = AgrupamentoNegociacoes.automaticos(
        List.of(recebimento, pagamento, taxa), Duration.ofSeconds(2));

    assertThat(grupos).hasSize(1);
    assertThat(grupos.getFirst()).extracting(LinhaAssociacao::registroId).containsExactly(1L, 2L, 3L);
    assertThat(AgrupamentoNegociacoes.resumir(grupos.getFirst(), false, null).taxas())
        .singleElement()
        .extracting(ParcelaGrupo::ticker)
        .isEqualTo("BNB");
  }

  @Test
  void agregaTiposCompatíveisDeCompraSemUsarIdRepetidoComoChave() {
    var base = linha(1, "cripto:SOL", "1", "123456789", 0, PapelLinha.RECEBIMENTO);
    var recebimento = new LinhaAssociacao(
        base.registroId(), base.versao(), base.instituicaoId(), base.contaId(), base.ativoId(),
        base.codigo(), base.ticker(), base.data(), base.quantidade(), "Transaction Buy",
        base.referencia(), base.papel());
    var pagamento = new LinhaAssociacao(
        2, 1, 1L, 1L, 1L, "fiat:BRL", "BRL", recebimento.data().plusNanos(1000000000L),
        new BigDecimal("-100"), "Transaction Spend", "123456789", PapelLinha.PAGAMENTO);

    assertThat(AgrupamentoNegociacoes.automaticos(
            List.of(recebimento, pagamento), Duration.ofSeconds(2)))
        .hasSize(1);
    assertThat(AgrupamentoNegociacoes.referenciaForte("123456789")).isFalse();
  }

  @Test
  void rejeitaReferenciasConflitantesMesmoManualmente() {
    assertThatThrownBy(() ->
            AgrupamentoNegociacoes.resumir(
                List.of(
                    linha(1, "cripto:SOL", "1", "Wallet/abc", 0, PapelLinha.RECEBIMENTO),
                    linha(2, "fiat:BRL", "-100", "Wallet/def", 0, PapelLinha.PAGAMENTO)),
                false,
                null))
        .isInstanceOf(OperacaoInvalidaException.class);
  }

  @Test
  void taxaEmTerceiroAtivoPreservaParcelaSemCompletarCusto() {
    var a = linha(1, "cripto:SOL", "1", "Wallet/abc", 0, PapelLinha.RECEBIMENTO);
    var b = linha(2, "fiat:BRL", "-100", "Wallet/abc", 0, PapelLinha.PAGAMENTO);
    var taxa =
        new LinhaAssociacao(
            3,
            1,
            1L,
            1L,
            3L,
            "cripto:BNB",
            "BNB",
            a.data(),
            new BigDecimal("-0.01"),
            "Transaction Fee",
            "Wallet/abc",
            PapelLinha.TAXA);
    var resumo = AgrupamentoNegociacoes.resumir(List.of(a, b, taxa), false, null);
    assertThat(resumo.principalBrl()).isEqualByComparingTo("100");
    assertThat(resumo.completo()).isFalse();
    assertThat(resumo.taxas()).hasSize(1);
  }

  @Test
  void variasExecucoesComReferenciaSomamSemPerderMultiplicidade() {
    var linhas =
        List.of(
            linha(1, "cripto:SOL", "1", "Wallet/abc", 0, PapelLinha.RECEBIMENTO),
            linha(2, "fiat:BRL", "-100", "Wallet/abc", 0, PapelLinha.PAGAMENTO),
            linha(3, "cripto:SOL", "1", "Wallet/abc", 0, PapelLinha.RECEBIMENTO),
            linha(4, "fiat:BRL", "-100", "Wallet/abc", 0, PapelLinha.PAGAMENTO));
    assertThat(AgrupamentoNegociacoes.automaticos(linhas)).isEmpty();
    assertThat(
            AgrupamentoNegociacoes.resumir(linhas, true, null)
                .recebimentos()
                .getFirst()
                .quantidade())
        .isEqualByComparingTo("2");
  }

  @Test
  void contasDiferentesNaoSaoAssociadas() {
    var a = linha(1, "cripto:SOL", "1", "Wallet/abc", 0, PapelLinha.RECEBIMENTO);
    var b =
        new LinhaAssociacao(
            2,
            1,
            1L,
            2L,
            1L,
            "fiat:BRL",
            "BRL",
            a.data(),
            new BigDecimal("-100"),
            "Buy Crypto With Fiat",
            "Wallet/abc",
            PapelLinha.PAGAMENTO);
    assertThat(AgrupamentoNegociacoes.automaticos(List.of(a, b))).isEmpty();
    assertThatThrownBy(() -> AgrupamentoNegociacoes.resumir(List.of(a, b), false, null))
        .isInstanceOf(OperacaoInvalidaException.class);
  }

  @Test
  void taxaEmBrlUsaCotacaoNominalECompoeCustoUmaVez() {
    var a = linha(1, "cripto:SOL", "1", "Wallet/abc", 0, PapelLinha.RECEBIMENTO);
    var b = linha(2, "fiat:BRL", "-100", "Wallet/abc", 0, PapelLinha.PAGAMENTO);
    var c = linha(3, "fiat:BRL", "-10", "Wallet/abc", 0, PapelLinha.TAXA);
    var resumo = AgrupamentoNegociacoes.resumir(List.of(a, b, c), false, new BigDecimal("2"));
    assertThat(AgrupamentoNegociacoes.custoFinal(resumo, new BigDecimal("2")))
        .isEqualByComparingTo("110");
    assertThat(resumo.principalBrl()).isEqualByComparingTo("100");
  }

  @Test
  void taxaForaDoCatalogoPodeSerVinculadaMasNaoCalculada() {
    var a = linha(1, "cripto:SOL", "1", "Wallet/abc", 0, PapelLinha.RECEBIMENTO);
    var b = linha(2, "fiat:BRL", "-100", "Wallet/abc", 0, PapelLinha.PAGAMENTO);
    var c =
        new LinhaAssociacao(
            3,
            1,
            1L,
            1L,
            null,
            "",
            "BNB",
            a.data(),
            new BigDecimal("-0.01"),
            "Transaction Fee",
            "Wallet/abc",
            PapelLinha.TAXA);
    var resumo = AgrupamentoNegociacoes.resumir(List.of(a, b, c), false, null);
    assertThat(resumo.completo()).isFalse();
    assertThat(resumo.taxas().getFirst().ticker()).isEqualTo("BNB");
    assertThat(resumo.pendencias()).anyMatch(m -> m.contains("catálogo"));
  }

  @Test
  void naoAceitaLinhasDuplicadasOuDirecaoIncompativel() {
    var a = linha(1, "cripto:SOL", "1", "Wallet/abc", 0, PapelLinha.RECEBIMENTO);
    var b = linha(2, "fiat:BRL", "-100", "Wallet/abc", 0, PapelLinha.PAGAMENTO);
    assertThatThrownBy(() -> AgrupamentoNegociacoes.resumir(List.of(a), false, null))
        .isInstanceOf(OperacaoInvalidaException.class);
    assertThatThrownBy(() -> AgrupamentoNegociacoes.resumir(List.of(a, a), false, null))
        .isInstanceOf(OperacaoInvalidaException.class);
    assertThatThrownBy(() ->
            AgrupamentoNegociacoes.resumir(
                List.of(a, linha(2, "fiat:BRL", "-100", "Wallet/abc", 0, PapelLinha.RECEBIMENTO)),
                false,
                null))
        .isInstanceOf(OperacaoInvalidaException.class);
    assertThat(AgrupamentoNegociacoes.resumir(List.of(a, b), true, null).completo()).isTrue();
    assertThat(AgrupamentoNegociacoes.referenciaForte("Via CashBalance")).isFalse();
    assertThat(AgrupamentoNegociacoes.referenciaForte("Via CashBalance - Wallet/")).isFalse();
  }

  @Test
  void parComStablecoinExigeCotacaoHistorica() {
    var a = linha(1, "cripto:SOL", "1", "Wallet/abc", 0, PapelLinha.RECEBIMENTO);
    var b = linha(2, "cripto:USDT", "-100", "Wallet/abc", 0, PapelLinha.PAGAMENTO);
    assertThat(AgrupamentoNegociacoes.resumir(List.of(a, b), true, null).completo()).isFalse();
    assertThat(
            AgrupamentoNegociacoes.resumir(List.of(a, b), true, new BigDecimal("5")).principalBrl())
        .isEqualByComparingTo("500");
  }

  @Test
  void vendaExigeTiposOriginaisCompativeis() {
    var compra = linha(1, "cripto:SOL", "-1", "Wallet/venda", 0, PapelLinha.PAGAMENTO);
    var receita = linha(2, "fiat:BRL", "200", "Wallet/venda", 0, PapelLinha.RECEBIMENTO);
    assertThat(AgrupamentoNegociacoes.automaticos(List.of(compra, receita))).isEmpty();
    var venda =
        new LinhaAssociacao(
            compra.registroId(),
            compra.versao(),
            compra.instituicaoId(),
            compra.contaId(),
            compra.ativoId(),
            compra.codigo(),
            compra.ticker(),
            compra.data(),
            compra.quantidade(),
            "Transaction Sold",
            compra.referencia(),
            compra.papel());
    var recebimento =
        new LinhaAssociacao(
            receita.registroId(),
            receita.versao(),
            receita.instituicaoId(),
            receita.contaId(),
            receita.ativoId(),
            receita.codigo(),
            receita.ticker(),
            receita.data(),
            receita.quantidade(),
            "Transaction Revenue",
            receita.referencia(),
            receita.papel());
    var resumo = AgrupamentoNegociacoes.resumir(List.of(venda, recebimento), true, null);
    assertThat(resumo.natureza()).isEqualTo(NaturezaRegistro.VENDA);
    var proposta = new PropostaGrupo(List.of(venda, recebimento), true, null, false);
    assertThat(
            ProjecaoGrupo.projetar(
                    "grupo:teste", proposta, resumo)
                .getFirst()
                .operacao()
                .tipo())
        .isEqualTo(TipoOperacao.VENDA);
    assertThat(AgrupamentoNegociacoes.custoFinal(resumo, null)).isNull();
  }

  @Test
  void dadosEssenciaisAusentesENulosNaoSaoAssociados() {
    var a = linha(1, "cripto:SOL", "1", "Wallet/abc", 0, PapelLinha.RECEBIMENTO);
    List<LinhaAssociacao> invalidas =
        List.of(
            new LinhaAssociacao(
                2,
                1,
                1L,
                1L,
                1L,
                "fiat:BRL",
                "BRL",
                null,
                new BigDecimal("-100"),
                "Buy Crypto With Fiat",
                "Wallet/abc",
                PapelLinha.PAGAMENTO),
            new LinhaAssociacao(
                2,
                1,
                1L,
                1L,
                1L,
                "fiat:BRL",
                "BRL",
                a.data(),
                null,
                "Buy Crypto With Fiat",
                "Wallet/abc",
                PapelLinha.PAGAMENTO),
            new LinhaAssociacao(
                2,
                1,
                1L,
                null,
                1L,
                "fiat:BRL",
                "BRL",
                a.data(),
                new BigDecimal("-100"),
                "Buy Crypto With Fiat",
                "Wallet/abc",
                PapelLinha.PAGAMENTO),
            linha(2, "fiat:BRL", "0", "Wallet/abc", 0, PapelLinha.PAGAMENTO),
            linha(2, "fiat:BRL", "-100", "Wallet/abc", 0, null),
            linha(2, "fiat:BRL", "100", "Wallet/abc", 0, PapelLinha.PAGAMENTO),
            linha(2, "fiat:BRL", "100", "Wallet/abc", 0, PapelLinha.TAXA));
    for (var invalida : invalidas)
      assertThatThrownBy(() -> AgrupamentoNegociacoes.resumir(List.of(a, invalida), false, null))
          .isInstanceOf(OperacaoInvalidaException.class);
    assertThat(AgrupamentoNegociacoes.referenciaForte(null)).isFalse();
    assertThat(AgrupamentoNegociacoes.referenciaForte(" ")).isFalse();
    var outraInstituicao =
        new LinhaAssociacao(
            2,
            1,
            2L,
            1L,
            1L,
            "fiat:BRL",
            "BRL",
            a.data(),
            new BigDecimal("-100"),
            "Buy Crypto With Fiat",
            "Wallet/abc",
            PapelLinha.PAGAMENTO);
    assertThatThrownBy(
            () -> AgrupamentoNegociacoes.resumir(List.of(a, outraInstituicao), false, null))
        .isInstanceOf(OperacaoInvalidaException.class);
  }

  @Test
  void papeisIncompletosEParesNaoSuportadosPermanecemPendentes() {
    var a = linha(1, "cripto:SOL", "1", "Wallet/abc", 0, PapelLinha.RECEBIMENTO);
    var pendente = linha(2, "fiat:BRL", "-100", "Wallet/abc", 0, PapelLinha.NAO_DEFINIDO);
    assertThat(AgrupamentoNegociacoes.resumir(List.of(a, pendente), false, null).completo())
        .isFalse();
    assertThat(AgrupamentoNegociacoes.automaticos(List.of(a, pendente))).isEmpty();
    var fiat = linha(1, "fiat:USD", "1", "Wallet/abc", 0, PapelLinha.RECEBIMENTO);
    var pagamento = linha(2, "fiat:BRL", "-100", "Wallet/abc", 0, PapelLinha.PAGAMENTO);
    assertThat(AgrupamentoNegociacoes.resumir(List.of(fiat, pagamento), true, null).natureza())
        .isNull();
    assertThat(
            AgrupamentoNegociacoes.resumir(
                    List.of(
                        a, linha(2, "cripto:ETH", "-100", "Wallet/abc", 0, PapelLinha.PAGAMENTO)),
                    true,
                    null)
                .natureza())
        .isNull();
    assertThat(
            AgrupamentoNegociacoes.resumir(
                    List.of(
                        pagamento,
                        linha(3, "fiat:BRL", "-100", "Wallet/abc", 0, PapelLinha.PAGAMENTO)),
                    true,
                    null)
                .natureza())
        .isNull();
  }

  @Test
  void variasMoedasDeTaxaNaoViraramTaxaUnica() {
    var a = linha(1, "cripto:SOL", "1", "Wallet/abc", 0, PapelLinha.RECEBIMENTO);
    var b = linha(2, "fiat:BRL", "-100", "Wallet/abc", 0, PapelLinha.PAGAMENTO);
    var taxa = linha(3, "cripto:SOL", "-0.01", "Wallet/abc", 0, PapelLinha.TAXA);
    assertThat(AgrupamentoNegociacoes.resumir(List.of(a, b, taxa), true, null).taxas())
        .hasSize(1);
    assertThat(
            AgrupamentoNegociacoes.resumir(
                    List.of(
                        a, b, taxa, linha(4, "fiat:BRL", "-1", "Wallet/abc", 0, PapelLinha.TAXA)),
                    false,
                    null)
                .completo())
        .isFalse();
    assertThat(
            AgrupamentoNegociacoes.automaticos(
                List.of(a, b, linha(3, "cripto:SOL", "0.01", "Wallet/abc", 0, PapelLinha.TAXA))))
        .isEmpty();
    assertThat(
            AgrupamentoNegociacoes.automaticos(
                List.of(a, linha(2, "fiat:BRL", "0", "Wallet/abc", 0, PapelLinha.PAGAMENTO))))
        .isEmpty();
  }

  @Test
  void taxaNaStablecoinUsaCotacaoHistoricaSemArredondamentoPrematuro() {
    var a = linha(1, "cripto:SOL", "1", "Wallet/abc", 0, PapelLinha.RECEBIMENTO);
    var b = linha(2, "cripto:USDT", "-100", "Wallet/abc", 0, PapelLinha.PAGAMENTO);
    var taxa = linha(3, "cripto:USDT", "-1", "Wallet/abc", 0, PapelLinha.TAXA);
    var resumo =
        AgrupamentoNegociacoes.resumir(
            List.of(a, b, taxa), false, new BigDecimal("5.123456789123456789"));
    assertThat(AgrupamentoNegociacoes.custoFinal(resumo, new BigDecimal("5.123456789123456789")))
        .isEqualByComparingTo("517.46913570");
    assertThat(AgrupamentoNegociacoes.resumir(List.of(a, b), true, BigDecimal.ZERO).completo())
        .isFalse();
  }

  @Test
  void naturezaOriginalConflitanteNaoEEscolhidaPorDirecao() {
    var a = linha(1, "cripto:SOL", "1", "Wallet/abc", 0, PapelLinha.RECEBIMENTO);
    var b = linha(2, "fiat:BRL", "-100", "Wallet/abc", 0, PapelLinha.PAGAMENTO);
    var conflito =
        new LinhaAssociacao(
            b.registroId(),
            b.versao(),
            b.instituicaoId(),
            b.contaId(),
            b.ativoId(),
            b.codigo(),
            b.ticker(),
            b.data(),
            b.quantidade(),
            "Transaction Sold",
            b.referencia(),
            b.papel());
    assertThat(AgrupamentoNegociacoes.resumir(List.of(a, conflito), true, null).natureza())
        .isNull();
    assertThat(AgrupamentoNegociacoes.automaticos(List.of(a, conflito))).isEmpty();
    var desconhecido =
        new LinhaAssociacao(
            a.registroId(),
            a.versao(),
            a.instituicaoId(),
            a.contaId(),
            a.ativoId(),
            a.codigo(),
            a.ticker(),
            a.data(),
            a.quantidade(),
            "Unknown",
            a.referencia(),
            PapelLinha.NAO_DEFINIDO);
    assertThat(AgrupamentoNegociacoes.automaticos(List.of(desconhecido, b))).isEmpty();
  }

  @Test
  void custoDaPreviaRespeitaArredondamentoIndividualDoPrincipalEDaTaxa() {
    var a = linha(1, "cripto:SOL", "1", "Wallet/abc", 0, PapelLinha.RECEBIMENTO);
    var b = linha(2, "cripto:USDT", "-0.00000002", "Wallet/abc", 0, PapelLinha.PAGAMENTO);
    var taxa = linha(3, "cripto:USDT", "-0.00000001", "Wallet/abc", 0, PapelLinha.TAXA);
    var resumo = AgrupamentoNegociacoes.resumir(List.of(a, b, taxa), false, new BigDecimal("0.5"));
    assertThat(AgrupamentoNegociacoes.custoFinal(resumo, new BigDecimal("0.5")))
        .isEqualByComparingTo("0.00000001");
  }
}
