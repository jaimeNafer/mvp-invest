package io.github.jaimenafer.lastro.instituicoes.infrastructure.persistence;

import io.github.jaimenafer.lastro.instituicoes.domain.model.Instituicao;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import org.springframework.data.repository.Repository;

public interface IInstituicaoJpaRepository extends Repository<Instituicao, Long> {
  Optional<Instituicao> findById(Long id);

  Optional<Instituicao> findByCodigo(String codigo);

  List<Instituicao> findAllByOrderByNomeAscIdAsc();

  List<Instituicao> findAllByIdIn(Collection<Long> ids);
}
