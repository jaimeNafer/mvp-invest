package io.github.jaimenafer.lastro.operacoes.application.usecase.impl;

import io.github.jaimenafer.lastro.ativos.application.contract.AtivoResumo;
import io.github.jaimenafer.lastro.operacoes.application.dto.*;
import io.github.jaimenafer.lastro.operacoes.domain.exception.OperacaoInvalidaException;
import io.github.jaimenafer.lastro.operacoes.domain.model.TipoOperacao;
import java.math.BigDecimal;
import java.time.*;
import java.time.format.DateTimeParseException;
import java.time.temporal.ChronoUnit;
import java.util.Map;
import java.util.Set;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import tools.jackson.databind.JsonNode;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class ValidacaoNegociacao {
  private static final Set<String> COTACOES = Set.of("fiat:BRL", "cripto:USDT", "cripto:USDC");

  public static DadosNegociacao normalizar(
      CadastroOperacao cadastro, Map<Long, AtivoResumo> ativos, boolean correcao) {
    var ativo = buscarAtivo(ativos, cadastro.ativoId());
    if (cadastro.tipo() == null || cadastro.tipo() == TipoOperacao.SALDO_INICIAL)
      throw new OperacaoInvalidaException(
          "Use o cadastro de saldo inicial para registrar uma abertura.");
    Instant instante = normalizarInstante(cadastro.ocorridaEm());
    var quantidade = decimal(cadastro.quantidade(), ativo.casasDecimais(), "quantidade", false);
    String motivo = cadastro.motivo();
    if (correcao && motivo.isEmpty())
      throw new OperacaoInvalidaException("Informe o motivo da correção.");
    if (!correcao && !motivo.isEmpty())
      throw new OperacaoInvalidaException("Motivo de alteração é exclusivo da correção.");
    if (cadastro.tipo() == TipoOperacao.APORTE) {
      if (!ativo.codigo().equals("fiat:BRL")
          || cadastro.moedaCotacaoId() != null
          || cadastro.totalBruto() != null
          || cadastro.cotacaoBrl() != null
          || cadastro.taxaAtivoId() != null
          || cadastro.taxaQuantidade() != null)
        throw new OperacaoInvalidaException("Aporte utiliza somente BRL, sem negociação ou taxa.");
      return new DadosNegociacao(
          cadastro.tipo(),
          cadastro.instituicaoId(),
          ativo.id(),
          instante,
          quantidade,
          null,
          null,
          null,
          null,
          BigDecimal.ZERO,
          cadastro.referencia(),
          motivo);
    }
    if (!ativo.cripto())
      throw new OperacaoInvalidaException("Selecione um criptoativo do catálogo para negociar.");
    var moeda = buscarAtivo(ativos, cadastro.moedaCotacaoId());
    if (!COTACOES.contains(moeda.codigo()) || moeda.id().equals(ativo.id()))
      throw new OperacaoInvalidaException(
          "Selecione BRL, USDT ou USDC, diferente do ativo negociado.");
    var total = decimal(cadastro.totalBruto(), moeda.casasDecimais(), "total bruto", false);
    var cotacao =
        cadastro.cotacaoBrl() == null && moeda.codigo().equals("fiat:BRL")
            ? BigDecimal.ONE
            : decimal(cadastro.cotacaoBrl(), 18, "cotação em reais", false);
    if (moeda.codigo().equals("fiat:BRL") && cotacao.compareTo(BigDecimal.ONE) != 0)
      throw new OperacaoInvalidaException("A cotação de BRL deve ser igual a 1.");
    BigDecimal taxa = normalizarTaxa(cadastro, ativos, ativo, moeda, quantidade, total);
    return new DadosNegociacao(
        cadastro.tipo(),
        cadastro.instituicaoId(),
        ativo.id(),
        instante,
        quantidade,
        moeda.id(),
        total,
        cotacao,
        cadastro.taxaAtivoId(),
        taxa,
        cadastro.referencia(),
        motivo);
  }

  private static BigDecimal normalizarTaxa(
      CadastroOperacao cadastro,
      Map<Long, AtivoResumo> ativos,
      AtivoResumo ativo,
      AtivoResumo moeda,
      BigDecimal quantidade,
      BigDecimal total) {
    BigDecimal taxa =
        cadastro.taxaQuantidade() == null
            ? BigDecimal.ZERO
            : decimal(cadastro.taxaQuantidade(), 18, "quantidade da taxa", true);
    if (taxa.signum() == 0 && cadastro.taxaAtivoId() != null)
      throw new OperacaoInvalidaException("Informe a moeda da taxa somente quando houver taxa.");
    if (taxa.signum() > 0) {
      var ativoTaxa = buscarAtivo(ativos, cadastro.taxaAtivoId());
      if (!ativoTaxa.id().equals(ativo.id()) && !ativoTaxa.id().equals(moeda.id()))
        throw new OperacaoInvalidaException(
            "A taxa deve ser cobrada em uma das moedas da negociação.");
      validarEscala(taxa, ativoTaxa.casasDecimais(), "quantidade da taxa");
      if (cadastro.tipo() == TipoOperacao.COMPRA
          && ativoTaxa.id().equals(ativo.id())
          && taxa.compareTo(quantidade) >= 0)
        throw new OperacaoInvalidaException("A taxa não pode consumir toda a compra.");
      if (cadastro.tipo() == TipoOperacao.VENDA
          && ativoTaxa.id().equals(moeda.id())
          && taxa.compareTo(total) >= 0)
        throw new OperacaoInvalidaException("A taxa não pode consumir toda a venda.");
    }
    return taxa;
  }

  private static Instant normalizarInstante(String valor) {
    Instant instante;
    try {
      instante = OffsetDateTime.parse(valor).toInstant().truncatedTo(ChronoUnit.MICROS);
    } catch (DateTimeParseException erro) {
      throw new OperacaoInvalidaException("Informe data e hora com fuso horário válidos.");
    }
    if (instante.atZone(ZoneId.of("America/Fortaleza")).getYear() < 1000)
      throw new OperacaoInvalidaException("Informe uma data a partir do ano 1000.");
    if (instante.isAfter(Instant.now()))
      throw new OperacaoInvalidaException("A operação não pode estar no futuro.");
    return instante;
  }

  private static AtivoResumo buscarAtivo(Map<Long, AtivoResumo> ativos, Long id) {
    var ativo = ativos.get(id);
    if (ativo == null)
      throw new OperacaoInvalidaException("Selecione um ativo disponível no catálogo.");
    return ativo;
  }

  private static BigDecimal decimal(JsonNode valor, int escala, String campo, boolean zero) {
    if (valor == null
        || !valor.isString()
        || valor.stringValue().length() > 160
        || !valor.stringValue().matches("(?:0|[1-9][0-9]*)(?:\\.[0-9]+)?"))
      throw new OperacaoInvalidaException("Informe " + campo + " como texto decimal válido.");
    var numero = new BigDecimal(valor.stringValue()).stripTrailingZeros();
    validarEscala(numero, escala, campo);
    if (numero.signum() < 0 || !zero && numero.signum() == 0)
      throw new OperacaoInvalidaException("Informe " + campo + " maior que zero.");
    return numero;
  }

  private static void validarEscala(BigDecimal numero, int escala, String campo) {
    if (Math.max(0, numero.scale()) > escala
        || Math.max(0, numero.precision() - numero.scale()) > 20)
      throw new OperacaoInvalidaException("O valor de " + campo + " excede a precisão permitida.");
  }
}
