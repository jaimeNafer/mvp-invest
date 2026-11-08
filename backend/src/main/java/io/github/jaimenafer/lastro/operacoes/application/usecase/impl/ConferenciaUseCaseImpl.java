package io.github.jaimenafer.lastro.operacoes.application.usecase.impl;

import static io.github.jaimenafer.lastro.operacoes.application.mapper.ConferenciaMapper.*;

import io.github.jaimenafer.lastro.acesso.application.contract.IUsuarioAtual;
import io.github.jaimenafer.lastro.ativos.application.contract.*;
import io.github.jaimenafer.lastro.operacoes.application.contract.*;
import io.github.jaimenafer.lastro.operacoes.application.dto.*;
import io.github.jaimenafer.lastro.operacoes.application.mapper.ConferenciaMapper;
import io.github.jaimenafer.lastro.operacoes.application.port.IAssociacoesLivro;
import io.github.jaimenafer.lastro.operacoes.application.port.IConsultaConferencia;
import io.github.jaimenafer.lastro.operacoes.application.port.IPreparacaoConciliacao;
import io.github.jaimenafer.lastro.operacoes.application.usecase.IConferenciaUseCase;
import io.github.jaimenafer.lastro.operacoes.domain.exception.*;
import io.github.jaimenafer.lastro.operacoes.domain.model.*;
import io.github.jaimenafer.lastro.operacoes.domain.repository.*;
import io.github.jaimenafer.lastro.operacoes.domain.service.ClassificacaoMovimentacao;
import java.math.BigDecimal;
import java.time.*;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.*;

@Service
@RequiredArgsConstructor
public class ConferenciaUseCaseImpl implements IConferenciaUseCase {
  private final IUsuarioAtual usuario;
  private final IPontoPartidaRepository pontos;
  private final IRegistroConferenciaRepository registros;
  private final IContaCustodiaRepository custodias;
  private final IAjusteConciliatorioRepository ajustes;
  private final IComandoConciliacaoRepository comandos;
  private final IConsultaConferencia consulta;
  private final IPreparacaoConciliacao preparacao;
  private final IConsultaAtivos ativos;
  private final IAssociacoesLivro associacoes;

  private static final ZoneId FUSO = ZoneId.of("America/Fortaleza");

  private PontoPartida bloquear() {
    Optional<PontoPartida> ponto = pontos.bloquear(usuario.id());
    if (ponto.isEmpty()) {
      pontos.inserir(PontoPartida.iniciar(usuario.id(), LocalDate.now(FUSO), Instant.now()));
      ponto = pontos.bloquear(usuario.id());
    }
    return ponto.orElseThrow();
  }

  private String revisao() {
    return Long.toString(pontos.findById(usuario.id()).map(PontoPartida::getRevisao).orElse(0L));
  }

  @Transactional(readOnly = true, isolation = Isolation.REPEATABLE_READ)
  public TimelineConferenciaResponse listar(
      int pagina, int tamanho, String estado, Long ativoId, Long contaId) {
    if (pagina < 0 || tamanho < 1 || tamanho > 100)
      throw new OperacaoInvalidaException("Informe página não negativa e tamanho entre 1 e 100.");
    EstadoConferencia filtro = null;
    if (estado != null && !estado.isBlank())
      try {
        filtro = EstadoConferencia.valueOf(estado);
      } catch (IllegalArgumentException e) {
        throw new OperacaoInvalidaException("Situação de conferência inválida.");
      }
    List<RegistroTimelineResponse> todas = consulta.registros();
    EstadoConferencia selecionado = filtro;
    List<RegistroTimelineResponse> filtradas =
        todas.stream()
            .filter(r -> selecionado == null || r.estado() == selecionado)
            .filter(r -> ativoId == null || ativoId.toString().equals(r.ativoId()))
            .filter(r -> contaId == null || contaId.toString().equals(r.contaId()))
            .toList();
    Set<String> gruposExibidos = new HashSet<>();
    filtradas =
        filtradas.stream()
            .filter(r -> r.chaveGrupo() == null || gruposExibidos.add(r.chaveGrupo()))
            .toList();
    List<RegistroTimelineResponse> itens =
        filtradas.stream().skip((long) pagina * tamanho).limit(tamanho).toList();
    Set<String> gruposNaPagina =
        itens.stream().map(RegistroTimelineResponse::chaveGrupo)
            .filter(Objects::nonNull).collect(Collectors.toSet());
    Map<String, List<RegistroTimelineResponse>> linhasPorGrupo =
        todas.stream()
            .filter(r -> r.chaveGrupo() != null && gruposNaPagina.contains(r.chaveGrupo()))
            .collect(Collectors.groupingBy(RegistroTimelineResponse::chaveGrupo));
    RegistroTimelineResponse proxima =
        filtradas.stream()
            .filter(
                r -> r.estado() != EstadoConferencia.COERENTE && !r.origem().equals("SINTETICA"))
            .min(prioridade(todas))
            .orElse(null);
    return new TimelineConferenciaResponse(
        itens,
        filtradas.size(),
        pagina,
        tamanho,
        revisao(),
        consulta.resumir(todas),
        proxima == null ? null : proxima.id(),
        proxima == null ? null : filtradas.indexOf(proxima) / tamanho,
        linhasPorGrupo);
  }

