package io.github.jaimenafer.lastro.ativos.application.contract;

import java.util.*;

public interface IConsultaAtivos {
  Optional<AtivoResumo> buscar(Long id);

  Optional<AtivoResumo> buscarPorCodigo(String codigo);

  List<AtivoResumo> buscarPorIds(Collection<Long> ids);
}
