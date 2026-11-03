package io.github.jaimenafer.lastro.operacoes.infrastructure.persistence;

import io.github.jaimenafer.lastro.operacoes.domain.model.PontoPartida;
import jakarta.persistence.LockModeType;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;

public interface IPontoPartidaJpaRepository extends JpaRepository<PontoPartida, String> {
  @Lock(LockModeType.PESSIMISTIC_WRITE)
  @Query("select p from PontoPartida p where p.perfilRef = :usuario")
  Optional<PontoPartida> bloquear(String usuario);
}
