package io.github.jaimenafer.lastro.importacoes;

import static org.assertj.core.api.Assertions.assertThat;

import io.github.jaimenafer.lastro.importacoes.application.mapper.IdentidadeImportacao;
import org.junit.jupiter.api.Test;

class IdentidadeImportacaoTests {
  @Test
  void identidadeLegadaPreservaUuidCincoDoImportadorAnterior() {
    assertThat(IdentidadeImportacao.legada("abc", "ref-1"))
        .isEqualTo("3096b5fb-d8cb-5773-9ab1-d7bc3b166a0d");
  }

  @Test
  void identidadeDoEventoNaoDependeDoArquivo() {
    assertThat(IdentidadeImportacao.evento("ref-1"))
        .isEqualTo("5af1685b-a61e-55e0-b89b-8e26435cc47e")
        .isNotEqualTo(IdentidadeImportacao.evento("ref-2"));
  }
}
