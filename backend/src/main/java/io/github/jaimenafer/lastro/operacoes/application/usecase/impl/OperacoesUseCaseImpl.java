package io.github.jaimenafer.lastro.operacoes.application.usecase.impl;

import io.github.jaimenafer.lastro.acesso.application.contract.IUsuarioAtual;
import io.github.jaimenafer.lastro.ativos.application.contract.*;
import io.github.jaimenafer.lastro.carteiras.application.contract.IConsultaCarteiras;
import io.github.jaimenafer.lastro.instituicoes.application.contract.*;
import io.github.jaimenafer.lastro.operacoes.application.dto.*;
import io.github.jaimenafer.lastro.operacoes.application.mapper.*;
import io.github.jaimenafer.lastro.operacoes.application.usecase.IOperacoesUseCase;
import io.github.jaimenafer.lastro.operacoes.domain.exception.*;
import io.github.jaimenafer.lastro.operacoes.domain.model.*;
import io.github.jaimenafer.lastro.operacoes.domain.repository.*;
import java.time.*;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.*;

@Service
@RequiredArgsConstructor
public class OperacoesUseCaseImpl implements IOperacoesUseCase {
  private final IOperacaoRepository operacoes;
  private final IPontoPartidaRepository pontos;
  private final IConsultaAtivos ativos;
  private final IConsultaInstituicoes instituicoes;
  private final IConsultaCarteiras carteiras;
  private final IUsuarioAtual usuario;
  private final io.github.jaimenafer.lastro.operacoes.application.port.IConsultaLivroConferencia
      conferencia;
  private static final ZoneId FUSO = ZoneId.of("America/Fortaleza");

  @Transactional(readOnly = true, isolation = Isolation.REPEATABLE_READ)
  public ListaOperacoesResponse listar(
      String tipo,
      String situacao,
      Long instituicaoId,
      Long ativoId,
      List<Long> carteiraIds,
      int pagina,
      int tamanho) {
    if (pagina < 0 || tamanho < 1 || tamanho > 100)
      throw new OperacaoInvalidaException("Verifique os filtros e a paginação.");
    TipoOperacao tipoSelecionado = tipoDoFiltro(tipo);
    SituacaoOperacao situacaoSelecionada = SaldoInicialMapper.situacaoDoFiltro(situacao);
    if (instituicaoId != null) ValidacaoSaldoInicial.idValido(instituicaoId);
    if (ativoId != null) ValidacaoSaldoInicial.idValido(ativoId);
    Set<Long> selecionadas = null;
    if (carteiraIds != null && !carteiraIds.isEmpty()) {
      carteiraIds.forEach(ValidacaoSaldoInicial::idValido);
      selecionadas =
          carteiras
              .instituicoesVinculadas(carteiraIds)
              .orElseThrow(() -> new OperacaoNaoEncontradaException("Carteira não encontrada."));
    }
    var ponto = pontos.findById(usuario.id());
    var historico = operacoes.historico(usuario.id());
    var ativosPorId = carregarAtivos(historico);
    var instituicoesPorId = carregarInstituicoes(historico);
    var livro = LivroConferenciaMapper.livro(conferencia.carregar(null));
    var calculos =
        livro.calculos().stream()
            .collect(Collectors.toMap(CalculoOperacao::operacaoId, Function.identity()));
    Set<Long> filtroInstituicoes = selecionadas;
    var filtradas =
        historico.stream()
            .filter(o -> tipoSelecionado == null || o.getTipo() == tipoSelecionado)
            .filter(o -> situacaoSelecionada == null || o.getSituacao() == situacaoSelecionada)
            .filter(o -> instituicaoId == null || o.getInstituicaoId().equals(instituicaoId))
            .filter(o ->
                ativoId == null
                    || o.getAtivoId().equals(ativoId)
                    || Objects.equals(o.getMoedaCotacaoId(), ativoId)
                    || Objects.equals(o.getTaxaAtivoId(), ativoId))
            .filter(o ->
                filtroInstituicoes == null || filtroInstituicoes.contains(o.getInstituicaoId()))
            .toList();
    var itens =
        filtradas.stream()
            .skip((long) pagina * tamanho)
            .limit(tamanho)
            .map(
                o -> OperacoesMapper.de(o, calculos.get(o.getId()), ativosPorId, instituicoesPorId))
            .toList();
    return new ListaOperacoesResponse(
        itens,
        filtradas.size(),
        pagina,
        tamanho,
        Long.toString(ponto.map(PontoPartida::getRevisao).orElse(0L)),
        ponto.map(PontoPartida::getDataReferencia).orElse(null));
  }

