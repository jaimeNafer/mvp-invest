package io.github.jaimenafer.lastro;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.doAnswer;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import io.github.jaimenafer.lastro.importacoes.domain.exception.ImportacaoInvalidaException;
import io.github.jaimenafer.lastro.importacoes.domain.model.Importacao;
import io.github.jaimenafer.lastro.importacoes.domain.repository.IImportacaoRepository;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.Callable;
import java.util.concurrent.Executors;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentMatchers;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.request.MockMultipartHttpServletRequestBuilder;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.mysql.MySQLContainer;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

@SpringBootTest(properties = "spring.config.import=")
@AutoConfigureMockMvc
@Testcontainers
class ImportacoesIntegrationTests {
  @Container @ServiceConnection
  static final MySQLContainer MYSQL = new MySQLContainer("mysql:8.0.42");

  private static final String HEADER =
      "id,datetime_tz_BRT,type,label,market_model_type,order_type,sent_amount,sent_currency,sent_value_BRL,sent_address,received_amount,received_currency,received_value_BRL,received_address,fee_amount,fee_currency,fee_value_BRL";
  @Autowired MockMvc mvc;
  @Autowired JdbcTemplate jdbc;
  @Autowired JsonMapper json;
  @MockitoSpyBean IImportacaoRepository repositorio;

  @Test
  void exclusaoLegadaPreservaOperacaoCompartilhadaComOutroArquivo() throws Exception {
    byte[] arquivo = relatorio("1000", "0.001");
    JsonNode recibo = corpo(confirmar("jaime", arquivo, previa("jaime", arquivo), UUID.randomUUID().toString())
        .andExpect(status().isCreated()));
    long id = Long.parseLong(recibo.path("id").asText());
    jdbc.update("INSERT INTO importacoes(perfil_ref,chave_requisicao,hash,decisoes,nome_arquivo,versao_leitor,arquivo,criado_em,novos,reconhecidos) "
        + "SELECT perfil_ref,UUID(),REPEAT('f',64),decisoes,'outro.csv',versao_leitor,arquivo,criado_em,0,2 FROM importacoes WHERE id=?", id);
    long outro = jdbc.queryForObject("SELECT id FROM importacoes WHERE hash=REPEAT('f',64)", Long.class);
    jdbc.update("INSERT INTO importacoes_eventos(importacao_id,linha,referencia,operacao_id) "
        + "SELECT ?,linha,referencia,operacao_id FROM importacoes_eventos WHERE importacao_id=?", outro, id);
    mvc.perform(delete("/api/v1/importacoes/recebimentos/importacao:" + id)
        .header("Authorization", bearer("jaime"))).andExpect(status().isNoContent());
    assertThat(contar("operacoes")).isEqualTo(2);
    assertThat(contar("importacoes_eventos")).isEqualTo(2);
    mvc.perform(delete("/api/v1/importacoes/recebimentos/importacao:" + outro)
        .header("Authorization", bearer("jaime"))).andExpect(status().isNoContent());
    assertThat(contar("operacoes")).isZero();
  }

  @Test
  void excluiImportacaoLegadaERestauraPossibilidadeDeReimportar() throws Exception {
    byte[] arquivo = relatorio("1000", "0.001");
    JsonNode recibo = corpo(confirmar("jaime", arquivo, previa("jaime", arquivo), UUID.randomUUID().toString())
        .andExpect(status().isCreated()));
    String id = recibo.path("id").asText();
    mvc.perform(delete("/api/v1/importacoes/recebimentos/importacao:" + id)
            .header("Authorization", bearer("jaime")))
        .andExpect(status().isNoContent());
    assertThat(contar("importacoes")).isZero();
    assertThat(contar("importacoes_eventos")).isZero();
    assertThat(contar("operacoes")).isZero();
    JsonNode nova = previa("jaime", arquivo);
    assertThat(nova.path("novos").asInt()).isEqualTo(2);
  }

  @BeforeEach
  void preparar() {
    jdbc.update("DELETE FROM importacoes_eventos");
    jdbc.update("DELETE FROM importacoes");
    jdbc.update("UPDATE operacoes SET substitui_id=NULL");
    jdbc.update("DELETE FROM operacoes");
    jdbc.update("DELETE FROM pontos_partida");
  }

