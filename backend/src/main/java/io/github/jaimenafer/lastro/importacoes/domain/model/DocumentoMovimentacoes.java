package io.github.jaimenafer.lastro.importacoes.domain.model;

import java.util.List;

public record DocumentoMovimentacoes(
    String hash, String nomeArquivo, String versaoLeitor, List<MovimentacaoRecebida> registros) {
  public DocumentoMovimentacoes {
    registros = List.copyOf(registros);
  }
}