  private Comparator<RegistroTimelineResponse> prioridade(List<RegistroTimelineResponse> linhas) {
    Map<String, Set<String>> afetados = new HashMap<>();
    var padrao = java.util.regex.Pattern.compile("(?:registro|operacao):[0-9]+");
    for (RegistroTimelineResponse linha : linhas)
      for (PendenciaConferenciaResponse p : linha.pendencias()) {
        var matcher = padrao.matcher(p.id());
        while (matcher.find())
          afetados.computeIfAbsent(matcher.group(), k -> new HashSet<>()).add(linha.id());
      }
    return Comparator.comparingInt(
            (RegistroTimelineResponse r) -> afetados.getOrDefault(r.id(), Set.of()).size())
        .reversed()
        .thenComparing(
            Comparator.comparingInt((RegistroTimelineResponse r) ->
                    r.estado() == EstadoConferencia.CORRECAO_NECESSARIA ? 1 : 0)
                .reversed())
        .thenComparing(r -> r.data() == null ? Instant.MAX : instante(r.data()))
        .thenComparing(RegistroTimelineResponse::id);
  }

  @Transactional(readOnly = true, isolation = Isolation.REPEATABLE_READ)
  public List<ContaConferenciaResponse> contas() {
    return consulta.contas();
  }

  @Transactional(readOnly = true, isolation = Isolation.REPEATABLE_READ)
  public PreviaConferenciaResponse prever(PreviaConferenciaRequest request) {
    validarRevisao(request.revisao());
    return preparacao.preparar(revisao());
  }

