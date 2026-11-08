package io.github.jaimenafer.lastro.operacoes.application.usecase.impl;

import static io.github.jaimenafer.lastro.operacoes.application.usecase.impl.IdentidadeSolicitacaoSaldo.chaveValida;
import static io.github.jaimenafer.lastro.operacoes.application.usecase.impl.IdentidadeSolicitacaoSaldo.fingerprint;
import static io.github.jaimenafer.lastro.operacoes.application.usecase.impl.ValidacaoSaldoInicial.*;

import io.github.jaimenafer.lastro.acesso.application.contract.IUsuarioAtual;
import io.github.jaimenafer.lastro.ativos.application.contract.AtivoResumo;
import io.github.jaimenafer.lastro.ativos.application.contract.IConsultaAtivos;
import io.github.jaimenafer.lastro.carteiras.application.contract.IConsultaCarteiras;
import io.github.jaimenafer.lastro.instituicoes.application.contract.IConsultaInstituicoes;
import io.github.jaimenafer.lastro.instituicoes.application.contract.InstituicaoResumo;
import io.github.jaimenafer.lastro.operacoes.application.dto.*;
import io.github.jaimenafer.lastro.operacoes.application.mapper.SaldoInicialMapper;
import io.github.jaimenafer.lastro.operacoes.application.usecase.ISaldoInicialUseCase;
import io.github.jaimenafer.lastro.operacoes.domain.exception.SaldoInicialConflitanteException;
import io.github.jaimenafer.lastro.operacoes.domain.exception.SaldoInicialInvalidoException;
import io.github.jaimenafer.lastro.operacoes.domain.exception.SaldoInicialNaoEncontradoException;
import io.github.jaimenafer.lastro.operacoes.domain.model.FatoPosicaoInicial;
import io.github.jaimenafer.lastro.operacoes.domain.model.FiltroOperacoes;
import io.github.jaimenafer.lastro.operacoes.domain.model.Operacao;
import io.github.jaimenafer.lastro.operacoes.domain.model.PontoPartida;
import io.github.jaimenafer.lastro.operacoes.domain.model.SituacaoOperacao;
import io.github.jaimenafer.lastro.operacoes.domain.model.TipoOperacao;
import io.github.jaimenafer.lastro.operacoes.domain.repository.IOperacaoRepository;
import io.github.jaimenafer.lastro.operacoes.domain.repository.IPontoPartidaRepository;
import io.github.jaimenafer.lastro.operacoes.domain.service.CalculoPosicaoInicial;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class SaldoInicialUseCaseImpl implements ISaldoInicialUseCase {
  private final IOperacaoRepository operacoes;
  private final IPontoPartidaRepository pontos;
  private final IConsultaAtivos ativos;
  private final IConsultaInstituicoes instituicoes;
  private final IConsultaCarteiras carteiras;
  private final IUsuarioAtual usuario;
  private final io.github.jaimenafer.lastro.operacoes.application.port.IConsultaLivroConferencia
      conferencia;

  @Transactional(readOnly = true)
  public ListaSaldosIniciais listar(
      Long instituicaoId,
      Long ativoId,
      List<Long> carteiraIds,
      String situacao,
      int pagina,
      int tamanho) {
    SituacaoOperacao situacaoSelecionada =
        validarFiltros(instituicaoId, ativoId, situacao, pagina, tamanho);
    var selecionadas = selecionarInstituicoesDasCarteiras(carteiraIds);
    var dados =
        operacoes.consultar(
            new FiltroOperacoes(
                usuario.id(),
                instituicaoId,
                ativoId,
                selecionadas,
                situacaoSelecionada,
                pagina,
                tamanho));
    return new ListaSaldosIniciais(
        mapearOperacoes(dados.itens()),
        pagina,
        tamanho,
        dados.total(),
        pontos.findById(usuario.id()).map(PontoPartida::getDataReferencia).orElse(null),
        !operacoes
            .findByPerfilRefAndSituacaoOrderByIdAsc(usuario.id(), SituacaoOperacao.CONFIRMADA)
            .isEmpty());
  }

  private SituacaoOperacao validarFiltros(
      Long instituicaoId, Long ativoId, String situacao, int pagina, int tamanho) {
    if (pagina < 0 || tamanho < 1 || tamanho > 100) {
      throw new SaldoInicialInvalidoException("Verifique os filtros e a paginação.");
    }
    SituacaoOperacao situacaoSelecionada = SaldoInicialMapper.situacaoDoFiltro(situacao);
    if (instituicaoId != null) idValido(instituicaoId);
    if (ativoId != null) idValido(ativoId);
    return situacaoSelecionada;
  }

  private Operacao buscarOperacao(long id) {
    idValido(id);
    var operacao = operacoes.buscarObrigatoria(id, usuario.id());
    if (operacao.getTipo() != TipoOperacao.SALDO_INICIAL)
      throw new SaldoInicialNaoEncontradoException("Saldo inicial não encontrado.");
    return operacao;
  }

  @Override
  @Transactional(readOnly = true)
  public SaldoInicialResponse buscar(long id) {
    return responder(buscarOperacao(id));
  }

  @Transactional(readOnly = true)
  public List<PosicaoInicialResponse> posicoes() {
    var confirmadas =
        operacoes.findByPerfilRefAndSituacaoOrderByIdAsc(usuario.id(), SituacaoOperacao.CONFIRMADA);
    var ativosPorId = carregarAtivos(confirmadas);
    var instituicoesPorId = carregarInstituicoes(confirmadas);
    var fatos =
        confirmadas.stream()
            .map(operacao ->
                new FatoPosicaoInicial(
                    operacao.getAtivoId(),
                    ativosPorId.get(operacao.getAtivoId()).ticker(),
                    operacao.getInstituicaoId(),
                    instituicoesPorId.get(operacao.getInstituicaoId()).nome(),
                    operacao.getQuantidade(),
                    operacao.getCustoTotalBrl()))
            .toList();
    return CalculoPosicaoInicial.calcular(fatos).stream().map(SaldoInicialMapper::posicao).toList();
  }

  @Transactional(isolation = Isolation.READ_COMMITTED)
  public ResultadoGravacaoSaldo gravar(
      CadastroSaldoInicial cadastro, String chave, Long anteriorId) {
    if (anteriorId != null) idValido(anteriorId);
    chave = chaveValida(chave);
    var dados = normalizar(SaldoInicialMapper.entrada(cadastro), anteriorId != null);
    var fingerprint = fingerprint(dados, anteriorId);
    var repetida = repeticao(chave, fingerprint);
    if (repetida.isPresent()) return new ResultadoGravacaoSaldo(responder(repetida.get()), true);
    var agora = agora();
    var ponto =
        pontos
            .bloquear(usuario.id())
            .orElseGet(
                () -> pontos.inserir(PontoPartida.iniciar(usuario.id(), dados.data(), agora)));
    repetida = repeticao(chave, fingerprint);
    if (repetida.isPresent()) return new ResultadoGravacaoSaldo(responder(repetida.get()), true);
    var anterior = anteriorId == null ? null : buscarOperacao(anteriorId);
    if (anterior != null) validarSaldoConfirmado(anterior);
    validarOuRedefinirDataInicial(ponto, dados, agora);
    InstituicaoResumo instituicao = buscarInstituicaoAtiva(dados.instituicaoId());
    AtivoResumo ativo = buscarAtivoDisponivel(dados.ativoId());
    validarQuantidadeECustoDoAtivo(dados, ativo);
    substituirSaldoAnterior(anterior, dados.motivo(), agora);
    var novo =
        Operacao.registrarSaldoInicial(
            usuario.id(),
            instituicao.id(),
            ativo.id(),
            dados.data(),
            dados.quantidade(),
            dados.custo(),
            dados.justificativa(),
            dados.motivo(),
            anteriorId,
            chave,
            fingerprint,
            agora);
    var historico = operacoes.historico(usuario.id());
    novo.ordenar(
        anterior == null
            ? historico.stream().mapToLong(Operacao::getOrdemLivro).max().orElse(0) + 1
            : anterior.getOrdemLivro());
    validarHistorico(historico, anterior, novo);
    var salvo = operacoes.saveAndFlush(novo);
    ponto.avancarRevisao(usuario.id(), agora);
    return new ResultadoGravacaoSaldo(SaldoInicialMapper.de(salvo, instituicao, ativo), false);
  }

  @Transactional(isolation = Isolation.READ_COMMITTED)
  public SaldoInicialResponse cancelar(long id, String motivo) {
    var ponto =
        pontos
            .bloquear(usuario.id())
            .orElseThrow(
                () -> new SaldoInicialNaoEncontradoException("Saldo inicial não encontrado."));
    var operacao = buscarOperacao(id);
    if (operacao.getSituacao() == SituacaoOperacao.CANCELADA
        && motivo.equals(operacao.getMotivoAlteracao())) return responder(operacao);
    validarSaldoConfirmado(operacao);
    validarHistorico(operacoes.historico(usuario.id()), operacao, null);
    operacao.encerrar(SituacaoOperacao.CANCELADA, motivo, usuario.id(), agora());
    ponto.avancarRevisao(usuario.id(), agora());
    operacoes.flush();
    return responder(operacao);
  }

  private void validarHistorico(List<Operacao> historico, Operacao anterior, Operacao nova) {
    var candidato =
        new ArrayList<>(
            historico.stream()
                .filter(o -> anterior == null || !o.getId().equals(anterior.getId()))
                .toList());
    if (nova != null) candidato.add(nova);
    conferencia.validarCandidato(candidato);
  }

  private InstituicaoResumo buscarInstituicaoAtiva(Long instituicaoId) {
    return instituicoes
        .buscar(instituicaoId)
        .filter(InstituicaoResumo::ativo)
        .orElseThrow(() -> new SaldoInicialInvalidoException("Selecione uma instituição ativa."));
  }

  private AtivoResumo buscarAtivoDisponivel(Long ativoId) {
    return ativos
        .buscar(ativoId)
        .filter(ativo -> ativo.ativo() && ativo.permiteSaldoInicial())
        .orElseThrow(() -> new SaldoInicialInvalidoException("Selecione um ativo disponível."));
  }

  private void validarSaldoConfirmado(Operacao operacao) {
    if (!operacao.confirmada())
      throw new SaldoInicialConflitanteException("O saldo já foi alterado. Atualize a listagem.");
  }

  private void substituirSaldoAnterior(Operacao anterior, String motivo, Instant agora) {
    if (anterior == null) return;
    anterior.encerrar(SituacaoOperacao.SUBSTITUIDA, motivo, usuario.id(), agora);
    operacoes.flush();
  }

  private Optional<Operacao> repeticao(String chave, String fingerprint) {
    var existente = operacoes.findByPerfilRefAndChaveRequisicao(usuario.id(), chave);
    if (existente.isPresent() && !existente.get().getFingerprint().equals(fingerprint)) {
      throw new SaldoInicialConflitanteException(
          "Esta solicitação já foi usada com outros dados. Atualize antes de enviar novamente.");
    }
    return existente;
  }

  private static Instant agora() {
    return Instant.now().truncatedTo(ChronoUnit.MICROS);
  }

  private Set<Long> selecionarInstituicoesDasCarteiras(List<Long> carteiraIds) {
    if (carteiraIds == null || carteiraIds.isEmpty()) return null;
    carteiraIds.forEach(ValidacaoSaldoInicial::idValido);
    return carteiras
        .instituicoesVinculadas(carteiraIds)
        .orElseThrow(() -> new SaldoInicialNaoEncontradoException("Carteira não encontrada."));
  }

  private void validarQuantidadeECustoDoAtivo(DadosSaldoInicial dados, AtivoResumo ativo) {
    if (Math.max(0, dados.quantidade().stripTrailingZeros().scale()) > ativo.casasDecimais()) {
      throw new SaldoInicialInvalidoException(
          "A quantidade aceita até " + ativo.casasDecimais() + " casas decimais para este ativo.");
    }
    if (ativo.codigo().equals("fiat:BRL") && dados.custo().compareTo(dados.quantidade()) != 0) {
      throw new SaldoInicialInvalidoException(
          "O custo do saldo em BRL deve ser igual à quantidade.");
    }
  }

  private void validarOuRedefinirDataInicial(
      PontoPartida ponto, DadosSaldoInicial dados, Instant agora) {
    boolean possuiConfirmados =
        operacoes.existsByPerfilRefAndSituacao(usuario.id(), SituacaoOperacao.CONFIRMADA);
    if (!ponto.getDataReferencia().equals(dados.data())) {
      if (possuiConfirmados)
        throw new SaldoInicialConflitanteException(
            "Use a data de início do acompanhamento: " + ponto.getDataReferencia() + ".");
      ponto.redefinir(dados.data(), agora, usuario.id());
    }
  }

  private SaldoInicialResponse responder(Operacao operacao) {
    var ativosPorId = carregarAtivos(Set.of(operacao.getAtivoId()));
    var instituicoesPorId = carregarInstituicoes(Set.of(operacao.getInstituicaoId()));
    return SaldoInicialMapper.de(
        operacao,
        instituicoesPorId.get(operacao.getInstituicaoId()),
        ativosPorId.get(operacao.getAtivoId()));
  }

  private List<SaldoInicialResponse> mapearOperacoes(List<Operacao> operacoesEncontradas) {
    var ativosPorId = carregarAtivos(operacoesEncontradas);
    var instituicoesPorId = carregarInstituicoes(operacoesEncontradas);
    return operacoesEncontradas.stream()
        .map(operacao ->
            SaldoInicialMapper.de(
                operacao,
                instituicoesPorId.get(operacao.getInstituicaoId()),
                ativosPorId.get(operacao.getAtivoId())))
        .toList();
  }

  private Map<Long, AtivoResumo> carregarAtivos(List<Operacao> operacoesEncontradas) {
    var ids = operacoesEncontradas.stream().map(Operacao::getAtivoId).collect(Collectors.toSet());
    return carregarAtivos(ids);
  }

  private Map<Long, AtivoResumo> carregarAtivos(Collection<Long> ativoIds) {
    return ativos.buscarPorIds(ativoIds).stream()
        .collect(Collectors.toMap(AtivoResumo::id, Function.identity()));
  }

  private Map<Long, InstituicaoResumo> carregarInstituicoes(List<Operacao> operacoesEncontradas) {
    var ids =
        operacoesEncontradas.stream().map(Operacao::getInstituicaoId).collect(Collectors.toSet());
    return carregarInstituicoes(ids);
  }

  private Map<Long, InstituicaoResumo> carregarInstituicoes(Collection<Long> instituicaoIds) {
    return instituicoes.buscarPorIds(instituicaoIds).stream()
        .collect(Collectors.toMap(InstituicaoResumo::id, Function.identity()));
  }
}
