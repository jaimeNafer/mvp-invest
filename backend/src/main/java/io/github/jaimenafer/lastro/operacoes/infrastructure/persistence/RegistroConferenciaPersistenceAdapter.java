package io.github.jaimenafer.lastro.operacoes.infrastructure.persistence;

import static org.hibernate.exception.ConstraintViolationException.ConstraintKind.UNIQUE;

import io.github.jaimenafer.lastro.operacoes.domain.exception.OperacaoConflitanteException;
import io.github.jaimenafer.lastro.operacoes.domain.exception.OperacaoNaoEncontradaException;
import io.github.jaimenafer.lastro.operacoes.domain.model.RegistroConferencia;
import io.github.jaimenafer.lastro.operacoes.domain.repository.IRegistroConferenciaRepository;
import java.util.List;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.hibernate.exception.ConstraintViolationException;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Repository;

@Repository
@RequiredArgsConstructor
public class RegistroConferenciaPersistenceAdapter implements IRegistroConferenciaRepository {
  private final IRegistroConferenciaJpaRepository jpa;
  private final LimpezaValoracoes limpezaValoracoes;

  @Override
  public void excluir(RegistroConferencia registro) {
    limpezaValoracoes.excluirDoRegistro(registro.getPerfilRef(), registro.getId());
    jpa.delete(registro);
  }

  @Override
  public List<RegistroConferencia> listar(String perfilRef) {
    return jpa.findByPerfilRefOrderByIdAsc(perfilRef);
  }

  @Override
  public RegistroConferencia buscar(String perfilRef, Long id) {
    return jpa.findByPerfilRefAndId(perfilRef, id)
        .orElseThrow(
            () -> new OperacaoNaoEncontradaException("Registro de conferência não encontrado."));
  }

  @Override
  public Optional<RegistroConferencia> buscarPorOrigem(String perfilRef, String origemRef) {
    return jpa.findByPerfilRefAndOrigemRef(perfilRef, origemRef);
  }

  @Override
  public RegistroConferencia salvar(RegistroConferencia registro) {
    try {
      return jpa.saveAndFlush(registro);
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
