package io.github.jaimenafer.lastro.operacoes.infrastructure.persistence;

import io.github.jaimenafer.lastro.operacoes.domain.model.CargaCotacao;
import io.github.jaimenafer.lastro.operacoes.domain.model.ParCotacao;
import io.github.jaimenafer.lastro.operacoes.domain.repository.ICargaCotacaoRepository;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@Repository
@RequiredArgsConstructor
@Transactional(propagation = Propagation.MANDATORY)
public class CargaCotacaoPersistenceAdapter implements ICargaCotacaoRepository {
  private final JdbcTemplate jdbc;

  @Override
  public void prepararParesConhecidos() {
    jdbc.update("""
        INSERT INTO pares_cotacao(ativo_base_id,ativo_cotacao_id,fonte,simbolo)
        SELECT base.id,moeda.id,'BINANCE',CONCAT(base.ticker,moeda.ticker)
        FROM ativos base JOIN ativos moeda ON moeda.codigo='cripto:USDT'
        WHERE base.codigo IN ('cripto:BTC','cripto:ETH','cripto:SOL','cripto:BNB',
          'cripto:IO','cripto:LISTA','cripto:LINK','cripto:NEAR','cripto:LDO',
          'cripto:ARKM','cripto:RONIN','cripto:PYTH','cripto:TIA','cripto:USDC')
          AND BINARY base.codigo=BINARY CONCAT('cripto:',base.ticker)
          AND BINARY moeda.ticker=BINARY 'USDT'
        ON DUPLICATE KEY UPDATE id=pares_cotacao.id
        """);
    jdbc.update("""
        INSERT INTO pares_cotacao(ativo_base_id,ativo_cotacao_id,fonte,simbolo)
        SELECT base.id,moeda.id,'BINANCE',CONCAT(base.ticker,moeda.ticker)
        FROM ativos base JOIN ativos moeda ON moeda.codigo='fiat:BRL'
        WHERE base.codigo IN ('cripto:USDT','cripto:USDC')
          AND BINARY base.codigo=BINARY CONCAT('cripto:',base.ticker)
          AND BINARY moeda.ticker=BINARY 'BRL'
        ON DUPLICATE KEY UPDATE id=pares_cotacao.id
        """);
  }

  @Override
  public List<ParCotacao> listarPares() {
    return jdbc.query("""
        SELECT id,ativo_base_id,ativo_cotacao_id,simbolo FROM pares_cotacao
        WHERE fonte='BINANCE' ORDER BY id
        """, (rs, indice) -> new ParCotacao(rs.getLong(1), rs.getLong(2), rs.getLong(3), rs.getString(4)));
  }

  @Override
  public void agendar(Long parId, String granularidade, Instant inicio, Instant fim, Instant agora) {
    jdbc.update("""
        UPDATE cargas_cotacao SET estado='PENDENTE',tentativas=0,proxima_tentativa_em=NULL,
          atualizada_em=?
        WHERE par_id=? AND granularidade=? AND inicio_em=? AND fim_em=?
          AND (estado='FALHA_ESGOTADA' OR
            (estado='PARCIAL' AND (fim_em<=? OR atualizada_em<=?)))
        """, utc(agora), parId, granularidade, utc(inicio), utc(fim), utc(agora),
        utc(agora.minusSeconds(24 * 60 * 60)));
    jdbc.update("""
        INSERT INTO cargas_cotacao(par_id,granularidade,inicio_em,fim_em,estado,atualizada_em)
        VALUES (?,?,?,?,'PENDENTE',?) ON DUPLICATE KEY UPDATE id=id
        """, parId, granularidade, utc(inicio), utc(fim), utc(agora));
  }

  @Override
  public Optional<CargaCotacao> reservar(Instant agora, Instant reservaAte, String token) {
    List<CargaCotacao> candidatas = jdbc.query("""
        SELECT c.id,c.par_id,p.simbolo,c.granularidade,c.inicio_em,c.fim_em,c.tentativas,c.estado
        FROM cargas_cotacao c JOIN pares_cotacao p ON p.id=c.par_id
        WHERE c.estado='PENDENTE'
          OR (c.estado='FALHA' AND c.proxima_tentativa_em IS NOT NULL AND c.proxima_tentativa_em<=?)
          OR (c.estado='EXECUTANDO' AND c.reserva_ate<=?)
          OR (c.estado='PARCIAL' AND (c.fim_em<=? OR c.atualizada_em<=?))
        ORDER BY (c.granularidade='HORA') DESC,c.id LIMIT 1 FOR UPDATE SKIP LOCKED
        """, (rs, indice) -> new CargaCotacao(rs.getLong(1), rs.getLong(2), rs.getString(3),
            rs.getString(4), rs.getObject(5, LocalDateTime.class).toInstant(ZoneOffset.UTC),
            rs.getObject(6, LocalDateTime.class).toInstant(ZoneOffset.UTC), token,
            "PARCIAL".equals(rs.getString(8))
                ? 1 : rs.getInt(7) + 1),
        utc(agora), utc(agora), utc(agora), utc(agora.minusSeconds(24 * 60 * 60)));
    if (candidatas.isEmpty()) return Optional.empty();
    CargaCotacao carga = candidatas.getFirst();
    jdbc.update("""
        UPDATE cargas_cotacao SET tentativas=CASE WHEN estado='PARCIAL'
          THEN 1 ELSE tentativas+1 END,estado='EXECUTANDO',reserva_token=?,reserva_ate=?,
          atualizada_em=? WHERE id=?
        """, token, utc(reservaAte), utc(agora), carga.id());
    return Optional.of(carga);
  }

  @Override
  public boolean concluir(Long id, String token, boolean semDados, Instant agora) {
    return concluir(id, token, semDados, agora, agora);
  }

  @Override
  public boolean concluir(Long id, String token, boolean semDados, Instant inicioConsulta, Instant agora) {
    return jdbc.update("""
        UPDATE cargas_cotacao SET estado=CASE WHEN fim_em>? THEN 'PARCIAL' ELSE ? END,
          reserva_token=NULL,reserva_ate=NULL,atualizada_em=?
        WHERE id=? AND estado='EXECUTANDO' AND reserva_token=? AND reserva_ate>?
        """, utc(inicioConsulta), semDados ? "SEM_DADOS" : "CONCLUIDA", utc(agora), id, token, utc(agora)) == 1;
  }

  @Override
  public boolean falhar(Long id, String token, Instant agora, Instant proximaTentativa) {
    return jdbc.update("""
        UPDATE cargas_cotacao SET estado=?,proxima_tentativa_em=?,reserva_token=NULL,
          reserva_ate=NULL,atualizada_em=?
        WHERE id=? AND estado='EXECUTANDO' AND reserva_token=? AND reserva_ate>?
        """, proximaTentativa == null ? "FALHA_ESGOTADA" : "FALHA",
        proximaTentativa == null ? null : utc(proximaTentativa), utc(agora), id, token,
        utc(agora)) == 1;
  }

  private LocalDateTime utc(Instant instante) {
    return LocalDateTime.ofInstant(instante, ZoneOffset.UTC);
  }
}
