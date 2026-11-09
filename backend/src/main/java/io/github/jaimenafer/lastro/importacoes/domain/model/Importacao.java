package io.github.jaimenafer.lastro.importacoes.domain.model;

import jakarta.persistence.*;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

@Entity
@Table(name = "importacoes")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Importacao {
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  @Getter
  private Long id;

  @Column(name = "perfil_ref", nullable = false, length = 120)
  private String perfilRef;

  @JdbcTypeCode(SqlTypes.CHAR)
  @Column(nullable = false, length = 36)
  private String chaveRequisicao;

  @JdbcTypeCode(SqlTypes.CHAR)
  @Column(nullable = false, length = 64)
  @Getter
  private String hash;

  @Column(nullable = false, length = 64)
  @Getter
  private String decisoes;

  @Column(nullable = false, length = 255)
  @Getter
  private String nomeArquivo;

  @Column(nullable = false, length = 40)
  private String versaoLeitor;

  @Basic(fetch = FetchType.LAZY)
  @Lob
  @Column(nullable = false, columnDefinition = "MEDIUMBLOB")
  private byte[] arquivo;

  @Column(nullable = false)
  @Getter
  private Instant criadoEm;

  @Column(nullable = false)
  @Getter
  private int novos;

  @Column(nullable = false)
  @Getter
  private int reconhecidos;

  @OneToMany(mappedBy = "importacao", cascade = {CascadeType.PERSIST, CascadeType.REMOVE})
  @OrderBy("linha ASC, id ASC")
  private List<EventoImportado> eventos = new ArrayList<>();

  public static Importacao registrar(
      String perfil,
      String chave,
      String hash,
      String decisoes,
      String nome,
      byte[] arquivo,
      int novos,
      int reconhecidos) {
    Importacao importacao = new Importacao();
    importacao.perfilRef = perfil;
    importacao.chaveRequisicao = chave;
    importacao.hash = hash;
    importacao.decisoes = decisoes;
    importacao.nomeArquivo = nome;
    importacao.arquivo = arquivo.clone();
    importacao.versaoLeitor = "binance-brl-v1";
    importacao.criadoEm = Instant.now().truncatedTo(ChronoUnit.MICROS);
    importacao.novos = novos;
    importacao.reconhecidos = reconhecidos;
    return importacao;
  }

  public void vincular(int linha, String referencia, String operacaoId) {
    eventos.add(EventoImportado.vincular(this, linha, referencia, Long.parseLong(operacaoId)));
  }

  public List<EventoImportado> eventosRegistrados() {
    return List.copyOf(eventos);
  }
}
