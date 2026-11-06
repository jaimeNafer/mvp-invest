package io.github.jaimenafer.lastro.importacoes.infrastructure.csv;

import io.github.jaimenafer.lastro.importacoes.application.port.ILeitorRelatorio;
import io.github.jaimenafer.lastro.importacoes.domain.model.ArquivoInterpretado;
import io.github.jaimenafer.lastro.importacoes.domain.model.DiagnosticoImportacao;
import io.github.jaimenafer.lastro.importacoes.domain.model.EventoImportacao;
import java.io.IOException;
import java.io.StringReader;
import java.io.UncheckedIOException;
import java.math.BigDecimal;
import java.nio.ByteBuffer;
import java.nio.charset.CharacterCodingException;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.time.format.ResolverStyle;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.HexFormat;
import java.util.List;
import java.util.Set;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVRecord;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class LeitorRelatorioBinance implements ILeitorRelatorio {
  @Value("${lastro.importacoes.max-bytes:10485760}")
  private int maximoBytes = 10 * 1024 * 1024;

  @Value("${lastro.importacoes.max-eventos:5000}")
  private int maximoEventos = 5000;

  private static final List<String> CABECALHO =
      List.of(
          "id",
          "datetime_tz_BRT",
          "type",
          "label",
          "market_model_type",
          "order_type",
          "sent_amount",
          "sent_currency",
          "sent_value_BRL",
          "sent_address",
          "received_amount",
          "received_currency",
          "received_value_BRL",
          "received_address",
          "fee_amount",
          "fee_currency",
          "fee_value_BRL");
  private static final CSVFormat FORMATO =
      CSVFormat.RFC4180
          .builder()
          .setHeader()
          .setSkipHeaderRecord(true)
          .setIgnoreEmptyLines(false)
          .setAllowMissingColumnNames(true)
          .get();
  private static final DateTimeFormatter DATA =
      DateTimeFormatter.ofPattern("uuuu-MM-dd-HH:mm:ss").withResolverStyle(ResolverStyle.STRICT);

  @Override
  public ArquivoInterpretado ler(byte[] conteudo) {
    String hash = hash(conteudo);
    if (conteudo.length > maximoBytes) {
      return falha(hash, 1, "O arquivo excede o limite de 10 MiB.");
    }
    String texto;
    try {
      texto = StandardCharsets.UTF_8.newDecoder().decode(ByteBuffer.wrap(conteudo)).toString();
    } catch (CharacterCodingException erro) {
      return falha(hash, 1, "O arquivo deve estar codificado em UTF-8.");
    }
    if (texto.startsWith("\ufeff")) texto = texto.substring(1);
    return interpretar(hash, texto);
  }

  private ArquivoInterpretado interpretar(String hash, String texto) {
    List<EventoImportacao> eventos = new ArrayList<>();
    List<DiagnosticoImportacao> erros = new ArrayList<>();
    Set<String> referencias = new HashSet<>();
    int linha = 2;
    try (CSVParser csv = FORMATO.parse(new StringReader(texto))) {
      if (!csv.getHeaderNames().equals(CABECALHO)) {
        return falha(hash, 1, "O cabeçalho não corresponde ao relatório aceito.");
      }
      for (CSVRecord registro : csv) {
        linha = Math.toIntExact(registro.getRecordNumber()) + 1;
        if (registro.getRecordNumber() > maximoEventos) {
          return falha(hash, linha, "O arquivo excede o limite de 5000 registros.");
        }
        interpretarRegistro(registro, linha, referencias, eventos, erros);
        linha++;
      }
    } catch (IOException | UncheckedIOException erro) {
      erros.add(new DiagnosticoImportacao(linha, "O arquivo contém um registro CSV malformado."));
    }
    if (eventos.isEmpty() && erros.isEmpty()) {
      erros.add(new DiagnosticoImportacao(1, "O arquivo não contém registros."));
    }
    eventos.sort(Comparator.comparing(EventoImportacao::ocorridaEm));
    return new ArquivoInterpretado(hash, erros.isEmpty() ? eventos : List.of(), erros);
  }

  private void interpretarRegistro(
      CSVRecord registro,
      int linha,
      Set<String> referencias,
      List<EventoImportacao> eventos,
      List<DiagnosticoImportacao> erros) {
    try {
      exigir(
          registro.size() == CABECALHO.size(),
          "O registro deve conter todas as colunas do cabeçalho.");
      String referencia = registro.get("id");
      exigir(!referencia.isBlank(), "O identificador do registro é obrigatório.");
      exigir(
          referencia.length() <= 160,
          "O identificador do registro deve ter no máximo 160 caracteres.");
      exigir(referencias.add(referencia), "O identificador do registro está repetido no arquivo.");
      eventos.add(evento(registro, linha, referencia));
    } catch (IllegalArgumentException erro) {
      erros.add(new DiagnosticoImportacao(linha, erro.getMessage()));
    }
  }

  private EventoImportacao evento(CSVRecord registro, int linha, String referencia) {
    LocalDateTime data = data(registro.get("datetime_tz_BRT"));
    BigDecimal taxa = decimal(registro, "fee_amount", false);
    String tipo = registro.get("type");
    String modelo = registro.get("market_model_type");
    String moedaTaxa = registro.get("fee_currency");
    if (tipo.equals("Deposit") && modelo.equals("FIAT")) {
      validarAporte(registro, taxa);
      BigDecimal quantidade = decimal(registro, "received_amount", true);
      return new EventoImportacao(
          linha,
          referencia,
          "APORTE",
          data.toInstant(ZoneOffset.ofHours(-3)),
          "BRL",
          quantidade,
          quantidade,
          taxa);
    }
    exigir(
        tipo.equals("Buy") && (modelo.equals("OCBS") || modelo.equals("CONVERT")),
        "O tipo de operação ou modelo de negociação não é aceito.");
    exigir(
        registro.get("sent_currency").equals("BRL"), "A compra deve usar BRL como moeda enviada.");
    exigir(
        moedaTaxa.equals("BRL") || (taxa.signum() == 0 && moedaTaxa.isEmpty()),
        "A taxa da compra deve estar em BRL.");
    exigir(
        !modelo.equals("CONVERT") || taxa.signum() == 0,
        "A compra CONVERT exige taxa explicitamente zero.");
    String codigoAtivo = registro.get("received_currency");
    exigir(
        codigoAtivo.matches("[A-Z][A-Z0-9]{0,19}") && !codigoAtivo.equals("BRL"),
        "A compra exige uma moeda recebida válida e diferente de BRL.");
    BigDecimal quantidade = decimal(registro, "received_amount", true);
    BigDecimal total = decimal(registro, "sent_amount", true);
    BigDecimal principal = total.subtract(taxa);
    exigir(principal.signum() > 0, "O total enviado deve ser maior que a taxa.");
    return new EventoImportacao(
        linha,
        referencia,
        "COMPRA",
        data.toInstant(ZoneOffset.ofHours(-3)),
        codigoAtivo,
        quantidade,
        principal,
        taxa);
  }

  private LocalDateTime data(String texto) {
    String mensagem = "A data deve ser válida e usar o formato yyyy-MM-dd-HH:mm:ss.";
    exigir(texto.matches("[0-9]{4}-[0-9]{2}-[0-9]{2}-[0-9]{2}:[0-9]{2}:[0-9]{2}"), mensagem);
    try {
      LocalDateTime data = LocalDateTime.parse(texto, DATA);
      exigir(data.getYear() > 0, mensagem);
      return data;
    } catch (DateTimeParseException erro) {
      throw new IllegalArgumentException(mensagem);
    }
  }

  private void validarAporte(CSVRecord registro, BigDecimal taxa) {
    exigir(registro.get("received_currency").equals("BRL"), "O depósito deve receber BRL.");
    exigir(depositoSemSaida(registro), "O depósito não deve conter moeda ou quantidade enviada.");
    exigir(taxa.signum() == 0, "O depósito exige taxa explicitamente zero.");
    exigir(
        registro.get("fee_currency").isEmpty() || registro.get("fee_currency").equals("BRL"),
        "A moeda da taxa do depósito deve ser BRL.");
  }

  private boolean depositoSemSaida(CSVRecord registro) {
    return registro.get("sent_currency").isEmpty()
        && (registro.get("sent_amount").isEmpty()
            || decimal(registro, "sent_amount", false).signum() == 0);
  }

  private BigDecimal decimal(CSVRecord registro, String campo, boolean positivo) {
    String texto = registro.get(campo);
    exigir(texto.length() <= 160, "O campo " + campo + " deve ter no máximo 160 caracteres.");
    exigir(
        texto.matches("[0-9]+(?:\\.[0-9]+)?"),
        "O campo " + campo + " exige um decimal explícito com ponto.");
    BigDecimal valor = new BigDecimal(texto);
    exigir(!positivo || valor.signum() > 0, "O campo " + campo + " deve ser maior que zero.");
    return valor;
  }

  private void exigir(boolean valido, String mensagem) {
    if (!valido) throw new IllegalArgumentException(mensagem);
  }

  private ArquivoInterpretado falha(String hash, int linha, String mensagem) {
    return new ArquivoInterpretado(
        hash, List.of(), List.of(new DiagnosticoImportacao(linha, mensagem)));
  }

  private String hash(byte[] conteudo) {
    try {
      return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(conteudo));
    } catch (NoSuchAlgorithmException erro) {
      throw new IllegalStateException("SHA-256 indisponível.", erro);
    }
  }
}
