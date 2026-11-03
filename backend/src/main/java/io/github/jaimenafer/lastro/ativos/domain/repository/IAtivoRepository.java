package io.github.jaimenafer.lastro.ativos.domain.repository;

import io.github.jaimenafer.lastro.ativos.domain.model.Ativo;
import java.util.List;
import java.util.Optional;

public interface IAtivoRepository {
  List<String> CODIGOS =
      List.of("fiat:BRL", "fiat:USD", "cripto:BTC", "cripto:ETH", "cripto:USDT", "cripto:USDC");

  List<Ativo> findAllByCodigoInOrderByTickerAscIdAsc(List<String> codigos);

  Optional<Ativo> findById(Long id);

  List<Ativo> findAllByIdIn(java.util.Collection<Long> ids);
}
