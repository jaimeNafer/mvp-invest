package io.github.jaimenafer.lastro.operacoes;

import static org.assertj.core.api.Assertions.assertThat;

import io.github.jaimenafer.lastro.ativos.application.contract.AtivoResumo;
import io.github.jaimenafer.lastro.instituicoes.application.contract.InstituicaoResumo;
import io.github.jaimenafer.lastro.operacoes.application.dto.ResumoConferenciaResponse;
import io.github.jaimenafer.lastro.operacoes.application.dto.RetratoConferencia;
import io.github.jaimenafer.lastro.operacoes.application.mapper.VisaoGeralConferenciaMapper;
import io.github.jaimenafer.lastro.operacoes.application.mapper.VisaoGeralMapper;
import io.github.jaimenafer.lastro.operacoes.domain.model.FatoLivro;
import io.github.jaimenafer.lastro.operacoes.domain.model.ItemConferenciaLivro;
import io.github.jaimenafer.lastro.operacoes.domain.model.PeriodoVisaoGeral;
import io.github.jaimenafer.lastro.operacoes.domain.model.PosicaoLivro;
import io.github.jaimenafer.lastro.operacoes.domain.model.ResultadoLivro;
import io.github.jaimenafer.lastro.operacoes.domain.model.ResumoVisaoGeral;
import io.github.jaimenafer.lastro.operacoes.domain.model.TipoOperacao;
import io.github.jaimenafer.lastro.operacoes.domain.service.MotorLivro;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

class ProjecaoConferenciaTests {
  @Test
  void pendenciaDatadaEmFevereiroPreservaJaneiroCalculavel() {
    var retrato =
        new RetratoConferencia(
            List.of(),
            new io.github.jaimenafer.lastro.operacoes.domain.model.ResultadoConferenciaLivro(
                List.of(), List.of(), List.of()),
            new ResumoConferenciaResponse(
                1, 0, 1, 0, 0, "0", 1, 0, List.of("BTC"), "Pendente em fevereiro", 0, 1, 0, 0, 0),
            Map.of(),
            Map.of(),
            false,
            false,
            LocalDate.of(2024, 2, 1));
    var inicio = LocalDate.of(2024, 1, 1);
    var resultado =
        VisaoGeralConferenciaMapper.de(
            1, inicio, new PeriodoVisaoGeral(inicio, LocalDate.of(2024, 2, 29)), retrato, 0, 0);
    assertThat(resultado.evolucaoResultado().meses().get(0).resultadoBrl()).isEqualTo("0");
    assertThat(resultado.evolucaoResultado().meses().get(0).acumuladoBrl()).isEqualTo("0");
    assertThat(resultado.evolucaoResultado().meses().get(1).resultadoBrl()).isNull();
  }

  @Test
  void registroSemDataNaoEquivaleACarteiraVazia() {
    var retrato =
        new RetratoConferencia(
            List.of(),
            new io.github.jaimenafer.lastro.operacoes.domain.model.ResultadoConferenciaLivro(
                List.of(), List.of(), List.of()),
            new ResumoConferenciaResponse(
                1, 0, 0, 1, 0, "0", 1, 0, List.of("BRL"), "Data desconhecida", 0, 1, 0, 0, 0),
            Map.of(),
            Map.of(),
            false,
            true,
            null);
    var data = LocalDate.of(2024, 5, 1);
    var resultado =
        VisaoGeralConferenciaMapper.de(1, data, new PeriodoVisaoGeral(data, data), retrato, 0, 0);
    assertThat(resultado.possuiRegistrosAteFim()).isTrue();
    assertThat(resultado.resumo().custoSaldosBrl()).isNull();
    assertThat(resultado.resumo().resultadoRealizadoBrl()).isNull();
    assertThat(resultado.disponibilidade().quantidadesCompletas()).isFalse();
    assertThat(resultado.disponibilidade().subtotalCustoBrl()).isEqualTo("0");
    assertThat(resultado.evolucaoResultado().meses())
        .singleElement()
        .satisfies(m -> assertThat(m.resultadoBrl()).isNull());
  }

