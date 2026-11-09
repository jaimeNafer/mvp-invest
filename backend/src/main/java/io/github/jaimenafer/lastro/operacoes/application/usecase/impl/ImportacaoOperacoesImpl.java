package io.github.jaimenafer.lastro.operacoes.application.usecase.impl;

import io.github.jaimenafer.lastro.acesso.application.contract.IUsuarioAtual;
import io.github.jaimenafer.lastro.ativos.application.contract.*;
import io.github.jaimenafer.lastro.instituicoes.application.contract.*;
import io.github.jaimenafer.lastro.operacoes.application.contract.*;
import io.github.jaimenafer.lastro.operacoes.application.dto.*;
import io.github.jaimenafer.lastro.operacoes.domain.exception.*;
import io.github.jaimenafer.lastro.operacoes.domain.model.*;
import io.github.jaimenafer.lastro.operacoes.domain.repository.*;
import java.math.BigDecimal;
import java.time.*;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.*;
import tools.jackson.databind.node.StringNode;

@Service
@RequiredArgsConstructor
public class ImportacaoOperacoesImpl implements IImportacaoOperacoes {
  private final IOperacaoRepository operacoes;
  private final IPontoPartidaRepository pontos;
  private final IConsultaAtivos ativos;
  private final IConsultaInstituicoes instituicoes;
  private final IUsuarioAtual usuario;
  private final io.github.jaimenafer.lastro.operacoes.application.port.IConsultaLivroConferencia
      conferencia;
  private static final ZoneId FUSO = ZoneId.of("America/Fortaleza");

  @Override
  public List<Long> versoesImportadas(List<Long> ids) {
    Set<Long> resultado = new HashSet<>(ids);
    List<Operacao> historico = operacoes.historico(usuario.id());
    boolean mudou;
    do {
      mudou = false;
      for (Operacao operacao : historico)
        if (operacao.getSubstituiId() != null && resultado.contains(operacao.getSubstituiId()))
          mudou |= resultado.add(operacao.getId());
    } while (mudou);
    return List.copyOf(resultado);
  }

  @Override
  public void excluirVersoesImportadas(List<Long> ids, List<Long> compartilhadas) {
    List<Operacao> historico = operacoes.historico(usuario.id());
    Set<Long> protegidos = new HashSet<>(compartilhadas);
    boolean mudou;
    do {
      mudou = false;
      for (Operacao operacao : historico)
        if (operacao.getSubstituiId() != null && ids.contains(operacao.getId())
            && (protegidos.contains(operacao.getId()) || protegidos.contains(operacao.getSubstituiId()))) {
          mudou |= protegidos.add(operacao.getId());
          mudou |= protegidos.add(operacao.getSubstituiId());
        }
    } while (mudou);
    historico.stream().filter(o -> ids.contains(o.getId()) && !protegidos.contains(o.getId()))
        .sorted(Comparator.comparing(Operacao::getId).reversed()).forEach(operacoes::excluir);
    pontos.bloquear(usuario.id()).ifPresent(p -> p.avancarRevisao(usuario.id(), agora()));
  }

  @Override
  @Transactional(isolation = Isolation.READ_COMMITTED)
  public void bloquearHistorico(
      List<MovimentoImportado> movimentos, boolean posicaoAnteriorZerada) {
    validarLote(movimentos);
    if (pontos.bloquear(usuario.id()).isEmpty()) iniciar(movimentos, posicaoAnteriorZerada);
  }

  @Override
  @Transactional(readOnly = true, isolation = Isolation.REPEATABLE_READ)
  public PreviaLoteOperacoes prever(
      List<MovimentoImportado> movimentos, boolean posicaoAnteriorZerada) {
    var ponto = pontos.findById(usuario.id());
    var historico = operacoes.historico(usuario.id());
    var candidatos = preparar(movimentos, historico, ponto.orElse(null), posicaoAnteriorZerada);
    validarLivro(historico, candidatos);
    return new PreviaLoteOperacoes(
        Long.toString(ponto.map(PontoPartida::getRevisao).orElse(0L)),
        reconhecer(movimentos, candidatos, chavesReconhecidas(candidatos)));
  }

