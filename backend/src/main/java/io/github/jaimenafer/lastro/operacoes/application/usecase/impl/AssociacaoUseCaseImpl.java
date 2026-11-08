package io.github.jaimenafer.lastro.operacoes.application.usecase.impl;

import static io.github.jaimenafer.lastro.operacoes.application.mapper.ConferenciaMapper.*;

import io.github.jaimenafer.lastro.acesso.application.contract.IUsuarioAtual;
import io.github.jaimenafer.lastro.ativos.application.contract.AtivoResumo;
import io.github.jaimenafer.lastro.ativos.application.contract.IConsultaAtivos;
import io.github.jaimenafer.lastro.operacoes.application.config.IConfiguracaoAssociacao;
import io.github.jaimenafer.lastro.operacoes.application.dto.*;
import io.github.jaimenafer.lastro.operacoes.application.port.*;
import io.github.jaimenafer.lastro.operacoes.application.usecase.IAssociacaoUseCase;
import io.github.jaimenafer.lastro.operacoes.domain.exception.*;
import io.github.jaimenafer.lastro.operacoes.domain.model.*;
import io.github.jaimenafer.lastro.operacoes.domain.repository.*;
import io.github.jaimenafer.lastro.operacoes.domain.service.AgrupamentoNegociacoes;
import java.math.BigDecimal;
import java.time.*;
import java.util.*;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.*;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true, isolation = Isolation.REPEATABLE_READ)
public class AssociacaoUseCaseImpl implements IAssociacaoUseCase {
  private final IUsuarioAtual usuario;
  private final IConsultaAtivos ativos;
  private final IPontoPartidaRepository pontos;
  private final IGrupoAssociacaoRepository grupos;
  private final IComandoConciliacaoRepository comandos;
  private final IAssociacoesLivro livro;
  private final IConsultaConferencia consulta;
  private final IConfiguracaoAssociacao propriedades;

  private String revisao() {
    return Long.toString(pontos.findById(usuario.id()).map(PontoPartida::getRevisao).orElse(0L));
  }

  private void validarRevisao(String esperada) {
    if (!revisao().equals(esperada))
      throw new OperacaoConflitanteException(
          "O histórico mudou. Atualize a associação antes de continuar.");
  }

  private Long grupoId(String numero) {
    if (numero == null || !numero.matches("OP-[1-9][0-9]*"))
      throw new OperacaoInvalidaException(
          "Informe o número da operação agrupada no formato OP-123.");
    return id(numero.substring(3));
  }

  private PropostaGrupo proposta(GrupoAssociacao grupo) {
    return ler(grupo.getPropostaJson(), PropostaGrupo.class);
  }

  private GrupoEmConferencia detalhes(GrupoAssociacao g) {
    PropostaGrupo p = proposta(g);
    ResumoGrupo resumo =
        p.desfazer()
            ? new ResumoGrupo(
                null,
                List.of(),
                List.of(),
                List.of(),
                null,
                false,
                List.of("Associação proposta para desfazimento."))
            : ValidacaoGrupo.resumir(p, catalogo(p.linhas()), propriedades.janelaAssociacao());
    return new GrupoEmConferencia(g.getId(), "grupo:" + g.getId(), p, resumo, g.pendente(), false);
  }

  public GrupoAssociacaoResponse buscar(String numero) {
    return livro.response(detalhes(grupos.buscar(usuario.id(), grupoId(numero))), revisao());
  }

