package io.github.jaimenafer.lastro.operacoes.domain.model;

import java.util.List;

public record LotePrecos(List<PrecoCandle> precos, String checksumArquivo) {
  public LotePrecos {
    precos = List.copyOf(precos);
  }
}
