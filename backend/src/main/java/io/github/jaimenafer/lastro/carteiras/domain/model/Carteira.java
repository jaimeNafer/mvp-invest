package io.github.jaimenafer.lastro.carteiras.domain.model;

import io.github.jaimenafer.lastro.carteiras.domain.exception.CarteiraInvalidaException;
import jakarta.persistence.*;
import java.util.Collection;
import java.util.HashSet;
import java.util.Set;
import java.util.stream.Collectors;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "carteiras")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Carteira {
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  @Getter
  private Long id;

  @Column(name = "perfil_ref", nullable = false, length = 120)
  private String perfilRef;

  @Column(nullable = false, length = 160)
  @Getter
  private String nome;

  @Column(nullable = false)
  @Getter
  private boolean ativo = true;

  @OneToMany(
      mappedBy = "carteira",
      cascade = {CascadeType.PERSIST, CascadeType.MERGE},
      orphanRemoval = true)
  private Set<Conta> contas = new HashSet<>();

  public static Carteira criar(String perfilRef, String nome) {
    var carteira = new Carteira();
    carteira.perfilRef = perfilRef;
    carteira.nome = nome;
    return carteira;
  }

  public Set<Conta> contasVinculadas() {
    return Set.copyOf(contas);
  }

  public void alterar(String nome, Collection<Long> instituicaoIds) {
    this.nome = nome;
    sincronizarInstituicoes(instituicaoIds);
  }

  public Set<Long> instituicaoIdsVinculadas() {
    return contas.stream().map(Conta::getInstituicaoId).collect(Collectors.toUnmodifiableSet());
  }

  public void validarEdicaoPermitida() {
    if (!ativo) throw new CarteiraInvalidaException("Reative a carteira para editá-la.");
  }

  public void sincronizarInstituicoes(Collection<Long> instituicaoIds) {
    Set<Long> selecionadas = new HashSet<>(instituicaoIds);
    contas.removeIf(conta -> !selecionadas.contains(conta.getInstituicaoId()));
    Set<Long> vinculadas = new HashSet<>(instituicaoIdsVinculadas());
    for (Long instituicaoId : instituicaoIds) {
      if (vinculadas.add(instituicaoId)) {
        contas.add(Conta.vincular(this, instituicaoId));
      }
    }
  }

  public void alterarSituacao(boolean ativo) {
    this.ativo = ativo;
  }
}
