package io.github.jaimenafer.lastro.operacoes.application.usecase.impl;

import io.github.jaimenafer.lastro.operacoes.application.dto.DadosSaldoInicial;
import io.github.jaimenafer.lastro.operacoes.domain.exception.SaldoInicialInvalidoException;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;
import java.util.List;
import java.util.UUID;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class IdentidadeSolicitacaoSaldo {

  public static String chaveValida(String chave) {
    if (chave == null || !chave.matches("[0-9a-fA-F]{8}(?:-[0-9a-fA-F]{4}){3}-[0-9a-fA-F]{12}")) {
      throw new SaldoInicialInvalidoException("Informe uma chave de solicitação válida.");
    }
    return UUID.fromString(chave).toString();
  }

  public static String fingerprint(DadosSaldoInicial dados, Long anterior) {
    var valores =
        List.of(
            anterior == null ? "CRIAR" : "SUBSTITUIR:" + anterior,
            dados.instituicaoId().toString(),
            dados.ativoId().toString(),
            dados.data().toString(),
            dados.quantidade().toPlainString(),
            dados.custo().toPlainString(),
            dados.justificativa(),
            dados.motivo());
    var texto =
        valores.stream()
            .map(valor -> valor.length() + ":" + valor)
            .collect(java.util.stream.Collectors.joining());
    try {
      return HexFormat.of()
          .formatHex(
              MessageDigest.getInstance("SHA-256").digest(texto.getBytes(StandardCharsets.UTF_8)));
    } catch (NoSuchAlgorithmException erro) {
      throw new IllegalStateException(erro);
    }
  }
}