  public CandidatosAssociacaoResponse candidatos(String registroId) {
    long id = registroId(registroId);
    List<LinhaAssociacao> todas = livro.linhas();
    LinhaAssociacao origem =
        todas.stream()
            .filter(l -> l.registroId() == id)
            .findFirst()
            .orElseThrow(() ->
                new OperacaoNaoEncontradaException(
                    "Linha importada não encontrada para associação."));
    Map<Long, String> ocupados = new HashMap<>();
    GrupoAssociacao grupoAtual = null;
    for (var g : grupos.listar(usuario.id())) {
      PropostaGrupo p = proposta(g);
      if (!p.desfazer())
        for (var l : p.linhas()) {
          ocupados.put(l.registroId(), "OP-" + g.getId());
          if (l.registroId() == id) grupoAtual = g;
        }
    }
    Set<Long> noGrupo =
        grupoAtual == null
            ? Set.of()
            : proposta(grupoAtual).linhas().stream()
                .map(LinhaAssociacao::registroId)
                .collect(Collectors.toSet());
    List<LinhaAssociacao> proximas =
        todas.stream()
            .filter(l ->
                l.registroId() == id
                    || noGrupo.contains(l.registroId())
                    || (origem.data() != null
                        && l.data() != null
                        && origem.contaId() != null
                        && origem.contaId().equals(l.contaId())
                        && origem.instituicaoId().equals(l.instituicaoId())
                        && Duration.between(origem.data(), l.data())
                                .abs()
                                .compareTo(propriedades.janelaAssociacao())
                            <= 0))
            .sorted(
                Comparator.comparing(
                        LinhaAssociacao::data, Comparator.nullsLast(Comparator.naturalOrder()))
                    .thenComparingLong(LinhaAssociacao::registroId))
            .toList();
    List<LinhaAssociacaoResponse> candidatas = livro.linhasResponse(proximas, ocupados);
    List<String> avisos = new ArrayList<>();
    if (candidatas.stream()
        .anyMatch(l ->
            !Objects.equals(l.data(), origem.data() == null ? null : origem.data().toString())))
      avisos.add(
          "Os horários são diferentes. A associação exige revisão manual e intervalo total de até"
              + " dois segundos.");
    if (candidatas.size() > 2)
      avisos.add(
          "Há várias linhas candidatas. Confira referências e papéis; o mesmo segundo não"
              + " identifica uma negociação.");
    if (candidatas.stream()
            .map(LinhaAssociacaoResponse::referencia)
            .filter(AgrupamentoNegociacoes::referenciaForte)
            .distinct()
            .count()
        > 1)
      avisos.add(
          "Há referências conflitantes entre os candidatos. Linhas dessas referências não podem"
              + " formar o mesmo grupo.");
    return new CandidatosAssociacaoResponse(
        revisao(),
        candidatas,
        grupoAtual == null ? null : livro.response(detalhes(grupoAtual), revisao()),
        List.copyOf(avisos));
  }

  public GrupoAssociacaoResponse prever(AssociacaoRequest request) {
    validarRevisao(request.revisao());
    GrupoAssociacao grupo =
        request.numero() == null ? null : grupos.buscar(usuario.id(), grupoId(request.numero()));
    if (grupo != null && !Long.toString(grupo.getVersao()).equals(request.versao()))
      throw new OperacaoConflitanteException("A operação agrupada mudou. Recarregue sua versão.");
    Map<Long, LinhaAssociacao> atuais =
        livro.linhas().stream().collect(Collectors.toMap(LinhaAssociacao::registroId, l -> l));
    List<LinhaAssociacao> selecionadas = new ArrayList<>();
    Map<Long, String> impedimentos = consulta.impedimentosOrigem();
    for (var selecao : request.linhas()) {
      long id = registroId(selecao.id());
      LinhaAssociacao l = atuais.get(id);
      if (l == null)
        throw new OperacaoNaoEncontradaException(
            "Linha importada não encontrada para este usuário.");
      if (!Long.toString(l.versao()).equals(selecao.versao()))
        throw new OperacaoConflitanteException(
            "Uma linha foi alterada. Atualize os candidatos antes de associar.");
      if (impedimentos.containsKey(id)) throw new OperacaoInvalidaException(impedimentos.get(id));
      selecionadas.add(
          new LinhaAssociacao(
              l.registroId(),
              l.versao(),
              l.instituicaoId(),
              l.contaId(),
              l.ativoId(),
              l.codigo(),
              l.ticker(),
              l.data(),
              l.quantidade(),
              l.operacao(),
              l.referencia(),
              selecao.papel(),
              l.taxaZeroDeclarada(),
              l.origemFisica()));
    }
    boolean desfazer = request.desfazer() || (grupo != null && selecionadas.size() < 2);
    if (desfazer && grupo == null)
      throw new OperacaoInvalidaException("Informe uma operação agrupada existente para desfazer.");
    if (desfazer) selecionadas = new ArrayList<>(proposta(grupo).linhas());
    selecionadas.sort(Comparator.comparingLong(LinhaAssociacao::registroId));
    Set<Long> ids =
        selecionadas.stream().map(LinhaAssociacao::registroId).collect(Collectors.toSet());
    if (!desfazer) validarDisponibilidade(ids, grupo);
    BigDecimal cotacao = numero(textoDecimal(request.cotacaoBrl(), "cotação em reais"));
    if (cotacao != null
        && (cotacao.signum() <= 0 || Math.max(0, cotacao.stripTrailingZeros().scale()) > 18))
      throw new OperacaoInvalidaException(
          "Informe cotação positiva em BRL com até dezoito casas decimais.");
    PropostaGrupo proposta =
        new PropostaGrupo(selecionadas, request.taxaZeroConfirmada(), cotacao, desfazer);
    ResumoGrupo resumo =
        desfazer
            ? new ResumoGrupo(
                null,
                List.of(),
                List.of(),
                List.of(),
                null,
                false,
                List.of(
                    "As linhas serão reprocessadas individualmente após confirmar o desfazimento."))
            : ValidacaoGrupo.resumir(
                proposta, catalogo(selecionadas), propriedades.janelaAssociacao());
    return livro.response(
        new GrupoEmConferencia(
            grupo == null ? null : grupo.getId(), "", proposta, resumo, true, false),
        revisao());
  }

