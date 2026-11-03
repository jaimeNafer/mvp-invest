package io.github.jaimenafer.lastro.ativos.infrastructure.persistence;

import io.github.jaimenafer.lastro.ativos.domain.model.*;
import io.github.jaimenafer.lastro.ativos.domain.repository.IAtivoRepository;
import java.util.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

@Repository
@RequiredArgsConstructor
public class AtivoPersistenceAdapter implements IAtivoRepository {
  private final IAtivoJpaRepository jpa;

  @Override
  public List<Ativo> findAllByCodigoInOrderByTickerAscIdAsc(List<String> codigos) {
    return jpa.findAllByCodigoInOrderByTickerAscIdAsc(codigos);
  }

  @Override
  public Optional<Ativo> findById(Long id) {
    return jpa.findById(id);
  }

  @Override
  public java.util.List<Ativo> findAllByIdIn(java.util.Collection<Long> ids) {
    return jpa.findAllByIdIn(ids);
  }
}
