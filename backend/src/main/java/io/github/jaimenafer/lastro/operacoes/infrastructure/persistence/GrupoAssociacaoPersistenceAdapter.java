package io.github.jaimenafer.lastro.operacoes.infrastructure.persistence;

import static org.hibernate.exception.ConstraintViolationException.ConstraintKind.UNIQUE;

import io.github.jaimenafer.lastro.operacoes.domain.exception.*;
import io.github.jaimenafer.lastro.operacoes.domain.model.*;
import io.github.jaimenafer.lastro.operacoes.domain.repository.IGrupoAssociacaoRepository;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.hibernate.exception.ConstraintViolationException;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Repository;

@Repository
@RequiredArgsConstructor
public class GrupoAssociacaoPersistenceAdapter implements IGrupoAssociacaoRepository {
  private final IGrupoAssociacaoJpaRepository jpa;
  private final IVinculoGrupoAssociacaoJpaRepository vinculos;
  private final LimpezaValoracoes limpezaValoracoes;

  public void excluir(GrupoAssociacao grupo) {
    limpezaValoracoes.excluirDoGrupo(grupo.getPerfilRef(), grupo.getId());
    vinculos.deleteByGrupoId(grupo.getId());
    vinculos.flush();
    jpa.delete(grupo);
    jpa.flush();
  }

  public List<GrupoAssociacao> listar(String perfil) {
    return jpa.findByPerfilRefOrderByIdAsc(perfil);
  }

  public GrupoAssociacao buscar(String perfil, Long id) {
    return jpa.findByPerfilRefAndId(perfil, id)
        .orElseThrow(() ->
            new OperacaoNaoEncontradaException(
                "Operação agrupada não encontrada para este usuário."));
  }

  public GrupoAssociacao salvar(GrupoAssociacao g) {
    return jpa.saveAndFlush(g);
  }

  public void sincronizarVinculos(Long grupo, List<Long> registros) {
    vinculos.deleteByGrupoId(grupo);
    vinculos.flush();
    try {
      for (Long registro : registros)
        vinculos.saveAndFlush(VinculoGrupoAssociacao.vincular(registro, grupo));
    } catch (DataIntegrityViolationException e) {
      if (e.getCause() instanceof ConstraintViolationException v && v.getKind() == UNIQUE)
        throw new OperacaoConflitanteException(
            "Uma linha já pertence a outra operação agrupada. Atualize a conferência.", e);
      throw e;
    }
  }

  public void flush() {
    jpa.flush();
  }
}
