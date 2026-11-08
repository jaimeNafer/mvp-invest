package io.github.jaimenafer.lastro.importacoes.domain.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Lob;
import jakarta.persistence.Table;
import java.time.Instant;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "arquivos_recebidos")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class ArquivoRecebido {
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  private String perfilRef;
  private String hash;
  private String nomeArquivo;
  private String versaoLeitor;
  private int registros;
  private Instant criadoEm;

  @Lob
  @Column(columnDefinition = "mediumblob")
  @Getter(AccessLevel.NONE)
  private byte[] conteudo;

  public static ArquivoRecebido receber(
      String perfilRef, DocumentoMovimentacoes documento, byte[] conteudo, Instant agora) {
    var arquivo = new ArquivoRecebido();
    arquivo.perfilRef = perfilRef;
    arquivo.hash = documento.hash();
    arquivo.nomeArquivo = documento.nomeArquivo();
    arquivo.versaoLeitor = documento.versaoLeitor();
    arquivo.registros = documento.registros().size();
    arquivo.conteudo = conteudo.clone();
    arquivo.criadoEm = agora;
    return arquivo;
  }
}
