package io.github.jaimenafer.lastro.operacoes.infrastructure.persistence;

import io.github.jaimenafer.lastro.operacoes.domain.model.RegistroConferencia;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface IRegistroConferenciaJpaRepository
    extends JpaRepository<RegistroConferencia, Long> {
  List<RegistroConferencia> findByPerfilRefOrderByIdAsc(String perfilRef);

  Optional<RegistroConferencia> findByPerfilRefAndId(String perfilRef, Long id);

  Optional<RegistroConferencia> findByPerfilRefAndOrigemRef(String perfilRef, String origemRef);
}
