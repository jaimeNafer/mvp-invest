package io.github.jaimenafer.lastro.operacoes.domain.service;

import io.github.jaimenafer.lastro.operacoes.domain.model.FatoPosicaoInicial;
import io.github.jaimenafer.lastro.operacoes.domain.model.PosicaoInicial;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.TreeMap;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class CalculoPosicaoInicial {

  public static List<PosicaoInicial> calcular(List<FatoPosicaoInicial> fatos) {
    var porAtivo = new TreeMap<String, List<FatoPosicaoInicial>>();
    fatos.forEach(f -> porAtivo.computeIfAbsent(f.ticker(), k -> new ArrayList<>()).add(f));
    return porAtivo.values().stream().map(CalculoPosicaoInicial::consolidarAtivo).toList();
  }

  private static PosicaoInicial consolidarAtivo(List<FatoPosicaoInicial> grupo) {
    var quantidade =
        grupo.stream().map(FatoPosicaoInicial::quantidade).reduce(BigDecimal.ZERO, BigDecimal::add);
    var custo =
        grupo.stream().map(FatoPosicaoInicial::custo).reduce(BigDecimal.ZERO, BigDecimal::add);
    return new PosicaoInicial(
        grupo.getFirst().ativoId(),
        grupo.getFirst().ticker(),
        quantidade,
        custo,
        custo.divide(quantidade, 36, RoundingMode.HALF_EVEN),
        grupo.stream()
            .sorted(
                Comparator.comparing(FatoPosicaoInicial::instituicaoNome)
                    .thenComparing(FatoPosicaoInicial::instituicaoId))
            .toList());
  }
}
