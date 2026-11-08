package io.github.jaimenafer.lastro.importacoes.domain.repository;

import io.github.jaimenafer.lastro.importacoes.domain.model.ArquivoRecebido;
import java.util.Optional;

public interface IArquivoRecebidoRepository {
  Optional<ArquivoRecebido> buscarPorHash(String perfilRef, String hash);

  ArquivoRecebido salvar(ArquivoRecebido arquivo);
}