  @Transactional(isolation = Isolation.READ_COMMITTED)
  public RevisaoConferenciaResponse confirmar(ConfirmacaoConferenciaRequest request, String chave) {
    PontoPartida ponto = bloquear();
    String fingerprint = fingerprint("confirmar", request);
    RevisaoConferenciaResponse repetida =
        repetir(chave, fingerprint, RevisaoConferenciaResponse.class);
    if (repetida != null) return repetida;
    validarRevisao(request.revisao());
    PreviaConferenciaResponse previa = preparacao.preparar(revisao());
    if (!previa.previaId().equals(request.previaId()))
      throw new OperacaoConflitanteException(
          "A prévia mudou. Atualize a conferência e revise as sugestões.");
    Set<String> aceitas = semDuplicidades(request.aceitarLacunaIds());
    Set<String> transferencias = semDuplicidades(request.aceitarCorrespondenciaIds());
    if (!previa.ajustes().stream()
            .map(AjusteConferenciaResponse::lacunaId)
            .collect(Collectors.toSet())
            .containsAll(aceitas)
        || !previa.correspondencias().stream()
            .map(CorrespondenciaConferenciaResponse::id)
            .collect(Collectors.toSet())
            .containsAll(transferencias))
      throw new OperacaoInvalidaException("A seleção contém sugestões que não pertencem à prévia.");
    for (AjusteConferenciaResponse ajuste : previa.ajustes())
      if (!ajuste.acao().equals("CRIAR")
          && !ajuste.acao().equals("MANTER")
          && !aceitas.contains(ajuste.lacunaId()))
        throw new OperacaoConflitanteException(
            "Confirme a redução ou encerramento dos ajustes anteriores antes de aplicar os"
                + " registros que os substituem.");
    Map<String, AjusteConciliatorio> anteriores =
        ajustes.listar(usuario.id()).stream()
            .collect(
                Collectors.toMap(
                    AjusteConciliatorio::getLacunaRef,
                    Function.identity(),
                    (a, b) -> a.getVersao() > b.getVersao() ? a : b));
    boolean mudou = associacoes.aplicar(consulta.impedimentosOrigem());
    for (AjusteConferenciaResponse a : previa.ajustes())
      if (aceitas.contains(a.lacunaId()) && !a.acao().equals("MANTER")) {
        AjusteConciliatorio anterior = anteriores.get(a.lacunaId());
        if (anterior != null && anterior.isAtivo()) {
          anterior.encerrar(Instant.now());
          ajustes.flush();
        }
        if (!a.acao().equals("ENCERRAR"))
          ajustes.salvar(
              AjusteConciliatorio.aceitar(
                  usuario.id(),
                  a.lacunaId(),
                  a.ancoraId(),
                  id(a.contaId()),
                  custodias.buscar(usuario.id(), id(a.contaId())).getInstituicaoId(),
                  id(a.ativoId()),
                  numero(a.quantidadeSugerida()),
                  numero(a.custoTotalBrl()),
                  anterior == null ? 1 : anterior.getVersao() + 1,
                  anterior == null ? null : anterior.getId(),
                  usuario.id(),
                  Instant.now()));
        mudou = true;
      }
    Map<String, String> pares = new HashMap<>();
    previa.correspondencias().stream()
        .filter(p -> transferencias.contains(p.id()))
        .forEach(p -> p.registroIds().forEach(id -> pares.put(id, p.id())));
    Map<Long, PropostaRegistro> propostas = consulta.propostasParaAplicar();
    for (RegistroConferencia r : registros.listar(usuario.id())) {
      PropostaRegistro p = propostas.get(r.getId());
      String ref = pares.get("registro:" + r.getId());
      if (ref != null) {
        p = ConferenciaMapper.proposta(r, true);
        p =
            new PropostaRegistro(
                p.data(),
                p.contaId(),
                p.ativoId(),
                p.quantidade(),
                p.custoTotalBrl(),
                p.natureza(),
                p.motivo(),
                p.moedaCotacaoId(),
                p.totalBruto(),
                p.cotacaoBrl(),
                p.taxaAtivoId(),
                p.taxaQuantidade(),
                p.taxaInformada(),
                ref,
                p.finalidadeCripto());
        r.editar(ConferenciaMapper.escrever(p), usuario.id(), Instant.now());
      }
      if (p != null && !p.equals(ConferenciaMapper.proposta(r, false))) {
        r.aplicar(ConferenciaMapper.escrever(p));
        mudou = true;
      }
    }
    if (mudou) ponto.avancarRevisao(usuario.id(), Instant.now());
    RevisaoConferenciaResponse resposta =
        new RevisaoConferenciaResponse(Long.toString(ponto.getRevisao()));
    salvarComando(chave, fingerprint, resposta);
    return resposta;
  }

  private Set<String> semDuplicidades(List<String> ids) {
    Set<String> unicos = new HashSet<>(ids);
    if (unicos.size() != ids.size())
      throw new OperacaoInvalidaException("Não repita sugestões na confirmação.");
    return unicos;
  }

