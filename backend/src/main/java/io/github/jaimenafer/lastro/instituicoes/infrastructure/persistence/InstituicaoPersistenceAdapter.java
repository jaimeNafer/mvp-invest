package io.github.jaimenafer.lastro.instituicoes.infrastructure.persistence;

import io.github.jaimenafer.lastro.instituicoes.domain.model.Instituicao;
import io.github.jaimenafer.lastro.instituicoes.domain.repository.IInstituicaoRepository;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

@Repository
@RequiredArgsConstructor
public class InstituicaoPersistenceAdapter implements IInstituicaoRepository {
  private final IInstituicaoJpaRepository jpa;

  @Override
  public Optional<Instituicao> findById(Long id) {
    return jpa.findById(id);
  }

  @Override
  public List<Instituicao> findAllByOrderByNomeAscIdAsc() {
    return jpa.findAllByOrderByNomeAscIdAsc();
  }

  @Override
  public List<Instituicao> findAllByIdIn(Collection<Long> ids) {
    return jpa.findAllByIdIn(ids);
  }
}
