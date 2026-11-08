package io.github.jaimenafer.lastro.operacoes.infrastructure.persistence;

import io.github.jaimenafer.lastro.operacoes.domain.model.ContaCustodia;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface IContaCustodiaJpaRepository extends JpaRepository<ContaCustodia, Long> {
  List<ContaCustodia> findByPerfilRefOrderByIdAsc(String perfilRef);

  Optional<ContaCustodia> findByPerfilRefAndId(String perfilRef, Long id);

  Optional<ContaCustodia> findByPerfilRefAndInstituicaoIdAndCodigo(
      String perfilRef, Long instituicaoId, String codigo);
}
