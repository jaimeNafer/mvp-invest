package io.github.jaimenafer.lastro.carteiras.infrastructure.persistence;

import io.github.jaimenafer.lastro.carteiras.domain.model.Carteira;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ICarteiraJpaRepository extends JpaRepository<Carteira, Long> {
  @EntityGraph(attributePaths = {"contas"})
  List<Carteira> findByPerfilRefOrderByNomeAscIdAsc(String perfilRef);

  @EntityGraph(attributePaths = {"contas"})
  List<Carteira> findByPerfilRefAndAtivoOrderByNomeAscIdAsc(String perfilRef, boolean ativo);

  @EntityGraph(attributePaths = {"contas"})
  Optional<Carteira> findByIdAndPerfilRef(long id, String perfilRef);
}