  @Transactional(readOnly = true, isolation = Isolation.REPEATABLE_READ)
  public OperacaoResponse buscar(long id) {
    var historico = operacoes.historico(usuario.id());
    var operacao = buscarNoHistorico(historico, id);
    return responder(operacao, historico);
  }

  @Transactional(readOnly = true, isolation = Isolation.REPEATABLE_READ)
  public PosicoesLivroResponse posicoes() {
    var ponto = pontos.findById(usuario.id());
    var retrato = conferencia.carregar(null);
    return new PosicoesLivroResponse(
        Long.toString(ponto.map(PontoPartida::getRevisao).orElse(0L)),
        LivroConferenciaMapper.posicoes(retrato),
        retrato.quantidadesCompletas());
  }

  @Transactional(readOnly = true, isolation = Isolation.REPEATABLE_READ)
  public PreviaOperacaoResponse prever(SolicitacaoPrevia solicitacao) {
    var ponto = pontos.findById(usuario.id());
    var historico = operacoes.historico(usuario.id());
    Operacao anterior = null;
    Operacao candidato = null;
    if (solicitacao.acao() == AcaoPrevia.CRIAR) {
      if (solicitacao.operacaoId() != null)
        throw new OperacaoInvalidaException("Criação não recebe operação anterior.");
    } else {
      if (solicitacao.operacaoId() == null)
        throw new OperacaoInvalidaException("Informe a operação a alterar.");
      anterior = buscarNoHistorico(historico, solicitacao.operacaoId());
      validarAlteracao(anterior);
    }
    if (solicitacao.acao() == AcaoPrevia.CANCELAR) {
      validarMotivo(solicitacao.motivo());
      if (solicitacao.operacao() != null)
        throw new OperacaoInvalidaException("Cancelamento recebe somente operação e motivo.");
    } else {
      if (solicitacao.operacao() == null)
        throw new OperacaoInvalidaException("Informe os dados da operação.");
      var dados = normalizar(solicitacao.operacao(), anterior != null);
      validarCatalogo(dados);
      validarData(ponto.orElse(null), dados);
      candidato = candidato(dados, anterior, "00000000-0000-0000-0000-000000000000", "", historico);
    }
    var alterado = historicoCandidato(historico, anterior, candidato);
    var ativosPorId = carregarAtivos(alterado);
    var instituicoesPorId = carregarInstituicoes(alterado);
    if (anterior != null) instituicoesPorId.putAll(carregarInstituicoes(List.of(anterior)));
    if (anterior != null) ativosPorId.putAll(carregarAtivos(List.of(anterior)));
    var retratoAtual = conferencia.carregar(null);
    ativosPorId.putAll(retratoAtual.ativos());
    instituicoesPorId.putAll(retratoAtual.instituicoes());
    var livro = conferencia.validarCandidato(alterado);
    Operacao exibida = candidato == null ? anterior : candidato;
    Long calculoId = candidato == null ? null : -1L;
    var calculo =
        livro.calculos().stream()
            .filter(c -> Objects.equals(c.operacaoId(), calculoId))
            .findFirst()
            .orElse(null);
    return new PreviaOperacaoResponse(
        Long.toString(ponto.map(PontoPartida::getRevisao).orElse(0L)),
        OperacoesMapper.de(exibida, calculo, ativosPorId, instituicoesPorId),
        OperacoesMapper.calculo(calculo),
        OperacoesMapper.posicoes(livro, ativosPorId, instituicoesPorId));
  }

