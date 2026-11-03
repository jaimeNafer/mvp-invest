package io.github.jaimenafer.lastro.carteiras.domain.model;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "contas")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Conta {
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  @Getter
  private Long id;

  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "carteira_id", nullable = false)
  private Carteira carteira;

  @Column(name = "instituicao_id", nullable = false)
  @Getter
  private Long instituicaoId;

  public static Conta vincular(Carteira carteira, Long instituicaoId) {
    var conta = new Conta();
    conta.carteira = carteira;
    conta.instituicaoId = instituicaoId;
    return conta;
  }
}
