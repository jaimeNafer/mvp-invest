package io.github.jaimenafer.lastro.ativos.infrastructure.persistence;

import io.github.jaimenafer.lastro.ativos.domain.model.*;
import io.github.jaimenafer.lastro.ativos.domain.repository.IAtivoRepository;
import java.util.*;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

@Repository
@RequiredArgsConstructor
public class AtivoPersistenceAdapter implements IAtivoRepository {
  private final IAtivoJpaRepository jpa;
  private final JdbcTemplate jdbc;

  @Override
  public Ativo identificarNaBinance(String simbolo) {
    String codigo = "binance:" + simbolo;
    jdbc.update(
        "INSERT INTO ativos (codigo, ticker, nome, tipo, casas_decimais, ativo)"
            + " VALUES (?, ?, NULL, NULL, 18, TRUE) ON DUPLICATE KEY UPDATE codigo = codigo",
        codigo,
        simbolo);
    return jpa.findByCodigo(codigo).orElseThrow();
  }

  @Override
  public Optional<Ativo> findByCodigo(String codigo) {
    return jpa.findByCodigo(codigo);
  }

  @Override
  public List<Ativo> findAllByOrderByTickerAscIdAsc() {
    return jpa.findAllByOrderByTickerAscIdAsc();
  }

  @Override
  public Optional<Ativo> findById(Long id) {
    return jpa.findById(id);
  }

  @Override
  public java.util.List<Ativo> findAllByIdIn(java.util.Collection<Long> ids) {
    return jpa.findAllByIdIn(ids);
  }
}
