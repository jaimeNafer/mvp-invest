package io.github.jaimenafer.lastro.operacoes.infrastructure.persistence;

import static org.hibernate.exception.ConstraintViolationException.ConstraintKind.UNIQUE;

import io.github.jaimenafer.lastro.operacoes.domain.exception.SaldoInicialConflitanteException;
import io.github.jaimenafer.lastro.operacoes.domain.exception.SaldoInicialNaoEncontradoException;
import io.github.jaimenafer.lastro.operacoes.domain.exception.OperacaoConflitanteException;
import io.github.jaimenafer.lastro.operacoes.domain.model.*;
import io.github.jaimenafer.lastro.operacoes.domain.model.SituacaoOperacao;
import io.github.jaimenafer.lastro.operacoes.domain.repository.IOperacaoRepository;
import jakarta.persistence.criteria.Predicate;
import java.util.*;
import lombok.RequiredArgsConstructor;
import org.hibernate.exception.ConstraintViolationException;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Repository;

@Repository
@RequiredArgsConstructor
public class OperacaoPersistenceAdapter implements IOperacaoRepository {
  private final IOperacaoJpaRepository jpa;

  @Override
  public Operacao buscarObrigatoria(Long operacaoId, String perfilRef) {
    return jpa.findByIdAndPerfilRef(operacaoId, perfilRef)
        .orElseThrow(() -> new SaldoInicialNaoEncontradoException("Saldo inicial não encontrado."));
  }

  @Override
  public Optional<Operacao> findByPerfilRefAndChaveRequisicao(String perfilRef, String chave) {
    return jpa.findByPerfilRefAndChaveRequisicao(perfilRef, chave);
  }

  @Override
  public List<Operacao> findByPerfilRefAndSituacaoOrderByIdAsc(
      String perfilRef, SituacaoOperacao situacao) {
    return jpa.findByPerfilRefAndTipoAndSituacaoOrderByIdAsc(
        perfilRef, TipoOperacao.SALDO_INICIAL, situacao);
  }

  @Override
  public boolean existsByPerfilRefAndSituacao(String perfilRef, SituacaoOperacao situacao) {
    return jpa.existsByPerfilRefAndSituacao(perfilRef, situacao);
  }

  @Override
  public Operacao saveAndFlush(Operacao operacao) {
    try {
      return jpa.saveAndFlush(operacao);
    } catch (DataIntegrityViolationException erro) {
      if (erro.getCause() instanceof ConstraintViolationException violacao
          && violacao.getKind() == UNIQUE)
        throw new SaldoInicialConflitanteException(
            "O saldo inicial ou esta solicitação já foi registrado. Atualize a listagem antes de"
                + " tentar novamente.",
            erro);
      throw erro;
    }
  }

  @Override
  public List<Operacao> historico(String perfilRef) {
    return jpa.findByPerfilRefOrderByOcorridaEmAscOrdemLivroAscIdAsc(perfilRef);
  }

  @Override
  public void salvarLote(List<Operacao> operacoes) {
    try {
      jpa.saveAllAndFlush(operacoes);
    } catch (DataIntegrityViolationException erro) {
      if (erro.getCause() instanceof ConstraintViolationException violacao
          && violacao.getKind() == UNIQUE)
        throw new OperacaoConflitanteException(
            "Uma das operações do lote já foi registrada. Atualize a prévia antes de confirmar.", erro);
      throw erro;
    }
  }

  @Override
  public void flush() {
    jpa.flush();
  }

  @Override
  public PaginaOperacoes consultar(FiltroOperacoes filtro) {
    Specification<Operacao> especificacao =
        (raiz, consulta, cb) -> {
          var condicoes = new ArrayList<Predicate>();
          condicoes.add(cb.equal(raiz.get("perfilRef"), filtro.perfilRef()));
          condicoes.add(cb.equal(raiz.get("tipo"), TipoOperacao.SALDO_INICIAL));
          if (filtro.situacao() != null)
            condicoes.add(cb.equal(raiz.get("situacao"), filtro.situacao()));
          if (filtro.instituicaoId() != null)
            condicoes.add(cb.equal(raiz.get("instituicaoId"), filtro.instituicaoId()));
          if (filtro.ativoId() != null)
            condicoes.add(cb.equal(raiz.get("ativoId"), filtro.ativoId()));
          if (filtro.instituicaoIds() != null)
            condicoes.add(
                filtro.instituicaoIds().isEmpty()
                    ? cb.disjunction()
                    : raiz.get("instituicaoId").in(filtro.instituicaoIds()));
          return cb.and(condicoes.toArray(Predicate[]::new));
        };
    var pagina =
        jpa.findAll(
            especificacao,
            PageRequest.of(filtro.pagina(), filtro.tamanho(), Sort.by("dataReferencia", "id")));
    return new PaginaOperacoes(pagina.getContent(), pagina.getTotalElements());
  }
}