  @Test
  void previaNaoGravaEConfirmacaoPreservaArquivoReciboEVinculos() throws Exception {
    byte[] arquivo = relatorio("1000", "0.001");
    JsonNode previa = previa("jaime", arquivo);
    assertThat(previa.path("novos").asInt()).isEqualTo(2);
    assertThat(contar("operacoes")).isZero();
    assertThat(contar("importacoes")).isZero();
    JsonNode recibo =
        corpo(
            confirmar("jaime", arquivo, previa, UUID.randomUUID().toString())
                .andExpect(status().isCreated()));
    assertThat(recibo.path("novos").asInt()).isEqualTo(2);
    assertThat(recibo.path("eventos").size()).isEqualTo(2);
    assertThat(contar("operacoes")).isEqualTo(2);
    assertThat(contar("importacoes_eventos")).isEqualTo(2);
    assertThat(jdbc.queryForObject("SELECT arquivo FROM importacoes", byte[].class))
        .isEqualTo(arquivo);
    mvc.perform(
            get("/api/v1/importacoes/" + recibo.path("id").asText())
                .header("Authorization", bearer("eliane")))
        .andExpect(status().isNotFound());
    mvc.perform(get("/api/v1/importacoes").header("Authorization", bearer("eliane")))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.total").value(0));
    mvc.perform(get("/api/v1/importacoes").header("Authorization", bearer("jaime")))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.total").value(1));
  }

  @Test
  void repeticaoDaConfirmacaoRetornaMesmoReciboMesmoComRevisaoAntiga() throws Exception {
    byte[] arquivo = relatorio("1000", "0.001");
    JsonNode previa = previa("jaime", arquivo);
    String chave = UUID.randomUUID().toString();
    JsonNode primeira =
        corpo(confirmar("jaime", arquivo, previa, chave).andExpect(status().isCreated()));
    JsonNode repetida =
        corpo(confirmar("jaime", arquivo, previa, chave).andExpect(status().isCreated()));
    assertThat(repetida).isEqualTo(primeira);
    confirmar("jaime", arquivo, previa, UUID.randomUUID().toString())
        .andExpect(status().isConflict());
    assertThat(contar("operacoes")).isEqualTo(2);
    assertThat(contar("importacoes")).isEqualTo(1);
    byte[] divergente =
        new String(relatorio("200", "0.0012"), StandardCharsets.UTF_8)
            .replace("buy,", "buy-nova,")
            .getBytes(StandardCharsets.UTF_8);
    JsonNode outraPrevia = previa("jaime", divergente);
    confirmar("jaime", divergente, outraPrevia, chave).andExpect(status().isConflict());
    assertThat(contar("importacoes")).isEqualTo(1);
  }

  @Test
  void falhaAoGravarReciboReverteTodasAsOperacoesDoLote() throws Exception {
    byte[] arquivo = relatorio("1000", "0.001");
    JsonNode previa = previa("jaime", arquivo);
    doAnswer(
            invocacao -> {
              invocacao.callRealMethod();
              throw new ImportacaoInvalidaException("Falha de confirmação do recibo.");
            })
        .when(repositorio)
        .salvar(ArgumentMatchers.any(Importacao.class));
    confirmar("jaime", arquivo, previa, UUID.randomUUID().toString())
        .andExpect(status().isBadRequest());
    assertThat(contar("operacoes")).isZero();
    assertThat(contar("importacoes_eventos")).isZero();
    assertThat(contar("importacoes")).isZero();
    assertThat(contar("pontos_partida")).isZero();
  }

  @Test
  void confirmacoesConcorrentesNaoDuplicamOperacoesNemRecibos() throws Exception {
    byte[] arquivo = relatorio("1000", "0.001");
    JsonNode previa = previa("jaime", arquivo);
    String chave = UUID.randomUUID().toString();
    Callable<Integer> confirmar =
        () -> confirmar("jaime", arquivo, previa, chave).andReturn().getResponse().getStatus();
    try (var executor = Executors.newFixedThreadPool(2)) {
      var resultados = executor.invokeAll(List.of(confirmar, confirmar));
      assertThat(resultados.get(0).get()).isIn(201, 409);
      assertThat(resultados.get(1).get()).isIn(201, 409);
      assertThat(List.of(resultados.get(0).get(), resultados.get(1).get())).contains(201);
    }
    assertThat(contar("operacoes")).isEqualTo(2);
    assertThat(contar("importacoes")).isEqualTo(1);
    confirmar("jaime", arquivo, previa, chave).andExpect(status().isCreated());
  }

