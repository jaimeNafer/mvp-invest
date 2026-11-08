package io.github.jaimenafer.lastro.operacoes.domain.model;

import jakarta.persistence.*;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

@Entity
@Table(name = "registros_conferencia")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class RegistroConferencia {
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  private String perfilRef;

  private String arquivoRef;

  private String origemRef;

  private Long instituicaoId;

  @JdbcTypeCode(SqlTypes.JSON)
  @Column(columnDefinition = "json", updatable = false)
  private String originalJson;

  @JdbcTypeCode(SqlTypes.JSON)
  @Column(columnDefinition = "json", updatable = false)
  private String errosJson;

  @JdbcTypeCode(SqlTypes.JSON)
  @Column(columnDefinition = "json")
  private String propostaJson;

  @JdbcTypeCode(SqlTypes.JSON)
  @Column(columnDefinition = "json")
  private String aplicadaJson;

  private long versao;

  @Getter(AccessLevel.NONE)
  @ElementCollection
  @CollectionTable(name = "historico_propostas", joinColumns = @JoinColumn(name = "registro_id"))
  @OrderColumn(name = "ordem")
  private List<HistoricoProposta> historico = new ArrayList<>();

  private String revisadoPor;

  private Instant revisadoEm;

  private Instant criadoEm;

  public static RegistroConferencia criar(
      String perfilRef,
      String arquivoRef,
      String origemRef,
      Long instituicaoId,
      String originalJson,
      String errosJson,
      String propostaJson,
      Instant agora) {
    var registro = new RegistroConferencia();
    registro.perfilRef = perfilRef;
    registro.arquivoRef = arquivoRef;
    registro.origemRef = origemRef;
    registro.instituicaoId = instituicaoId;
    registro.originalJson = originalJson;
    registro.errosJson = errosJson;
    registro.propostaJson = propostaJson;
    registro.versao = 1;
    registro.criadoEm = agora;
    return registro;
  }

  public void editar(String propostaJson, String autor, Instant agora) {
    if (java.util.Objects.equals(this.propostaJson, propostaJson)) return;
    historico.add(HistoricoProposta.registrar(versao, this.propostaJson, autor, agora));
    this.propostaJson = propostaJson;
    versao = Math.addExact(versao, 1);
    revisadoPor = null;
    revisadoEm = null;
  }

  public List<HistoricoProposta> versoesAnteriores() {
    return List.copyOf(historico);
  }

  public void aplicar() {
    aplicar(propostaJson);
  }

  public void aplicar(String propostaEfetivaJson) {
    aplicadaJson = propostaEfetivaJson;
  }

  public void revisar(String autor, Instant agora) {
    revisadoPor = autor;
    revisadoEm = agora;
  }

  public boolean possuiAlteracaoPendente() {
    return !java.util.Objects.equals(propostaJson, aplicadaJson);
  }
}
