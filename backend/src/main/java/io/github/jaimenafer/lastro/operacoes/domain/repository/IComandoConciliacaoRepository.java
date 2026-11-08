package io.github.jaimenafer.lastro.operacoes.domain.repository;

import io.github.jaimenafer.lastro.operacoes.domain.model.ComandoConciliacao;
import java.util.Optional;
import java.util.UUID;

public interface IComandoConciliacaoRepository {
  Optional<ComandoConciliacao> buscar(String perfilRef, UUID chave);

  ComandoConciliacao salvar(ComandoConciliacao comando);
}