  @Test
  void repeticoesConcorrentesRetornamReciboAposSerializarHistoricoExistente() throws Exception {
    jdbc.update(
        "INSERT INTO"
            + " pontos_partida(perfil_ref,data_referencia,revisao,criado_em,criado_por,alterado_em,alterado_por)"
            + " VALUES ('jaime','2026-01-01',0,NOW(6),'jaime',NOW(6),'jaime')");
    byte[] arquivo = relatorio("1000", "0.001");
    JsonNode previa = previa("jaime", arquivo);
    String chave = UUID.randomUUID().toString();
    var chegaram = new java.util.concurrent.CountDownLatch(2);
    var consultas = new java.util.concurrent.atomic.AtomicInteger();
    doAnswer(
            invocacao -> {
              Object resultado = invocacao.callRealMethod();
              if (consultas.incrementAndGet() <= 2) {
                chegaram.countDown();
                if (!chegaram.await(10, java.util.concurrent.TimeUnit.SECONDS))
                  throw new IllegalStateException("Confirmações não se sobrepuseram no teste.");
              }
              return resultado;
            })
        .when(repositorio)
        .buscarPorChave("jaime", chave);
    Callable<Integer> confirmar =
        () -> confirmar("jaime", arquivo, previa, chave).andReturn().getResponse().getStatus();
    try (var executor = Executors.newFixedThreadPool(2)) {
      var resultados = executor.invokeAll(List.of(confirmar, confirmar));
      assertThat(resultados.get(0).get()).isEqualTo(201);
      assertThat(resultados.get(1).get()).isEqualTo(201);
    }
    assertThat(contar("operacoes")).isEqualTo(2);
    assertThat(contar("importacoes")).isEqualTo(1);
    assertThat(
            jdbc.queryForObject(
                "SELECT revisao FROM pontos_partida WHERE perfil_ref='jaime'", Long.class))
        .isEqualTo(1);
  }

  @Test
  void arquivoMalformadoRetornaDiagnosticosSemPersistir() throws Exception {
    mvc.perform(requisicao("/previa", "jaime", "id,valor\n1,2".getBytes(StandardCharsets.UTF_8)))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.erros").isNotEmpty());
    assertThat(contar("importacoes")).isZero();
    assertThat(contar("operacoes")).isZero();
  }

  @Test
  void previaAlteradaNaoPermiteConfirmacao() throws Exception {
    byte[] arquivo = relatorio("1000", "0.001");
    JsonNode previa = previa("jaime", arquivo);
    mvc.perform(
            requisicao("", "jaime", arquivo)
                .param("hash", previa.path("hash").asText())
                .param("revisao", previa.path("revisao").asText())
                .param("previaId", "invalida")
                .header("Idempotency-Key", UUID.randomUUID().toString()))
        .andExpect(status().isConflict());
    assertThat(contar("operacoes")).isZero();
    assertThat(contar("importacoes")).isZero();
  }

  @Test
  void arquivoSobrepostoReconheceEventosMesmoComHashDiferente() throws Exception {
    byte[] arquivo = relatorio("1000", "0.001");
    confirmar("jaime", arquivo, previa("jaime", arquivo), UUID.randomUUID().toString())
        .andExpect(status().isCreated());
    String deposito =
        String.join(
            ",",
            "dep-2",
            "2026-01-03-10:00:00",
            "Deposit",
            "",
            "FIAT",
            "",
            "",
            "",
            "",
            "",
            "50",
            "BRL",
            "",
            "",
            "0",
            "",
            "");
    byte[] ampliado =
        (new String(arquivo, StandardCharsets.UTF_8) + deposito + "\n")
            .getBytes(StandardCharsets.UTF_8);
    JsonNode previa = previa("jaime", ampliado);
    assertThat(previa.path("novos").asInt()).isEqualTo(1);
    assertThat(previa.path("reconhecidos").asInt()).isEqualTo(2);
    JsonNode recibo =
        corpo(
            confirmar("jaime", ampliado, previa, UUID.randomUUID().toString())
                .andExpect(status().isCreated()));
    assertThat(recibo.path("reconhecidos").asInt()).isEqualTo(2);
    assertThat(contar("operacoes")).isEqualTo(3);
    assertThat(contar("importacoes")).isEqualTo(2);
  }

