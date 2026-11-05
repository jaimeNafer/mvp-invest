package io.github.jaimenafer.lastro.operacoes;

import static org.assertj.core.api.Assertions.assertThat;

import io.github.jaimenafer.lastro.operacoes.domain.model.*;
import io.github.jaimenafer.lastro.operacoes.domain.service.ConferenciaRegistros;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import org.junit.jupiter.api.Test;

class ConferenciaRegistrosTests {
  @Test
  void repeticaoPreservaMultiplicidadeInstituicaoEMaiusculas() {
    var primeira = negociacao(1L, "ordem");
    var segunda = negociacao(1L, "ordem");
    var outraInstituicao = negociacao(2L, "ordem");
    var outraCaixa = negociacao(1L, "ORDEM");
    var vazia = negociacao(1L, "");
    var cancelada = negociacao(1L, "ordem");
    cancelada.encerrar(SituacaoOperacao.CANCELADA, "Revisão", "a", Instant.now());
    var avisos =
        ConferenciaRegistros.selecionar(
            List.of(primeira, segunda, outraInstituicao, outraCaixa, vazia, cancelada),
            TipoAvisoConferencia.REFERENCIA_REPETIDA);
    assertThat(avisos).containsExactly(primeira, segunda);
  }

  @Test
  void custoZeroDeclaradoNaoConfundeCustoCalculadoNuloOuCustoPositivo() {
    var zero = abertura("0");
    var positiva = abertura("10");
    assertThat(
            ConferenciaRegistros.selecionar(
                List.of(zero, positiva, negociacao(1L, "x")),
                TipoAvisoConferencia.CUSTO_ZERO_DECLARADO))
        .containsExactly(zero);
  }

  private Operacao abertura(String custo) {
    return Operacao.registrarSaldoInicial(
        "a",
        1L,
        2L,
        LocalDate.parse("2026-10-01"),
        BigDecimal.ONE,
        new BigDecimal(custo),
        "Origem comprovada",
        "",
        null,
        "chave",
        "fp",
        Instant.now());
  }

  private Operacao negociacao(Long instituicao, String referencia) {
    return Operacao.registrarNegociacao(
        "a",
        TipoOperacao.COMPRA,
        instituicao,
        2L,
        Instant.parse("2026-10-01T03:00:00Z"),
        BigDecimal.ONE,
        1L,
        BigDecimal.TEN,
        BigDecimal.ONE,
        null,
        BigDecimal.ZERO,
        referencia,
        "",
        null,
        "chave",
        "fp",
        Instant.now());
  }
}
