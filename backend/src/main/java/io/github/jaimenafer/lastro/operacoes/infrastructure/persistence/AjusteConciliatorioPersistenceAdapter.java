package io.github.jaimenafer.lastro.operacoes.infrastructure.persistence;

import static org.hibernate.exception.ConstraintViolationException.ConstraintKind.UNIQUE;

import io.github.jaimenafer.lastro.operacoes.domain.exception.OperacaoConflitanteException;
import io.github.jaimenafer.lastro.operacoes.domain.model.AjusteConciliatorio;
import io.github.jaimenafer.lastro.operacoes.domain.repository.IAjusteConciliatorioRepository;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.hibernate.exception.ConstraintViolationException;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Repository;

@Repository
@RequiredArgsConstructor
public class AjusteConciliatorioPersistenceAdapter implements IAjusteConciliatorioRepository {
  private final IAjusteConciliatorioJpaRepository jpa;

  @Override
  public List<AjusteConciliatorio> listar(String perfilRef) {
    return jpa.findByPerfilRefOrderByIdAsc(perfilRef);
  }

  @Override
  public List<AjusteConciliatorio> listarAtivos(String perfilRef) {
    return jpa.findByPerfilRefAndAtivoTrueOrderByIdAsc(perfilRef);
  }

  @Override
  public AjusteConciliatorio salvar(AjusteConciliatorio ajuste) {
    try {
      return jpa.saveAndFlush(ajuste);
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

  @Override
  public void flush() {
    jpa.flush();
  }
}