  @Override
  @Transactional(isolation = Isolation.READ_COMMITTED)
  public ResultadoLoteOperacoes confirmar(
      List<MovimentoImportado> movimentos, boolean posicaoAnteriorZerada, long revisao) {
    validarLote(movimentos);
    var ponto =
        pontos.bloquear(usuario.id()).orElseGet(() -> iniciar(movimentos, posicaoAnteriorZerada));
    if (ponto.getRevisao() != revisao)
      throw new OperacaoConflitanteException(
          "O histórico mudou. Atualize a prévia antes de confirmar.");
    var historico = operacoes.historico(usuario.id());
    var candidatos = preparar(movimentos, historico, ponto, posicaoAnteriorZerada);
    validarLivro(historico, candidatos);
    var reconhecidas = chavesReconhecidas(candidatos);
    boolean novas = reconhecidas.size() != candidatos.size();
    if (novas)
      operacoes.salvarLote(candidatos.values().stream().filter(o -> o.getId() == null).toList());
    if (novas) ponto.avancarRevisao(usuario.id(), agora());
    return new ResultadoLoteOperacoes(
        Long.toString(ponto.getRevisao()), reconhecer(movimentos, candidatos, reconhecidas));
  }

  private PontoPartida iniciar(List<MovimentoImportado> movimentos, boolean posicaoAnteriorZerada) {
    exigirPosicaoAnteriorZerada(posicaoAnteriorZerada);
    var primeiro =
        movimentos.stream().min(Comparator.comparing(MovimentoImportado::ocorridaEm)).orElseThrow();
    if (!primeiro.tipo().equals("APORTE"))
      throw new OperacaoInvalidaException(
          "O primeiro registro deve ser um aporte. Cadastre a posição anterior quando houver saldo"
              + " prévio.");
    return pontos.inserir(
        PontoPartida.iniciar(
            usuario.id(), primeiro.ocorridaEm().atZone(FUSO).toLocalDate(), agora()));
  }

