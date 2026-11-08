package io.github.jaimenafer.lastro.operacoes.domain.repository;

import io.github.jaimenafer.lastro.operacoes.domain.model.GrupoAssociacao;
import java.util.List;

public interface IGrupoAssociacaoRepository {
  List<GrupoAssociacao> listar(String perfil);

  GrupoAssociacao buscar(String perfil, Long id);

  GrupoAssociacao salvar(GrupoAssociacao grupo);

  void sincronizarVinculos(Long grupo, List<Long> registros);

  void flush();
}
