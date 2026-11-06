package io.github.jaimenafer.lastro.ativos.domain.repository;

import io.github.jaimenafer.lastro.ativos.domain.model.Ativo;
import java.util.List;
import java.util.Optional;

public interface IAtivoRepository {
  List<Ativo> findAllByOrderByTickerAscIdAsc();

  Optional<Ativo> findById(Long id);

  Optional<Ativo> findByCodigo(String codigo);

  List<Ativo> findAllByIdIn(java.util.Collection<Long> ids);
}
