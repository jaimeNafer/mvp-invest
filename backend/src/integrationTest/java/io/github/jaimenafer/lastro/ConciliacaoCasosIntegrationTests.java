package io.github.jaimenafer.lastro;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.io.ByteArrayOutputStream;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.mysql.MySQLContainer;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

@SpringBootTest(properties = "spring.config.import=")
@AutoConfigureMockMvc
@Testcontainers
class ConciliacaoCasosIntegrationTests {
  @Container @ServiceConnection
  static final MySQLContainer MYSQL = new MySQLContainer("mysql:8.0.42");

  @Autowired MockMvc mvc;
  @Autowired JsonMapper json;

  @Test
  void reconheceMesmoArquivoSemDuplicarOcorrencias() throws Exception {
    String perfil = perfil();
    byte[] arquivo =
        planilha(new String[][] {{"2024-05-01 10:00:00", "Spot", "Deposit", "BTC", "1"}});
    receber(perfil, "primeiro.xlsx", arquivo).andExpect(status().isCreated());
    JsonNode repeticao =
        corpo(receber(perfil, "outro-nome.xlsx", arquivo).andExpect(status().isCreated()));
    assertThat(repeticao.path("arquivos").get(0).path("reconhecido").asBoolean()).isTrue();
    assertThat(timeline(perfil).path("total").asInt()).isEqualTo(1);
  }

  @Test
  void arquivoERegistrosPermanecemIsoladosEntrePerfis() throws Exception {
    String primeiro = perfil();
    String segundo = perfil();
    byte[] arquivo =
        planilha(new String[][] {{"2024-05-01 10:00:00", "Spot", "Deposit", "BTC", "1"}});
    receber(primeiro, "origem.xlsx", arquivo).andExpect(status().isCreated());
    assertThat(timeline(segundo).path("total").asInt()).isZero();
    JsonNode recebimento =
        corpo(receber(segundo, "origem.xlsx", arquivo).andExpect(status().isCreated()));
    assertThat(recebimento.path("arquivos").get(0).path("reconhecido").asBoolean()).isFalse();
    String idPrimeiro = texto(timeline(primeiro).path("itens").get(0), "id");
    String idSegundo = texto(timeline(segundo).path("itens").get(0), "id");
    assertThat(idPrimeiro).isNotEqualTo(idSegundo);
    JsonNode estadoSegundo = timeline(segundo);
    revisar(segundo, idPrimeiro, texto(estadoSegundo, "revisao"), "1", UUID.randomUUID().toString())
        .andExpect(status().isNotFound());
    assertThat(timeline(primeiro).path("itens").get(0).path("revisadoEm").isNull()).isTrue();
  }

  @Test
  void preservaOriginaisInvalidosSemConverterAusenciasEmZero() throws Exception {
    String perfil = perfil();
    byte[] arquivo =
        planilha(
            new String[][] {{"data inválida", "Spot", "Deposit", "BTC", "quantidade inválida"}});
    receber(perfil, "incompleto.xlsx", arquivo).andExpect(status().isCreated());
    JsonNode estado = timeline(perfil);
    JsonNode registro = estado.path("itens").get(0);
    assertThat(registro.path("data").isNull()).isTrue();
    assertThat(registro.path("quantidade").isNull()).isTrue();
    assertThat(registro.path("custoTotalBrl").isNull()).isTrue();
    assertThat(texto(registro.path("camposOriginais"), "Tempo")).isEqualTo("data inválida");
    assertThat(texto(registro.path("camposOriginais"), "Alterar")).isEqualTo("quantidade inválida");
    assertThat(texto(registro, "estado")).isEqualTo("CORRECAO_NECESSARIA");
    assertThat(registro.path("pendencias").isEmpty()).isFalse();
    JsonNode previa = previa(perfil, texto(estado, "revisao"));
    confirmar(perfil, previa, List.of(), List.of(), UUID.randomUUID().toString())
        .andExpect(status().isOk());
    JsonNode depois = timeline(perfil).path("itens").get(0);
    assertThat(depois.path("aplicado").asBoolean()).isFalse();
    assertThat(depois.path("quantidade").isNull()).isTrue();
    assertThat(texto(depois, "id")).isEqualTo(texto(registro, "id"));
  }

