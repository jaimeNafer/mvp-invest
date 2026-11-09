package io.github.jaimenafer.lastro.operacoes.infrastructure.persistence;

import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

@Repository
@RequiredArgsConstructor
public class LimpezaValoracoes {
  private final JdbcTemplate jdbc;

  public void excluirDoRegistro(String perfil, long registroId) {
    excluir(perfil, "registro_id", registroId);
  }

  public void excluirDoGrupo(String perfil, long grupoId) {
    excluir(perfil, "grupo_id", grupoId);
  }

  private void excluir(String perfil, String coluna, long id) {
    jdbc.update("DELETE h FROM historico_valoracoes_manuais h JOIN valoracoes_manuais v ON v.id=h.valoracao_id WHERE v.perfil_ref=? AND v." + coluna + "=?", perfil, id);
    jdbc.update("DELETE FROM valoracoes_manuais WHERE perfil_ref=? AND " + coluna + "=?", perfil, id);
  }
}