  @Transactional(isolation = Isolation.READ_COMMITTED)
  public RegistroTimelineResponse editar(String id, EdicaoRegistroRequest request, String chave) {
    PontoPartida ponto = bloquear();
    String fingerprint = fingerprint("editar:" + id, request);
    RegistroTimelineResponse repetida = repetir(chave, fingerprint, RegistroTimelineResponse.class);
    if (repetida != null) return repetida;
    validarRevisao(request.revisao());
    RegistroConferencia r = registros.buscar(usuario.id(), registroId(id));
    validarVersao(r, request.versao());
    PropostaRegistro p =
        new PropostaRegistro(
            request.data(),
            request.contaId(),
            request.ativoId(),
            textoDecimal(request.quantidade(), "quantidade"),
            textoDecimal(request.custoTotalBrl(), "custo total em reais"),
            request.natureza(),
            request.motivo(),
            request.moedaCotacaoId(),
            textoDecimal(request.totalBruto(), "total bruto"),
            textoDecimal(request.cotacaoBrl(), "cotação em reais"),
            request.taxaAtivoId(),
            textoDecimal(request.taxaQuantidade(), "quantidade da taxa"),
            Boolean.TRUE.equals(request.taxaInformada()),
            null,
            request.finalidadeCripto());
    if (p.natureza() == NaturezaRegistro.CONVERSAO)
      throw new OperacaoInvalidaException("Associe as linhas para registrar uma conversão.");
    if (p.natureza() == NaturezaRegistro.CREDITO_AUTOMATICO
        && (!"IMPORTADA".equals(ConferenciaMapper.origem(r).origem())
            || ClassificacaoMovimentacao.subtipoCredito(ConferenciaMapper.origem(r).operacao()) == null))
      throw new OperacaoInvalidaException("O registro não corresponde a um crédito automático reconhecido.");
    if (p.natureza() == NaturezaRegistro.MOVIMENTACAO_INTERNA
        && ClassificacaoMovimentacao.classificarMovimentacaoEarn(
            ConferenciaMapper.origem(r).operacao(), numero(p.quantidade()))
            != NaturezaRegistro.MOVIMENTACAO_INTERNA)
      throw new OperacaoInvalidaException("A movimentação interna exige tipo e sinal compatíveis.");
    validarProposta(p, r.getInstituicaoId());
    if (!p.equals(ConferenciaMapper.proposta(r, true))) {
      r.editar(ConferenciaMapper.escrever(p), usuario.id(), Instant.now());
      ponto.avancarRevisao(usuario.id(), Instant.now());
    }
    registros.flush();
    RegistroTimelineResponse resposta = linha(id);
    salvarComando(chave, fingerprint, resposta);
    return resposta;
  }

  @Transactional(isolation = Isolation.READ_COMMITTED)
  public RegistroTimelineResponse revisar(String id, RevisaoRegistroRequest request, String chave) {
    PontoPartida ponto = bloquear();
    String fingerprint = fingerprint("revisar:" + id, request);
    RegistroTimelineResponse repetida = repetir(chave, fingerprint, RegistroTimelineResponse.class);
    if (repetida != null) return repetida;
    validarRevisao(request.revisao());
    RegistroConferencia r = registros.buscar(usuario.id(), registroId(id));
    validarVersao(r, request.versao());
    r.revisar(usuario.id(), Instant.now());
    registros.flush();
    RegistroTimelineResponse resposta = linha(id);
    salvarComando(chave, fingerprint, resposta);
    return resposta;
  }

  @Transactional(isolation = Isolation.READ_COMMITTED)
  public RevisaoConferenciaResponse saldoAnterior(
      SaldoAnteriorConferenciaRequest request, String chave) {
    PontoPartida ponto = bloquear();
    String fingerprint = fingerprint("saldo", request);
    RevisaoConferenciaResponse repetida =
        repetir(chave, fingerprint, RevisaoConferenciaResponse.class);
    if (repetida != null) return repetida;
    validarRevisao(request.revisao());
    RegistroConferencia ancora = registros.buscar(usuario.id(), registroId(request.ancoraId()));
    PropostaRegistro p =
        new PropostaRegistro(
            request.data(),
            request.contaId(),
            request.ativoId(),
            textoDecimal(request.quantidade(), "quantidade"),
            textoDecimal(request.custoTotalBrl(), "custo total em reais"),
            NaturezaRegistro.SALDO_ANTERIOR,
            request.motivo(),
            null,
            null,
            null,
            null,
            null,
            false,
            null);
    validarProposta(p, ancora.getInstituicaoId());
    if (instante(p.data()) == null
        || numero(p.quantidade()) == null
        || numero(p.quantidade()).signum() <= 0)
      throw new OperacaoInvalidaException("Informe data e quantidade positiva do saldo anterior.");
    RegistroConferencia r =
        RegistroConferencia.criar(
            usuario.id(),
            null,
            "manual:" + chave,
            ancora.getInstituicaoId(),
            ConferenciaMapper.escrever(
                new OrigemRegistro(
                    Map.of(),
                    List.of(),
                    "Saldo anterior declarado",
                    ativos.buscar(id(p.ativoId())).orElseThrow().ticker(),
                    "MANUAL",
                    null,
                    null,
                    null)),
            "[]",
            ConferenciaMapper.escrever(p),
            Instant.now());
    registros.salvar(r);
    ponto.avancarRevisao(usuario.id(), Instant.now());
    RevisaoConferenciaResponse resposta =
        new RevisaoConferenciaResponse(Long.toString(ponto.getRevisao()));
    salvarComando(chave, fingerprint, resposta);
    return resposta;
  }