  private Map<String, Operacao> preparar(
      List<MovimentoImportado> movimentos,
      List<Operacao> historico,
      PontoPartida ponto,
      boolean posicaoAnteriorZerada) {
    validarLote(movimentos);
    if (ponto == null) exigirPosicaoAnteriorZerada(posicaoAnteriorZerada);
    var porChave =
        historico.stream()
            .collect(Collectors.toMap(Operacao::getChaveRequisicao, Function.identity()));
    Set<Long> ids = new HashSet<>();
    movimentos.forEach(m -> ids.add(m.ativoId()));
    var brl =
        ativos
            .buscarPorCodigo("fiat:BRL")
            .orElseThrow(() -> new OperacaoInvalidaException("BRL não encontrado no catálogo."));
    ids.add(brl.id());
    var catalogo =
        ativos.buscarPorIds(ids).stream()
            .collect(Collectors.toMap(AtivoResumo::id, Function.identity()));
    var instituicoesPorId =
        instituicoes
            .buscarPorIds(
                movimentos.stream()
                    .map(MovimentoImportado::instituicaoId)
                    .collect(Collectors.toSet()))
            .stream()
            .collect(Collectors.toMap(InstituicaoResumo::id, Function.identity()));
    var resultado = new LinkedHashMap<String, Operacao>();
    long ordem = historico.stream().mapToLong(Operacao::getOrdemLivro).max().orElse(0);
    for (var movimento :
        movimentos.stream().sorted(Comparator.comparing(MovimentoImportado::ocorridaEm)).toList()) {
      var dados = normalizar(movimento, brl.id(), catalogo);
      var existente = porChave.get(movimento.chave());
      if (existente == null && movimento.chaveLegada() != null)
        existente = porChave.get(movimento.chaveLegada());
      if (existente == null && movimento.operacaoOriginalId() != null) {
        existente =
            historico.stream()
                .filter(o -> o.getId().equals(movimento.operacaoOriginalId()))
                .findFirst()
                .orElseThrow(() ->
                    new OperacaoConflitanteException(
                        "A origem comprovada não corresponde ao histórico deste usuário."));
      }
      if (existente == null
          && historico.stream()
              .anyMatch(o ->
                  o.getInstituicaoId().equals(movimento.instituicaoId())
                      && Objects.equals(o.getReferencia(), movimento.referencia()))) {
        throw new OperacaoConflitanteException(
            "Existe um registro com a mesma referência sem origem comprovada. Confira a"
                + " correspondência antes de importar.");
      }
      if (existente != null) {
        validarConteudoOriginal(dados, existente);
        resultado.put(movimento.chave(), existente);
        continue;
      }
      validarNovo(dados, ponto, catalogo, instituicoesPorId);
      var candidato =
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
              "",
              null,
              IdentidadeNegociacao.chave(movimento.chave()),
              IdentidadeNegociacao.fingerprint(dados, null),
              agora());
      candidato.ordenar(++ordem);
      resultado.put(movimento.chave(), candidato);
    }
    return resultado;
  }

  private DadosNegociacao normalizar(
      MovimentoImportado movimento, long brlId, Map<Long, AtivoResumo> catalogo) {
    TipoOperacao tipo;
    try {
      tipo = TipoOperacao.valueOf(movimento.tipo());
    } catch (IllegalArgumentException erro) {
      throw new OperacaoInvalidaException("Tipo não permitido na importação.");
    }
    if (tipo != TipoOperacao.APORTE && tipo != TipoOperacao.COMPRA)
      throw new OperacaoInvalidaException(
          "Este relatório permite somente aportes e compras em BRL.");
    boolean compra = tipo == TipoOperacao.COMPRA;
    var cadastro =
        new CadastroOperacao(
            tipo,
            movimento.instituicaoId(),
            movimento.ativoId(),
            movimento.ocorridaEm().toString(),
            texto(movimento.quantidade()),
            compra ? brlId : null,
            compra ? texto(movimento.principal()) : null,
            compra ? texto(BigDecimal.ONE) : null,
            compra && movimento.taxa().signum() > 0 ? brlId : null,
            compra ? texto(movimento.taxa()) : null,
            movimento.referencia(),
            "");
    return ValidacaoNegociacao.normalizar(cadastro, catalogo, false);
  }

  private void validarNovo(
      DadosNegociacao dados,
      PontoPartida ponto,
      Map<Long, AtivoResumo> catalogo,
      Map<Long, InstituicaoResumo> instituicoesPorId) {
    var instituicao = instituicoesPorId.get(dados.instituicaoId());
    if (instituicao == null || !instituicao.ativo())
      throw new OperacaoInvalidaException("Selecione uma instituição ativa.");
    if (!catalogo.get(dados.ativoId()).ativo()
        || dados.moedaCotacaoId() != null && !catalogo.get(dados.moedaCotacaoId()).ativo())
      throw new OperacaoInvalidaException("Selecione ativos disponíveis no catálogo.");
    if (ponto != null
        && dados.ocorridaEm().atZone(FUSO).toLocalDate().isBefore(ponto.getDataReferencia()))
      throw new OperacaoInvalidaException(
          "A operação não pode ser anterior ao início do acompanhamento.");
  }

  private void validarConteudoOriginal(DadosNegociacao dados, Operacao original) {
    var anterior =
        new DadosNegociacao(
            original.getTipo(),
            original.getInstituicaoId(),
            original.getAtivoId(),
            original.getOcorridaEm(),
            decimal(original.getQuantidade()),
            original.getMoedaCotacaoId(),
            decimal(original.getTotalBruto()),
            decimal(original.getCotacaoBrl()),
            original.getTaxaAtivoId(),
            decimal(original.getTaxaQuantidade()),
            original.getReferencia(),
            "");
    if (!IdentidadeNegociacao.fingerprint(dados, null)
        .equals(IdentidadeNegociacao.fingerprint(anterior, null)))
      throw new OperacaoConflitanteException(
          "O evento já registrado possui conteúdo financeiro diferente. Confira o arquivo e a"
              + " origem antes de importar.");
  }

  private void validarLivro(List<Operacao> historico, Map<String, Operacao> candidatos) {
    var completo = new ArrayList<>(historico);
    candidatos.values().stream().filter(o -> o.getId() == null).forEach(completo::add);
    conferencia.validarCandidato(completo);
  }

  private List<ReconhecimentoOperacao> reconhecer(
      List<MovimentoImportado> movimentos,
      Map<String, Operacao> candidatos,
      Set<String> reconhecidas) {
    return movimentos.stream()
        .map(m -> {
          var operacao = candidatos.get(m.chave());
          boolean reconhecida = reconhecidas.contains(m.chave());
          return new ReconhecimentoOperacao(
              m.chave(),
              operacao.getId() == null ? null : operacao.getId().toString(),
              reconhecida);
        })
        .toList();
  }

  private Set<String> chavesReconhecidas(Map<String, Operacao> candidatos) {
    return candidatos.entrySet().stream()
        .filter(e -> e.getValue().getId() != null)
        .map(Map.Entry::getKey)
        .collect(Collectors.toSet());
  }

  private void validarLote(List<MovimentoImportado> movimentos) {
    if (movimentos.isEmpty())
      throw new OperacaoInvalidaException("Selecione um arquivo com operações.");
    if (movimentos.stream().map(MovimentoImportado::chave).distinct().count() != movimentos.size())
      throw new OperacaoInvalidaException("O arquivo contém identificadores de evento repetidos.");
  }

  private void exigirPosicaoAnteriorZerada(boolean confirmada) {
    if (!confirmada)
      throw new OperacaoInvalidaException(
          "Confirme a posição anterior zerada ou cadastre o saldo inicial antes de importar.");
  }

  private static BigDecimal decimal(BigDecimal valor) {
    return valor == null ? null : valor.stripTrailingZeros();
  }

  private static StringNode texto(BigDecimal valor) {
    return valor == null ? null : StringNode.valueOf(valor.toPlainString());
  }

  private static Instant agora() {
    return Instant.now().truncatedTo(ChronoUnit.MICROS);
  }
}
