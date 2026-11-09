package io.github.jaimenafer.lastro.operacoes.application.contract;

import java.util.List;

public interface IImportacaoOperacoes {
  List<Long> versoesImportadas(List<Long> ids);

  void excluirVersoesImportadas(List<Long> ids, List<Long> compartilhadas);
  void bloquearHistorico(List<MovimentoImportado> movimentos, boolean posicaoAnteriorZerada);

  PreviaLoteOperacoes prever(List<MovimentoImportado> movimentos, boolean posicaoAnteriorZerada);

  ResultadoLoteOperacoes confirmar(
      List<MovimentoImportado> movimentos, boolean posicaoAnteriorZerada, long revisao);
}
