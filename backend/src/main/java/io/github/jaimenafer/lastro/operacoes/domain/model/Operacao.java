package io.github.jaimenafer.lastro.operacoes.domain.model;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

@Entity
@Table(name = "operacoes")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Operacao {
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @Column(length = 120)
  private String perfilRef;

  @Column(name = "instituicao_id", nullable = false)
  private Long instituicaoId;

  @Column(name = "ativo_id", nullable = false)
  private Long ativoId;

  @Enumerated(EnumType.STRING)
  @JdbcTypeCode(SqlTypes.VARCHAR)
  @Column(length = 20, nullable = false)
  private TipoOperacao tipo = TipoOperacao.SALDO_INICIAL;

  private LocalDate dataReferencia;
  private Instant ocorridaEm;

  @Column(precision = 38, scale = 18)
  private BigDecimal quantidade;

  @Column(precision = 38, scale = 8)
  private BigDecimal custoTotalBrl;

  @Enumerated(EnumType.STRING)
  @JdbcTypeCode(SqlTypes.VARCHAR)
  @Column(length = 12, nullable = false)
  private SituacaoOperacao situacao = SituacaoOperacao.CONFIRMADA;

  private Long substituiId;

  @Column(length = 300)
  private String justificativaCustoZero;

  @Column(length = 300)
  private String motivoAlteracao;

  @JdbcTypeCode(SqlTypes.CHAR)
  @Column(length = 36)
  private String chaveRequisicao;

  @JdbcTypeCode(SqlTypes.CHAR)
  @Column(length = 64)
  private String fingerprint;

  private Instant criadoEm;
  private String criadoPor;
  private Instant alteradoEm;
  private String alteradoPor;

  public static Operacao registrarSaldoInicial(
      String usuario,
      Long instituicaoId,
      Long ativoId,
      LocalDate data,
      BigDecimal quantidade,
      BigDecimal custo,
      String justificativa,
      String motivo,
      Long anterior,
      String chave,
      String fingerprint,
      Instant agora) {
    var operacao = new Operacao();
    operacao.perfilRef = usuario;
    operacao.instituicaoId = instituicaoId;
    operacao.ativoId = ativoId;
    operacao.dataReferencia = data;
    operacao.ocorridaEm = data.atStartOfDay(ZoneId.of("America/Fortaleza")).toInstant();
    operacao.quantidade = quantidade;
    operacao.custoTotalBrl = custo;
    operacao.justificativaCustoZero = justificativa.isEmpty() ? null : justificativa;
    operacao.motivoAlteracao = motivo.isEmpty() ? null : motivo;
    operacao.substituiId = anterior;
    operacao.chaveRequisicao = chave;
    operacao.fingerprint = fingerprint;
    operacao.criadoEm = operacao.alteradoEm = agora;
    operacao.criadoPor = operacao.alteradoPor = usuario;
    return operacao;
  }

  public boolean confirmada() {
    return situacao == SituacaoOperacao.CONFIRMADA;
  }

  public void encerrar(SituacaoOperacao situacao, String motivo, String autor, Instant agora) {
    this.situacao = situacao;
    motivoAlteracao = motivo;
    alteradoPor = autor;
    alteradoEm = agora;
  }
}
