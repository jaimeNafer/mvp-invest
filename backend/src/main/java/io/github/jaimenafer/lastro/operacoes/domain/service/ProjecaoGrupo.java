package io.github.jaimenafer.lastro.operacoes.domain.service;

import io.github.jaimenafer.lastro.operacoes.domain.model.*;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.*;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class ProjecaoGrupo {
  public static boolean projetaParcelasConsolidadas(PropostaGrupo proposta, ResumoGrupo resumo) {
    return !resumo.completo() && resumo.natureza() != null
        && proposta.linhas().stream().allMatch(l -> l.ativoId() != null && l.papel() != PapelLinha.NAO_DEFINIDO)
        && proposta.linhas().stream().filter(l -> l.papel() != PapelLinha.TAXA)
            .allMatch(l -> Set.of("binance convert", "convert")
                .contains(l.operacao().strip().toLowerCase(Locale.ROOT)));
  }

  private static List<ItemConferenciaLivro> parcelasConsolidadas(
      List<LinhaAssociacao> linhas, ResumoGrupo resumo, Instant data) {
    Map<Long, BigDecimal> quantidades = new TreeMap<>();
    resumo.pagamentos().forEach(p -> quantidades.merge(p.ativoId(), p.quantidade().negate(), BigDecimal::add));
    resumo.recebimentos().forEach(p -> quantidades.merge(p.ativoId(), p.quantidade(), BigDecimal::add));
    resumo.taxas().forEach(p -> quantidades.merge(p.ativoId(), p.quantidade().negate(), BigDecimal::add));
    List<ItemConferenciaLivro> resultado = new ArrayList<>();
    for (var parcela : quantidades.entrySet()) {
      LinhaAssociacao representante = linhas.stream()
          .filter(l -> Objects.equals(l.ativoId(), parcela.getKey()))
          .min(Comparator.comparingLong(LinhaAssociacao::registroId)).orElseThrow();
      resultado.add(new ItemConferenciaLivro(
          "registro:" + representante.registroId(), representante.contaId(),
          representante.instituicaoId(), parcela.getKey(), data, representante.registroId(),
          parcela.getValue(), null, null, false, null, null));
    }
    return List.copyOf(resultado);
  }

  public static List<ItemConferenciaLivro> projetar(
      String identificador, PropostaGrupo proposta, ResumoGrupo resumo) {
    List<LinhaAssociacao> linhas = proposta.linhas();
    LinhaAssociacao primeira =
        linhas.stream()
            .min(
                Comparator.comparing(LinhaAssociacao::data)
                    .thenComparingLong(LinhaAssociacao::registroId))
            .orElseThrow();
    long ordem = linhas.stream().mapToLong(LinhaAssociacao::registroId).min().orElseThrow();
    if (projetaParcelasConsolidadas(proposta, resumo))
      return parcelasConsolidadas(linhas, resumo, primeira.data());
    if (!resumo.completo())
      return linhas.stream()
          .filter(l -> l.ativoId() != null)
          .map(l ->
              new ItemConferenciaLivro(
                  "registro:" + l.registroId(),
                  l.contaId(),
                  l.instituicaoId(),
                  l.ativoId(),
                  l.data(),
                  l.registroId(),
                  l.quantidade(),
                  null,
                  null,
                  false,
                  null,
                  null))
          .toList();
    boolean compra = resumo.natureza() == NaturezaRegistro.COMPRA
        || resumo.natureza() == NaturezaRegistro.CONVERSAO;
    ParcelaGrupo base = (compra ? resumo.recebimentos() : resumo.pagamentos()).getFirst(),
        moeda = (compra ? resumo.pagamentos() : resumo.recebimentos()).getFirst();
    ParcelaGrupo taxa = resumo.taxas().isEmpty() ? null : resumo.taxas().getFirst();
    FatoLivro fato =
        new FatoLivro(
            Long.MIN_VALUE / 4 - ordem,
            compra ? TipoOperacao.COMPRA : TipoOperacao.VENDA,
            primeira.instituicaoId(),
            base.ativoId(),
            primeira.data(),
            ordem,
            base.quantidade(),
            null,
            moeda.ativoId(),
            moeda.quantidade(),
            moeda.codigo().equals("fiat:BRL") ? BigDecimal.ONE : proposta.cotacaoBrl(),
            taxa == null ? null : taxa.ativoId(),
            taxa == null ? BigDecimal.ZERO : taxa.quantidade());
    return List.of(
        new ItemConferenciaLivro(
            identificador,
            primeira.contaId(),
            primeira.instituicaoId(),
            base.ativoId(),
            primeira.data(),
            ordem,
            base.quantidade(),
            null,
            fato,
            false,
            null,
            null));
  }
}
