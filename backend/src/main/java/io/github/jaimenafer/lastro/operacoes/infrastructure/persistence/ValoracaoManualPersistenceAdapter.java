package io.github.jaimenafer.lastro.operacoes.infrastructure.persistence;

import io.github.jaimenafer.lastro.operacoes.domain.model.ValoracaoManual;
import io.github.jaimenafer.lastro.operacoes.domain.repository.IValoracaoManualRepository;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@Repository
@RequiredArgsConstructor
public class ValoracaoManualPersistenceAdapter implements IValoracaoManualRepository {
  private final JdbcTemplate jdbc;

  @Override
  public List<ValoracaoManual> listar(String perfil) {
    return jdbc.query("""
        SELECT registro_id,grupo_id,valor_brl,identidade,versao,alterado_por,alterado_em
        FROM valoracoes_manuais WHERE perfil_ref=? ORDER BY id
        """, (rs, indice) -> new ValoracaoManual(rs.getObject(1, Long.class),
            rs.getObject(2, Long.class), rs.getBigDecimal(3), rs.getString(4), rs.getLong(5), rs.getString(6),
            rs.getObject(7, LocalDateTime.class).toInstant(ZoneOffset.UTC)), perfil);
  }

  @Override
  @Transactional(propagation = Propagation.MANDATORY)
  public void salvar(String perfil, Long registroId, Long grupoId, BigDecimal valor, String identidade, Instant agora) {
    jdbc.update("""
        INSERT INTO valoracoes_manuais(perfil_ref,registro_id,grupo_id,valor_brl,identidade,versao,alterado_por,alterado_em)
        VALUES (?,?,?,?,?,1,?,?) ON DUPLICATE KEY UPDATE id=id
        """, perfil, registroId, grupoId, valor, identidade, perfil, LocalDateTime.ofInstant(agora, ZoneOffset.UTC));
    Map<String, Object> atual = jdbc.queryForMap("""
        SELECT id,valor_brl,identidade FROM valoracoes_manuais WHERE perfil_ref=?
          AND ((registro_id=? AND ? IS NOT NULL) OR (grupo_id=? AND ? IS NOT NULL)) FOR UPDATE
        """, perfil, registroId, registroId, grupoId, grupoId);
    if (((BigDecimal) atual.get("valor_brl")).compareTo(valor) == 0
        && java.util.Objects.equals(atual.get("identidade"), identidade)) return;
    long id = ((Number) atual.get("id")).longValue();
    jdbc.update("""
        INSERT INTO historico_valoracoes_manuais(valoracao_id,versao,valor_brl,identidade,alterado_por,alterado_em)
        SELECT id,versao,valor_brl,identidade,alterado_por,alterado_em FROM valoracoes_manuais WHERE id=?
        """, id);
    jdbc.update("""
        UPDATE valoracoes_manuais SET valor_brl=?,identidade=?,versao=versao+1,alterado_por=?,alterado_em=? WHERE id=?
        """, valor, identidade, perfil, LocalDateTime.ofInstant(agora, ZoneOffset.UTC), id);
  }
}
