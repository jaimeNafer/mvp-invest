package io.github.jaimenafer.lastro.operacoes.domain.service;

import io.github.jaimenafer.lastro.operacoes.domain.exception.OperacaoConflitanteException;
import io.github.jaimenafer.lastro.operacoes.domain.exception.OperacaoInvalidaException;
import io.github.jaimenafer.lastro.operacoes.domain.model.CalculoOperacao;
import io.github.jaimenafer.lastro.operacoes.domain.model.CustodiaLivro;
import io.github.jaimenafer.lastro.operacoes.domain.model.FatoLivro;
import io.github.jaimenafer.lastro.operacoes.domain.model.PosicaoLivro;
import io.github.jaimenafer.lastro.operacoes.domain.model.ResultadoLivro;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class MotorLivro {
  private static final BigDecimal ZERO = BigDecimal.ZERO.setScale(8);

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
            case COMPRA -> comprar(fato, posicoes, codigosAtivos);
            case VENDA -> vender(fato, posicoes, codigosAtivos);
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

  private static CalculoOperacao comprar(
      FatoLivro fato, Map<Long, PosicaoLivro> posicoes, Map<Long, String> codigosAtivos) {
    BigDecimal brutoBrl = valorBrutoBrl(fato, codigosAtivos);
    BigDecimal taxaQuantidade = taxaQuantidade(fato);
    boolean taxaNaCotacao = fato.moedaCotacaoId().equals(fato.taxaAtivoId());
    BigDecimal taxaBrl = valorTaxaBrl(fato, brutoBrl, codigosAtivos);
    BigDecimal custoPrincipal = debitar(posicoes, fato, fato.moedaCotacaoId(), fato.totalBruto());
    BigDecimal custoTaxa =
        taxaNaCotacao ? debitar(posicoes, fato, fato.moedaCotacaoId(), taxaQuantidade) : ZERO;
    BigDecimal quantidadeAdquirida =
        taxaNaCotacao ? fato.quantidade() : fato.quantidade().subtract(taxaQuantidade);
    BigDecimal custoAdquirido = taxaNaCotacao ? brutoBrl.add(taxaBrl) : brutoBrl;
    creditar(posicoes, fato.ativoId(), fato.instituicaoId(), quantidadeAdquirida, custoAdquirido);
    BigDecimal resultadoMoeda =
        resultadoSaidaMoeda(fato.moedaCotacaoId(), brutoBrl, custoPrincipal, codigosAtivos);
    BigDecimal resultadoTaxa =
        taxaNaCotacao
            ? resultadoSaidaMoeda(fato.moedaCotacaoId(), taxaBrl, custoTaxa, codigosAtivos)
            : ZERO;
    return resultado(
        fato.id(),
        brutoBrl,
        taxaBrl,
        custoPrincipal.add(custoTaxa),
        ZERO,
        resultadoMoeda,
        resultadoTaxa);
  }

  private static CalculoOperacao vender(
      FatoLivro fato, Map<Long, PosicaoLivro> posicoes, Map<Long, String> codigosAtivos) {
    BigDecimal brutoBrl = valorBrutoBrl(fato, codigosAtivos);
    BigDecimal taxaQuantidade = taxaQuantidade(fato);
    boolean taxaNaCotacao = fato.moedaCotacaoId().equals(fato.taxaAtivoId());
    BigDecimal taxaBrl = valorTaxaBrl(fato, brutoBrl, codigosAtivos);
    BigDecimal custoPrincipal = debitar(posicoes, fato, fato.ativoId(), fato.quantidade());
    BigDecimal custoTaxa =
        taxaNaCotacao ? ZERO : debitar(posicoes, fato, fato.ativoId(), taxaQuantidade);
    BigDecimal quantidadeRecebida =
        taxaNaCotacao ? fato.totalBruto().subtract(taxaQuantidade) : fato.totalBruto();
    BigDecimal custoRecebido = taxaNaCotacao ? brutoBrl.subtract(taxaBrl) : brutoBrl;
    creditar(
        posicoes, fato.moedaCotacaoId(), fato.instituicaoId(), quantidadeRecebida, custoRecebido);
    BigDecimal resultadoNegociacao = brutoBrl.subtract(custoPrincipal);
    BigDecimal resultadoTaxa = taxaNaCotacao ? taxaBrl.negate() : custoTaxa.negate();
    return resultado(
        fato.id(),
        brutoBrl,
        taxaBrl,
        custoPrincipal.add(custoTaxa),
        resultadoNegociacao,
        ZERO,
        resultadoTaxa);
  }

  private static BigDecimal valorBrutoBrl(FatoLivro fato, Map<Long, String> codigosAtivos) {
    return arredondar(fato.totalBruto().multiply(cotacaoBrl(fato, codigosAtivos)));
  }

  private static BigDecimal valorTaxaBrl(
      FatoLivro fato, BigDecimal brutoBrl, Map<Long, String> codigosAtivos) {
    BigDecimal taxa = taxaQuantidade(fato);
    if (taxa.signum() == 0) return ZERO;
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
    return "fiat:BRL".equals(codigosAtivos.get(ativoId)) ? ZERO : valorBrl.subtract(custo);
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
        posicoes, ativoId, quantidadeAnterior.add(quantidade), custoAnterior.add(custo), custodias);
  }

  private static BigDecimal debitar(
      Map<Long, PosicaoLivro> posicoes, FatoLivro fato, Long ativoId, BigDecimal quantidade) {
    if (quantidade.signum() == 0) return ZERO;
    PosicaoLivro anterior = posicoes.get(ativoId);
    Map<Long, BigDecimal> custodias = custodias(anterior);
    BigDecimal custodia = custodias.getOrDefault(fato.instituicaoId(), BigDecimal.ZERO);
    if (anterior == null || custodia.compareTo(quantidade) < 0) {
      String operacao = fato.id() < 0 ? "esta operação" : "a operação " + fato.id();
      throw new OperacaoConflitanteException("Saldo insuficiente na instituição para " + operacao + ".");
    }
    BigDecimal custoBaixado =
        anterior.quantidade().compareTo(quantidade) == 0
            ? anterior.custoTotalBrl()
            : anterior
                .custoTotalBrl()
                .multiply(quantidade)
                .divide(anterior.quantidade(), 8, RoundingMode.HALF_EVEN);
    custodias.put(fato.instituicaoId(), custodia.subtract(quantidade));
    atualizarPosicao(
        posicoes,
        ativoId,
        anterior.quantidade().subtract(quantidade),
        anterior.custoTotalBrl().subtract(custoBaixado),
        custodias);
    return custoBaixado;
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
    if (quantidade.signum() == 0) {
      posicoes.remove(ativoId);
      return;
    }
    List<CustodiaLivro> instituicoes =
        custodias.entrySet().stream()
            .filter(c -> c.getValue().signum() > 0)
            .map(c -> new CustodiaLivro(c.getKey(), c.getValue()))
            .toList();
    posicoes.put(
        ativoId,
        new PosicaoLivro(
            ativoId,
            quantidade,
            arredondar(custo),
            custo.divide(quantidade, 36, RoundingMode.HALF_EVEN),
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
        arredondar(negociacao.add(moeda).add(resultadoTaxa)));
  }

  private static BigDecimal arredondar(BigDecimal valor) {
    BigDecimal arredondado = valor.setScale(8, RoundingMode.HALF_EVEN);
    if (arredondado.precision() > 38)
      throw new OperacaoInvalidaException("O cálculo em reais excede a capacidade suportada.");
    return arredondado;
  }
}
