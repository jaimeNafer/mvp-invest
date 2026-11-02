package io.github.jaimenafer.lastro.carteiras;

import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CarteiraRepository extends JpaRepository<Carteira, Long> {
    @EntityGraph(attributePaths = {"contas", "contas.instituicao"})
    List<Carteira> findByPerfilRefOrderByNomeAscIdAsc(String perfilRef);
    @EntityGraph(attributePaths = {"contas", "contas.instituicao"})
    List<Carteira> findByPerfilRefAndAtivoOrderByNomeAscIdAsc(String perfilRef, boolean ativo);
    @EntityGraph(attributePaths = {"contas", "contas.instituicao"})
    Optional<Carteira> findByIdAndPerfilRef(long id, String perfilRef);
}
