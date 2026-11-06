package io.github.jaimenafer.lastro.operacoes.application.usecase.impl;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

import io.github.jaimenafer.lastro.acesso.application.contract.IUsuarioAtual;
import io.github.jaimenafer.lastro.ativos.application.contract.*;
import io.github.jaimenafer.lastro.instituicoes.application.contract.*;
import io.github.jaimenafer.lastro.operacoes.application.contract.*;
import io.github.jaimenafer.lastro.operacoes.domain.exception.*;
import io.github.jaimenafer.lastro.operacoes.domain.model.*;
import io.github.jaimenafer.lastro.operacoes.domain.repository.*;
import java.math.BigDecimal;
import java.time.*;
import java.util.*;
import org.junit.jupiter.api.*;

class LoteOperacoesTests {
  IOperacaoRepository operacoes = mock(IOperacaoRepository.class);
  IPontoPartidaRepository pontos = mock(IPontoPartidaRepository.class);
  IConsultaAtivos ativos = mock(IConsultaAtivos.class);
  IConsultaInstituicoes instituicoes = mock(IConsultaInstituicoes.class);
  IUsuarioAtual usuario = mock(IUsuarioAtual.class);
  ImportacaoOperacoesImpl lote;
  List<Operacao> historico = new ArrayList<>();
  AtivoResumo brl = new AtivoResumo(1L, "fiat:BRL", "BRL", "Real", 2, true, true, false);
  AtivoResumo btc = new AtivoResumo(2L, "cripto:BTC", "BTC", "Bitcoin", 8, true, true, true);

  @BeforeEach
  void preparar() {
    when(usuario.id()).thenReturn("teste");
    when(operacoes.historico("teste")).thenAnswer(i -> new ArrayList<>(historico));
    when(ativos.buscarPorCodigo("fiat:BRL")).thenReturn(Optional.of(brl));
    when(ativos.buscarPorIds(any())).thenReturn(List.of(brl, btc));
    when(instituicoes.buscarPorIds(any()))
        .thenReturn(List.of(new InstituicaoResumo(1L, "Binance", true)));
    when(pontos.findById("teste")).thenReturn(Optional.empty());
    when(pontos.bloquear("teste")).thenReturn(Optional.empty());
    when(pontos.inserir(any())).thenAnswer(i -> i.getArgument(0));
    lote = new ImportacaoOperacoesImpl(operacoes, pontos, ativos, instituicoes, usuario);
  }

  MovimentoImportado aporte(String valor) {
    return new MovimentoImportado(
        "10000000-0000-4000-8000-000000000001",
        null,
        "APORTE",
        1,
        1,
        Instant.parse("2024-01-01T13:00:00Z"),
        new BigDecimal(valor),
        null,
        BigDecimal.ZERO,
        "dep-1",
        null);
  }

  MovimentoImportado compra(String principal) {
    return new MovimentoImportado(
        "10000000-0000-4000-8000-000000000002",
        null,
        "COMPRA",
        1,
        2,
        Instant.parse("2024-01-01T14:00:00Z"),
        new BigDecimal("0.01"),
        new BigDecimal(principal),
        new BigDecimal("1"),
        "buy-1",
        null);
  }

  @Test
  void previaValidaLoteSemGravarERejeitaSaldoInsuficiente() {
    assertThat(lote.prever(List.of(aporte("101"), compra("100")), true).eventos()).hasSize(2);
    assertThatThrownBy(() -> lote.prever(List.of(aporte("100"), compra("100")), true))
        .isInstanceOf(OperacaoConflitanteException.class)
        .hasMessageContaining("Saldo");
    verify(operacoes, never()).saveAndFlush(any());
  }

  @Test
  void exigeDeclaracaoDePosicaoAnteriorAntesDeIniciar() {
    assertThatThrownBy(() -> lote.prever(List.of(aporte("101")), false))
        .isInstanceOf(OperacaoInvalidaException.class)
        .hasMessageContaining("posição anterior");
  }

  @Test
  void revisaoObsoletaNaoGrava() {
    assertThatThrownBy(() -> lote.confirmar(List.of(aporte("101")), true, 3))
        .isInstanceOf(OperacaoConflitanteException.class);
    verify(operacoes, never()).saveAndFlush(any());
  }

