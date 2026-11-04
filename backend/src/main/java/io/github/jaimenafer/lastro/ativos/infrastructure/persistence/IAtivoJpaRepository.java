package io.github.jaimenafer.lastro.ativos.infrastructure.persistence;

import io.github.jaimenafer.lastro.ativos.domain.model.Ativo;
import java.util.List;
import java.util.Optional;
import org.springframework.data.repository.Repository;

public interface IAtivoJpaRepository extends Repository<Ativo, Long> {
  List<Ativo> findAllByOrderByTickerAscIdAsc();

  Optional<Ativo> findById(Long id);

  List<Ativo> findAllByIdIn(java.util.Collection<Long> ids);
}
