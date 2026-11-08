package io.github.jaimenafer.lastro.operacoes.infrastructure.persistence;

import io.github.jaimenafer.lastro.operacoes.domain.model.ComandoConciliacao;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface IComandoConciliacaoJpaRepository extends JpaRepository<ComandoConciliacao, Long> {
  Optional<ComandoConciliacao> findByPerfilRefAndChave(String perfilRef, String chave);
}
