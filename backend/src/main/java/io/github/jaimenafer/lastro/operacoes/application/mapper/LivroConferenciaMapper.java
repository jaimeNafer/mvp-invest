package io.github.jaimenafer.lastro.operacoes.application.mapper;

import io.github.jaimenafer.lastro.operacoes.application.dto.PosicaoLivroResponse;
import io.github.jaimenafer.lastro.operacoes.application.dto.RetratoConferencia;
import io.github.jaimenafer.lastro.operacoes.domain.model.CustodiaLivro;
import io.github.jaimenafer.lastro.operacoes.domain.model.ItemConferenciaLivro;
import io.github.jaimenafer.lastro.operacoes.domain.model.PosicaoLivro;
import io.github.jaimenafer.lastro.operacoes.domain.model.ResultadoLivro;
import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.function.Function;
import java.util.stream.Collectors;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class LivroConferenciaMapper {
  public static ResultadoLivro livro(RetratoConferencia retrato) {
    List<PosicaoLivro> posicoes =
        retrato.livro().posicoes().stream()
            .map(p -> {
              Map<Long, BigDecimal> custodias = new TreeMap<>();
              p.contas()
                  .forEach(
                      c -> custodias.merge(c.instituicaoId(), c.quantidade(), BigDecimal::add));
              return new PosicaoLivro(
                  p.ativoId(),
                  p.quantidade(),
                  p.custoTotalBrl(),
                  p.custoMedioBrl(),
                  custodias.entrySet().stream()
                      .map(c -> new CustodiaLivro(c.getKey(), c.getValue()))
                      .toList());
            })
            .toList();
    return new ResultadoLivro(
        posicoes,
        retrato.livro().efeitos().stream()
            .filter(e -> e.calculo() != null)
            .map(e -> e.calculo())
            .toList());
  }

  public static List<PosicaoLivroResponse> posicoes(RetratoConferencia retrato) {
    var porAtivo =
        retrato.livro().posicoes().stream()
            .collect(Collectors.toMap(p -> p.ativoId(), Function.identity()));
    Set<String> ajustes = identidadesAjustes(retrato);
    return OperacoesMapper.posicoes(livro(retrato), retrato.ativos(), retrato.instituicoes())
        .stream()
        .map(p -> {
          var origem = porAtivo.get(Long.valueOf(p.ativoId()));
          return new PosicaoLivroResponse(
              p.ativoId(),
              p.ativoTicker(),
              p.quantidade(),
              p.custoTotalBrl(),
              p.custoMedioBrl(),
              p.instituicoes(),
              origem.quantidadeDisponivel(),
              origem.causas().stream().anyMatch(ajustes::contains),
              origem.causas());
        })
        .toList();
  }

  public static Set<String> identidadesAjustes(RetratoConferencia retrato) {
    return retrato.itens().stream()
        .filter(ItemConferenciaLivro::ajuste)
        .map(i -> i.contaId() + ":" + i.ativoId() + ":" + i.ancoraId())
        .collect(Collectors.toSet());
  }
}
