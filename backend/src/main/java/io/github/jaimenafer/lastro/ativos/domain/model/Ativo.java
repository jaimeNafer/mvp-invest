package io.github.jaimenafer.lastro.ativos.domain.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

@Entity
@Table(name = "ativos")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Ativo {
  @Id private Long id;
  private String codigo;
  private String ticker;
  private String nome;

  @Enumerated(EnumType.STRING)
  @JdbcTypeCode(SqlTypes.VARCHAR)
  @Column(length = 6, nullable = false)
  private TipoAtivo tipo;

  @Column(name = "casas_decimais")
  private byte casasDecimais;

  private boolean ativo;
}