  @Test
  void revisarNaoResolveLacunaEEdicaoNaoHerdaRevisaoAnterior() throws Exception {
    String perfil = perfil();
    receber(
            perfil,
            "saida.xlsx",
            planilha(new String[][] {{"2024-05-02 10:00:00", "Spot", "Withdraw", "BTC", "-0.03"}}))
        .andExpect(status().isCreated());
    JsonNode antes = timeline(perfil);
    JsonNode original = antes.path("itens").get(0);
    String id = texto(original, "id");
    JsonNode revisado =
        corpo(
            revisar(
                    perfil,
                    id,
                    texto(antes, "revisao"),
                    texto(original, "versao"),
                    UUID.randomUUID().toString())
                .andExpect(status().isOk()));
    assertThat(revisado.path("saldoAnteriorPendente").asBoolean()).isTrue();
    assertThat(revisado.path("revisadoEm").isNull()).isFalse();
    assertThat(texto(revisado, "revisadoPor")).isNotBlank();
    JsonNode estado = timeline(perfil);
    JsonNode atual = estado.path("itens").get(0);
    var alteracao = proposta(estado, atual);
    alteracao.put("quantidade", "-0.04");
    alterar(perfil, id, alteracao).andExpect(status().isOk());
    JsonNode editado = timeline(perfil);
    JsonNode novaVersao = editado.path("itens").get(0);
    assertThat(novaVersao.path("revisadoEm").isNull()).isTrue();
    assertThat(new BigDecimal(texto(novaVersao, "quantidade"))).isEqualByComparingTo("-0.04");
    revisar(
            perfil,
            id,
            texto(editado, "revisao"),
            texto(atual, "versao"),
            UUID.randomUUID().toString())
        .andExpect(status().isConflict());
  }

  @Test
  void custoZeroDeclaradoContinuaDistintoDeCustoDesconhecido() throws Exception {
    String perfil = perfil();
    receber(
            perfil,
            "entradas.xlsx",
            planilha(
                new String[][] {
                  {"2024-05-01 10:00:00", "Spot", "Deposit", "BTC", "1"},
                  {"2024-05-02 10:00:00", "Funding", "Deposit", "ETH", "2"}
                }))
        .andExpect(status().isCreated());
    JsonNode estado = timeline(perfil);
    JsonNode btc = porTicker(estado, "BTC");
    assertThat(btc.path("custoTotalBrl").isNull()).isTrue();
    var proposta = proposta(estado, btc);
    proposta.put("natureza", "SALDO_ANTERIOR");
    proposta.put("custoTotalBrl", "0");
    proposta.put("motivo", "Custo zero declarado na documentação sintética.");
    alterar(perfil, texto(btc, "id"), proposta).andExpect(status().isOk());
    JsonNode depois = timeline(perfil);
    assertThat(new BigDecimal(texto(porTicker(depois, "BTC"), "custoTotalBrl"))).isZero();
    assertThat(porTicker(depois, "ETH").path("custoTotalBrl").isNull()).isTrue();
  }

  @Test
  void rejeitarAjustePreservaRegistroAplicadoComDeficitExplicito() throws Exception {
    String perfil = perfil();
    receber(
            perfil,
            "saida.xlsx",
            planilha(new String[][] {{"2024-05-02 10:00:00", "Spot", "Withdraw", "BTC", "-0.03"}}))
        .andExpect(status().isCreated());
    JsonNode estado = timeline(perfil);
    String id = texto(estado.path("itens").get(0), "id");
    JsonNode previa = previa(perfil, texto(estado, "revisao"));
    assertThat(previa.path("ajustes").size()).isEqualTo(1);
    confirmar(perfil, previa, List.of(), List.of(), UUID.randomUUID().toString())
        .andExpect(status().isOk());
    JsonNode depois = timeline(perfil);
    assertThat(depois.path("total").asInt()).isEqualTo(1);
    assertThat(depois.path("resumo").path("ajustesAtivos").asInt()).isZero();
    JsonNode registro = depois.path("itens").get(0);
    assertThat(texto(registro, "id")).isEqualTo(id);
    assertThat(registro.path("aplicado").asBoolean()).isTrue();
    assertThat(registro.path("saldoAnteriorPendente").asBoolean()).isTrue();
  }

  @Test
  void previaObsoletaNaoEConfirmadaDepoisDeNovoRecebimento() throws Exception {
    String perfil = perfil();
    receber(
            perfil,
            "primeiro.xlsx",
            planilha(new String[][] {{"2024-05-01 10:00:00", "Spot", "Deposit", "BTC", "1"}}))
        .andExpect(status().isCreated());
    JsonNode previa = previa(perfil, texto(timeline(perfil), "revisao"));
    receber(
            perfil,
            "segundo.xlsx",
            planilha(new String[][] {{"2024-05-02 10:00:00", "Spot", "Deposit", "BTC", "2"}}))
        .andExpect(status().isCreated());
    confirmar(perfil, previa, List.of(), List.of(), UUID.randomUUID().toString())
        .andExpect(status().isConflict());
    for (JsonNode registro : timeline(perfil).path("itens"))
      assertThat(registro.path("aplicado").asBoolean()).isFalse();
  }

