package io.github.jaimenafer.lastro.operacoes.application.usecase;

import io.github.jaimenafer.lastro.operacoes.application.dto.RevisaoConferenciaResponse;
import io.github.jaimenafer.lastro.operacoes.application.dto.ValoracaoManualRequest;

public interface IValoracaoManualUseCase {
  RevisaoConferenciaResponse registrarGrupo(String numero, ValoracaoManualRequest request, String chave);
  RevisaoConferenciaResponse registrarLinha(String id, ValoracaoManualRequest request, String chave);
}
