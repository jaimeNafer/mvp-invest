package io.github.jaimenafer.lastro.operacoes.domain.repository;

import io.github.jaimenafer.lastro.operacoes.domain.model.ComandoConciliacao;
import java.util.Optional;
import java.util.UUID;
import java.util.Collection;

public interface IComandoConciliacaoRepository {
  Optional<ComandoConciliacao> buscar(String perfilRef, UUID chave);

  ComandoConciliacao salvar(ComandoConciliacao comando);

  void excluirRespostasDeRegistros(String perfilRef, Collection<String> ids);

  void excluirRespostasDeGrupos(String perfilRef, Collection<String> numeros);
}
