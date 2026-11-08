package io.github.jaimenafer.lastro.operacoes.domain.model;

import jakarta.persistence.*;
import java.time.Instant;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

@Embeddable
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class HistoricoProposta {
  private long versao;

  @JdbcTypeCode(SqlTypes.JSON)
  @Column(columnDefinition = "json")
  private String propostaJson;

  private String alteradoPor;
  private Instant alteradoEm;

  public static HistoricoProposta registrar(
      long versao, String propostaJson, String autor, Instant agora) {
    var historico = new HistoricoProposta();
    historico.versao = versao;
    historico.propostaJson = propostaJson;
    historico.alteradoPor = autor;
    historico.alteradoEm = agora;
    return historico;
  }
}