  @Test
  void chaveRepetidaReutilizaRespostaMasRecusaOutroAceite() throws Exception {
    String perfil = perfil();
    receber(
            perfil,
            "saida.xlsx",
            planilha(new String[][] {{"2024-05-02 10:00:00", "Spot", "Withdraw", "BTC", "-0.03"}}))
        .andExpect(status().isCreated());
    JsonNode previa = previa(perfil, texto(timeline(perfil), "revisao"));
    String lacuna = texto(previa.path("ajustes").get(0), "lacunaId");
    String chave = UUID.randomUUID().toString();
    JsonNode primeira =
        corpo(
            confirmar(perfil, previa, List.of(lacuna), List.of(), chave)
                .andExpect(status().isOk()));
    JsonNode repetida =
        corpo(
            confirmar(perfil, previa, List.of(lacuna), List.of(), chave)
                .andExpect(status().isOk()));
    assertThat(repetida).isEqualTo(primeira);
    confirmar(perfil, previa, List.of(), List.of(), chave).andExpect(status().isConflict());
    assertThat(timeline(perfil).path("resumo").path("ajustesAtivos").asInt()).isEqualTo(1);
  }

  @Test
  void saldoFundingNaoFinanciaSaidaSpotSemTransferencia() throws Exception {
    String perfil = perfil();
    receber(
            perfil,
            "custodias.xlsx",
            planilha(
                new String[][] {
                  {"2024-05-01 10:00:00", "Funding", "Deposit", "BTC", "1"},
                  {"2024-05-02 10:00:00", "Spot", "Withdraw", "BTC", "-0.4"}
                }))
        .andExpect(status().isCreated());
    JsonNode previa = previa(perfil, texto(timeline(perfil), "revisao"));
    assertThat(previa.path("correspondencias").isEmpty()).isTrue();
    assertThat(previa.path("ajustes").size()).isEqualTo(1);
    JsonNode ajuste = previa.path("ajustes").get(0);
    assertThat(texto(ajuste, "contaNome")).isEqualTo("Spot");
    assertThat(new BigDecimal(texto(ajuste, "quantidadeSugerida"))).isEqualByComparingTo("0.4");
  }

  @Test
  void transferenciaExplicitaConfirmadaMoveCustodiaSemLacuna() throws Exception {
    String perfil = perfil();
    receber(
            perfil,
            "transferencia.xlsx",
            planilha(
                new String[][] {
                  {"2024-05-01 10:00:00", "Funding", "Deposit", "BTC", "1"},
                  {
                    "2024-05-02 10:00:00",
                    "Funding",
                    "Transfer Between Spot and Funding",
                    "BTC",
                    "-0.4"
                  },
                  {
                    "2024-05-02 10:00:00", "Spot", "Transfer Between Spot and Funding", "BTC", "0.4"
                  },
                  {"2024-05-03 10:00:00", "Spot", "Withdraw", "BTC", "-0.4"}
                }))
        .andExpect(status().isCreated());
    JsonNode previa = previa(perfil, texto(timeline(perfil), "revisao"));
    assertThat(previa.path("correspondencias").size()).isEqualTo(1);
    JsonNode correspondencia = previa.path("correspondencias").get(0);
    assertThat(correspondencia.path("registroIds").size()).isEqualTo(2);
    assertThat(new BigDecimal(texto(correspondencia, "quantidade"))).isEqualByComparingTo("0.4");
    confirmar(
            perfil,
            previa,
            List.of(),
            List.of(texto(correspondencia, "id")),
            UUID.randomUUID().toString())
        .andExpect(status().isOk());
    JsonNode depois = timeline(perfil);
    assertThat(depois.path("resumo").path("ajustesAtivos").asInt()).isZero();
    assertThat(previa(perfil, texto(depois, "revisao")).path("ajustes").isEmpty()).isTrue();
    for (JsonNode registro : depois.path("itens"))
      assertThat(registro.path("aplicado").asBoolean()).isTrue();
  }

  private Map<String, Object> proposta(JsonNode estado, JsonNode registro) {
    var proposta = new LinkedHashMap<String, Object>();
    proposta.put("revisao", texto(estado, "revisao"));
    proposta.put("versao", texto(registro, "versao"));
    for (String campo :
        List.of("data", "contaId", "ativoId", "quantidade", "custoTotalBrl", "natureza")) {
      proposta.put(campo, registro.path(campo).isNull() ? null : texto(registro, campo));
    }
    proposta.put("motivo", "Conferência da origem sintética.");
    return proposta;
  }

