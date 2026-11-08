package io.github.jaimenafer.lastro.operacoes.domain.service;

import io.github.jaimenafer.lastro.operacoes.domain.model.LinhaAssociacao;
import io.github.jaimenafer.lastro.operacoes.domain.model.PropostaGrupo;
import java.nio.charset.StandardCharsets;
import java.math.BigDecimal;
import java.time.Instant;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Comparator;
import java.util.HexFormat;
import java.util.stream.Collectors;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class IdentidadeValoracao {
  public static String grupo(PropostaGrupo grupo) {
    String conteudo = grupo.linhas().stream().sorted(Comparator.comparingLong(LinhaAssociacao::registroId))
        .map(l -> String.join("|", Long.toString(l.registroId()), String.valueOf(l.instituicaoId()),
            String.valueOf(l.contaId()), String.valueOf(l.ativoId()), String.valueOf(l.data()),
            l.quantidade() == null ? "null" : l.quantidade().stripTrailingZeros().toPlainString(),
            String.valueOf(l.papel())))
        .collect(Collectors.joining(";"));
    return hash(conteudo);
  }

  public static String registro(long registroId, Long instituicaoId, Long contaId, Long ativoId,
      Instant data, BigDecimal quantidade) {
    return hash(String.join("|", Long.toString(registroId), String.valueOf(instituicaoId),
        String.valueOf(contaId), String.valueOf(ativoId), String.valueOf(data),
        quantidade == null ? "null" : quantidade.stripTrailingZeros().toPlainString()));
  }

  private static String hash(String conteudo) {
    try {
      return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256")
          .digest(conteudo.getBytes(StandardCharsets.UTF_8)));
    } catch (NoSuchAlgorithmException erro) {
      throw new IllegalStateException(erro);
    }
  }
}
