package io.github.jaimenafer.lastro.operacoes.domain.service;

import io.github.jaimenafer.lastro.operacoes.domain.exception.OperacaoConflitanteException;
import io.github.jaimenafer.lastro.operacoes.domain.exception.OperacaoInvalidaException;
import io.github.jaimenafer.lastro.operacoes.domain.model.CalculoOperacao;
import io.github.jaimenafer.lastro.operacoes.domain.model.CustodiaConferenciaLivro;
import io.github.jaimenafer.lastro.operacoes.domain.model.CustodiaLivro;
import io.github.jaimenafer.lastro.operacoes.domain.model.EfeitoConferenciaLivro;
import io.github.jaimenafer.lastro.operacoes.domain.model.EstadoConferencia;
import io.github.jaimenafer.lastro.operacoes.domain.model.FatoLivro;
import io.github.jaimenafer.lastro.operacoes.domain.model.ItemConferenciaLivro;
import io.github.jaimenafer.lastro.operacoes.domain.model.LacunaLivro;
import io.github.jaimenafer.lastro.operacoes.domain.model.PosicaoConferenciaLivro;
import io.github.jaimenafer.lastro.operacoes.domain.model.PosicaoLivro;
import io.github.jaimenafer.lastro.operacoes.domain.model.ResultadoConferenciaLivro;
import io.github.jaimenafer.lastro.operacoes.domain.model.ResultadoLivro;
import io.github.jaimenafer.lastro.operacoes.domain.model.TipoOperacao;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class MotorLivro {
  private static final BigDecimal ZERO = BigDecimal.ZERO.setScale(8);

  public static ResultadoConferenciaLivro conferir(
      List<ItemConferenciaLivro> itens, Map<Long, String> codigosAtivos) {
    return conferir(itens, codigosAtivos, new ArrayDeque<>(), new TreeMap<>());
  }

  public static Map<Instant, List<PosicaoConferenciaLivro>> posicoesAte(
      List<ItemConferenciaLivro> itens, Map<Long, String> codigosAtivos, List<Instant> cortes) {
    Map<Instant, List<PosicaoConferenciaLivro>> retratos = new TreeMap<>();
    var ordenados = cortes.stream().distinct().sorted().toList();
    if (ordenados.isEmpty()) return Map.of();
    var transferencias = agruparTransferencias(itens);
    boolean atravessaCorte = transferencias.values().stream().anyMatch(pernas ->
        ordenados.stream().anyMatch(corte -> pernas.stream().anyMatch(i -> !i.ocorridaEm().isAfter(corte))
            && pernas.stream().anyMatch(i -> i.ocorridaEm().isAfter(corte))));
    if (atravessaCorte) {
      for (Instant corte : ordenados) {
        List<ItemConferenciaLivro> prefixo = new ArrayList<>(itens.stream()
            .filter(i -> !i.ajuste() && !i.ocorridaEm().isAfter(corte)).toList());
        Set<String> ancoras = new LinkedHashSet<>();
        prefixo.forEach(i -> ancoras.add(i.id()));
        itens.stream().filter(i -> i.ajuste() && ancoras.contains(i.ancoraId())).forEach(prefixo::add);
        retratos.put(corte, conferir(prefixo, codigosAtivos).posicoes());
      }
    } else {
      conferir(itens, codigosAtivos, new ArrayDeque<>(ordenados), retratos);
    }
    return Map.copyOf(retratos);
  }

  private static ResultadoConferenciaLivro conferir(List<ItemConferenciaLivro> itens,
      Map<Long, String> codigosAtivos, ArrayDeque<Instant> cortes,
      Map<Instant, List<PosicaoConferenciaLivro>> retratos) {
    Map<Long, PosicaoLivro> posicoes = new TreeMap<>();
    Map<Long, Long> instituicoes = new TreeMap<>();
    Map<Long, Set<String>> causas = new TreeMap<>();
    Map<Long, Map<Long, BigDecimal>> saldosPropostas = new TreeMap<>();
    List<EfeitoConferenciaLivro> efeitos = new ArrayList<>();
    List<LacunaLivro> lacunas = new ArrayList<>();
    Map<String, List<ItemConferenciaLivro>> transferencias = agruparTransferencias(itens);
    Set<String> custosCreditosAutomaticos = itens.stream()
        .filter(ItemConferenciaLivro::creditoAutomatico)
        .map(i -> "custo:" + i.id())
        .collect(java.util.stream.Collectors.toSet());
    Map<String, Instant> datasAncoras = new HashMap<>();
    if (!cortes.isEmpty())
      itens.stream().filter(i -> !i.ajuste()).forEach(i -> datasAncoras.put(i.id(), i.ocorridaEm()));
    for (ItemConferenciaLivro item : ordenarConferencia(itens)) {
      if (!cortes.isEmpty()) {
        Instant data = item.ajuste() ? datasAncoras.get(item.ancoraId()) : item.ocorridaEm();
        while (!cortes.isEmpty() && (data == null || cortes.getFirst().isBefore(data)))
          retratos.put(cortes.removeFirst(), posicoesConferencia(posicoes, instituicoes, causas));
      }
      instituicoes.put(item.contaId(), item.instituicaoId());
      if (item.ajuste() && !ajusteValido(item, itens)) {
        efeitos.add(
            new EfeitoConferenciaLivro(
                item.id(),
                EstadoConferencia.CORRECAO_NECESSARIA,
                List.of("ancora:" + item.id()),
                null));
        continue;
      }
      if (item.transferenciaRef() != null
          && !transferenciaValida(transferencias.get(item.transferenciaRef()))) {
        efeitos.add(
            new EfeitoConferenciaLivro(
                item.id(),
                EstadoConferencia.CORRECAO_NECESSARIA,
                List.of("transferencia:" + item.transferenciaRef()),
                null));
        continue;
      }
      Map<Long, PosicaoLivro> anteriores = new TreeMap<>(posicoes);
      Set<String> dependencias = dependenciasDaOperacao(item, causas);
      CalculoOperacao calculo = processarConferencia(item, posicoes, codigosAtivos);
      List<Long> alterados = ativosAlterados(anteriores, posicoes);
      boolean deficit =
          registrarDeficits(item, anteriores, posicoes, saldosPropostas, lacunas, dependencias);
      if (item.ajuste())
        dependencias.add(identidadeLacuna(item.contaId(), item.ativoId(), item.ancoraId()));
      if (entradaComCustoDesconhecido(item, codigosAtivos)) {
        dependencias.add("custo:" + item.id());
      }
      for (Long ativoId : alterados) {
        if (!posicoes.containsKey(ativoId)) causas.remove(ativoId);
        else {
          Set<String> causasPosicao =
              causas.computeIfAbsent(ativoId, ignorado -> new LinkedHashSet<>());
          dependencias.stream()
              .filter(c -> !c.startsWith("custo:") || posicoes.get(ativoId).custoTotalBrl() == null)
              .forEach(causasPosicao::add);
        }
      }
      Set<String> pendencias = new LinkedHashSet<>(dependencias);
      pendencias.removeAll(custosCreditosAutomaticos);
      EstadoConferencia estado =
          deficit
              ? EstadoConferencia.CORRECAO_NECESSARIA
              : pendencias.isEmpty() ? EstadoConferencia.COERENTE : EstadoConferencia.PENDENTE;
      efeitos.add(
          new EfeitoConferenciaLivro(item.id(), estado, List.copyOf(pendencias), calculo));
    }
    List<PosicaoConferenciaLivro> resultado = posicoesConferencia(posicoes, instituicoes, causas);
    while (!cortes.isEmpty()) retratos.put(cortes.removeFirst(), resultado);
    return new ResultadoConferenciaLivro(resultado, efeitos, lacunas);
  }

  private static List<PosicaoConferenciaLivro> posicoesConferencia(Map<Long, PosicaoLivro> posicoes,
      Map<Long, Long> instituicoes, Map<Long, Set<String>> causas) {
    return posicoes.values().stream().map(p -> posicaoConferencia(p, instituicoes, causas)).toList();
  }

  private static boolean entradaComCustoDesconhecido(
      ItemConferenciaLivro item, Map<Long, String> codigos) {
    boolean entradaObservada = item.operacao() == null && item.custoEntradaBrl() == null;
    boolean aberturaSemCusto =
        item.operacao() != null
            && item.operacao().tipo() == TipoOperacao.SALDO_INICIAL
            && item.operacao().custoTotalBrl() == null;
    return item.transferenciaRef() == null
        && item.quantidade().signum() > 0
        && !"fiat:BRL".equals(codigos.get(item.ativoId()))
        && (entradaObservada || aberturaSemCusto);
  }

  private static List<ItemConferenciaLivro> ordenarConferencia(List<ItemConferenciaLivro> itens) {
    Map<String, List<ItemConferenciaLivro>> ajustes = new HashMap<>();
    for (ItemConferenciaLivro item : itens) {
      if (item.ajuste())
        ajustes.computeIfAbsent(item.ancoraId(), ignorado -> new ArrayList<>()).add(item);
    }
    List<ItemConferenciaLivro> ordenados = new ArrayList<>();
    itens.stream()
        .filter(i -> !i.ajuste())
        .sorted(
            Comparator.comparing(ItemConferenciaLivro::ocorridaEm)
                .thenComparingLong(ItemConferenciaLivro::ordem)
                .thenComparing(ItemConferenciaLivro::id))
        .forEach(i -> {
          ordenados.addAll(
              ajustes.getOrDefault(i.id(), List.of()).stream()
                  .sorted(Comparator.comparing(ItemConferenciaLivro::id))
                  .toList());
          ordenados.add(i);
        });
    Set<String> ancoras =
        itens.stream()
            .filter(i -> !i.ajuste())
            .map(ItemConferenciaLivro::id)
            .collect(java.util.stream.Collectors.toSet());
    itens.stream()
        .filter(i -> i.ajuste() && !ancoras.contains(i.ancoraId()))
        .sorted(Comparator.comparing(ItemConferenciaLivro::id))
        .forEach(ordenados::add);
    return ordenados;
  }

  private static boolean ajusteValido(
      ItemConferenciaLivro ajuste, List<ItemConferenciaLivro> itens) {
    return ajuste.quantidade().signum() > 0
        && ajuste.operacao() == null
        && ajuste.transferenciaRef() == null
        && itens.stream()
            .anyMatch(i ->
                !i.ajuste()
                    && i.id().equals(ajuste.ancoraId())
                    && i.contaId().equals(ajuste.contaId()));
  }

  private static Map<String, List<ItemConferenciaLivro>> agruparTransferencias(
      List<ItemConferenciaLivro> itens) {
    Map<String, List<ItemConferenciaLivro>> grupos = new HashMap<>();
    for (ItemConferenciaLivro item : itens) {
      if (item.transferenciaRef() != null)
        grupos.computeIfAbsent(item.transferenciaRef(), ignorado -> new ArrayList<>()).add(item);
    }
    return grupos;
  }

  private static boolean transferenciaValida(List<ItemConferenciaLivro> pernas) {
    if (pernas.size() != 2) return false;
    ItemConferenciaLivro primeira = pernas.get(0);
    ItemConferenciaLivro segunda = pernas.get(1);
    return primeira.operacao() == null
        && segunda.operacao() == null
        && !primeira.ajuste()
        && !segunda.ajuste()
        && primeira.ativoId().equals(segunda.ativoId())
        && primeira.instituicaoId().equals(segunda.instituicaoId())
        && !primeira.contaId().equals(segunda.contaId())
        && primeira.quantidade().signum() != 0
        && primeira.quantidade().add(segunda.quantidade()).signum() == 0;
  }

  private static Set<String> dependenciasDaOperacao(
      ItemConferenciaLivro item, Map<Long, Set<String>> causas) {
    Long ativoConsumido = ativoConsumido(item);
    Set<String> resultado = ativoConsumido == null
        ? new LinkedHashSet<>() : new LinkedHashSet<>(causas.getOrDefault(ativoConsumido, Set.of()));
    if (item.operacao() != null && item.operacao().taxaExternaBrl() != null)
      resultado.addAll(causas.getOrDefault(item.operacao().taxaAtivoId(), Set.of()));
    return resultado;
  }

  private static Long ativoConsumido(ItemConferenciaLivro item) {
    if (item.operacao() != null) {
      return switch (item.operacao().tipo()) {
        case COMPRA -> item.operacao().moedaCotacaoId();
        case VENDA, RETIRADA -> item.operacao().ativoId();
        case SALDO_INICIAL, APORTE -> null;
      };
    }
    return item.quantidade().signum() < 0 || item.transferenciaRef() != null
        ? item.ativoId()
        : null;
  }

  private static CalculoOperacao processarConferencia(
      ItemConferenciaLivro item,
      Map<Long, PosicaoLivro> posicoes,
      Map<Long, String> codigosAtivos) {
    if (item.transferenciaRef() != null) {
      PosicaoLivro anterior = posicoes.get(item.ativoId());
      Map<Long, BigDecimal> custodias = custodias(anterior);
      custodias.merge(item.contaId(), item.quantidade(), BigDecimal::add);
      BigDecimal custo = anterior == null ? null : anterior.custoTotalBrl();
      if (custodias.get(item.contaId()).signum() < 0
          && !"fiat:BRL".equals(codigosAtivos.get(item.ativoId()))) custo = null;
      atualizarPosicao(
          posicoes,
          item.ativoId(),
          anterior == null ? BigDecimal.ZERO : anterior.quantidade(),
          custo,
          custodias);
      return null;
    }
    FatoLivro fato = fatoNaConta(item);
    if (item.operacao() != null) {
      return switch (fato.tipo()) {
        case SALDO_INICIAL ->
            registrarEntrada(
                fato,
                "fiat:BRL".equals(codigosAtivos.get(fato.ativoId()))
                    ? fato.quantidade()
                    : fato.custoTotalBrl(),
                posicoes);
        case APORTE -> registrarEntrada(fato, fato.quantidade(), posicoes);
        case RETIRADA -> registrarRetirada(fato, posicoes, codigosAtivos, true);
        case COMPRA -> comprar(fato, posicoes, codigosAtivos, true);
        case VENDA -> vender(fato, posicoes, codigosAtivos, true);
      };
    }
    if (item.quantidade().signum() > 0) {
      BigDecimal custo =
          "fiat:BRL".equals(codigosAtivos.get(item.ativoId()))
              ? item.quantidade()
              : item.custoEntradaBrl();
      creditar(posicoes, item.ativoId(), item.contaId(), item.quantidade(), custo);
    } else {
      debitar(posicoes, fato, item.ativoId(), item.quantidade().negate(), true, codigosAtivos);
    }
    return null;
  }

  private static FatoLivro fatoNaConta(ItemConferenciaLivro item) {
    FatoLivro fato = item.operacao();
    if (fato == null)
      return new FatoLivro(
          -1L,
          null,
          item.contaId(),
          item.ativoId(),
          item.ocorridaEm(),
          item.ordem(),
          item.quantidade(),
          item.custoEntradaBrl(),
          null,
          null,
          null,
          null,
          null);
    return new FatoLivro(
        fato.id(),
        fato.tipo(),
        item.contaId(),
        fato.ativoId(),
        fato.ocorridaEm(),
        fato.ordemLivro(),
        fato.quantidade(),
        fato.custoTotalBrl(),
        fato.moedaCotacaoId(),
        fato.totalBruto(),
        fato.cotacaoBrl(),
        fato.taxaAtivoId(),
        fato.taxaQuantidade(),
        fato.valorTotalValoradoBrl(), fato.taxaExternaBrl());
  }

  private static List<Long> ativosAlterados(
      Map<Long, PosicaoLivro> anteriores, Map<Long, PosicaoLivro> atuais) {
    Set<Long> ativos = new TreeSet<>(anteriores.keySet());
    ativos.addAll(atuais.keySet());
    return ativos.stream()
        .filter(id -> !Objects.equals(anteriores.get(id), atuais.get(id)))
        .toList();
  }

  private static boolean registrarDeficits(
      ItemConferenciaLivro item,
      Map<Long, PosicaoLivro> anteriores,
      Map<Long, PosicaoLivro> atuais,
      Map<Long, Map<Long, BigDecimal>> saldosPropostas,
      List<LacunaLivro> lacunas,
      Set<String> dependencias) {
    boolean deficit = false;
    for (Long ativoId : ativosAlterados(anteriores, atuais)) {
      Map<Long, BigDecimal> antes = custodias(anteriores.get(ativoId));
      Map<Long, BigDecimal> depois = custodias(atuais.get(ativoId));
      BigDecimal saldo = depois.getOrDefault(item.contaId(), BigDecimal.ZERO);
      BigDecimal movimento = saldo.subtract(antes.getOrDefault(item.contaId(), BigDecimal.ZERO));
      Map<Long, BigDecimal> propostas =
          saldosPropostas.computeIfAbsent(ativoId, ignorado -> new TreeMap<>());
      BigDecimal projetado = propostas.getOrDefault(item.contaId(), BigDecimal.ZERO).add(movimento);
      if (projetado.signum() < 0) {
        String identidade = identidadeLacuna(item.contaId(), ativoId, item.id());
        lacunas.add(
            new LacunaLivro(
                identidade,
                item.id(),
                item.contaId(),
                item.instituicaoId(),
                ativoId,
                projetado.negate()));
        dependencias.add(identidade);
        projetado = BigDecimal.ZERO;
      }
      propostas.put(item.contaId(), projetado);
      if (saldo.signum() < 0) deficit = true;
    }
    return deficit;
  }

  private static String identidadeLacuna(Long contaId, Long ativoId, String ancoraId) {
    return contaId + ":" + ativoId + ":" + ancoraId;
  }

  private static PosicaoConferenciaLivro posicaoConferencia(
      PosicaoLivro posicao, Map<Long, Long> instituicoes, Map<Long, Set<String>> causas) {
    List<CustodiaConferenciaLivro> contas =
        posicao.instituicoes().stream()
            .map(c ->
                new CustodiaConferenciaLivro(
                    c.instituicaoId(),
                    instituicoes.get(c.instituicaoId()),
                    c.quantidade(),
                    c.quantidade().signum() >= 0))
            .toList();
    return new PosicaoConferenciaLivro(
        posicao.ativoId(),
        posicao.quantidade(),
        posicao.custoTotalBrl(),
        posicao.custoMedioBrl(),
        contas.stream().allMatch(CustodiaConferenciaLivro::quantidadeDisponivel),
        contas,
        List.copyOf(causas.getOrDefault(posicao.ativoId(), Set.of())));
  }

  public static ResultadoLivro calcular(List<FatoLivro> fatos, Map<Long, String> codigosAtivos) {
    Map<Long, PosicaoLivro> posicoes = new TreeMap<>();
    List<CalculoOperacao> calculos = new ArrayList<>(fatos.size());
    List<FatoLivro> fatosOrdenados =
        fatos.stream()
            .sorted(
                Comparator.comparing(FatoLivro::ocorridaEm)
                    .thenComparingLong(FatoLivro::ordemLivro)
                    .thenComparing(FatoLivro::id))
            .toList();
    for (FatoLivro fato : fatosOrdenados) {
      CalculoOperacao calculo =
          switch (fato.tipo()) {
            case SALDO_INICIAL -> registrarEntrada(fato, fato.custoTotalBrl(), posicoes);
            case APORTE -> registrarEntrada(fato, fato.quantidade(), posicoes);
            case RETIRADA -> registrarRetirada(fato, posicoes, codigosAtivos, false);
            case COMPRA -> comprar(fato, posicoes, codigosAtivos, false);
            case VENDA -> vender(fato, posicoes, codigosAtivos, false);
          };
      calculos.add(calculo);
    }
    return new ResultadoLivro(List.copyOf(posicoes.values()), calculos);
  }

  private static CalculoOperacao registrarEntrada(
      FatoLivro fato, BigDecimal custo, Map<Long, PosicaoLivro> posicoes) {
    creditar(posicoes, fato.ativoId(), fato.instituicaoId(), fato.quantidade(), custo);
    return resultado(fato.id(), arredondar(custo), ZERO, ZERO, ZERO, ZERO, ZERO);
  }

  private static CalculoOperacao registrarRetirada(
      FatoLivro fato,
      Map<Long, PosicaoLivro> posicoes,
      Map<Long, String> codigos,
      boolean tolerarDeficit) {
    if (!"fiat:BRL".equals(codigos.get(fato.ativoId())))
      throw new IllegalArgumentException("A retirada deve ser realizada em reais.");
    debitar(posicoes, fato, fato.ativoId(), fato.quantidade(), tolerarDeficit, codigos);
    return resultado(fato.id(), arredondar(fato.quantidade()), ZERO, ZERO, ZERO, ZERO, ZERO);
  }

  private static CalculoOperacao comprar(
      FatoLivro fato,
      Map<Long, PosicaoLivro> posicoes,
      Map<Long, String> codigosAtivos,
      boolean tolerarDeficit) {
    BigDecimal brutoBrl = valorBrutoBrl(fato, codigosAtivos);
    BigDecimal taxaQuantidade = taxaQuantidade(fato);
    boolean taxaNaCotacao = fato.moedaCotacaoId().equals(fato.taxaAtivoId());
    boolean taxaExterna = fato.taxaExternaBrl() != null;
    BigDecimal taxaBrl = valorTaxaBrl(fato, brutoBrl, codigosAtivos);
    BigDecimal custoPrincipal =
        debitar(
            posicoes,
            fato,
            fato.moedaCotacaoId(),
            fato.totalBruto(),
            tolerarDeficit,
            codigosAtivos);
    BigDecimal custoTaxa =
        taxaExterna
            ? debitar(posicoes, fato, fato.taxaAtivoId(), taxaQuantidade, tolerarDeficit, codigosAtivos)
            : taxaNaCotacao
            ? debitar(
                posicoes,
                fato,
                fato.moedaCotacaoId(),
                taxaQuantidade,
                tolerarDeficit,
                codigosAtivos)
            : ZERO;
    BigDecimal quantidadeAdquirida =
        taxaNaCotacao || taxaExterna ? fato.quantidade() : fato.quantidade().subtract(taxaQuantidade);
    BigDecimal custoAdquirido = taxaNaCotacao || taxaExterna ? brutoBrl.add(taxaBrl) : brutoBrl;
    creditar(posicoes, fato.ativoId(), fato.instituicaoId(), quantidadeAdquirida, custoAdquirido);
    BigDecimal resultadoMoeda =
        resultadoSaidaMoeda(fato.moedaCotacaoId(), brutoBrl, custoPrincipal, codigosAtivos);
    BigDecimal resultadoTaxa =
        taxaExterna
            ? resultadoSaidaMoeda(fato.taxaAtivoId(), taxaBrl, custoTaxa, codigosAtivos)
            : taxaNaCotacao
            ? resultadoSaidaMoeda(fato.moedaCotacaoId(), taxaBrl, custoTaxa, codigosAtivos)
            : ZERO;
    return resultado(
        fato.id(),
        brutoBrl,
        taxaBrl,
        somar(custoPrincipal, custoTaxa),
        ZERO,
        resultadoMoeda,
        resultadoTaxa);
  }

  private static CalculoOperacao vender(
      FatoLivro fato,
      Map<Long, PosicaoLivro> posicoes,
      Map<Long, String> codigosAtivos,
      boolean tolerarDeficit) {
    BigDecimal brutoBrl = valorBrutoBrl(fato, codigosAtivos);
    BigDecimal taxaQuantidade = taxaQuantidade(fato);
    boolean taxaNaCotacao = fato.moedaCotacaoId().equals(fato.taxaAtivoId());
    boolean taxaExterna = fato.taxaExternaBrl() != null;
    BigDecimal taxaBrl = valorTaxaBrl(fato, brutoBrl, codigosAtivos);
    BigDecimal custoPrincipal =
        debitar(posicoes, fato, fato.ativoId(), fato.quantidade(), tolerarDeficit, codigosAtivos);
    BigDecimal custoTaxa =
        taxaExterna
            ? debitar(posicoes, fato, fato.taxaAtivoId(), taxaQuantidade, tolerarDeficit, codigosAtivos)
            : taxaNaCotacao
            ? ZERO
            : debitar(
                posicoes, fato, fato.ativoId(), taxaQuantidade, tolerarDeficit, codigosAtivos);
    BigDecimal quantidadeRecebida =
        taxaNaCotacao ? fato.totalBruto().subtract(taxaQuantidade) : fato.totalBruto();
    BigDecimal custoRecebido = taxaNaCotacao ? brutoBrl.subtract(taxaBrl) : brutoBrl;
    creditar(
        posicoes, fato.moedaCotacaoId(), fato.instituicaoId(), quantidadeRecebida, custoRecebido);
    BigDecimal resultadoNegociacao = subtrair(brutoBrl, custoPrincipal);
    BigDecimal resultadoTaxa = taxaNaCotacao ? taxaBrl.negate() : negar(custoTaxa);
    return resultado(
        fato.id(),
        brutoBrl,
        taxaBrl,
        somar(custoPrincipal, custoTaxa),
        resultadoNegociacao,
        ZERO,
        resultadoTaxa);
  }

  private static BigDecimal valorBrutoBrl(FatoLivro fato, Map<Long, String> codigosAtivos) {
    if ("fiat:BRL".equals(codigosAtivos.get(fato.moedaCotacaoId()))) return arredondar(fato.totalBruto());
    if (fato.valorTotalValoradoBrl() != null) return arredondar(fato.valorTotalValoradoBrl());
    return arredondar(fato.totalBruto().multiply(cotacaoBrl(fato, codigosAtivos)));
  }

  private static BigDecimal valorTaxaBrl(
      FatoLivro fato, BigDecimal brutoBrl, Map<Long, String> codigosAtivos) {
    BigDecimal taxa = taxaQuantidade(fato);
    if (taxa.signum() == 0) return ZERO;
    if (fato.taxaExternaBrl() != null) return arredondar(fato.taxaExternaBrl());
    if (fato.moedaCotacaoId().equals(fato.taxaAtivoId())) {
      return arredondar(taxa.multiply(cotacaoBrl(fato, codigosAtivos)));
    }
    return brutoBrl.multiply(taxa).divide(fato.quantidade(), 8, RoundingMode.HALF_EVEN);
  }

  private static BigDecimal cotacaoBrl(FatoLivro fato, Map<Long, String> codigosAtivos) {
    return "fiat:BRL".equals(codigosAtivos.get(fato.moedaCotacaoId()))
        ? BigDecimal.ONE
        : fato.cotacaoBrl();
  }

  private static BigDecimal taxaQuantidade(FatoLivro fato) {
    return fato.taxaQuantidade() == null ? BigDecimal.ZERO : fato.taxaQuantidade();
  }

  private static BigDecimal resultadoSaidaMoeda(
      Long ativoId, BigDecimal valorBrl, BigDecimal custo, Map<Long, String> codigosAtivos) {
    return "fiat:BRL".equals(codigosAtivos.get(ativoId)) ? ZERO : subtrair(valorBrl, custo);
  }

  private static void creditar(
      Map<Long, PosicaoLivro> posicoes,
      Long ativoId,
      Long instituicaoId,
      BigDecimal quantidade,
      BigDecimal custo) {
    if (quantidade.signum() == 0) return;
    PosicaoLivro anterior = posicoes.get(ativoId);
    BigDecimal quantidadeAnterior = anterior == null ? BigDecimal.ZERO : anterior.quantidade();
    BigDecimal custoAnterior = anterior == null ? ZERO : anterior.custoTotalBrl();
    Map<Long, BigDecimal> custodias = custodias(anterior);
    custodias.merge(instituicaoId, quantidade, BigDecimal::add);
    atualizarPosicao(
        posicoes,
        ativoId,
        quantidadeAnterior.add(quantidade),
        somar(custoAnterior, custo),
        custodias);
  }

  private static BigDecimal debitar(
      Map<Long, PosicaoLivro> posicoes,
      FatoLivro fato,
      Long ativoId,
      BigDecimal quantidade,
      boolean tolerarDeficit,
      Map<Long, String> codigosAtivos) {
    if (quantidade.signum() == 0) return ZERO;
    PosicaoLivro anterior = posicoes.get(ativoId);
    Map<Long, BigDecimal> custodias = custodias(anterior);
    BigDecimal custodia = custodias.getOrDefault(fato.instituicaoId(), BigDecimal.ZERO);
    boolean insuficiente = anterior == null || custodia.compareTo(quantidade) < 0;
    if (insuficiente && !tolerarDeficit) {
      String operacao = fato.id() < 0 ? "esta operação" : "a operação " + fato.id();
      throw new OperacaoConflitanteException(
          "Saldo insuficiente na instituição para " + operacao + ".");
    }
    BigDecimal quantidadeAnterior = anterior == null ? BigDecimal.ZERO : anterior.quantidade();
    BigDecimal custoAnterior = anterior == null ? ZERO : anterior.custoTotalBrl();
    BigDecimal custoBaixado =
        custoDaSaida(quantidadeAnterior, custoAnterior, quantidade, insuficiente);
    if (tolerarDeficit && "fiat:BRL".equals(codigosAtivos.get(ativoId))) {
      custoBaixado = arredondar(quantidade);
      custoAnterior = arredondar(quantidadeAnterior);
    }
    custodias.put(fato.instituicaoId(), custodia.subtract(quantidade));
    atualizarPosicao(
        posicoes,
        ativoId,
        quantidadeAnterior.subtract(quantidade),
        subtrair(custoAnterior, custoBaixado),
        custodias);
    return custoBaixado;
  }

  private static BigDecimal custoDaSaida(
      BigDecimal quantidadeAnterior,
      BigDecimal custoAnterior,
      BigDecimal quantidade,
      boolean insuficiente) {
    if (insuficiente || custoAnterior == null || quantidadeAnterior.signum() <= 0) return null;
    return quantidadeAnterior.compareTo(quantidade) == 0
        ? custoAnterior
        : custoAnterior.multiply(quantidade).divide(quantidadeAnterior, 8, RoundingMode.HALF_EVEN);
  }

  private static Map<Long, BigDecimal> custodias(PosicaoLivro posicao) {
    Map<Long, BigDecimal> custodias = new TreeMap<>();
    if (posicao != null) {
      posicao.instituicoes().forEach(c -> custodias.put(c.instituicaoId(), c.quantidade()));
    }
    return custodias;
  }

  private static void atualizarPosicao(
      Map<Long, PosicaoLivro> posicoes,
      Long ativoId,
      BigDecimal quantidade,
      BigDecimal custo,
      Map<Long, BigDecimal> custodias) {
    if (quantidade.signum() == 0 && custodias.values().stream().allMatch(q -> q.signum() == 0)) {
      posicoes.remove(ativoId);
      return;
    }
    List<CustodiaLivro> instituicoes =
        custodias.entrySet().stream()
            .filter(c -> c.getValue().signum() != 0)
            .map(c -> new CustodiaLivro(c.getKey(), c.getValue()))
            .toList();
    posicoes.put(
        ativoId,
        new PosicaoLivro(
            ativoId,
            quantidade,
            arredondar(custo),
            custo == null || quantidade.signum() <= 0
                ? null
                : custo.divide(quantidade, 36, RoundingMode.HALF_EVEN),
            instituicoes));
  }

  private static CalculoOperacao resultado(
      Long operacaoId,
      BigDecimal bruto,
      BigDecimal taxa,
      BigDecimal custoBaixado,
      BigDecimal negociacao,
      BigDecimal moeda,
      BigDecimal resultadoTaxa) {
    return new CalculoOperacao(
        operacaoId,
        arredondar(bruto),
        arredondar(taxa),
        arredondar(custoBaixado),
        arredondar(negociacao),
        arredondar(moeda),
        arredondar(resultadoTaxa),
        arredondar(somar(somar(negociacao, moeda), resultadoTaxa)));
  }

  private static BigDecimal somar(BigDecimal primeiro, BigDecimal segundo) {
    return primeiro == null || segundo == null ? null : primeiro.add(segundo);
  }

  private static BigDecimal subtrair(BigDecimal primeiro, BigDecimal segundo) {
    return primeiro == null || segundo == null ? null : primeiro.subtract(segundo);
  }

  private static BigDecimal negar(BigDecimal valor) {
    return valor == null ? null : valor.negate();
  }

  private static BigDecimal arredondar(BigDecimal valor) {
    if (valor == null) return null;
    BigDecimal arredondado = valor.setScale(8, RoundingMode.HALF_EVEN);
    if (arredondado.precision() > 38)
      throw new OperacaoInvalidaException("O cálculo em reais excede a capacidade suportada.");
    return arredondado;
  }
}
