package io.github.jaimenafer.lastro.operacoes.infrastructure.persistence;

import static org.hibernate.exception.ConstraintViolationException.ConstraintKind.UNIQUE;

import io.github.jaimenafer.lastro.operacoes.domain.exception.OperacaoConflitanteException;
import io.github.jaimenafer.lastro.operacoes.domain.exception.OperacaoNaoEncontradaException;
import io.github.jaimenafer.lastro.operacoes.domain.model.ContaCustodia;
import io.github.jaimenafer.lastro.operacoes.domain.repository.IContaCustodiaRepository;
import java.util.List;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.hibernate.exception.ConstraintViolationException;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Repository;

@Repository
@RequiredArgsConstructor
public class ContaCustodiaPersistenceAdapter implements IContaCustodiaRepository {
  private final IContaCustodiaJpaRepository jpa;

  @Override
  public List<ContaCustodia> listar(String perfilRef) {
    return jpa.findByPerfilRefOrderByIdAsc(perfilRef);
  }

  @Override
  public ContaCustodia buscar(String perfilRef, Long id) {
    return jpa.findByPerfilRefAndId(perfilRef, id)
        .orElseThrow(() -> new OperacaoNaoEncontradaException("Conta de custódia não encontrada."));
  }

  @Override
  public Optional<ContaCustodia> buscarPorCodigo(
      String perfilRef, Long instituicaoId, String codigo) {
    return jpa.findByPerfilRefAndInstituicaoIdAndCodigo(perfilRef, instituicaoId, codigo);
  }

  @Override
  public ContaCustodia salvar(ContaCustodia conta) {
    try {
      return jpa.saveAndFlush(conta);
    } catch (DataIntegrityViolationException erro) {
      if (erro.getCause() instanceof ConstraintViolationException violacao
          && violacao.getKind() == UNIQUE)
        throw new OperacaoConflitanteException(
            "A conciliação foi alterada por outra solicitação. Atualize os dados antes de"
                + " continuar.",
            erro);
      throw erro;
    }
  }
}
