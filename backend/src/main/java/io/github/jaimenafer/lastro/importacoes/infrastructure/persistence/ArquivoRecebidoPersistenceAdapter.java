package io.github.jaimenafer.lastro.importacoes.infrastructure.persistence;

import io.github.jaimenafer.lastro.importacoes.domain.model.ArquivoRecebido;
import io.github.jaimenafer.lastro.importacoes.domain.repository.IArquivoRecebidoRepository;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

@Repository
@RequiredArgsConstructor
public class ArquivoRecebidoPersistenceAdapter implements IArquivoRecebidoRepository {
  private final IArquivoRecebidoJpaRepository jpa;

  public Optional<ArquivoRecebido> buscarPorHash(String perfilRef, String hash) {
    return jpa.findByPerfilRefAndHash(perfilRef, hash);
  }

  public ArquivoRecebido salvar(ArquivoRecebido arquivo) {
    return jpa.saveAndFlush(arquivo);
  }
}