  private void validarProposta(PropostaRegistro p, Long instituicao) {
    ContaCustodia conta = custodias.buscar(usuario.id(), id(p.contaId()));
    if (!conta.getInstituicaoId().equals(instituicao))
      throw new OperacaoInvalidaException(
          "A conta de custódia deve pertencer à mesma instituição do registro.");
    Instant data = instante(p.data());
    if (data != null && data.isAfter(Instant.now()))
      throw new OperacaoInvalidaException("A data do registro não pode ser futura.");
    BigDecimal quantidade = numero(p.quantidade());
    BigDecimal custo = numero(p.custoTotalBrl());
    if (custo != null
        && (custo.signum() < 0 || Math.max(0, custo.stripTrailingZeros().scale()) > 8))
      throw new OperacaoInvalidaException("Informe custo não negativo com até 8 casas decimais.");
    if (custo != null && custo.signum() == 0 && (p.motivo() == null || p.motivo().isBlank()))
      throw new OperacaoInvalidaException("Explique o custo zero declarado.");
    if (p.ativoId() != null) {
      AtivoResumo ativo =
          ativos
              .buscar(id(p.ativoId()))
              .orElseThrow(
                  () -> new OperacaoInvalidaException("Ativo não encontrado no catálogo."));
      if (quantidade != null
          && Math.max(0, quantidade.stripTrailingZeros().scale()) > ativo.casasDecimais())
        throw new OperacaoInvalidaException("A quantidade excede a precisão do ativo.");
    }
    if (p.natureza() != NaturezaRegistro.OBSERVADA
        && p.natureza() != NaturezaRegistro.RETIRADA
        && p.natureza() != NaturezaRegistro.SAIDA_CRIPTO
        && p.natureza() != NaturezaRegistro.MOVIMENTACAO_INTERNA
        && quantidade != null
        && quantidade.signum() <= 0)
      throw new OperacaoInvalidaException("A quantidade da operação financeira deve ser positiva.");
    if (p.natureza() == NaturezaRegistro.RETIRADA
        && quantidade != null && quantidade.signum() >= 0)
      throw new OperacaoInvalidaException("A retirada deve ter quantidade negativa.");
    if (p.natureza() == NaturezaRegistro.SAIDA_CRIPTO) {
      if (quantidade != null && quantidade.signum() >= 0)
        throw new OperacaoInvalidaException("A saída de criptomoeda deve ter quantidade negativa.");
      if (p.ativoId() != null && !ativos.buscar(id(p.ativoId())).orElseThrow().cripto())
        throw new OperacaoInvalidaException("A saída de criptomoeda exige um criptoativo.");
      if (p.taxaInformada() && p.taxaQuantidade() == null)
        throw new OperacaoInvalidaException("Informe a quantidade da taxa conhecida.");
      if (p.taxaAtivoId() != null && !p.taxaAtivoId().equals(p.ativoId()))
        throw new OperacaoInvalidaException("A taxa incluída deve usar o ativo da saída.");
      if (p.taxaQuantidade() != null && quantidade != null
          && numero(p.taxaQuantidade()).compareTo(quantidade.abs()) > 0)
        throw new OperacaoInvalidaException("A taxa incluída não pode exceder a saída.");
    } else if (p.finalidadeCripto() != null) {
      throw new OperacaoInvalidaException("A finalidade pertence a uma saída de criptomoeda.");
    }
    for (String valor : Arrays.asList(p.totalBruto(), p.cotacaoBrl(), p.taxaQuantidade()))
      if (valor != null && numero(valor).signum() < 0)
        throw new OperacaoInvalidaException("Valores de negociação não podem ser negativos.");
    for (String valor : Arrays.asList(p.moedaCotacaoId(), p.taxaAtivoId()))
      if (valor != null)
        ativos
            .buscar(id(valor))
            .orElseThrow(
                () -> new OperacaoInvalidaException("Moeda da negociação não encontrada."));
    if ((p.natureza() == NaturezaRegistro.APORTE || p.natureza() == NaturezaRegistro.RETIRADA)
        && p.ativoId() != null
        && !ativos.buscar(id(p.ativoId())).orElseThrow().codigo().equals("fiat:BRL"))
      throw new OperacaoInvalidaException("Aporte e retirada utilizam somente BRL.");
    boolean negociacao =
        p.natureza() == NaturezaRegistro.COMPRA || p.natureza() == NaturezaRegistro.VENDA;
    if (negociacao
        && legivel(p)
        && p.moedaCotacaoId() != null
        && p.totalBruto() != null
        && p.cotacaoBrl() != null
        && p.taxaInformada()
        && p.taxaQuantidade() != null) {
      Set<Long> ids = new HashSet<>();
      ids.add(id(p.ativoId()));
      ids.add(id(p.moedaCotacaoId()));
      if (p.taxaAtivoId() != null) ids.add(id(p.taxaAtivoId()));
      var catalogo =
          ativos.buscarPorIds(ids).stream()
              .collect(Collectors.toMap(AtivoResumo::id, Function.identity()));
      ValidacaoNegociacao.normalizar(
          new CadastroOperacao(
              p.natureza() == NaturezaRegistro.COMPRA ? TipoOperacao.COMPRA : TipoOperacao.VENDA,
              instituicao,
              id(p.ativoId()),
              p.data(),
              texto(p.quantidade()),
              id(p.moedaCotacaoId()),
              texto(p.totalBruto()),
              texto(p.cotacaoBrl()),
              id(p.taxaAtivoId()),
              texto(p.taxaQuantidade()),
              "",
              ""),
          catalogo,
          false);
    }
  }

