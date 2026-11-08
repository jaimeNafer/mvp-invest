package io.github.jaimenafer.lastro.operacoes.domain.repository;

import io.github.jaimenafer.lastro.operacoes.domain.model.RegistroConferencia;
import java.util.List;
import java.util.Optional;

public interface IRegistroConferenciaRepository {
  List<RegistroConferencia> listar(String perfilRef);

  RegistroConferencia buscar(String perfilRef, Long id);

  Optional<RegistroConferencia> buscarPorOrigem(String perfilRef, String origemRef);

  RegistroConferencia salvar(RegistroConferencia registro);

  void flush();
}
