package io.github.jaimenafer.lastro.importacoes.infrastructure.arquivos;

import io.github.jaimenafer.lastro.importacoes.application.port.ILeitorMovimentacoes;
import io.github.jaimenafer.lastro.importacoes.domain.exception.ImportacaoInvalidaException;
import io.github.jaimenafer.lastro.importacoes.domain.model.DocumentoMovimentacoes;
import io.github.jaimenafer.lastro.importacoes.domain.model.MovimentacaoRecebida;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HashSet;
import java.util.HexFormat;
import java.util.Locale;
import java.util.zip.ZipInputStream;
import javax.xml.XMLConstants;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.xml.sax.SAXException;

@Component
public class LeitorMovimentacoesBinance implements ILeitorMovimentacoes {
  @Value("${lastro.conferencia.max-bytes:10485760}")
  private int maximoBytes = 10 * 1024 * 1024;

  @Value("${lastro.conferencia.max-registros:10000}")
  private int maximoRegistros = 10000;

  @Value("${lastro.conferencia.max-descompactados:104857600}")
  private long maximoDescompactados = 100 * 1024 * 1024;

  @Override
  public DocumentoMovimentacoes ler(String nome, byte[] conteudo) {
    if (conteudo == null || conteudo.length == 0) {
      throw new ImportacaoInvalidaException("O arquivo não contém dados legíveis.");
    }
    if (conteudo.length > maximoBytes) {
      throw new ImportacaoInvalidaException("O arquivo excede o limite de tamanho permitido.");
    }
    boolean planilha = conteudo.length >= 4 && conteudo[0] == 80 && conteudo[1] == 75;
    if (planilha) validarPacote(conteudo);
    var registros =
        planilha
            ? planilha(conteudo)
            : new LeitorCsvMovimentacoes().ler(conteudo, maximoRegistros);
    if (registros.isEmpty())
      throw new ImportacaoInvalidaException("O arquivo não contém movimentações para importar.");
    return new DocumentoMovimentacoes(hash(conteudo), nome, "binance-movimentos-v1", registros);
  }

  private java.util.List<MovimentacaoRecebida> planilha(byte[] conteudo) {
    var relatorio = new LeitorPlanilhaRelatorioBinance().ler(conteudo, maximoRegistros);
    return relatorio == null
        ? new LeitorPlanilhaMovimentacoes().ler(conteudo, maximoRegistros)
        : relatorio;
  }

  private void validarPacote(byte[] conteudo) {
    long total = 0;
    var nomes = new HashSet<String>();
    try (var zip = new ZipInputStream(new ByteArrayInputStream(conteudo))) {
      var entrada = zip.getNextEntry();
      while (entrada != null) {
        String nome = entrada.getName().toLowerCase(Locale.ROOT);
        if (!nomes.add(nome)
            || nomes.size() > 2000
            || nome.contains("..")
            || nome.contains("vbaproject")
            || nome.startsWith("xl/externallinks/")) {
          throw new ImportacaoInvalidaException("A planilha contém recursos não permitidos.");
        }
        var relacao = nome.endsWith(".rels") ? new java.io.ByteArrayOutputStream() : null;
        byte[] buffer = new byte[8192];
        int lidos;
        while ((lidos = zip.read(buffer)) != -1) {
          total += lidos;
          if (total > maximoDescompactados) {
            throw new ImportacaoInvalidaException(
                "A planilha excede o limite de descompactação permitido.");
          }
          if (relacao != null) relacao.write(buffer, 0, lidos);
        }
        if (relacao != null) validarRelacionamentos(relacao.toByteArray());
        zip.closeEntry();
        entrada = zip.getNextEntry();
      }
      if (!nomes.contains("xl/workbook.xml") || !nomes.contains("[content_types].xml")) {
        throw new ImportacaoInvalidaException("O arquivo não é uma planilha XLSX legível.");
      }
    } catch (IOException erro) {
      throw new ImportacaoInvalidaException("Não foi possível ler a planilha XLSX.");
    }
  }

  private void validarRelacionamentos(byte[] conteudo) {
    try {
      var factory = DocumentBuilderFactory.newInstance();
      factory.setNamespaceAware(true);
      factory.setFeature("http://apache.org/xml/features/disallow-doctype-decl", true);
      factory.setAttribute(XMLConstants.ACCESS_EXTERNAL_DTD, "");
      factory.setAttribute(XMLConstants.ACCESS_EXTERNAL_SCHEMA, "");
      var documento = factory.newDocumentBuilder().parse(new ByteArrayInputStream(conteudo));
      var relacoes = documento.getElementsByTagNameNS("*", "Relationship");
      for (int i = 0; i < relacoes.getLength(); i++) {
        var modo = relacoes.item(i).getAttributes().getNamedItem("TargetMode");
        if (modo != null && modo.getNodeValue().equalsIgnoreCase("External")) {
          throw new ImportacaoInvalidaException(
              "A planilha contém vínculos externos não permitidos.");
        }
      }
    } catch (ParserConfigurationException | SAXException | IOException erro) {
      throw new ImportacaoInvalidaException("A planilha contém relacionamentos ilegíveis.");
    }
  }

  private String hash(byte[] conteudo) {
    try {
      return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(conteudo));
    } catch (NoSuchAlgorithmException erro) {
      throw new IllegalStateException("SHA-256 indisponível.", erro);
    }
  }
}
