package io.github.jaimenafer.lastro.operacoes.domain.service;

import io.github.jaimenafer.lastro.operacoes.domain.model.*;
import java.util.*;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class ConferenciaRegistros {
  public static List<Operacao> selecionar(List<Operacao> historico, TipoAvisoConferencia tipo) {
    List<Operacao> vigentes = historico.stream().filter(Operacao::confirmada).toList();
    if (tipo == TipoAvisoConferencia.CUSTO_ZERO_DECLARADO) {
      return vigentes.stream().filter(ConferenciaRegistros::aberturaComCustoZero).toList();
    }
    Map<Long, Map<String, Long>> referencias = new HashMap<>();
    for (Operacao operacao : vigentes) {
      if (possuiReferencia(operacao)) {
        referencias
            .computeIfAbsent(operacao.getInstituicaoId(), id -> new HashMap<>())
            .merge(operacao.getReferencia(), 1L, Long::sum);
      }
    }
    return vigentes.stream()
        .filter(ConferenciaRegistros::possuiReferencia)
        .filter(o -> referencias.get(o.getInstituicaoId()).get(o.getReferencia()) > 1)
        .toList();
  }

  private static boolean aberturaComCustoZero(Operacao operacao) {
    return operacao.getTipo() == TipoOperacao.SALDO_INICIAL
        && operacao.getCustoTotalBrl() != null
        && operacao.getCustoTotalBrl().signum() == 0;
  }

  private static boolean possuiReferencia(Operacao operacao) {
    return operacao.getReferencia() != null && !operacao.getReferencia().isBlank();
  }
}
