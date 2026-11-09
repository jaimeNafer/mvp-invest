package io.github.jaimenafer.lastro.operacoes.application.dto;

import java.util.List;

public record ItemHistoricoOperacaoResponse(
    OperacaoResponse operacao, RegistroTimelineResponse importada,
    List<RegistroTimelineResponse> linhasOriginais) {
  public String data() {
    return operacao == null ? importada.data() : operacao.ocorridaEm().toString();
  }
}