  @Test
  void projetaQuantidadeEReceitaSemApresentarSubtotalComoResultadoCompleto() {
    var instante = Instant.parse("2026-11-01T12:00:00Z");
    var entrada =
        new ItemConferenciaLivro(
            "r:1", 1L, 1L, 2L, instante, 1, new BigDecimal("2"), null, null, false, null, null);
    var venda =
        new FatoLivro(
            1L,
            TipoOperacao.VENDA,
            1L,
            2L,
            instante.plusSeconds(1),
            2,
            BigDecimal.ONE,
            null,
            1L,
            new BigDecimal("400"),
            null,
            null,
            null);
    var itemVenda =
        new ItemConferenciaLivro(
            "op:1",
            1L,
            1L,
            2L,
            venda.ocorridaEm(),
            2,
            BigDecimal.ONE,
            null,
            venda,
            false,
            null,
            null);
    var itens = List.of(entrada, itemVenda);
    var retrato =
        new RetratoConferencia(
            itens,
            MotorLivro.conferir(itens, Map.of(1L, "fiat:BRL", 2L, "cripto:BTC")),
            new ResumoConferenciaResponse(
                2, 0, 2, 0, 0, "0", 2, 1, List.of("BTC"), "Há custo pendente.", 0, 2, 0, 0, 0),
            Map.of(
                1L,
                new AtivoResumo(1L, "fiat:BRL", "BRL", "Real", 8, true, true, false),
                2L,
                new AtivoResumo(2L, "cripto:BTC", "BTC", "Bitcoin", 18, true, true, true)),
            Map.of(1L, new InstituicaoResumo(1L, "Binance", true)),
            true,
            false,
            null);
    var data = LocalDate.of(2026, 11, 1);
    var resultado =
        VisaoGeralConferenciaMapper.de(1, data, new PeriodoVisaoGeral(data, data), retrato, 0, 0);
    assertThat(resultado).isNotNull();
    assertThat(resultado.resumo().custoSaldosBrl()).isNull();
    assertThat(resultado.resumo().resultadoRealizadoBrl()).isNull();
    assertThat(resultado.disponibilidade().subtotalCustoBrl()).isEqualTo("400");
    assertThat(resultado.disponibilidade().subtotalResultadoBrl()).isEqualTo("0");
    assertThat(resultado.posicoes())
        .filteredOn(p -> p.ativoTicker().equals("BTC"))
        .singleElement()
        .satisfies(p -> {
          assertThat(p.quantidade()).isEqualTo("1");
          assertThat(p.custoTotalBrl()).isNull();
        });
    assertThat(resultado.evolucaoResultado().meses())
        .singleElement()
        .satisfies(m -> assertThat(m.resultadoBrl()).isNull());
    assertThat(resultado.resultadosPorAtivo())
        .singleElement()
        .satisfies(r -> assertThat(r.resultadoTotalBrl()).isNull());
  }

  @Test
  void mapperPreservaCustoDesconhecidoSemInventarParticipacao() {
    var livro =
        new ResultadoLivro(
            List.of(new PosicaoLivro(2L, BigDecimal.ONE, null, null, List.of())), List.of());
    var resumo =
        new ResumoVisaoGeral(
            null,
            null,
            BigDecimal.ZERO,
            BigDecimal.ZERO,
            0,
            null,
            BigDecimal.ZERO,
            BigDecimal.ZERO);
    var data = LocalDate.of(2026, 11, 1);
    var resultado =
        VisaoGeralMapper.de(
            1,
            data,
            new PeriodoVisaoGeral(data, data),
            true,
            resumo,
            livro,
            Map.of(2L, new AtivoResumo(2L, "cripto:BTC", "BTC", "Bitcoin", 18, true, true, true)),
            Map.of(),
            List.of(),
            List.of(),
            0,
            0);
    assertThat(resultado.resumo().custoSaldosBrl()).isNull();
    assertThat(resultado.posicoes())
        .singleElement()
        .satisfies(p -> {
          assertThat(p.custoTotalBrl()).isNull();
          assertThat(p.participacaoPercentual()).isNull();
        });
  }
}
