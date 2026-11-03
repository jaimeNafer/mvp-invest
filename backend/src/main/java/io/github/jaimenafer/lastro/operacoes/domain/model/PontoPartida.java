package io.github.jaimenafer.lastro.operacoes.domain.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import java.time.LocalDate;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "pontos_partida")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class PontoPartida {
  @Id
  @Column(name = "perfil_ref", length = 120)
  private String perfilRef;

  private LocalDate dataReferencia;
  private Instant criadoEm;
  private String criadoPor;
  private Instant alteradoEm;
  private String alteradoPor;

  public static PontoPartida iniciar(String usuario, LocalDate data, Instant agora) {
    var pontoPartida = new PontoPartida();
    pontoPartida.perfilRef = usuario;
    pontoPartida.dataReferencia = data;
    pontoPartida.criadoEm = pontoPartida.alteradoEm = agora;
    pontoPartida.criadoPor = pontoPartida.alteradoPor = usuario;
    return pontoPartida;
  }

  public void redefinir(LocalDate data, Instant agora, String autor) {
    dataReferencia = data;
    alteradoEm = agora;
    alteradoPor = autor;
  }
}
