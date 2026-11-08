package io.github.jaimenafer.lastro.importacoes.application.dto;

import java.util.List;

public record RecebimentoResponse(
    List<ArquivoRecebidoResponse> arquivos,
    int recebidos,
    int reconhecidos,
    int problemas,
    String revisao) {
  public RecebimentoResponse {
    arquivos = List.copyOf(arquivos);
  }
}
