package io.github.jaimenafer.lastro.importacoes.domain.model;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "importacoes_eventos")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class EventoImportado {
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "importacao_id")
  private Importacao importacao;

  @Getter private int linha;

  @Getter
  @Column(nullable = false, length = 160)
  private String referencia;

  @Getter
  @Column(nullable = false)
  private Long operacaoId;

  public static EventoImportado vincular(
      Importacao importacao, int linha, String referencia, Long operacaoId) {
    EventoImportado evento = new EventoImportado();
    evento.importacao = importacao;
    evento.linha = linha;
    evento.referencia = referencia;
    evento.operacaoId = operacaoId;
    return evento;
  }
}
