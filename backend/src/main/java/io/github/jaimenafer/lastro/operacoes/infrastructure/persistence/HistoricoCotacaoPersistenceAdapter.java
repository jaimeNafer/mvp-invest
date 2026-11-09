package io.github.jaimenafer.lastro.operacoes.infrastructure.persistence;

import io.github.jaimenafer.lastro.operacoes.domain.model.CotacaoHistorica;
import io.github.jaimenafer.lastro.operacoes.domain.repository.IHistoricoCotacaoRepository;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@Repository
@RequiredArgsConstructor
public class HistoricoCotacaoPersistenceAdapter implements IHistoricoCotacaoRepository {
  private final JdbcTemplate jdbc;

  @Override
  public Instant ultimaAtualizacao() {
    LocalDateTime atualizacao = jdbc.queryForObject(
        "SELECT MAX(obtido_em) FROM cotacoes_historicas", LocalDateTime.class);
    return atualizacao == null ? null : atualizacao.toInstant(ZoneOffset.UTC);
  }

  @Override
  @Transactional(propagation = Propagation.MANDATORY)
  public void registrar(Long parId, String granularidade, Instant instante, BigDecimal preco,
      String checksum, Instant obtida) {
    jdbc.update("""
        INSERT INTO cotacoes_historicas(par_id,granularidade,instante,preco,checksum,obtido_em)
        VALUES (?,?,?,?,?,?) ON DUPLICATE KEY UPDATE id=id
        """, parId, granularidade, LocalDateTime.ofInstant(instante, ZoneOffset.UTC), preco, checksum, LocalDateTime.ofInstant(obtida, ZoneOffset.UTC));
    Map<String, Object> atual = jdbc.queryForMap("""
        SELECT id,versao,preco,checksum FROM cotacoes_historicas
        WHERE par_id=? AND granularidade=? AND instante=? FOR UPDATE
        """, parId, granularidade, LocalDateTime.ofInstant(instante, ZoneOffset.UTC));
    if (((BigDecimal) atual.get("preco")).compareTo(preco) == 0
        && Objects.equals(atual.get("checksum"), checksum)) return;
    long id = ((Number) atual.get("id")).longValue();
    jdbc.update("""
        INSERT INTO historico_cotacoes(cotacao_id,versao,preco,checksum,obtido_em)
        SELECT id,versao,preco,checksum,obtido_em FROM cotacoes_historicas WHERE id=?
        """, id);
    jdbc.update("""
        UPDATE cotacoes_historicas SET preco=?,checksum=?,obtido_em=?,versao=versao+1 WHERE id=?
        """, preco, checksum, LocalDateTime.ofInstant(obtida, ZoneOffset.UTC), id);
  }

  @Override
  public List<CotacaoHistorica> buscar(Instant inicio, Instant fim, String granularidade) {
    return jdbc.query("""
        SELECT p.ativo_base_id,p.ativo_cotacao_id,c.instante,c.preco,p.fonte,c.granularidade,c.versao,c.checksum,c.obtido_em
        FROM cotacoes_historicas c JOIN pares_cotacao p ON p.id=c.par_id
        WHERE c.granularidade=? AND c.instante>=? AND c.instante<?
        ORDER BY c.instante,p.ativo_base_id,p.ativo_cotacao_id
        """, (rs, n) -> new CotacaoHistorica(rs.getLong(1), rs.getLong(2),
            rs.getObject(3, LocalDateTime.class).toInstant(ZoneOffset.UTC), rs.getBigDecimal(4), rs.getString(5), rs.getString(6),
            rs.getLong(7), rs.getString(8), rs.getObject(9, LocalDateTime.class).toInstant(ZoneOffset.UTC)), granularidade,
        LocalDateTime.ofInstant(inicio, ZoneOffset.UTC), LocalDateTime.ofInstant(fim, ZoneOffset.UTC));
  }
}
