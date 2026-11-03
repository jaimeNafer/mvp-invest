package io.github.jaimenafer.lastro.operacoes.application.usecase.impl;

import io.github.jaimenafer.lastro.operacoes.application.dto.DadosNegociacao;
import io.github.jaimenafer.lastro.operacoes.domain.exception.OperacaoInvalidaException;
import java.nio.charset.StandardCharsets;
import java.security.*;
import java.util.*;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class IdentidadeNegociacao {
  public static String chave(String chave) {
    if (chave == null || !chave.matches("[0-9a-fA-F]{8}(?:-[0-9a-fA-F]{4}){3}-[0-9a-fA-F]{12}"))
      throw new OperacaoInvalidaException("Informe uma chave de solicitação válida.");
    return UUID.fromString(chave).toString();
  }

  public static String fingerprint(DadosNegociacao dados, Long anterior) {
    var valores =
        List.of(
            String.valueOf(anterior),
            dados.tipo().name(),
            dados.instituicaoId().toString(),
            dados.ativoId().toString(),
            dados.ocorridaEm().toString(),
            dados.quantidade().toPlainString(),
            String.valueOf(dados.moedaCotacaoId()),
            decimal(dados.totalBruto()),
            decimal(dados.cotacaoBrl()),
            String.valueOf(dados.taxaAtivoId()),
            dados.taxaQuantidade().toPlainString(),
            dados.referencia(),
            dados.motivo());
    String conteudo =
        valores.stream()
            .map(valor -> valor.length() + ":" + valor)
            .collect(java.util.stream.Collectors.joining());
    try {
      return HexFormat.of()
          .formatHex(
              MessageDigest.getInstance("SHA-256")
                  .digest(conteudo.getBytes(StandardCharsets.UTF_8)));
    } catch (NoSuchAlgorithmException erro) {
      throw new IllegalStateException(erro);
    }
  }

  private static String decimal(java.math.BigDecimal valor) {
    return valor == null ? "" : valor.toPlainString();
  }
}
