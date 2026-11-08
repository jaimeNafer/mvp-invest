package io.github.jaimenafer.lastro.operacoes.application.dto;

import java.util.List;

public record ResumoConferenciaResponse(
    long total,
    long coerentes,
    long pendentes,
    long inconsistentes,
    long ajustesAtivos,
    String percentualCoerencia,
    long registrosAfetados,
    long vendasAfetadas,
    List<String> ativosAfetados,
    String mensagem,
    long operacoesCoerentes,
    long operacoesPendentes,
    long creditosAutomaticos,
    long naoClassificados,
    long operacoesAgrupadas) {
  public ResumoConferenciaResponse {
    ativosAfetados = List.copyOf(ativosAfetados);
  }
}
