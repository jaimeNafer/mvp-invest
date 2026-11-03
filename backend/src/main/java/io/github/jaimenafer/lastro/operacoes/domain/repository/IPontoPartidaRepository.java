package io.github.jaimenafer.lastro.operacoes.domain.repository;

import io.github.jaimenafer.lastro.operacoes.domain.model.PontoPartida;
import java.util.Optional;

public interface IPontoPartidaRepository {
  Optional<PontoPartida> bloquear(String usuario);

  Optional<PontoPartida> findById(String usuario);

  PontoPartida inserir(PontoPartida ponto);
}
