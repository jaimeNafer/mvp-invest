package io.github.jaimenafer.lastro.operacoes.application.mapper;

import io.github.jaimenafer.lastro.ativos.application.contract.AtivoResumo;
import io.github.jaimenafer.lastro.operacoes.domain.model.*;
import io.github.jaimenafer.lastro.operacoes.domain.service.MotorLivro;
import java.util.*;
import java.util.stream.Collectors;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class LivroMapper {
  public static FatoLivro fato(Operacao o) {
    return new FatoLivro(
        o.getId() == null ? -1L : o.getId(),
        o.getTipo(),
        o.getInstituicaoId(),
        o.getAtivoId(),
        o.getOcorridaEm(),
        o.getOrdemLivro(),
        o.getQuantidade(),
        o.getCustoTotalBrl(),
        o.getMoedaCotacaoId(),
        o.getTotalBruto(),
        o.getCotacaoBrl(),
        o.getTaxaAtivoId(),
        o.getTaxaQuantidade());
  }

  public static Set<Long> ativosDoHistorico(List<Operacao> historico) {
    var ids = new HashSet<Long>();
    for (var o : historico) {
      ids.add(o.getAtivoId());
      if (o.getMoedaCotacaoId() != null) ids.add(o.getMoedaCotacaoId());
      if (o.getTaxaAtivoId() != null) ids.add(o.getTaxaAtivoId());
    }
    return ids;
  }

  public static ResultadoLivro calcular(List<Operacao> historico, List<AtivoResumo> ativos) {
    return MotorLivro.calcular(
        historico.stream().filter(Operacao::confirmada).map(LivroMapper::fato).toList(),
        ativos.stream().collect(Collectors.toMap(AtivoResumo::id, AtivoResumo::codigo)));
  }
}
