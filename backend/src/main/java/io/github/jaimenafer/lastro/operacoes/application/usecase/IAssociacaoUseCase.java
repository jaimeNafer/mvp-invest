package io.github.jaimenafer.lastro.operacoes.application.usecase;

import io.github.jaimenafer.lastro.operacoes.application.dto.*;

public interface IAssociacaoUseCase {
  CandidatosAssociacaoResponse candidatos(String registroId);

  GrupoAssociacaoResponse buscar(String numero);

  GrupoAssociacaoResponse prever(AssociacaoRequest request);

  RevisaoAssociacaoResponse salvar(AssociacaoRequest request, String chave);
}
