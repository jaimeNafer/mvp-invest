package io.github.jaimenafer.lastro.instituicoes.domain.repository;

import io.github.jaimenafer.lastro.instituicoes.domain.model.Instituicao;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface IInstituicaoRepository {
  Optional<Instituicao> findById(Long id);

  Optional<Instituicao> findByCodigo(String codigo);

  List<Instituicao> findAllByOrderByNomeAscIdAsc();

  List<Instituicao> findAllByIdIn(Collection<Long> ids);
}
