package io.github.jaimenafer.lastro.operacoes.domain.service;

import io.github.jaimenafer.lastro.operacoes.domain.exception.OperacaoInvalidaException;
import io.github.jaimenafer.lastro.operacoes.domain.model.*;
import java.math.BigDecimal;
import java.time.Duration;
import java.time.Instant;
import java.util.*;
import java.util.stream.Collectors;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class AgrupamentoNegociacoes {
  public static ResumoGrupo resumir(
      List<LinhaAssociacao> linhas, boolean taxaZero, BigDecimal cotacaoBrl) {
    return resumir(linhas, taxaZero, cotacaoBrl, Duration.ofSeconds(2));
  }

  public static ResumoGrupo resumir(
      List<LinhaAssociacao> linhas,
      boolean taxaZero,
      BigDecimal cotacaoBrl,
      Duration janela) {
    validarLinhas(linhas, janela);
    List<ParcelaGrupo> entradas = parcelas(linhas, PapelLinha.RECEBIMENTO),
        saidas = parcelas(linhas, PapelLinha.PAGAMENTO),
        taxas = parcelas(linhas, PapelLinha.TAXA);
    List<String> pendencias = new ArrayList<>();
    if (linhas.stream().anyMatch(l -> l.ativoId() == null))
      pendencias.add(
          "Há ativo ainda não identificado no catálogo. Sua quantidade e custo permanecem"
              + " pendentes.");
    boolean conversao = linhas.stream().filter(l -> l.papel() != PapelLinha.TAXA)
        .allMatch(l -> familia(l.operacao()).equals("conversao"));
    NaturezaRegistro natureza = null;
    if (entradas.size() == 1 && saidas.size() == 1
        && !Objects.equals(entradas.getFirst().ativoId(), saidas.getFirst().ativoId())) {
      if (cotacao(saidas.getFirst().codigo())
          && entradas.getFirst().codigo().startsWith("cripto:")
          && (saidas.getFirst().codigo().equals("fiat:BRL")
              || !cotacao(entradas.getFirst().codigo()))) natureza = NaturezaRegistro.COMPRA;
      else if (cotacao(entradas.getFirst().codigo())
          && saidas.getFirst().codigo().startsWith("cripto:")
          && (entradas.getFirst().codigo().equals("fiat:BRL")
              || !cotacao(saidas.getFirst().codigo()))) natureza = NaturezaRegistro.VENDA;
      else if (conversao && entradas.getFirst().codigo().startsWith("cripto:")
          && saidas.getFirst().codigo().startsWith("cripto:")) natureza = NaturezaRegistro.CONVERSAO;
    }
    if (!familiasCompativeis(linhas) || !naturezaCompativel(natureza, linhas)) {
      natureza = null;
      pendencias.add(
          "Os tipos originais indicam naturezas conflitantes. Revise a composição do grupo.");
    }
    if (natureza == null)
      pendencias.add("Os papéis e o par ainda não determinam uma operação suportada.");
    if (linhas.stream().anyMatch(l -> l.papel() == PapelLinha.NAO_DEFINIDO))
      pendencias.add("Há linhas cujo papel ainda não foi definido.");
    Set<Long> par = new HashSet<>();
    entradas.forEach(p -> par.add(p.ativoId()));
    saidas.forEach(p -> par.add(p.ativoId()));
    if (taxas.size() > 1 || taxas.stream().anyMatch(p -> !par.contains(p.ativoId())))
      pendencias.add("Taxa em terceiro ativo ou em várias moedas: cálculo financeiro pendente.");
    ParcelaGrupo moeda =
        natureza == NaturezaRegistro.COMPRA
            ? saidas.getFirst()
            : natureza == NaturezaRegistro.VENDA ? entradas.getFirst()
            : natureza == NaturezaRegistro.CONVERSAO ? saidas.getFirst() : null;
    BigDecimal principal = null;
    if (moeda != null) {
      if (moeda.codigo().equals("fiat:BRL")) principal = moeda.quantidade();
      else if (cotacaoBrl != null && cotacaoBrl.signum() > 0)
        principal =
            moeda.quantidade().multiply(cotacaoBrl).setScale(8, java.math.RoundingMode.HALF_EVEN);
      else if (!conversao) pendencias.add("Informe a cotação histórica em BRL da moeda de cotação.");
    }
    return new ResumoGrupo(
        natureza,
        entradas,
        saidas,
        taxas,
        principal,
        pendencias.isEmpty() && (moeda == null || principal != null),
        List.copyOf(pendencias));
  }

  public static BigDecimal custoFinal(ResumoGrupo resumo, BigDecimal cotacaoBrl) {
    if (!resumo.completo() || (resumo.natureza() != NaturezaRegistro.COMPRA
        && resumo.natureza() != NaturezaRegistro.CONVERSAO)) return null;
    BigDecimal custo = resumo.principalBrl();
    ParcelaGrupo moeda = resumo.pagamentos().getFirst();
    if (!resumo.taxas().isEmpty() && resumo.taxas().getFirst().ativoId().equals(moeda.ativoId())) {
      BigDecimal cotacao = moeda.codigo().equals("fiat:BRL") ? BigDecimal.ONE : cotacaoBrl;
      custo =
          custo.add(
              resumo
                  .taxas()
                  .getFirst()
                  .quantidade()
                  .multiply(cotacao)
                  .setScale(8, java.math.RoundingMode.HALF_EVEN));
    }
    return custo.setScale(8, java.math.RoundingMode.HALF_EVEN);
  }

  public static void validarLinhas(List<LinhaAssociacao> linhas) {
    validarLinhas(linhas, Duration.ofSeconds(2));
  }

  public static void validarLinhas(List<LinhaAssociacao> linhas, Duration janela) {
    if (linhas.size() < 2)
      throw new OperacaoInvalidaException("Selecione pelo menos duas linhas para associar.");
    if (linhas.stream().map(LinhaAssociacao::registroId).distinct().count() != linhas.size())
      throw new OperacaoInvalidaException("Não repita linhas na associação.");
    LinhaAssociacao primeira = linhas.getFirst();
    if (linhas.stream()
        .anyMatch(l ->
            l.data() == null
                || l.quantidade() == null
                || l.quantidade().signum() == 0
                || l.contaId() == null))
      throw new OperacaoInvalidaException(
          "Resolva data, conta, ativo e quantidade antes de associar.");
    if (linhas.stream()
        .anyMatch(l ->
            !Objects.equals(primeira.contaId(), l.contaId())
                || !Objects.equals(primeira.instituicaoId(), l.instituicaoId())))
      throw new OperacaoInvalidaException(
          "As linhas devem pertencer à mesma instituição e conta de custódia.");
    var inicio =
        linhas.stream().map(LinhaAssociacao::data).min(Comparator.naturalOrder()).orElseThrow();
    var fim =
        linhas.stream().map(LinhaAssociacao::data).max(Comparator.naturalOrder()).orElseThrow();
    if (Duration.between(inicio, fim).compareTo(janela) > 0)
      throw new OperacaoInvalidaException(
          "A diferença entre a primeira e a última linha excede a janela permitida.");
    if (linhas.stream()
            .map(LinhaAssociacao::referencia)
            .filter(AgrupamentoNegociacoes::referenciaForte)
            .distinct()
            .count()
        > 1)
      throw new OperacaoInvalidaException("As referências externas das linhas são conflitantes.");
    if (linhas.stream()
        .anyMatch(l ->
            l.papel() == null
                || (l.papel() == PapelLinha.RECEBIMENTO && l.quantidade().signum() < 0)
                || ((l.papel() == PapelLinha.PAGAMENTO || l.papel() == PapelLinha.TAXA)
                    && l.quantidade().signum() > 0)))
      throw new OperacaoInvalidaException(
          "O papel deve respeitar a direção original da movimentação.");
  }

  public static List<List<LinhaAssociacao>> automaticos(List<LinhaAssociacao> linhas) {
    List<List<LinhaAssociacao>> grupos = new ArrayList<>();
    automaticos(linhas, Duration.ZERO).forEach(grupos::add);
    return List.copyOf(grupos);
  }

  public static List<List<LinhaAssociacao>> automaticos(
      List<LinhaAssociacao> linhas, Duration janela) {
    Map<String, List<LinhaAssociacao>> contas = new TreeMap<>();
    linhas.stream()
        .filter(AgrupamentoNegociacoes::candidataAutomatica)
        .forEach(l -> contas.computeIfAbsent(chaveCandidatura(l), k -> new ArrayList<>()).add(l));
    List<List<LinhaAssociacao>> grupos = new ArrayList<>();
    for (List<LinhaAssociacao> conta : contas.values()) {
      conta.sort(
          Comparator.comparing(LinhaAssociacao::data)
              .thenComparingLong(LinhaAssociacao::registroId));
      for (int inicio = 0; inicio < conta.size(); ) {
        Instant primeiroInstante = conta.get(inicio).data();
        int fim = inicio + 1;
        while (fim < conta.size()
            && Duration.between(primeiroInstante, conta.get(fim).data()).compareTo(janela) <= 0) {
          fim++;
        }
        List<LinhaAssociacao> candidatas = List.copyOf(conta.subList(inicio, fim));
        List<LinhaAssociacao> grupo = grupoInequivoco(candidatas, janela);
        if (!grupo.isEmpty() && !haTaxaProximaSemVinculo(grupo, linhas, janela)
            && !haContrapartidaConcorrente(grupo, conta, janela)) grupos.add(grupo);
        inicio = fim;
      }
    }
    return List.copyOf(grupos);
  }

  private static boolean haContrapartidaConcorrente(
      List<LinhaAssociacao> grupo, List<LinhaAssociacao> candidatas, Duration janela) {
    if (grupo.stream().filter(l -> l.papel() != PapelLinha.TAXA)
        .noneMatch(l -> familia(l.operacao()).equals("conversao"))) return false;
    return candidatas.stream()
        .filter(l -> !grupo.contains(l) && l.papel() != PapelLinha.TAXA
            && familia(l.operacao()).equals("conversao"))
        .anyMatch(candidata -> grupo.stream()
            .filter(l -> l.papel() != PapelLinha.TAXA)
            .anyMatch(l -> l.papel() != candidata.papel()
                && !Objects.equals(l.ativoId(), candidata.ativoId())
                && Duration.between(l.data(), candidata.data()).abs().compareTo(janela) <= 0));
  }

  private static boolean haTaxaProximaSemVinculo(
      List<LinhaAssociacao> grupo, List<LinhaAssociacao> linhas, Duration janela) {
    LinhaAssociacao primeira = grupo.getFirst();
    Instant inicio = grupo.stream().map(LinhaAssociacao::data).min(Comparator.naturalOrder()).orElseThrow();
    Instant fim = grupo.stream().map(LinhaAssociacao::data).max(Comparator.naturalOrder()).orElseThrow();
    return linhas.stream().anyMatch(l ->
        (l.papel() == PapelLinha.TAXA
                || (l.papel() == PapelLinha.NAO_DEFINIDO
                    && l.operacao().strip().toLowerCase(Locale.ROOT).contains("fee")))
            && l.data() != null
            && Objects.equals(l.instituicaoId(), primeira.instituicaoId())
            && Objects.equals(l.contaId(), primeira.contaId())
            && !grupo.contains(l)
            && (l.origemFisica() == null || primeira.origemFisica() == null
                || l.origemFisica().equals(primeira.origemFisica()))
            && (!referenciaForte(l.referencia())
                || Objects.equals(l.referencia(), primeira.referencia()))
            && Duration.between(
                    l.data().isBefore(inicio) ? l.data() : inicio,
                    l.data().isAfter(fim) ? l.data() : fim)
                .compareTo(janela) <= 0);
  }

  private static boolean candidataAutomatica(LinhaAssociacao linha) {
    return linha.data() != null
        && linha.contaId() != null
        && linha.instituicaoId() != null
        && linha.quantidade() != null
        && linha.quantidade().signum() != 0
        && tipoNegociacao(linha.operacao())
        && linha.papel() != PapelLinha.NAO_DEFINIDO;
  }

  private static String chaveCandidatura(LinhaAssociacao linha) {
    String referencia = referenciaForte(linha.referencia()) ? "ref:" + linha.referencia().strip()
        : linha.origemFisica() != null
            && (familia(linha.operacao()).equals("conversao") || linha.papel() == PapelLinha.TAXA)
            ? "origem:" + linha.origemFisica() : "";
    return linha.instituicaoId() + "|" + linha.contaId() + "|" + referencia;
  }

  private static List<LinhaAssociacao> grupoInequivoco(
      List<LinhaAssociacao> linhas, Duration janela) {
    List<LinhaAssociacao> negociacoes =
        linhas.stream().filter(l -> l.papel() != PapelLinha.TAXA).toList();
    if (negociacoes.stream().anyMatch(l -> familia(l.operacao()).isEmpty())) return List.of();
    Map<String, List<LinhaAssociacao>> porFamilia =
        negociacoes.stream().collect(Collectors.groupingBy(l -> familia(l.operacao())));
    if (porFamilia.size() != 1) return List.of();
    List<LinhaAssociacao> base = porFamilia.values().iterator().next();
    List<LinhaAssociacao> taxas =
        linhas.stream().filter(l -> l.papel() == PapelLinha.TAXA).toList();
    if (!grupoBaseInequivoco(base, janela)
        || taxas.stream().anyMatch(l -> l.quantidade().signum() > 0)) return List.of();
    var combinado = new ArrayList<LinhaAssociacao>(base);
    combinado.addAll(taxas);
    try {
      resumir(combinado, false, null, janela);
      return List.copyOf(combinado);
    } catch (OperacaoInvalidaException erro) {
      return List.of();
    }
  }

  private static boolean grupoBaseInequivoco(List<LinhaAssociacao> linhas, Duration janela) {
    if (linhas.size() < 2 || !familiasCompativeis(linhas)) return false;
    long recebimentos = linhas.stream().filter(l -> l.papel() == PapelLinha.RECEBIMENTO).count();
    long pagamentos = linhas.stream().filter(l -> l.papel() == PapelLinha.PAGAMENTO).count();
    if (recebimentos == 0 || pagamentos == 0) return false;
    boolean conversao = linhas.stream().allMatch(l -> familia(l.operacao()).equals("conversao"));
    boolean referenciaComum = linhas.stream().allMatch(l -> referenciaForte(l.referencia()))
        && linhas.stream().map(LinhaAssociacao::referencia).distinct().count() == 1;
    if (conversao && linhas.size() > 2 && !referenciaComum) return false;
    if (recebimentos > 1 && pagamentos > 1 && !(conversao && referenciaComum)) return false;
    ResumoGrupo resumo = resumir(linhas, false, null, janela);
    return resumo.natureza() != null
        && linhas.stream().noneMatch(l -> l.papel() == PapelLinha.NAO_DEFINIDO);
  }

  public static boolean referenciaForte(String referencia) {
    if (referencia == null || referencia.isBlank()) return false;
    String texto = referencia.strip();
    return !texto.matches("[0-9]+")
        && (!texto.toLowerCase(Locale.ROOT).startsWith("via cashbalance")
            || texto.matches("(?i).*wallet/[^\\s]+.*"))
        && !texto.equalsIgnoreCase("Via CashBalance")
        && !texto.matches("(?i).*Wallet/\\s*");
  }

  public static boolean tipoNegociacao(String valor) {
    return Set.of(
            "binance convert",
            "convert",
            "buy crypto with fiat",
            "transaction buy",
            "transaction spend",
            "transaction sold",
            "transaction revenue",
            "transaction fee",
            "buy",
            "sell",
            "fee",
            "trading fee")
        .contains(valor.strip().toLowerCase(Locale.ROOT));
  }

  private static boolean naturezaCompativel(
      NaturezaRegistro natureza, List<LinhaAssociacao> linhas) {
    return linhas.stream()
        .filter(l -> l.papel() != PapelLinha.TAXA)
        .allMatch(l -> {
          String familia = familia(l.operacao());
          return natureza == null
              || familia.isEmpty()
              || familia.equals("conversao")
              || (natureza == NaturezaRegistro.COMPRA && !familia.equals("venda"))
              || (natureza == NaturezaRegistro.VENDA && familia.equals("venda"));
        });
  }

  private static boolean familiasCompativeis(List<LinhaAssociacao> linhas) {
    return linhas.stream()
            .filter(l -> l.papel() != PapelLinha.TAXA)
            .map(l -> familia(l.operacao()))
            .filter(f -> !f.isEmpty())
            .distinct()
            .count()
        <= 1;
  }

  private static String familia(String operacao) {
    return switch (operacao.strip().toLowerCase(Locale.ROOT)) {
      case "binance convert", "convert" -> "conversao";
      case "buy crypto with fiat" -> "fiat";
      case "transaction buy", "transaction spend", "buy" -> "compra";
      case "transaction sold", "transaction revenue", "sell" -> "venda";
      default -> "";
    };
  }

  private static boolean cotacao(String codigo) {
    return Set.of("fiat:BRL", "cripto:USDT", "cripto:USDC").contains(codigo);
  }

  private static List<ParcelaGrupo> parcelas(List<LinhaAssociacao> linhas, PapelLinha papel) {
    Map<String, ParcelaGrupo> totais = new TreeMap<>();
    for (var l : linhas)
      if (l.papel() == papel)
        totais.compute(
            l.ativoId() == null ? "desconhecido:" + l.ticker() : l.ativoId().toString(),
            (id, a) ->
                new ParcelaGrupo(
                    l.ativoId(),
                    l.codigo(),
                    l.ticker(),
                    (a == null ? BigDecimal.ZERO : a.quantidade()).add(l.quantidade().abs())));
    return List.copyOf(totais.values());
  }
}
