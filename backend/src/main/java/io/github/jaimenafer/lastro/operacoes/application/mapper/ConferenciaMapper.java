package io.github.jaimenafer.lastro.operacoes.application.mapper;

import io.github.jaimenafer.lastro.operacoes.application.dto.*;
import io.github.jaimenafer.lastro.operacoes.domain.exception.*;
import io.github.jaimenafer.lastro.operacoes.domain.model.*;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.format.DateTimeParseException;
import java.util.*;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class ConferenciaMapper {
  private static final JsonMapper json = JsonMapper.builder().build();

  public static String textoDecimal(JsonNode valor, String campo) {
    if (valor == null || valor.isNull()) return null;
    if (!valor.isString() || valor.stringValue().length() > 160)
      throw new OperacaoInvalidaException("Informe " + campo + " como texto decimal válido.");
    return valor.stringValue().isBlank() ? null : valor.stringValue();
  }

  public static JsonNode texto(String valor) {
    return valor == null ? null : json.readTree(json.writeValueAsString(valor));
  }

  public static String escrever(Object valor) {
    return json.writeValueAsString(valor);
  }

  public static <T> T ler(String valor, Class<T> tipo) {
    return json.readValue(valor, tipo);
  }

  public static PropostaRegistro proposta(RegistroConferencia r, boolean atual) {
    String valor = atual ? r.getPropostaJson() : r.getAplicadaJson();
    return valor == null ? null : ler(valor, PropostaRegistro.class);
  }

  public static OrigemRegistro origem(RegistroConferencia r) {
    return ler(r.getOriginalJson(), OrigemRegistro.class);
  }

  public static String decimal(BigDecimal valor) {
    return valor == null ? null : valor.stripTrailingZeros().toPlainString();
  }

  public static BigDecimal numero(String valor) {
    if (valor == null || valor.isBlank()) return null;
    if (valor.length() > 160 || !valor.matches("-?[0-9]+(\\.[0-9]+)?"))
      throw new OperacaoInvalidaException(
          "Informe o valor decimal usando ponto e sem separador de milhares.");
    BigDecimal n = new BigDecimal(valor);
    if (Math.max(0, n.stripTrailingZeros().scale()) > 18 || n.precision() - n.scale() > 20)
      throw new OperacaoInvalidaException(
          "O valor excede a precisão permitida de 20 inteiros e 18 decimais.");
    return n;
  }

  public static Instant instante(String valor) {
    if (valor == null || valor.isBlank()) return null;
    try {
      return OffsetDateTime.parse(valor).toInstant();
    } catch (DateTimeParseException e) {
      throw new OperacaoInvalidaException("Informe a data e hora com fuso horário.");
    }
  }

  public static Long id(String valor) {
    if (valor == null || valor.isBlank()) return null;
    try {
      long n = Long.parseLong(valor);
      if (n > 0) return n;
    } catch (NumberFormatException e) {
    }
    throw new OperacaoInvalidaException("Informe um identificador positivo válido.");
  }

  public static long registroId(String valor) {
    if (valor == null || !valor.startsWith("registro:"))
      throw new OperacaoNaoEncontradaException("Registro não encontrado para este usuário.");
    return id(valor.substring(9));
  }

  public static String hash(String texto) {
    try {
      return HexFormat.of()
          .formatHex(
              MessageDigest.getInstance("SHA-256").digest(texto.getBytes(StandardCharsets.UTF_8)));
    } catch (NoSuchAlgorithmException e) {
      throw new IllegalStateException("SHA-256 indisponível.", e);
    }
  }

  public static boolean legivel(PropostaRegistro p) {
    return p != null
        && p.data() != null
        && p.contaId() != null
        && p.ativoId() != null
        && p.quantidade() != null;
  }

  public static ItemConferenciaLivro item(RegistroConferencia r, PropostaRegistro p) {
    if (!legivel(p) || p.natureza() == NaturezaRegistro.MOVIMENTACAO_INTERNA) return null;
    BigDecimal quantidade = numero(p.quantidade());
    FatoLivro fato = fato(r, p, quantidade);
    if (fato == null && p.natureza() == NaturezaRegistro.VENDA) quantidade = quantidade.negate();
    return new ItemConferenciaLivro(
        "registro:" + r.getId(),
        id(p.contaId()),
        r.getInstituicaoId(),
        id(p.ativoId()),
        instante(p.data()),
        r.getId(),
        quantidade,
        numero(p.custoTotalBrl()),
        fato,
        false,
        null,
        p.transferenciaRef(),
        p.natureza() == NaturezaRegistro.CREDITO_AUTOMATICO);
  }

  private static FatoLivro fato(RegistroConferencia r, PropostaRegistro p, BigDecimal quantidade) {
    if (p.natureza() == null || p.natureza() == NaturezaRegistro.OBSERVADA
        || p.natureza() == NaturezaRegistro.SAIDA_CRIPTO
        || p.natureza() == NaturezaRegistro.CREDITO_AUTOMATICO) return null;
    TipoOperacao tipo =
        switch (p.natureza()) {
          case SALDO_ANTERIOR -> TipoOperacao.SALDO_INICIAL;
          case APORTE -> TipoOperacao.APORTE;
          case RETIRADA -> TipoOperacao.RETIRADA;
          case COMPRA -> TipoOperacao.COMPRA;
          case VENDA -> TipoOperacao.VENDA;
          default -> throw new IllegalStateException();
        };
    boolean negociacao = tipo == TipoOperacao.COMPRA || tipo == TipoOperacao.VENDA;
    OrigemRegistro origem = origem(r);
    String taxaOriginal = origem.campos() == null ? "" : origem.campos().getOrDefault("fee_amount", "");
    boolean taxaAusenteNaImportacao =
        "IMPORTADA".equals(origem.origem())
            && (taxaOriginal.isBlank() || taxaOriginal.matches("0+(\\.0+)?"))
            && !p.taxaInformada();
    if (negociacao
        && ((!p.taxaInformada() && !taxaAusenteNaImportacao)
            || p.moedaCotacaoId() == null
            || p.totalBruto() == null
            || p.cotacaoBrl() == null
            || (p.taxaQuantidade() == null && !taxaAusenteNaImportacao)
            || (p.taxaQuantidade() != null && numero(p.taxaQuantidade()).signum() > 0 && p.taxaAtivoId() == null))) return null;
    return new FatoLivro(
        Long.MIN_VALUE / 2 - r.getId(),
        tipo,
        r.getInstituicaoId(),
        id(p.ativoId()),
        instante(p.data()),
        r.getId(),
        tipo == TipoOperacao.RETIRADA ? quantidade.abs() : quantidade,
        numero(p.custoTotalBrl()),
        id(p.moedaCotacaoId()),
        numero(p.totalBruto()),
        numero(p.cotacaoBrl()),
        id(p.taxaAtivoId()),
        taxaAusenteNaImportacao ? BigDecimal.ZERO : numero(p.taxaQuantidade()));
  }
}
