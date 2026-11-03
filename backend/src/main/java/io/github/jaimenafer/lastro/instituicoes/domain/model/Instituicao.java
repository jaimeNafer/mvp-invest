package io.github.jaimenafer.lastro.instituicoes.domain.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "instituicoes")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Instituicao {
  @Id @Getter private Long id;

  @Column(nullable = false, length = 160)
  private String nome;

  @Column(nullable = false)
  private boolean ativo;
}
