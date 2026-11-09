package io.github.jaimenafer.lastro.operacoes.domain.repository;

import io.github.jaimenafer.lastro.operacoes.domain.model.CotacaoHistorica;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

public interface IHistoricoCotacaoRepository {
  void registrar(Long parId, String granularidade, Instant instante, BigDecimal preco, String checksum, Instant obtida);
  List<CotacaoHistorica> buscar(Instant inicio, Instant fim, String granularidade);
  Instant ultimaAtualizacao();
}
