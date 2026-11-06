package io.github.jaimenafer.lastro.importacoes.application.port;

import io.github.jaimenafer.lastro.importacoes.domain.model.ArquivoInterpretado;

public interface ILeitorRelatorio {
  ArquivoInterpretado ler(byte[] conteudo);
}