  @Transactional(isolation = Isolation.READ_COMMITTED)
  public GravacaoOperacaoResponse gravar(
      ConfirmacaoOperacao confirmacao, String chave, Long anteriorId) {
    chave = IdentidadeNegociacao.chave(chave);
    var dados = normalizar(confirmacao.operacao(), anteriorId != null);
    var fingerprint = IdentidadeNegociacao.fingerprint(dados, anteriorId);
    var repetida = repeticao(chave, fingerprint);
    if (repetida.isPresent())
      return new GravacaoOperacaoResponse(
          responder(repetida.get(), operacoes.historico(usuario.id())), true);
    Instant agora = agora();
    var ponto =
        pontos
            .bloquear(usuario.id())
            .orElseGet(() ->
                pontos.inserir(
                    PontoPartida.iniciar(
                        usuario.id(), dados.ocorridaEm().atZone(FUSO).toLocalDate(), agora)));
    repetida = repeticao(chave, fingerprint);
    if (repetida.isPresent())
      return new GravacaoOperacaoResponse(
          responder(repetida.get(), operacoes.historico(usuario.id())), true);
    validarRevisao(ponto, confirmacao.revisao());
    validarCatalogo(dados);
    validarData(ponto, dados);
    var historico = operacoes.historico(usuario.id());
    var anterior = anteriorId == null ? null : buscarNoHistorico(historico, anteriorId);
    if (anterior != null) validarAlteracao(anterior);
    var nova = candidato(dados, anterior, chave, fingerprint, historico);
    var alterado = historicoCandidato(historico, anterior, nova);
    conferencia.validarCandidato(alterado);
    if (anterior != null) {
      anterior.encerrar(SituacaoOperacao.SUBSTITUIDA, dados.motivo(), usuario.id(), agora);
      operacoes.flush();
    }
    operacoes.saveAndFlush(nova);
    ponto.avancarRevisao(usuario.id(), agora);
    return new GravacaoOperacaoResponse(responder(nova, operacoes.historico(usuario.id())), false);
  }

  @Transactional(isolation = Isolation.READ_COMMITTED)
  public OperacaoResponse cancelar(long id, CancelamentoOperacao cancelamento) {
    var ponto =
        pontos
            .bloquear(usuario.id())
            .orElseThrow(() -> new OperacaoNaoEncontradaException("Operação não encontrada."));
    var historico = operacoes.historico(usuario.id());
    var anterior = buscarNoHistorico(historico, id);
    if (anterior.getTipo() == TipoOperacao.SALDO_INICIAL)
      throw new OperacaoInvalidaException(
          "Use o cadastro de saldo inicial para alterar uma abertura.");
    if (anterior.getSituacao() == SituacaoOperacao.CANCELADA
        && cancelamento.motivo().equals(anterior.getMotivoAlteracao()))
      return responder(anterior, historico);
    validarRevisao(ponto, cancelamento.revisao());
    validarAlteracao(anterior);
    var alterado = historicoCandidato(historico, anterior, null);
    conferencia.validarCandidato(alterado);
    anterior.encerrar(SituacaoOperacao.CANCELADA, cancelamento.motivo(), usuario.id(), agora());
    operacoes.flush();
    ponto.avancarRevisao(usuario.id(), agora());
    return responder(anterior, operacoes.historico(usuario.id()));
  }

  private DadosNegociacao normalizar(CadastroOperacao cadastro, boolean correcao) {
    var ids = new HashSet<Long>();
    ids.add(cadastro.ativoId());
    if (cadastro.moedaCotacaoId() != null) ids.add(cadastro.moedaCotacaoId());
    if (cadastro.taxaAtivoId() != null) ids.add(cadastro.taxaAtivoId());
    return ValidacaoNegociacao.normalizar(
        cadastro,
        ativos.buscarPorIds(ids).stream()
            .collect(Collectors.toMap(AtivoResumo::id, Function.identity())),
        correcao);
  }

  private Operacao candidato(
      DadosNegociacao dados,
      Operacao anterior,
      String chave,
      String fingerprint,
      List<Operacao> historico) {
    var nova =
        Operacao.registrarNegociacao(
            usuario.id(),
            dados.tipo(),
            dados.instituicaoId(),
            dados.ativoId(),
            dados.ocorridaEm(),
            dados.quantidade(),
            dados.moedaCotacaoId(),
            dados.totalBruto(),
            dados.cotacaoBrl(),
            dados.taxaAtivoId(),
            dados.taxaQuantidade(),
            dados.referencia(),
            dados.motivo(),
            anterior == null ? null : anterior.getId(),
            chave,
            fingerprint,
            agora());
    nova.ordenar(
        anterior == null
            ? historico.stream().mapToLong(Operacao::getOrdemLivro).max().orElse(0) + 1
            : anterior.getOrdemLivro());
    return nova;
  }

