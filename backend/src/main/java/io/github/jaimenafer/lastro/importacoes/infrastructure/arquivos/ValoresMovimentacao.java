package io.github.jaimenafer.lastro.importacoes.infrastructure.arquivos;

import java.math.BigDecimal;
import java.text.Normalizer;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.time.format.ResolverStyle;
import java.util.Collection;
import java.util.List;
import java.util.Locale;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
final class ValoresMovimentacao {
  static String normalizar(String valor) {
    return Normalizer.normalize(valor, Normalizer.Form.NFD)
        .replaceAll("\\p{M}", "")
        .strip()
        .toLowerCase(Locale.ROOT)
        .replaceAll("\\s+", " ");
  }

  static boolean mensagemSemResultados(Collection<String> valores) {
    List<String> preenchidos = valores.stream().filter(v -> !v.isBlank()).toList();
    return preenchidos.size() == 1
        && normalizar(preenchidos.getFirst()).equals("nenhum dado corresponde aos criterios.");
  }

  static BigDecimal quantidade(String texto, List<String> erros) {
    if (texto.length() > 160
        || !texto.matches("[+-]?[0-9]+(?:\\.[0-9]+)?(?:[eE][+-]?[0-9]{1,3})?")) {
      erros.add("A quantidade exige um decimal válido com ponto.");
      return null;
    }
    return new BigDecimal(texto);
  }

  static Instant instante(String texto, String padrao, List<String> erros) {
    try {
      var data =
          LocalDateTime.parse(
              texto, DateTimeFormatter.ofPattern(padrao).withResolverStyle(ResolverStyle.STRICT));
      if (data.getYear() < 1 || data.getYear() > 9999)
        throw new DateTimeParseException("Ano inválido", texto, 0);
      return data.toInstant(ZoneOffset.ofHours(-3));
    } catch (DateTimeParseException erro) {
      erros.add("A data deve ser válida e respeitar o formato do relatório.");
      return null;
    }
  }
}
