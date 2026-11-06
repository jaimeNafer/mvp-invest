package io.github.jaimenafer.lastro.instituicoes.application.contract;

import java.util.*;

public interface IConsultaInstituicoes {
  Optional<InstituicaoResumo> buscar(Long id);

  Optional<InstituicaoResumo> buscarPorCodigo(String codigo);

  List<InstituicaoResumo> buscarPorIds(Collection<Long> ids);
}
