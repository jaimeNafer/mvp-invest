package io.github.jaimenafer.lastro.operacoes.domain.service;

import io.github.jaimenafer.lastro.operacoes.domain.model.NaturezaRegistro;
import io.github.jaimenafer.lastro.operacoes.domain.model.SubtipoCredito;
import java.math.BigDecimal;
import java.text.Normalizer;
import java.util.Locale;
import java.util.Map;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class ClassificacaoMovimentacao {
  public static SubtipoCredito subtipoCredito(String operacao) {
    if (operacao == null) return null;
    return switch (operacao.strip().toLowerCase(Locale.ROOT)) {
      case "megadrop rewards" -> SubtipoCredito.BONIFICACAO;
      case "launchpool airdrop - system distribution" -> SubtipoCredito.AIRDROP;
      case "simple earn locked rewards" -> SubtipoCredito.RENDIMENTO;
      default -> null;
    };
  }

  public static boolean movimentacaoEarn(String operacao) {
    if (operacao == null) return false;
    return switch (operacao.strip().toLowerCase(Locale.ROOT)) {
      case "simple earn locked subscription", "simple earn flexible subscription",
          "simple earn locked redemption", "simple earn flexible redemption" -> true;
      default -> false;
    };
  }

  public static NaturezaRegistro classificarMovimentacaoEarn(String operacao, BigDecimal quantidade) {
    if (!movimentacaoEarn(operacao) || quantidade == null) return NaturezaRegistro.OBSERVADA;
    boolean aplicacao = operacao.strip().toLowerCase(Locale.ROOT).endsWith("subscription");
    return (aplicacao ? quantidade.signum() < 0 : quantidade.signum() > 0)
        ? NaturezaRegistro.MOVIMENTACAO_INTERNA : NaturezaRegistro.OBSERVADA;
  }

  public static NaturezaRegistro classificarSaidaCripto(
      String operacao, BigDecimal quantidade, boolean cripto) {
    return cripto && "Withdraw".equalsIgnoreCase(operacao)
            && quantidade != null && quantidade.signum() < 0
        ? NaturezaRegistro.SAIDA_CRIPTO
        : NaturezaRegistro.OBSERVADA;
  }

  public static NaturezaRegistro classificar(
      String operacao,
      String moeda,
      BigDecimal quantidade,
      Map<String, String> campos,
      boolean possivelTransferencia) {
    if (!"BRL".equalsIgnoreCase(moeda)
        || quantidade == null
        || possivelTransferencia
        || contemIndicacaoTransferencia(campos)) return NaturezaRegistro.OBSERVADA;
    if ("Deposit".equalsIgnoreCase(operacao) && quantidade.signum() > 0)
      return NaturezaRegistro.APORTE;
    if ("Withdraw".equalsIgnoreCase(operacao) && quantidade.signum() < 0)
      return NaturezaRegistro.RETIRADA;
    return NaturezaRegistro.OBSERVADA;
  }

  private static boolean contemIndicacaoTransferencia(Map<String, String> campos) {
    return campos.entrySet().stream()
        .filter(e -> e.getKey().matches("(?i).*(remark|observ|label|address|destin|origin).*"))
        .map(Map.Entry::getValue)
        .filter(java.util.Objects::nonNull)
        .map(s ->
            Normalizer.normalize(s, Normalizer.Form.NFD)
                .replaceAll("\\p{M}", "")
                .toLowerCase(Locale.ROOT))
        .anyMatch(ClassificacaoMovimentacao::indicaTransferenciaInternaOuAmbigua);
  }

  private static boolean indicaTransferenciaInternaOuAmbigua(String texto) {
    if (texto.contains("interno") || texto.contains("interna") || texto.contains("internal"))
      return true;
    return texto.contains("transfer")
        && !texto.contains("bancari")
        && !texto.contains("banco")
        && !texto.contains("bank");
  }
}
