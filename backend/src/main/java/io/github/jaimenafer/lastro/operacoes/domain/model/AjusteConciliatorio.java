package io.github.jaimenafer.lastro.operacoes.domain.model;

import io.github.jaimenafer.lastro.operacoes.domain.exception.OperacaoInvalidaException;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "ajustes_conciliatorios")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class AjusteConciliatorio {
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  private String perfilRef;

  private String lacunaRef;

  private String ancoraRef;

  private Long contaCustodiaId;

  private Long instituicaoId;

  private Long ativoId;

  @Column(precision = 38, scale = 18)
  private BigDecimal quantidade;

  @Column(precision = 38, scale = 8)
  private BigDecimal custoTotalBrl;

  private long versao;

  private boolean ativo;

  private Long anteriorId;

  private String aceitoPor;

  private Instant criadoEm;

  private Instant encerradoEm;

  public static AjusteConciliatorio aceitar(
      String perfilRef,
      String lacunaRef,
      String ancoraRef,
      Long contaCustodiaId,
      Long instituicaoId,
      Long ativoId,
      BigDecimal quantidade,
      BigDecimal custoTotalBrl,
      long versao,
      Long anteriorId,
      String autor,
      Instant agora) {
    if (quantidade == null || quantidade.signum() <= 0)
      throw new OperacaoInvalidaException("A quantidade do ajuste deve ser positiva.");
    if (custoTotalBrl != null && custoTotalBrl.signum() < 0)
      throw new OperacaoInvalidaException("O custo do ajuste não pode ser negativo.");
    var ajuste = new AjusteConciliatorio();
    ajuste.perfilRef = perfilRef;
    ajuste.lacunaRef = lacunaRef;
    ajuste.ancoraRef = ancoraRef;
    ajuste.contaCustodiaId = contaCustodiaId;
    ajuste.instituicaoId = instituicaoId;
    ajuste.ativoId = ativoId;
    ajuste.quantidade = quantidade;
    ajuste.custoTotalBrl =
        custoTotalBrl == null ? null : custoTotalBrl.setScale(8, RoundingMode.HALF_EVEN);
    ajuste.versao = versao;
    ajuste.anteriorId = anteriorId;
    ajuste.ativo = true;
    ajuste.aceitoPor = autor;
    ajuste.criadoEm = agora;
    return ajuste;
  }

  public void encerrar(Instant agora) {
    if (!ativo) return;
    ativo = false;
    encerradoEm = agora;
  }
}
