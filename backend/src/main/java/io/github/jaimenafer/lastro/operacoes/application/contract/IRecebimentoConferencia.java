package io.github.jaimenafer.lastro.operacoes.application.contract;

import java.util.List;

public interface IRecebimentoConferencia {
  void bloquearHistorico();

  void excluirArquivo(String arquivoRef);

  void excluirSaldoAnterior(String id, String revisao);

  void validarReferenciasDisponiveis(List<String> referencias);

  String revisao();

  String registrar(String arquivoRef, String hash, List<LinhaConferenciaRecebida> registros);
}