  @Test
  void fonteAlteradaComMesmaReferenciaERecusada() throws Exception {
    byte[] arquivo = relatorio("1000", "0.001");
    confirmar("jaime", arquivo, previa("jaime", arquivo), UUID.randomUUID().toString())
        .andExpect(status().isCreated());
    mvc.perform(requisicao("/previa", "jaime", relatorio("200", "0.0012")))
        .andExpect(status().isConflict());
    assertThat(contar("operacoes")).isEqualTo(2);
    assertThat(contar("importacoes")).isEqualTo(1);
  }

  @Test
  void reconheceIdentidadeLegadaPreservandoOperacoesExistentes() throws Exception {
    byte[] arquivo = relatorio("1000", "0.001");
    JsonNode previa = previa("jaime", arquivo);
    JsonNode recibo =
        corpo(
            confirmar("jaime", arquivo, previa, UUID.randomUUID().toString())
                .andExpect(status().isCreated()));
    for (JsonNode evento : recibo.path("eventos")) {
      String chave =
          io.github.jaimenafer.lastro.importacoes.application.mapper.IdentidadeImportacao.legada(
              previa.path("hash").asText(), evento.path("referencia").asText());
      jdbc.update(
          "UPDATE operacoes SET chave_requisicao=? WHERE id=?",
          chave,
          evento.path("operacaoId").asText());
    }
    jdbc.update("DELETE FROM importacoes_eventos");
    jdbc.update("DELETE FROM importacoes");
    JsonNode reconhecida = previa("jaime", arquivo);
    assertThat(reconhecida.path("reconhecidos").asInt()).isEqualTo(2);
    confirmar("jaime", arquivo, reconhecida, UUID.randomUUID().toString())
        .andExpect(status().isCreated());
    assertThat(contar("operacoes")).isEqualTo(2);
    String compraId =
        jdbc.queryForObject("SELECT id FROM operacoes WHERE tipo='COMPRA'", String.class);
    long revisao =
        jdbc.queryForObject(
            "SELECT revisao FROM pontos_partida WHERE perfil_ref='jaime'", Long.class);
    mvc.perform(
            post("/api/v1/operacoes/" + compraId + "/cancelamento")
                .header("Authorization", bearer("jaime"))
                .contentType("application/json")
                .content(
                    json.writeValueAsString(
                        java.util.Map.of("revisao", revisao, "motivo", "Cancelamento conferido"))))
        .andExpect(status().isOk());
    byte[] sobreposto =
        (new String(arquivo, StandardCharsets.UTF_8)
                + String.join(
                    ",",
                    "dep-2",
                    "2026-01-03-10:00:00",
                    "Deposit",
                    "",
                    "FIAT",
                    "",
                    "",
                    "",
                    "",
                    "",
                    "50",
                    "BRL",
                    "",
                    "",
                    "0",
                    "",
                    "")
                + "\n")
            .getBytes(StandardCharsets.UTF_8);
    JsonNode outraPrevia = previa("jaime", sobreposto);
    assertThat(outraPrevia.path("reconhecidos").asInt()).isEqualTo(2);
    confirmar("jaime", sobreposto, outraPrevia, UUID.randomUUID().toString())
        .andExpect(status().isCreated());
    assertThat(contar("operacoes")).isEqualTo(3);
    assertThat(
            jdbc.queryForObject(
                "SELECT situacao FROM operacoes WHERE id=?", String.class, compraId))
        .isEqualTo("CANCELADA");
  }

  @Test
  void migrationDocumentaColunasEPreservaVinculosFinanceiros() {
    Long semComentario =
        jdbc.queryForObject(
            "SELECT COUNT(*) FROM information_schema.columns WHERE table_schema=DATABASE() AND"
                + " table_name IN ('importacoes','importacoes_eventos') AND column_comment=''",
            Long.class);
    assertThat(semComentario).isZero();
    assertThat(
            jdbc.queryForObject(
                "SELECT COUNT(*) FROM information_schema.referential_constraints WHERE"
                    + " constraint_schema=DATABASE() AND table_name='importacoes_eventos'",
                Long.class))
        .isEqualTo(2);
  }

