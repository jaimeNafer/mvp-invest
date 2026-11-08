package io.github.jaimenafer.lastro.importacoes.application.port;

import io.github.jaimenafer.lastro.importacoes.domain.model.DocumentoMovimentacoes;

public interface ILeitorMovimentacoes {
  DocumentoMovimentacoes ler(String nome, byte[] conteudo);
}
