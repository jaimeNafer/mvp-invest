package io.github.jaimenafer.lastro.operacoes.domain.model;

import jakarta.persistence.*;
import java.time.Instant;
import java.util.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

@Entity
@Table(name = "grupos_associacao")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class GrupoAssociacao {
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  private String perfilRef;

  @JdbcTypeCode(SqlTypes.JSON)
  @Column(columnDefinition = "json")
  private String propostaJson;

  @JdbcTypeCode(SqlTypes.JSON)
  @Column(columnDefinition = "json")
  private String aplicadaJson;

  private long versao;
  private String alteradoPor;
  private Instant alteradoEm;
  private Instant criadoEm;

  @Getter(AccessLevel.NONE)
  @ElementCollection
  @CollectionTable(
      name = "historico_grupos_associacao",
      joinColumns = @JoinColumn(name = "grupo_id"))
  @OrderColumn(name = "ordem")
  private List<HistoricoProposta> historico = new ArrayList<>();

  public static GrupoAssociacao criar(String perfil, String proposta, Instant agora) {
    var g = new GrupoAssociacao();
    g.perfilRef = perfil;
    g.propostaJson = proposta;
    g.versao = 1;
    g.alteradoPor = perfil;
    g.alteradoEm = agora;
    g.criadoEm = agora;
    return g;
  }

  public void propor(String proposta, String autor, Instant agora) {
    if (Objects.equals(propostaJson, proposta)) return;
    historico.add(HistoricoProposta.registrar(versao, propostaJson, alteradoPor, alteradoEm));
    propostaJson = proposta;
    versao++;
    alteradoPor = autor;
    alteradoEm = agora;
  }

  public boolean pendente() {
    return !Objects.equals(propostaJson, aplicadaJson);
  }

  public void aplicar() {
    aplicadaJson = propostaJson;
  }

  public List<HistoricoProposta> versoesAnteriores() {
    return List.copyOf(historico);
  }
}
