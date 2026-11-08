package io.github.jaimenafer.lastro.importacoes.infrastructure.persistence;

import io.github.jaimenafer.lastro.importacoes.domain.model.ArquivoRecebido;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface IArquivoRecebidoJpaRepository extends JpaRepository<ArquivoRecebido, Long> {
  Optional<ArquivoRecebido> findByPerfilRefAndHash(String perfilRef, String hash);
}