  private Map<Long, AtivoResumo> catalogo(List<LinhaAssociacao> linhas) {
    return ativos
        .buscarPorIds(
            linhas.stream()
                .map(LinhaAssociacao::ativoId)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet()))
        .stream()
        .collect(Collectors.toMap(AtivoResumo::id, a -> a));
  }

  private void validarDisponibilidade(Set<Long> selecionadas, GrupoAssociacao destino) {
    for (var outro : grupos.listar(usuario.id()))
      if (destino == null || !destino.getId().equals(outro.getId())) {
        for (String json : Arrays.asList(outro.getPropostaJson(), outro.getAplicadaJson()))
          if (json != null) {
            PropostaGrupo p = ler(json, PropostaGrupo.class);
            if (!p.desfazer()
                && p.linhas().stream().anyMatch(l -> selecionadas.contains(l.registroId())))
              throw new OperacaoConflitanteException(
                  "Uma linha já pertence a outra operação agrupada. Desfaça e confirme o vínculo"
                      + " anterior primeiro.");
          }
      }
  }

  @Transactional(isolation = Isolation.READ_COMMITTED)
  public RevisaoAssociacaoResponse salvar(AssociacaoRequest request, String chave) {
    UUID comando;
    try {
      comando = UUID.fromString(chave);
    } catch (IllegalArgumentException e) {
      throw new OperacaoInvalidaException("Informe chave de idempotência UUID válida.");
    }
    PontoPartida ponto =
        pontos
            .bloquear(usuario.id())
            .orElseThrow(() ->
                new OperacaoInvalidaException(
                    "Receba os registros antes de criar uma associação."));
    String fingerprint = hash("associar|" + escrever(request));
    var anterior = comandos.buscar(usuario.id(), comando);
    if (anterior.isPresent()) {
      if (!fingerprint.equals(anterior.get().getFingerprint()))
        throw new OperacaoConflitanteException(
            "A chave de idempotência já foi usada com outro conteúdo.");
      return ler(anterior.get().getRespostaJson(), RevisaoAssociacaoResponse.class);
    }
    GrupoAssociacaoResponse previa = prever(request);
    if (!previa.previaId().equals(request.previaId()))
      throw new OperacaoConflitanteException(
          "A associação mudou. Gere uma nova prévia antes de salvar.");
    Map<Long, LinhaAssociacao> atuais =
        livro.linhas().stream().collect(Collectors.toMap(LinhaAssociacao::registroId, l -> l));
    List<LinhaAssociacao> linhas = new ArrayList<>();
    for (var l : previa.linhas()) {
      var atual = atuais.get(registroId(l.id()));
      linhas.add(
          new LinhaAssociacao(
              atual.registroId(),
              atual.versao(),
              atual.instituicaoId(),
              atual.contaId(),
              atual.ativoId(),
              atual.codigo(),
              atual.ticker(),
              atual.data(),
              atual.quantidade(),
              atual.operacao(),
              atual.referencia(),
              l.papel(),
              atual.taxaZeroDeclarada()));
    }
    PropostaGrupo proposta =
        new PropostaGrupo(
            linhas,
            request.taxaZeroConfirmada(),
            numero(textoDecimal(request.cotacaoBrl(), "cotação em reais")),
            previa.desfazer());
    GrupoAssociacao grupo;
    if (request.numero() == null)
      grupo = grupos.salvar(GrupoAssociacao.criar(usuario.id(), escrever(proposta), Instant.now()));
    else {
      grupo = grupos.buscar(usuario.id(), grupoId(request.numero()));
      grupo.propor(escrever(proposta), usuario.id(), Instant.now());
      grupos.flush();
    }
    livro.reservar(grupo);
    ponto.avancarRevisao(usuario.id(), Instant.now());
    RevisaoAssociacaoResponse resposta =
        new RevisaoAssociacaoResponse(Long.toString(ponto.getRevisao()), "OP-" + grupo.getId());
    comandos.salvar(
        ComandoConciliacao.registrar(
            usuario.id(), comando, fingerprint, escrever(resposta), Instant.now()));
    return resposta;
  }
}
