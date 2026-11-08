package io.github.jaimenafer.lastro.operacoes.infrastructure.persistence;

import io.github.jaimenafer.lastro.operacoes.domain.model.GrupoAssociacao;
import java.util.*;
import org.springframework.data.jpa.repository.JpaRepository;

public interface IGrupoAssociacaoJpaRepository extends JpaRepository<GrupoAssociacao, Long> {
  List<GrupoAssociacao> findByPerfilRefOrderByIdAsc(String perfil);

  Optional<GrupoAssociacao> findByPerfilRefAndId(String perfil, Long id);
}
