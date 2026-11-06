package io.github.jaimenafer.lastro.importacoes.domain.model;

import java.util.List;

public record ArquivoInterpretado(
    String hash, List<EventoImportacao> eventos, List<DiagnosticoImportacao> erros) {
  public ArquivoInterpretado {
    eventos = List.copyOf(eventos);
    erros = List.copyOf(erros);
  }
}
