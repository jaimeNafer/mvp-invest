package io.github.jaimenafer.lastro.operacoes.domain.model;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "contas_custodia")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class ContaCustodia {
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  private String perfilRef;

  private Long instituicaoId;

  private String codigo;

  private String nome;

  private Instant criadoEm;

  public static ContaCustodia criar(
      String perfilRef, Long instituicaoId, String codigo, String nome, Instant agora) {
    var conta = new ContaCustodia();
    conta.perfilRef = perfilRef;
    conta.instituicaoId = instituicaoId;
    conta.codigo = codigo;
    conta.nome = nome;
    conta.criadoEm = agora;
    return conta;
  }
}
