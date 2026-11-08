package io.github.jaimenafer.lastro.operacoes.application.usecase;

import io.github.jaimenafer.lastro.operacoes.application.dto.*;
import io.github.jaimenafer.lastro.operacoes.application.dto.VisaoGeralResponse;
import java.util.List;

public interface IVisaoGeralUseCase {
  VisaoGeralResponse consultar(FiltroVisaoGeral filtro);

  AvisosConferenciaResponse avisos(FiltroAvisosConferencia filtro);

  List<PatrimonioDiarioResponse> patrimonioDiario(FiltroVisaoGeral filtro);
}