  private void validarRevisao(String esperado) {
    if (!revisao().equals(esperado))
      throw new OperacaoConflitanteException(
          "O histórico mudou. Atualize a prévia antes de confirmar.");
  }

  private void validarVersao(RegistroConferencia r, String versao) {
    if (!Long.toString(r.getVersao()).equals(versao))
      throw new OperacaoConflitanteException(
          "O registro foi alterado. Recarregue seus dados antes de editar.");
  }

  private RegistroTimelineResponse linha(String id) {
    return consulta.registros().stream()
        .filter(r -> r.id().equals(id))
        .findFirst()
        .orElseThrow(() -> new OperacaoNaoEncontradaException("Registro não encontrado."));
  }

  private String fingerprint(String acao, Object request) {
    return hash(acao + "|" + ConferenciaMapper.escrever(request));
  }

  private UUID chave(String chave) {
    try {
      return UUID.fromString(chave);
    } catch (IllegalArgumentException e) {
      throw new OperacaoInvalidaException("Informe uma chave de idempotência UUID válida.");
    }
  }

  private <T> T repetir(String chave, String fingerprint, Class<T> tipo) {
    Optional<ComandoConciliacao> anterior = comandos.buscar(usuario.id(), chave(chave));
    if (anterior.isEmpty()) return null;
    if (!anterior.get().getFingerprint().equals(fingerprint))
      throw new OperacaoConflitanteException(
          "A chave de idempotência já foi usada com outro conteúdo.");
    return ConferenciaMapper.ler(anterior.get().getRespostaJson(), tipo);
  }

  private void salvarComando(String chave, String fingerprint, Object resposta) {
    comandos.salvar(
        ComandoConciliacao.registrar(
            usuario.id(),
            chave(chave),
            fingerprint,
            ConferenciaMapper.escrever(resposta),
            Instant.now()));
  }
}