  @Test
  void importaCincoMilEventosSemTruncarOuDuplicar() throws Exception {
    var csv = new StringBuilder(HEADER).append("\n");
    for (int i = 0; i < 5000; i++) {
      csv.append(
              String.join(
                  ",",
                  "deposito-" + i,
                  "2026-01-01-10:00:00",
                  "Deposit",
                  "",
                  "FIAT",
                  "",
                  "",
                  "",
                  "",
                  "",
                  "1",
                  "BRL",
                  "",
                  "",
                  "0",
                  "",
                  ""))
          .append("\n");
    }
    byte[] arquivo = csv.toString().getBytes(StandardCharsets.UTF_8);
    JsonNode previa = previa("jaime", arquivo);
    assertThat(previa.path("novos").asInt()).isEqualTo(5000);
    String chave = UUID.randomUUID().toString();
    JsonNode recibo =
        corpo(confirmar("jaime", arquivo, previa, chave).andExpect(status().isCreated()));
    assertThat(recibo.path("eventos").size()).isEqualTo(5000);
    assertThat(contar("operacoes")).isEqualTo(5000);
    assertThat(
            jdbc.queryForObject(
                "SELECT SUM(quantidade) FROM operacoes", java.math.BigDecimal.class))
        .isEqualByComparingTo("5000");
    confirmar("jaime", arquivo, previa, chave).andExpect(status().isCreated());
    assertThat(contar("operacoes")).isEqualTo(5000);
  }

  long contar(String tabela) {
    return jdbc.queryForObject("SELECT COUNT(*) FROM " + tabela, Long.class);
  }

  JsonNode previa(String perfil, byte[] arquivo) throws Exception {
    return corpo(mvc.perform(requisicao("/previa", perfil, arquivo)).andExpect(status().isOk()));
  }

  ResultActions confirmar(String perfil, byte[] arquivo, JsonNode previa, String chave)
      throws Exception {
    return mvc.perform(
        requisicao("", perfil, arquivo)
            .param("hash", previa.path("hash").asText())
            .param("revisao", previa.path("revisao").asText())
            .param("previaId", previa.path("previaId").asText())
            .header("Idempotency-Key", chave));
  }

  MockMultipartHttpServletRequestBuilder requisicao(String caminho, String perfil, byte[] arquivo) {
    return multipart("/api/v1/importacoes" + caminho)
        .file(new MockMultipartFile("arquivo", "relatorio.csv", "text/csv", arquivo))
        .param("aportesProprios", "true")
        .param("taxasIncluidas", "true")
        .param("posicaoAnteriorZerada", "true")
        .header("Authorization", bearer(perfil));
  }

  JsonNode corpo(ResultActions resultado) throws Exception {
    return json.readTree(resultado.andReturn().getResponse().getContentAsString());
  }

  String bearer(String perfil) {
    var encoder = Base64.getUrlEncoder().withoutPadding();
    return "Bearer "
        + encoder.encodeToString("{\"alg\":\"none\"}".getBytes(StandardCharsets.UTF_8))
        + "."
        + encoder.encodeToString(
            ("{\"sub\":\"" + perfil + "\",\"name\":\"Teste\",\"role\":\"acesso_total\"}")
                .getBytes(StandardCharsets.UTF_8))
        + ".";
  }

  byte[] relatorio(String principal, String quantidade) {
    String deposito =
        String.join(
            ",",
            "dep",
            "2026-01-01-10:00:00",
            "Deposit",
            "",
            "FIAT",
            "",
            "",
            "",
            "",
            "",
            "2000",
            "BRL",
            "",
            "",
            "0",
            "",
            "");
    String compra =
        String.join(
            ",",
            "buy",
            "2026-01-02-10:00:00",
            "Buy",
            "",
            "OCBS",
            "",
            principal,
            "BRL",
            "",
            "",
            quantidade,
            "BTC",
            "",
            "",
            "0",
            "BRL",
            "");
    return (HEADER + "\n" + deposito + "\n" + compra + "\n").getBytes(StandardCharsets.UTF_8);
  }
}