  @Test
  void reconheceOriginalCanceladaSemReativar() throws Exception {
    var original =
        Operacao.registrarNegociacao(
            "teste",
            TipoOperacao.APORTE,
            1L,
            1L,
            Instant.parse("2024-01-01T13:00:00Z"),
            new BigDecimal("101"),
            null,
            null,
            null,
            null,
            BigDecimal.ZERO,
            "dep-1",
            "",
            null,
            aporte("101").chave(),
            "",
            Instant.now());
    var id = Operacao.class.getDeclaredField("id");
    id.setAccessible(true);
    id.set(original, 9L);
    original.encerrar(SituacaoOperacao.CANCELADA, "Correção", "teste", Instant.now());
    historico.add(original);
    var resultado = lote.prever(List.of(aporte("101.00")), true);
    assertThat(resultado.eventos().getFirst().reconhecida()).isTrue();
    assertThat(resultado.eventos().getFirst().operacaoId()).isEqualTo("9");
    assertThat(original.getSituacao()).isEqualTo(SituacaoOperacao.CANCELADA);
  }

  @Test
  void eventoReconhecidoComConteudoDiferenteConflita() throws Exception {
    reconheceOriginalCanceladaSemReativar();
    assertThatThrownBy(() -> lote.prever(List.of(aporte("102")), true))
        .isInstanceOf(OperacaoConflitanteException.class);
  }

  @Test
  void todasReconhecidasNaoAvancamRevisao() throws Exception {
    reconheceOriginalCanceladaSemReativar();
    var ponto = PontoPartida.iniciar("teste", LocalDate.of(2024, 1, 1), Instant.now());
    when(pontos.findById("teste")).thenReturn(Optional.of(ponto));
    when(pontos.bloquear("teste")).thenReturn(Optional.of(ponto));
    lote.confirmar(List.of(aporte("101")), false, 0);
    assertThat(ponto.getRevisao()).isZero();
    verify(operacoes, never()).saveAndFlush(any());
  }

  @Test
  void reconheceOrigemComprovadaMesmoQuandoChaveLegadaNaoCoincide() throws Exception {
    reconheceOriginalCanceladaSemReativar();
    var original = aporte("101");
    var origem =
        new MovimentoImportado(
            "20000000-0000-4000-8000-000000000001",
            null,
            original.tipo(),
            1,
            1,
            original.ocorridaEm(),
            original.quantidade(),
            null,
            BigDecimal.ZERO,
            original.referencia(),
            9L);
    assertThat(lote.prever(List.of(origem), true).eventos().getFirst().reconhecida()).isTrue();
  }

  @Test
  void naoDuplicaCorrespondenciaSemOrigemComprovada() throws Exception {
    reconheceOriginalCanceladaSemReativar();
    var original = aporte("101");
    var semOrigem =
        new MovimentoImportado(
            "20000000-0000-4000-8000-000000000001",
            null,
            original.tipo(),
            1,
            1,
            original.ocorridaEm(),
            original.quantidade(),
            null,
            BigDecimal.ZERO,
            original.referencia(),
            null);
    assertThatThrownBy(() -> lote.prever(List.of(semOrigem), true))
        .isInstanceOf(OperacaoConflitanteException.class)
        .hasMessageContaining("origem");
  }

  @Test
  void validaTambemOperacoesPosterioresAoArquivo() {
    var gastoPosterior =
        Operacao.registrarNegociacao(
            "teste",
            TipoOperacao.COMPRA,
            1L,
            2L,
            Instant.parse("2024-02-01T13:00:00Z"),
            new BigDecimal("0.01"),
            1L,
            new BigDecimal("100"),
            BigDecimal.ONE,
            null,
            BigDecimal.ZERO,
            "buy-2",
            "",
            null,
            UUID.randomUUID().toString(),
            "",
            Instant.now());
    historico.add(gastoPosterior);
    assertThatThrownBy(() -> lote.prever(List.of(aporte("101"), compra("100")), true))
        .isInstanceOf(OperacaoConflitanteException.class)
        .hasMessageContaining("Saldo");
  }
}
