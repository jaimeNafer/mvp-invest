package io.github.jaimenafer.lastro.operacoes.infrastructure.persistence;

import static org.hibernate.exception.ConstraintViolationException.ConstraintKind.UNIQUE;

import io.github.jaimenafer.lastro.operacoes.domain.exception.OperacaoConflitanteException;
import io.github.jaimenafer.lastro.operacoes.domain.model.ComandoConciliacao;
import io.github.jaimenafer.lastro.operacoes.domain.repository.IComandoConciliacaoRepository;
import java.util.Optional;
import java.util.UUID;
import java.util.Collection;
import lombok.RequiredArgsConstructor;
import org.hibernate.exception.ConstraintViolationException;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Repository;

@Repository
@RequiredArgsConstructor
public class ComandoConciliacaoPersistenceAdapter implements IComandoConciliacaoRepository {
  private final IComandoConciliacaoJpaRepository jpa;

  public void excluirRespostasDeRegistros(String perfilRef, Collection<String> ids) {
    if (!ids.isEmpty()) jpa.excluirRespostasDeRegistros(perfilRef, ids);
  }

  public void excluirRespostasDeGrupos(String perfilRef, Collection<String> numeros) {
    if (!numeros.isEmpty()) jpa.excluirRespostasDeGrupos(perfilRef, numeros);
  }

  @Override
  public Optional<ComandoConciliacao> buscar(String perfilRef, UUID chave) {
    return jpa.findByPerfilRefAndChave(perfilRef, chave.toString());
  }

  @Override
  public ComandoConciliacao salvar(ComandoConciliacao comando) {
    try {
      return jpa.saveAndFlush(comando);
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
