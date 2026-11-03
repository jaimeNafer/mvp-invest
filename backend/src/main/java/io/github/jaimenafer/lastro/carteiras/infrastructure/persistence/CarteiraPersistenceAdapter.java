package io.github.jaimenafer.lastro.carteiras.infrastructure.persistence;

import static org.hibernate.exception.ConstraintViolationException.ConstraintKind.UNIQUE;

import io.github.jaimenafer.lastro.carteiras.domain.exception.CarteiraConflitanteException;
import io.github.jaimenafer.lastro.carteiras.domain.exception.CarteiraNaoEncontradaException;
import io.github.jaimenafer.lastro.carteiras.domain.model.*;
import io.github.jaimenafer.lastro.carteiras.domain.repository.ICarteiraRepository;
import java.util.*;
import lombok.RequiredArgsConstructor;
import org.hibernate.exception.ConstraintViolationException;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Repository;

@Repository
@RequiredArgsConstructor
public class CarteiraPersistenceAdapter implements ICarteiraRepository {
  private final ICarteiraJpaRepository jpa;

  @Override
  public List<Carteira> findByPerfilRefOrderByNomeAscIdAsc(String perfilRef) {
    return jpa.findByPerfilRefOrderByNomeAscIdAsc(perfilRef);
  }

  @Override
  public List<Carteira> findByPerfilRefAndAtivoOrderByNomeAscIdAsc(
      String perfilRef, boolean ativo) {
    return jpa.findByPerfilRefAndAtivoOrderByNomeAscIdAsc(perfilRef, ativo);
  }

  @Override
  public Optional<Carteira> findByIdAndPerfilRef(long id, String perfilRef) {
    return jpa.findByIdAndPerfilRef(id, perfilRef);
  }

  @Override
  public Carteira buscarObrigatoria(long carteiraId, String perfilRef) {
    return jpa.findByIdAndPerfilRef(carteiraId, perfilRef)
        .orElseThrow(() -> new CarteiraNaoEncontradaException("Carteira não encontrada."));
  }

  @Override
  public Carteira saveAndFlush(Carteira carteira) {
    try {
      return jpa.saveAndFlush(carteira);
    } catch (DataIntegrityViolationException erro) {
      if (erro.getCause() instanceof ConstraintViolationException violacao
          && violacao.getKind() == UNIQUE)
        throw new CarteiraConflitanteException(
            "Já existe uma carteira com este nome para o usuário. Informe outro nome.", erro);
      throw erro;
    }
  }
}
