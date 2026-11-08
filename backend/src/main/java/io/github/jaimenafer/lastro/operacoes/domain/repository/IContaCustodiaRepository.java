package io.github.jaimenafer.lastro.operacoes.domain.repository;

import io.github.jaimenafer.lastro.operacoes.domain.model.ContaCustodia;
import java.util.List;
import java.util.Optional;

public interface IContaCustodiaRepository {
  List<ContaCustodia> listar(String perfilRef);

  ContaCustodia buscar(String perfilRef, Long id);

  Optional<ContaCustodia> buscarPorCodigo(String perfilRef, Long instituicaoId, String codigo);

  ContaCustodia salvar(ContaCustodia conta);
}
