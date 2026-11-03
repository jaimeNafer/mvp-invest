package io.github.jaimenafer.lastro.operacoes.infrastructure.persistence;

import static org.hibernate.exception.ConstraintViolationException.ConstraintKind.UNIQUE;

import io.github.jaimenafer.lastro.operacoes.domain.exception.SaldoInicialConflitanteException;
import io.github.jaimenafer.lastro.operacoes.domain.model.*;
import io.github.jaimenafer.lastro.operacoes.domain.repository.IPontoPartidaRepository;
import jakarta.persistence.PersistenceContext;
import java.util.*;
import lombok.RequiredArgsConstructor;
import org.hibernate.exception.ConstraintViolationException;
import org.springframework.stereotype.Repository;

@Repository
@RequiredArgsConstructor
public class PontoPartidaPersistenceAdapter implements IPontoPartidaRepository {
  private final IPontoPartidaJpaRepository jpa;

  @Override
  public Optional<PontoPartida> bloquear(String usuario) {
    return jpa.bloquear(usuario);
  }

  @Override
  public Optional<PontoPartida> findById(String usuario) {
    return jpa.findById(usuario);
  }

  @PersistenceContext private jakarta.persistence.EntityManager entidades;

  @Override
  public PontoPartida inserir(PontoPartida ponto) {
    try {
      entidades.persist(ponto);
      entidades.flush();
      return ponto;
    } catch (ConstraintViolationException erro) {
      if (erro.getKind() == UNIQUE)
        throw new SaldoInicialConflitanteException(
            "Outra solicitação iniciou o acompanhamento deste usuário. Atualize os saldos e tente"
                + " novamente.",
            erro);
      throw erro;
    }
  }
}
