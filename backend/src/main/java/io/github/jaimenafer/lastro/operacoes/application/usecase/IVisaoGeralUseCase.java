package io.github.jaimenafer.lastro.operacoes.application.usecase;

import io.github.jaimenafer.lastro.operacoes.application.dto.*;
import io.github.jaimenafer.lastro.operacoes.application.dto.VisaoGeralResponse;

public interface IVisaoGeralUseCase {
  VisaoGeralResponse consultar(FiltroVisaoGeral filtro);

  AvisosConferenciaResponse avisos(FiltroAvisosConferencia filtro);
}
