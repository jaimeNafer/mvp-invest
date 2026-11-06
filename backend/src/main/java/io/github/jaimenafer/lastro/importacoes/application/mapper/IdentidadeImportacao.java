package io.github.jaimenafer.lastro.importacoes.application.mapper;

import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;
import java.util.UUID;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class IdentidadeImportacao {
  public static String evento(String referencia) {
    return uuid("lastro:binance:relatorio-brl:v1:" + referencia);
  }

  public static String legada(String hash, String referencia) {
    return uuid("lastro:binance2024:" + hash + ":" + referencia);
  }

  public static String resumo(byte[] conteudo) {
    return HexFormat.of().formatHex(digest("SHA-256", conteudo));
  }

  private static String uuid(String valor) {
    UUID namespace = UUID.fromString("6ba7b811-9dad-11d1-80b4-00c04fd430c8");
    byte[] nome = valor.getBytes(StandardCharsets.UTF_8);
    ByteBuffer entrada = ByteBuffer.allocate(16 + nome.length);
    entrada
        .putLong(namespace.getMostSignificantBits())
        .putLong(namespace.getLeastSignificantBits())
        .put(nome);
    byte[] hash = digest("SHA-1", entrada.array());
    hash[6] = (byte) ((hash[6] & 0x0f) | 0x50);
    hash[8] = (byte) ((hash[8] & 0x3f) | 0x80);
    ByteBuffer resultado = ByteBuffer.wrap(hash);
    return new UUID(resultado.getLong(), resultado.getLong()).toString();
  }

  private static byte[] digest(String algoritmo, byte[] conteudo) {
    try {
      return MessageDigest.getInstance(algoritmo).digest(conteudo);
    } catch (NoSuchAlgorithmException erro) {
      throw new IllegalStateException("Algoritmo de resumo indisponível.", erro);
    }
  }
}
