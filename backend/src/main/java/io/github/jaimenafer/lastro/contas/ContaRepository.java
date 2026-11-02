package io.github.jaimenafer.lastro.contas;

import java.util.Collection;
import java.util.List;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ContaRepository extends JpaRepository<Conta, Long> {
    @EntityGraph(attributePaths = "instituicao")
    List<Conta> findAllByPerfilRefAndInstituicaoIdIn(String perfilRef, Collection<Long> instituicoes);
}
