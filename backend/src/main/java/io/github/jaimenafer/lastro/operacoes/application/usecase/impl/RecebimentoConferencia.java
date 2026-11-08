package io.github.jaimenafer.lastro.operacoes.application.usecase.impl;

import static io.github.jaimenafer.lastro.operacoes.application.mapper.ConferenciaMapper.*;

import io.github.jaimenafer.lastro.acesso.application.contract.IUsuarioAtual;
import io.github.jaimenafer.lastro.ativos.application.contract.*;
import io.github.jaimenafer.lastro.instituicoes.application.contract.IConsultaInstituicoes;
import io.github.jaimenafer.lastro.operacoes.application.contract.*;
import io.github.jaimenafer.lastro.operacoes.application.dto.*;
import io.github.jaimenafer.lastro.operacoes.application.mapper.ConferenciaMapper;
import io.github.jaimenafer.lastro.operacoes.domain.exception.*;
import io.github.jaimenafer.lastro.operacoes.domain.model.*;
import io.github.jaimenafer.lastro.operacoes.domain.repository.*;
import java.math.BigDecimal;
import java.text.Normalizer;
import java.time.*;
import java.util.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.*;

@Service
@RequiredArgsConstructor
public class RecebimentoConferencia implements IRecebimentoConferencia {
  private final IUsuarioAtual usuario;
  private final IPontoPartidaRepository pontos;
  private final IOperacaoRepository operacoes;
  private final IRegistroConferenciaRepository registros;
  private final IContaCustodiaRepository custodias;
  private final IConsultaAtivos ativos;
  private final IConsultaInstituicoes instituicoes;
  private static final ZoneId FUSO = ZoneId.of("America/Fortaleza");

  @Transactional(isolation = Isolation.READ_COMMITTED)
  public void bloquearHistorico() {
    bloquear();
  }

  public void validarReferenciasDisponiveis(List<String> referencias) {
    Set<String> ids = new HashSet<>(referencias);
    boolean recebida =
        registros.listar(usuario.id()).stream()
            .map(r -> ConferenciaMapper.origem(r).campos().get("id"))
            .filter(Objects::nonNull)
            .anyMatch(ids::contains);
    if (recebida)
      throw new OperacaoConflitanteException(
          "Há eventos deste relatório já recebidos no Histórico. Continue a conferência para evitar"
              + " aplicar a mesma referência novamente.");
  }

  private PontoPartida bloquear() {
    Optional<PontoPartida> ponto = pontos.bloquear(usuario.id());
    if (ponto.isEmpty()) {
      pontos.inserir(PontoPartida.iniciar(usuario.id(), LocalDate.now(FUSO), Instant.now()));
      ponto = pontos.bloquear(usuario.id());
    }
    return ponto.orElseThrow();
  }

  public String revisao() {
    return Long.toString(pontos.findById(usuario.id()).map(PontoPartida::getRevisao).orElse(0L));
  }

  @Transactional(isolation = Isolation.READ_COMMITTED)
  public String registrar(String arquivoRef, String hash, List<LinhaConferenciaRecebida> linhas) {
    PontoPartida ponto = bloquear();
    Long instituicao =
        instituicoes
            .buscarPorCodigo("binance")
            .orElseThrow(
                () -> new OperacaoInvalidaException("Binance não está cadastrada no catálogo."))
            .id();
    Map<String, ContaCustodia> porNome = new HashMap<>();
    Map<String, Optional<AtivoResumo>> porMoeda = new HashMap<>();
    boolean mudou = false;
    LocalDate primeira =
        linhas.stream()
            .map(LinhaConferenciaRecebida::ocorridaEm)
            .filter(Objects::nonNull)
            .filter(i -> !i.isAfter(Instant.now()) && i.atZone(FUSO).getYear() >= 1000)
            .map(i -> i.atZone(FUSO).toLocalDate())
            .min(LocalDate::compareTo)
            .orElse(null);
    if (primeira != null
        && primeira.isBefore(ponto.getDataReferencia())
        && operacoes.historico(usuario.id()).stream()
            .noneMatch(o -> o.confirmada() && o.getTipo() == TipoOperacao.SALDO_INICIAL))
      ponto.redefinir(primeira, Instant.now(), usuario.id());
    for (LinhaConferenciaRecebida linha : linhas) {
      String origemRef =
          hash(
              hash
                  + "|"
                  + linha.aba()
                  + "|"
                  + linha.linha()
                  + "|"
                  + linha.camposOriginais().getOrDefault("componente", ""));
      if (registros.buscarPorOrigem(usuario.id(), origemRef).isPresent()) continue;
      String contaNome =
          linha.conta() == null || linha.conta().isBlank()
              ? "Não especificada"
              : linha.conta().strip();
      String codigo =
          contaNome.equals("Não especificada") ? "nao_especificada" : normalizar(contaNome);
      if (codigo.isEmpty() || codigo.length() > 100) codigo = "conta_" + hash(contaNome);
      String nomeExibido =
          contaNome.length() > 160 ? contaNome.substring(0, 157) + "..." : contaNome;
      ContaCustodia conta =
          porNome.computeIfAbsent(
              codigo,
              k ->
                  custodias
                      .buscarPorCodigo(usuario.id(), instituicao, k)
                      .orElseGet(() ->
                          custodias.salvar(
                              ContaCustodia.criar(
                                  usuario.id(), instituicao, k, nomeExibido, Instant.now()))));
      Optional<AtivoResumo> ativo =
          porMoeda.computeIfAbsent(
              linha.moeda(),
              k ->
                  ativos.buscarPorCodigo(
                      k.equals("BRL") || k.equals("USD") ? "fiat:" + k : "cripto:" + k));
      BigDecimal quantidade = linha.quantidade();
      if (quantidade != null
          && (Math.max(0, quantidade.stripTrailingZeros().scale()) > 18
              || quantidade.precision() - quantidade.scale() > 20)) quantidade = null;
      PropostaRegistro proposta =
          new PropostaRegistro(
              linha.ocorridaEm() == null
                      || linha.ocorridaEm().isAfter(Instant.now())
                      || linha.ocorridaEm().atZone(FUSO).getYear() < 1000
                  ? null
                  : linha.ocorridaEm().toString(),
              conta.getId().toString(),
              ativo.map(a -> a.id().toString()).orElse(null),
              decimal(quantidade),
              null,
              NaturezaRegistro.OBSERVADA,
              null,
              null,
              null,
              null,
              null,
              null,
              false,
              null);
      OrigemRegistro origem =
          new OrigemRegistro(
              linha.camposOriginais(),
              linha.erros(),
              linha.operacao(),
              linha.moeda(),
              "IMPORTADA",
              arquivoRef,
              linha.linha(),
              linha.aba());
      registros.salvar(
          RegistroConferencia.criar(
              usuario.id(),
              arquivoRef,
              origemRef,
              instituicao,
              ConferenciaMapper.escrever(origem),
              ConferenciaMapper.escrever(linha.erros()),
              ConferenciaMapper.escrever(proposta),
              Instant.now()));
      mudou = true;
    }
    if (mudou) ponto.avancarRevisao(usuario.id(), Instant.now());
    return Long.toString(ponto.getRevisao());
  }

  private String normalizar(String nome) {
    return Normalizer.normalize(nome, Normalizer.Form.NFD)
        .replaceAll("\\p{M}", "")
        .toLowerCase(Locale.ROOT)
        .replaceAll("[^a-z0-9]+", "_");
  }
}
