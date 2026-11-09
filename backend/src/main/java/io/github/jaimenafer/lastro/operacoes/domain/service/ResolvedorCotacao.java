package io.github.jaimenafer.lastro.operacoes.domain.service;

import io.github.jaimenafer.lastro.operacoes.domain.model.CotacaoHistorica;
import io.github.jaimenafer.lastro.operacoes.domain.model.ResultadoCotacao;
import java.math.BigDecimal;
import java.math.MathContext;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class ResolvedorCotacao {
  private static final Duration TOLERANCIA = Duration.ofHours(24);
  private static final Duration DIFERENCA_ENTRE_COTACOES = Duration.ofHours(1);
  private static final int MAXIMO_ARESTAS = 3;

  public static Optional<ResultadoCotacao> resolver(
      Long base, Long destino, Instant instante, List<CotacaoHistorica> cotacoes) {
    if (base.equals(destino)) return Optional.of(new ResultadoCotacao(BigDecimal.ONE, false, List.of()));
    List<CotacaoHistorica> disponiveis = cotacoes.stream()
        .filter(c -> c.preco().signum() > 0 && !c.instante().isAfter(instante)
            && Duration.between(c.instante(), instante).compareTo(TOLERANCIA) <= 0)
        .sorted(Comparator.comparing(CotacaoHistorica::instante).reversed()
            .thenComparing(CotacaoHistorica::baseId).thenComparing(CotacaoHistorica::moedaId))
        .toList();
    int inicio = 1;
    int fim = disponiveis.size();
    Optional<ResultadoCotacao> melhor = Optional.empty();
    while (inicio <= fim) {
      int limite = inicio + (fim - inicio) / 2;
      Instant corte = disponiveis.get(limite - 1).instante();
      List<CotacaoHistorica> recentes = disponiveis.stream()
          .takeWhile(c -> !c.instante().isBefore(corte)).toList();
      Optional<ResultadoCotacao> resultado = caminhoMaisCurto(base, destino, instante, recentes);
      if (resultado.isPresent()) {
        melhor = resultado;
        fim = limite - 1;
      } else inicio = limite + 1;
    }
    return melhor;
  }

  private static Optional<ResultadoCotacao> caminhoMaisCurto(Long base, Long destino,
      Instant instante, List<CotacaoHistorica> disponiveis) {
    for (int tamanho = 1; tamanho <= MAXIMO_ARESTAS; tamanho++) {
      Optional<ResultadoCotacao> resultado = buscar(
          base, destino, instante, disponiveis, new HashSet<>(Set.of(base)),
          new ArrayList<>(), BigDecimal.ONE, tamanho);
      if (resultado.isPresent()) return resultado;
    }
    return Optional.empty();
  }

  private static Optional<ResultadoCotacao> buscar(
      Long atual, Long destino, Instant instante, List<CotacaoHistorica> cotacoes,
      Set<Long> visitados, List<CotacaoHistorica> caminho, BigDecimal preco, int restantes) {
    if (restantes == 0) {
      if (!atual.equals(destino)) return Optional.empty();
      boolean aproximado = caminho.size() > 1 || caminho.stream().anyMatch(c -> !c.instante().equals(instante));
      return Optional.of(new ResultadoCotacao(preco, aproximado, List.copyOf(caminho)));
    }
    for (CotacaoHistorica cotacao : cotacoes) {
      boolean direta = cotacao.baseId().equals(atual);
      if (!direta && !cotacao.moedaId().equals(atual)) continue;
      Long proximo = direta ? cotacao.moedaId() : cotacao.baseId();
      if (visitados.contains(proximo) || !compativel(caminho, cotacao)) continue;
      visitados.add(proximo);
      caminho.add(cotacao);
      BigDecimal fator = direta ? cotacao.preco() : BigDecimal.ONE.divide(cotacao.preco(), MathContext.DECIMAL128);
      Optional<ResultadoCotacao> resultado = buscar(proximo, destino, instante, cotacoes,
          visitados, caminho, preco.multiply(fator, MathContext.DECIMAL128), restantes - 1);
      caminho.removeLast();
      visitados.remove(proximo);
      if (resultado.isPresent()) return resultado;
    }
    return Optional.empty();
  }

  private static boolean compativel(List<CotacaoHistorica> caminho, CotacaoHistorica candidata) {
    return caminho.stream().allMatch(c -> Duration.between(c.instante(), candidata.instante()).abs()
        .compareTo(DIFERENCA_ENTRE_COTACOES) <= 0);
  }
}
