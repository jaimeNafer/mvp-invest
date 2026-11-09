package io.github.jaimenafer.lastro.operacoes.application.dto;

import io.github.jaimenafer.lastro.ativos.application.contract.AtivoResumo;
import io.github.jaimenafer.lastro.instituicoes.application.contract.InstituicaoResumo;
import io.github.jaimenafer.lastro.operacoes.domain.model.ItemConferenciaLivro;
import io.github.jaimenafer.lastro.operacoes.domain.model.ResultadoConferenciaLivro;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;

public record RetratoConferencia(
    List<ItemConferenciaLivro> itens,
    ResultadoConferenciaLivro livro,
    ResumoConferenciaResponse resumo,
    Map<Long, AtivoResumo> ativos,
    Map<Long, InstituicaoResumo> instituicoes,
    boolean quantidadesCompletas,
    boolean incertezaSemData,
    LocalDate primeiraDataPendente,
    List<GrupoEmConferencia> gruposAplicados) {
  public RetratoConferencia(List<ItemConferenciaLivro> itens, ResultadoConferenciaLivro livro,
      ResumoConferenciaResponse resumo, Map<Long, AtivoResumo> ativos,
      Map<Long, InstituicaoResumo> instituicoes, boolean quantidadesCompletas,
      boolean incertezaSemData, LocalDate primeiraDataPendente) {
    this(itens, livro, resumo, ativos, instituicoes, quantidadesCompletas, incertezaSemData,
        primeiraDataPendente, List.of());
  }
  public RetratoConferencia {
    itens = List.copyOf(itens);
    ativos = Map.copyOf(ativos);
    instituicoes = Map.copyOf(instituicoes);
    gruposAplicados = List.copyOf(gruposAplicados);
  }
}
