package io.github.jaimenafer.lastro.importacoes.domain.repository;

import io.github.jaimenafer.lastro.importacoes.domain.model.ArquivoRecebido;
import io.github.jaimenafer.lastro.importacoes.domain.model.ResumoArquivoRecebido;
import java.util.List;
import java.util.Optional;

public interface IArquivoRecebidoRepository {
  ArquivoRecebido buscar(String perfilRef, Long id);

  void excluir(ArquivoRecebido arquivo);
  Optional<ArquivoRecebido> buscarPorHash(String perfilRef, String hash);

  ArquivoRecebido salvar(ArquivoRecebido arquivo);

  List<ResumoArquivoRecebido> listar(String perfilRef, int pagina, int tamanho);

  long contar(String perfilRef);
}
