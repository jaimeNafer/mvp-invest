package io.github.jaimenafer.lastro.operacoes.domain.repository;

import io.github.jaimenafer.lastro.operacoes.domain.model.AjusteConciliatorio;
import java.util.List;

public interface IAjusteConciliatorioRepository {
  List<AjusteConciliatorio> listar(String perfilRef);

  List<AjusteConciliatorio> listarAtivos(String perfilRef);

  AjusteConciliatorio salvar(AjusteConciliatorio ajuste);

  void flush();
}