  private JsonNode porTicker(JsonNode estado, String ticker) {
    for (JsonNode registro : estado.path("itens"))
      if (ticker.equals(texto(registro, "ativoTicker"))) return registro;
    throw new AssertionError("Ativo esperado ausente da timeline sintética.");
  }

  private String perfil() {
    return "caso-" + UUID.randomUUID();
  }

  private String texto(JsonNode node, String campo) {
    return campo.equals("versao") ? node.path(campo).asString() : node.path(campo).stringValue();
  }

  private ResultActions receber(String perfil, String nome, byte[] bytes) throws Exception {
    return mvc.perform(
        multipart("/api/v1/importacoes/recebimentos")
            .file(
                new MockMultipartFile(
                    "arquivos",
                    nome,
                    "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet",
                    bytes))
            .header("Authorization", bearer(perfil)));
  }

  private JsonNode timeline(String perfil) throws Exception {
    return corpo(
        mvc.perform(get("/api/v1/conciliacao/timeline").header("Authorization", bearer(perfil)))
            .andExpect(status().isOk()));
  }

  private JsonNode previa(String perfil, String revisao) throws Exception {
    return corpo(
        enviar(perfil, "/api/v1/conciliacao/previa", Map.of("revisao", revisao), null)
            .andExpect(status().isOk()));
  }

  private ResultActions revisar(
      String perfil, String id, String revisao, String versao, String chave) throws Exception {
    return enviar(
        perfil,
        "/api/v1/conciliacao/registros/" + id + "/revisao",
        Map.of("revisao", revisao, "versao", versao),
        chave);
  }

  private ResultActions alterar(String perfil, String id, Map<String, Object> proposta)
      throws Exception {
    return mvc.perform(
        put("/api/v1/conciliacao/registros/" + id)
            .header("Authorization", bearer(perfil))
            .header("Idempotency-Key", UUID.randomUUID().toString())
            .contentType("application/json")
            .content(json.writeValueAsString(proposta)));
  }

  private ResultActions confirmar(
      String perfil,
      JsonNode previa,
      List<String> lacunas,
      List<String> correspondencias,
      String chave)
      throws Exception {
    return enviar(
        perfil,
        "/api/v1/conciliacao/confirmacoes",
        Map.of(
            "revisao",
            texto(previa, "revisao"),
            "previaId",
            texto(previa, "previaId"),
            "aceitarLacunaIds",
            lacunas,
            "aceitarCorrespondenciaIds",
            correspondencias),
        chave);
  }

  private ResultActions enviar(String perfil, String rota, Object corpo, String chave)
      throws Exception {
    var request =
        post(rota)
            .header("Authorization", bearer(perfil))
            .contentType("application/json")
            .content(json.writeValueAsString(corpo));
    if (chave != null) request.header("Idempotency-Key", chave);
    return mvc.perform(request);
  }

  private JsonNode corpo(ResultActions acao) throws Exception {
    return json.readTree(acao.andReturn().getResponse().getContentAsString());
  }

  private String bearer(String perfil) {
    var codificador = Base64.getUrlEncoder().withoutPadding();
    return "Bearer "
        + codificador.encodeToString("{\"alg\":\"none\"}".getBytes(StandardCharsets.UTF_8))
        + "."
        + codificador.encodeToString(
            ("{\"sub\":\"" + perfil + "\",\"name\":\"Teste\",\"role\":\"acesso_total\"}")
                .getBytes(StandardCharsets.UTF_8))
        + ".";
  }

  private byte[] planilha(String[][] dados) throws Exception {
    try (var workbook = new XSSFWorkbook();
        var output = new ByteArrayOutputStream()) {
      var sheet = workbook.createSheet("Movimentos");
      sheet.createRow(0).createCell(0).setCellValue("Período (UTC--3)");
      var header = sheet.createRow(1);
      String[] nomes = {"Tempo", "Conta", "Operação", "Moeda", "Alterar"};
      for (int i = 0; i < nomes.length; i++) header.createCell(i).setCellValue(nomes[i]);
      for (int i = 0; i < dados.length; i++) {
        var linha = sheet.createRow(i + 2);
        for (int j = 0; j < dados[i].length; j++) linha.createCell(j).setCellValue(dados[i][j]);
      }
      workbook.write(output);
      return output.toByteArray();
    }
  }
}