  private List<Operacao> historicoCandidato(
      List<Operacao> historico, Operacao anterior, Operacao candidato) {
    var alterado =
        new ArrayList<>(
            historico.stream()
                .filter(o -> anterior == null || !o.getId().equals(anterior.getId()))
                .toList());
    if (candidato != null) alterado.add(candidato);
    return alterado;
  }

  private void validarCatalogo(DadosNegociacao dados) {
    if (instituicoes.buscar(dados.instituicaoId()).filter(InstituicaoResumo::ativo).isEmpty())
      throw new OperacaoInvalidaException("Selecione uma instituição ativa.");
    var ids = new HashSet<Long>();
    ids.add(dados.ativoId());
    if (dados.moedaCotacaoId() != null) ids.add(dados.moedaCotacaoId());
    if (dados.taxaAtivoId() != null) ids.add(dados.taxaAtivoId());
    if (ativos.buscarPorIds(ids).stream().anyMatch(a -> !a.ativo()))
      throw new OperacaoInvalidaException("Selecione ativos disponíveis no catálogo.");
  }

  private Operacao buscarNoHistorico(List<Operacao> historico, long id) {
    return historico.stream()
        .filter(o -> o.getId().equals(id))
        .findFirst()
        .orElseThrow(() -> new OperacaoNaoEncontradaException("Operação não encontrada."));
  }

  private void validarAlteracao(Operacao operacao) {
    if (operacao.getTipo() == TipoOperacao.SALDO_INICIAL)
      throw new OperacaoInvalidaException(
          "Use o cadastro de saldo inicial para alterar uma abertura.");
    if (!operacao.confirmada())
      throw new OperacaoConflitanteException("A operação já foi alterada. Atualize a listagem.");
  }

  private void validarData(PontoPartida ponto, DadosNegociacao dados) {
    if (ponto != null
        && dados.ocorridaEm().atZone(FUSO).toLocalDate().isBefore(ponto.getDataReferencia()))
      throw new OperacaoInvalidaException(
          "A operação não pode ser anterior ao início do acompanhamento.");
  }

  private void validarMotivo(String motivo) {
    if (motivo == null || motivo.isBlank())
      throw new OperacaoInvalidaException("Informe o motivo do cancelamento.");
  }

  private void validarRevisao(PontoPartida ponto, Long revisao) {
    if (revisao == null || ponto.getRevisao() != revisao)
      throw new OperacaoConflitanteException(
          "O histórico mudou. Atualize a prévia antes de confirmar.");
  }

  private Optional<Operacao> repeticao(String chave, String fingerprint) {
    var existente = operacoes.findByPerfilRefAndChaveRequisicao(usuario.id(), chave);
    if (existente.isPresent() && !existente.get().getFingerprint().equals(fingerprint))
      throw new OperacaoConflitanteException(
          "Esta solicitação já foi usada com outros dados. Atualize antes de enviar novamente.");
    return existente;
  }

  private Map<Long, AtivoResumo> carregarAtivos(List<Operacao> historico) {
    return ativos.buscarPorIds(LivroMapper.ativosDoHistorico(historico)).stream()
        .collect(Collectors.toMap(AtivoResumo::id, Function.identity()));
  }

  private Map<Long, InstituicaoResumo> carregarInstituicoes(List<Operacao> historico) {
    return instituicoes
        .buscarPorIds(
            historico.stream().map(Operacao::getInstituicaoId).collect(Collectors.toSet()))
        .stream()
        .collect(Collectors.toMap(InstituicaoResumo::id, Function.identity()));
  }

  private OperacaoResponse responder(Operacao operacao, List<Operacao> historico) {
    var ativosPorId = carregarAtivos(historico);
    var livro = LivroConferenciaMapper.livro(conferencia.carregar(null));
    var calculo =
        livro.calculos().stream()
            .filter(c -> c.operacaoId().equals(operacao.getId()))
            .findFirst()
            .orElse(null);
    return OperacoesMapper.de(operacao, calculo, ativosPorId, carregarInstituicoes(historico));
  }

  private TipoOperacao tipoDoFiltro(String tipo) {
    if (tipo == null || tipo.isBlank()) return null;
    try {
      return TipoOperacao.valueOf(tipo);
    } catch (IllegalArgumentException erro) {
      throw new OperacaoInvalidaException("Informe um tipo de operação válido.");
    }
  }

  private static Instant agora() {
    return Instant.now().truncatedTo(ChronoUnit.MICROS);
  }
}
