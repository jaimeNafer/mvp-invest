package io.github.jaimenafer.lastro.operacoes.infrastructure.persistence;

import io.github.jaimenafer.lastro.operacoes.domain.model.VinculoGrupoAssociacao;
import org.springframework.data.jpa.repository.JpaRepository;

public interface IVinculoGrupoAssociacaoJpaRepository
    extends JpaRepository<VinculoGrupoAssociacao, Long> {
  void deleteByGrupoId(Long grupoId);
}
