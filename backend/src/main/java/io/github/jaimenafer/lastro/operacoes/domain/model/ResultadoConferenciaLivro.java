package io.github.jaimenafer.lastro.operacoes.domain.model;

import java.util.List;

public record ResultadoConferenciaLivro(
    List<PosicaoConferenciaLivro> posicoes,
    List<EfeitoConferenciaLivro> efeitos,
    List<LacunaLivro> lacunas) {
  public ResultadoConferenciaLivro {
    posicoes = List.copyOf(posicoes);
    efeitos = List.copyOf(efeitos);
    lacunas = List.copyOf(lacunas);
  }
}
