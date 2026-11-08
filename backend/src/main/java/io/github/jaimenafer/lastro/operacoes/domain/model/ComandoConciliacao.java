package io.github.jaimenafer.lastro.operacoes.domain.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.UUID;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

@Entity
@Table(name = "comandos_conciliacao")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class ComandoConciliacao {
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  private String perfilRef;

  private String chave;

  private String fingerprint;

  @JdbcTypeCode(SqlTypes.JSON)
  @Column(columnDefinition = "json")
  private String respostaJson;

  private Instant criadoEm;

  public static ComandoConciliacao registrar(
      String perfilRef, UUID chave, String fingerprint, String respostaJson, Instant agora) {
    var comando = new ComandoConciliacao();
    comando.perfilRef = perfilRef;
    comando.chave = chave.toString();
    comando.fingerprint = fingerprint;
    comando.respostaJson = respostaJson;
    comando.criadoEm = agora;
    return comando;
  }
}
