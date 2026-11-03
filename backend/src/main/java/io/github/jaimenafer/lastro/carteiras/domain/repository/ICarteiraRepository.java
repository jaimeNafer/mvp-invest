package io.github.jaimenafer.lastro.carteiras.domain.repository;

import io.github.jaimenafer.lastro.carteiras.domain.model.Carteira;
import java.util.List;
import java.util.Optional;

public interface ICarteiraRepository {

  List<Carteira> findByPerfilRefOrderByNomeAscIdAsc(String perfilRef);

  List<Carteira> findByPerfilRefAndAtivoOrderByNomeAscIdAsc(String perfilRef, boolean ativo);

  Optional<Carteira> findByIdAndPerfilRef(long id, String perfilRef);

  Carteira buscarObrigatoria(long carteiraId, String perfilRef);

  Carteira saveAndFlush(Carteira carteira);
}
