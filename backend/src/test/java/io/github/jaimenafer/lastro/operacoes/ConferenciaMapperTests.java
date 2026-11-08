package io.github.jaimenafer.lastro.operacoes;

import static org.assertj.core.api.Assertions.*;

import io.github.jaimenafer.lastro.operacoes.application.dto.*;
import io.github.jaimenafer.lastro.operacoes.application.mapper.ConferenciaMapper;
import io.github.jaimenafer.lastro.operacoes.domain.model.*;
import java.time.Instant;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

class ConferenciaMapperTests {
  @Test
  void leAssociacaoSalvaAntesDoCampoDeTaxaDeclarada() {
    String json = """
        {"linhas":[{"registroId":1,"versao":1,"instituicaoId":1,"contaId":1,
        "ativoId":2,"codigo":"SOL","ticker":"SOL","data":"2024-05-26T22:05:32Z",
        "quantidade":1,"operacao":"Buy","referencia":null,"papel":"RECEBIMENTO"}],
        "taxaZeroConfirmada":false,"cotacaoBrl":null,"desfazer":false}
        """;
    PropostaGrupo proposta = ConferenciaMapper.ler(json, PropostaGrupo.class);
    assertThat(proposta.linhas()).hasSize(1);
    assertThat(proposta.linhas().getFirst().taxaZeroDeclarada()).isFalse();
  }

  @Test
  void camposOpcionaisEmBrancoPermanecemDesconhecidos() {
    var p =
        new PropostaRegistro(
            " ",
            "1",
            "",
            "1",
            null,
            NaturezaRegistro.OBSERVADA,
            null,
            "",
            null,
            null,
            "",
            null,
            false,
            null);
    assertThat(p.data()).isNull();
    assertThat(p.ativoId()).isNull();
    assertThat(ConferenciaMapper.legivel(p)).isFalse();
  }

  @Test
  void custoDoAjusteRespeitaHalfEvenAntesDaPersistencia() {
    var baixo =
        AjusteConciliatorio.aceitar(
            "u",
            "gap",
            "r",
            1L,
            1L,
            1L,
            java.math.BigDecimal.ONE,
            new java.math.BigDecimal("0.000000005"),
            1,
            null,
            "u",
            Instant.now());
    var alto =
        AjusteConciliatorio.aceitar(
            "u",
            "gap",
            "r",
            1L,
            1L,
            1L,
            java.math.BigDecimal.ONE,
            new java.math.BigDecimal("0.000000015"),
            1,
            null,
            "u",
            Instant.now());
    assertThat(baixo.getCustoTotalBrl()).isZero();
    assertThat(alto.getCustoTotalBrl()).isEqualByComparingTo("0.00000002");
  }

  @Test
  void taxaZeroInformadaNaoExigeMoedaDeTaxa() {
    RegistroConferencia r =
        RegistroConferencia.criar("teste", "1", "origem", 1L, "{}", "[]", "{}", Instant.now());
    ReflectionTestUtils.setField(r, "id", 1L);
    PropostaRegistro p =
        new PropostaRegistro(
            "2024-01-01T12:00:00Z",
            "1",
            "2",
            "1",
            null,
            NaturezaRegistro.COMPRA,
            null,
            "3",
            "100",
            "1",
            null,
            "0",
            true,
            null);
    var item = ConferenciaMapper.item(r, p);
    assertThat(item.operacao()).isNotNull();
    assertThat(item.operacao().taxaQuantidade()).isZero();
    assertThat(item.operacao().taxaAtivoId()).isNull();
  }

  @Test
  void taxaDesconhecidaNaoViraTaxaZero() {
    RegistroConferencia r =
        RegistroConferencia.criar("teste", "1", "origem", 1L, "{}", "[]", "{}", Instant.now());
    ReflectionTestUtils.setField(r, "id", 1L);
    PropostaRegistro p =
        new PropostaRegistro(
            "2024-01-01T12:00:00Z",
            "1",
            "2",
            "1",
            null,
            NaturezaRegistro.COMPRA,
            null,
            "3",
            "100",
            "1",
            null,
            null,
            false,
            null);
    assertThat(ConferenciaMapper.item(r, p).operacao()).isNull();
  }

  @Test
  void correcoesPreservamVersoesAnterioresEOriginal() {
    Instant agora = Instant.parse("2024-01-01T12:00:00Z");
    RegistroConferencia r =
        RegistroConferencia.criar("teste", "1", "origem", 1L, "original", "[]", "primeira", agora);
    r.aplicar();
    r.revisar("teste", agora);
    r.editar("segunda", "teste", agora.plusSeconds(1));
    assertThat(r.getOriginalJson()).isEqualTo("original");
    assertThat(r.getAplicadaJson()).isEqualTo("primeira");
    assertThat(r.getRevisadoEm()).isNull();
    assertThat(r.versoesAnteriores())
        .singleElement()
        .satisfies(v -> {
          assertThat(v.getVersao()).isEqualTo(1);
          assertThat(v.getPropostaJson()).isEqualTo("primeira");
        });
    assertThatThrownBy(() -> r.versoesAnteriores().clear())
        .isInstanceOf(UnsupportedOperationException.class);
  }

  @Test
  void entradaDecimalJsonNumericaNaoSofreCoercaoSilenciosa() {
    assertThatThrownBy(() ->
            ConferenciaMapper.textoDecimal(
                ConferenciaMapper.ler("0.1", tools.jackson.databind.JsonNode.class), "quantidade"))
        .hasMessageContaining("texto decimal");
  }
}
