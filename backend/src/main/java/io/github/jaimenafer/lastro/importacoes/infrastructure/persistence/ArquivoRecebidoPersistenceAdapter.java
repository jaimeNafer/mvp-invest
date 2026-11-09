package io.github.jaimenafer.lastro.importacoes.infrastructure.persistence;

import io.github.jaimenafer.lastro.importacoes.domain.model.ArquivoRecebido;
import io.github.jaimenafer.lastro.importacoes.domain.model.ResumoArquivoRecebido;
import io.github.jaimenafer.lastro.importacoes.domain.repository.IArquivoRecebidoRepository;
import io.github.jaimenafer.lastro.importacoes.domain.exception.ImportacaoNaoEncontradaException;
import java.util.Optional;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Repository;

@Repository
@RequiredArgsConstructor
public class ArquivoRecebidoPersistenceAdapter implements IArquivoRecebidoRepository {
  private final IArquivoRecebidoJpaRepository jpa;

  public ArquivoRecebido buscar(String perfilRef, Long id) {
    return jpa.findByPerfilRefAndId(perfilRef, id)
        .orElseThrow(() -> new ImportacaoNaoEncontradaException("Arquivo não encontrado."));
  }

  public void excluir(ArquivoRecebido arquivo) {
    jpa.delete(arquivo);
    jpa.flush();
  }

  public Optional<ArquivoRecebido> buscarPorHash(String perfilRef, String hash) {
    return jpa.findByPerfilRefAndHash(perfilRef, hash);
  }

  public ArquivoRecebido salvar(ArquivoRecebido arquivo) {
    return jpa.saveAndFlush(arquivo);
  }

  public List<ResumoArquivoRecebido> listar(String perfilRef, int pagina, int tamanho) {
    return jpa.listar(perfilRef, PageRequest.of(pagina, tamanho));
  }

  public long contar(String perfilRef) {
    return jpa.countByPerfilRef(perfilRef);
  }
}
