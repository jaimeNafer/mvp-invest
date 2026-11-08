package io.github.jaimenafer.lastro.operacoes.infrastructure.persistence;

import io.github.jaimenafer.lastro.operacoes.domain.model.AjusteConciliatorio;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface IAjusteConciliatorioJpaRepository
    extends JpaRepository<AjusteConciliatorio, Long> {
  List<AjusteConciliatorio> findByPerfilRefOrderByIdAsc(String perfilRef);

  List<AjusteConciliatorio> findByPerfilRefAndAtivoTrueOrderByIdAsc(String perfilRef);
}
