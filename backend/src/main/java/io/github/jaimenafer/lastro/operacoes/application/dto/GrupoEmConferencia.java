package io.github.jaimenafer.lastro.operacoes.application.dto;

import io.github.jaimenafer.lastro.operacoes.domain.model.*;
import java.math.BigDecimal;

public record GrupoEmConferencia(
    Long id,
    String identificador,
    PropostaGrupo proposta,
    ResumoGrupo resumo,
    boolean pendente,
    boolean automatico,
    Valoracao valoracao,
    BigDecimal taxaExternaBrl) {
  public GrupoEmConferencia(Long id, String identificador, PropostaGrupo proposta,
      ResumoGrupo resumo, boolean pendente, boolean automatico) {
    this(id, identificador, proposta, resumo, pendente, automatico, null, null);
  }
}
