package io.github.jaimenafer.lastro.operacoes.application.port;

import io.github.jaimenafer.lastro.operacoes.application.dto.*;
import io.github.jaimenafer.lastro.operacoes.domain.model.*;
import java.time.Instant;
import java.util.List;
import java.util.Map;

public interface IConsultaConferencia {
  List<ItemConferenciaLivro> projetar(boolean propostas, boolean incluirAjustes, Instant ate);

  List<RegistroTimelineResponse> registros();

  List<RegistroTimelineResponse> registrosAplicados();

  ResumoConferenciaResponse resumir(List<RegistroTimelineResponse> registros);

  List<ContaConferenciaResponse> contas();

  Map<Long, String> impedimentos();

  Map<Long, String> impedimentosOrigem();

  Map<Long, PropostaRegistro> propostasParaAplicar();
}
