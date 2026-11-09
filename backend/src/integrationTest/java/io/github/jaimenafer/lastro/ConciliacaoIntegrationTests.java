package io.github.jaimenafer.lastro;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.doAnswer;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import java.io.ByteArrayOutputStream;
import java.nio.charset.StandardCharsets;
import java.util.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.web.servlet.*;
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean;
import io.github.jaimenafer.lastro.importacoes.domain.repository.IArquivoRecebidoRepository;
import org.testcontainers.junit.jupiter.*;
import org.testcontainers.mysql.MySQLContainer;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

@SpringBootTest(properties = "spring.config.import=")
@AutoConfigureMockMvc
@Testcontainers
class ConciliacaoIntegrationTests {
  @Container @ServiceConnection
  static final MySQLContainer MYSQL = new MySQLContainer("mysql:8.0.42");

  @Autowired MockMvc mvc;
  @Autowired JdbcTemplate jdbc;
  @Autowired JsonMapper json;
  @MockitoSpyBean IArquivoRecebidoRepository arquivosRecebidos;

  @Test
  void arquivosSemMovimentacoesNaoCriamRecebimentosNemRegistros() throws Exception {
    String perfil = "vazios-" + UUID.randomUUID();
    String cabecalho = "id,datetime_tz_BRT,type,label,market_model_type,order_type,sent_amount,sent_currency,sent_value_BRL,sent_address,received_amount,received_currency,received_value_BRL,received_address,fee_amount,fee_currency,fee_value_BRL";
    JsonNode resposta = corpo(mvc.perform(multipart("/api/v1/importacoes/recebimentos")
        .file(new MockMultipartFile("arquivos", "vazio.csv", "text/csv", (cabecalho + "\n,,,,,,,,,,,,,,,,\n").getBytes(StandardCharsets.UTF_8)))
        .file(new MockMultipartFile("arquivos", "vazio.xlsx", "application/octet-stream", planilha(new String[0][])))
        .header("Authorization", bearer(perfil))).andExpect(status().isCreated()));
    assertThat(resposta.path("problemas").asInt()).isEqualTo(2);
    for (JsonNode arquivo : resposta.path("arquivos"))
      assertThat(arquivo.path("erro").asText()).contains("não contém movimentações");
    assertThat(timeline(perfil).path("total").asInt()).isZero();
    assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM arquivos_recebidos WHERE perfil_ref=?", Long.class, perfil)).isZero();
  }

  @Test
  void excluirSaldoAnteriorManualPreservaRegistroImportado() throws Exception {
    verificarExclusaoSaldoAnterior(false);
  }

  @Test
  void excluirSaldoAnteriorConfirmadoRecalculaHistorico() throws Exception {
    verificarExclusaoSaldoAnterior(true);
  }

  private void verificarExclusaoSaldoAnterior(boolean confirmado) throws Exception {
    String perfil = "manual-conferencia-" + UUID.randomUUID();
    receber(perfil, planilha(new String[][] {{"2024-05-02 10:00:00", "Spot", "Deposit", "BTC", "1"}}))
        .andExpect(status().isCreated());
    JsonNode antes = timeline(perfil);
    JsonNode importado = antes.path("itens").get(0);
    enviar(perfil, "/api/v1/conciliacao/saldos-anteriores", Map.of(
        "revisao", antes.path("revisao").asText(), "ancoraId", importado.path("id").asText(),
        "contaId", importado.path("contaId").asText(), "ativoId", importado.path("ativoId").asText(),
        "data", "2024-05-01T10:00:00-03:00", "quantidade", "2", "motivo", "Saldo informado"),
        UUID.randomUUID().toString()).andExpect(status().isOk());
    if (confirmado) aplicar(perfil);
    JsonNode comManual = timeline(perfil);
    JsonNode manual = null;
    for (JsonNode item : comManual.path("itens")) if ("MANUAL".equals(item.path("origem").asText())) manual = item;
    assertThat(manual).isNotNull();
    if (confirmado) {
      assertThat(manual.path("aplicado").asBoolean()).isTrue();
      assertThat(quantidadeBtc(perfil)).isEqualByComparingTo("3");
    }
    mvc.perform(delete("/api/v1/conciliacao/registros/" + manual.path("id").asText())
        .param("revisao", antes.path("revisao").asText())
        .header("Authorization", bearer(perfil))).andExpect(status().isConflict());
    mvc.perform(delete("/api/v1/conciliacao/registros/" + manual.path("id").asText())
        .param("revisao", "0")
        .header("Authorization", bearer("outro-" + UUID.randomUUID()))).andExpect(status().isNotFound());
    mvc.perform(delete("/api/v1/conciliacao/registros/" + manual.path("id").asText())
        .param("revisao", comManual.path("revisao").asText())
        .header("Authorization", bearer(perfil))).andExpect(status().isNoContent());
    JsonNode depois = timeline(perfil);
    if (confirmado) assertThat(quantidadeBtc(perfil)).isEqualByComparingTo("1");
    assertThat(depois.path("total").asInt()).isEqualTo(1);
    assertThat(depois.path("itens").get(0).path("id").asText()).isEqualTo(importado.path("id").asText());
    mvc.perform(delete("/api/v1/conciliacao/registros/" + importado.path("id").asText())
        .param("revisao", depois.path("revisao").asText())
        .header("Authorization", bearer(perfil))).andExpect(status().isConflict());
  }

  private java.math.BigDecimal quantidadeBtc(String perfil) throws Exception {
    JsonNode posicoes = corpo(mvc.perform(get("/api/v1/posicoes")
        .header("Authorization", bearer(perfil))).andExpect(status().isOk()));
    java.math.BigDecimal quantidade = java.math.BigDecimal.ZERO;
    for (JsonNode posicao : posicoes.path("itens"))
      if ("BTC".equals(posicao.path("ativoTicker").asText()))
        quantidade = quantidade.add(new java.math.BigDecimal(posicao.path("quantidade").asText()));
    return quantidade;
  }

  @Test
  void excluirArquivoRemoveRespostasDeComandosDosRegistrosExcluidos() throws Exception {
    String perfil = "exclusao-comando-" + UUID.randomUUID();
    byte[] arquivo = planilha(new String[][] {{"2024-05-02 10:00:00", "Spot", "Deposit", "BRL", "100"}});
    String id = corpo(receber(perfil, arquivo).andExpect(status().isCreated())).path("arquivos").get(0).path("id").asText();
    JsonNode atual = timeline(perfil);
    JsonNode registro = atual.path("itens").get(0);
    String registroId = registro.path("id").asText();
    editarRegistro(perfil, atual, registro, Map.of("motivo", "Correção de teste"));
    assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM comandos_conciliacao WHERE perfil_ref=? AND JSON_UNQUOTE(JSON_EXTRACT(resposta_json, '$.id'))=?", Long.class, perfil, registroId)).isEqualTo(1);
    mvc.perform(delete("/api/v1/importacoes/recebimentos/" + id).header("Authorization", bearer(perfil)))
        .andExpect(status().isNoContent());
    assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM comandos_conciliacao WHERE perfil_ref=? AND JSON_UNQUOTE(JSON_EXTRACT(resposta_json, '$.id'))=?", Long.class, perfil, registroId)).isZero();
  }

  @Test
  void exclusoesConcorrentesNaoAfetamOutroArquivo() throws Exception {
    String perfil = "exclusao-concorrente-" + UUID.randomUUID();
    byte[] primeiro = planilha(new String[][] {{"2024-05-02 10:00:00", "Spot", "Deposit", "BRL", "100"}});
    byte[] segundo = planilha(new String[][] {{"2024-05-03 10:00:00", "Spot", "Deposit", "BRL", "50"}});
    String id = corpo(receber(perfil, primeiro).andExpect(status().isCreated())).path("arquivos").get(0).path("id").asText();
    receber(perfil, segundo).andExpect(status().isCreated());
    try (var executor = java.util.concurrent.Executors.newFixedThreadPool(2)) {
      java.util.concurrent.Callable<Integer> excluir = () -> mvc.perform(delete("/api/v1/importacoes/recebimentos/" + id)
          .header("Authorization", bearer(perfil))).andReturn().getResponse().getStatus();
      var resultados = executor.invokeAll(List.of(excluir, excluir));
      assertThat(List.of(resultados.get(0).get(), resultados.get(1).get())).containsExactlyInAnyOrder(204, 404);
    }
    assertThat(timeline(perfil).path("total").asInt()).isEqualTo(1);
    assertThat(timeline(perfil).path("itens").get(0).path("quantidade").asText()).isEqualTo("50");
  }

  @Test
  void exclusaoRemoveValoracoesVinculadasQuandoTabelasDaVersao16Existem() throws Exception {
    String perfil = "exclusao-valoracao-" + UUID.randomUUID();
    byte[] arquivo = planilha(new String[][] {{"2024-05-02 10:00:00", "Spot", "Deposit", "BRL", "100"}});
    String id = corpo(receber(perfil, arquivo).andExpect(status().isCreated())).path("arquivos").get(0).path("id").asText();
    long registro = jdbc.queryForObject("SELECT id FROM registros_conferencia WHERE perfil_ref=?", Long.class, perfil);
    jdbc.update("INSERT INTO valoracoes_manuais(perfil_ref,registro_id,valor_brl,versao,alterado_por,alterado_em) VALUES (?,?,1,1,?,UTC_TIMESTAMP(6))", perfil, registro, perfil);
    long valoracao = jdbc.queryForObject("SELECT id FROM valoracoes_manuais WHERE perfil_ref=? AND registro_id=?", Long.class, perfil, registro);
    try {
      jdbc.update("INSERT INTO historico_valoracoes_manuais(valoracao_id,versao,valor_brl,alterado_por,alterado_em) VALUES (?,1,1,?,UTC_TIMESTAMP(6))", valoracao, perfil);
      mvc.perform(delete("/api/v1/importacoes/recebimentos/" + id).header("Authorization", bearer(perfil)))
          .andExpect(status().isNoContent());
      assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM valoracoes_manuais WHERE id=?", Long.class, valoracao)).isZero();
      assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM historico_valoracoes_manuais WHERE valoracao_id=?", Long.class, valoracao)).isZero();
    } finally {
      jdbc.update("DELETE FROM historico_valoracoes_manuais WHERE valoracao_id=?", valoracao);
      jdbc.update("DELETE FROM valoracoes_manuais WHERE id=?", valoracao);
    }
  }

  @Test
  void falhaAoExcluirArquivoReverteExclusaoDosRegistros() throws Exception {
    String perfil = "exclusao-falha-" + UUID.randomUUID();
    byte[] arquivo = planilha(new String[][] {{"2024-05-02 10:00:00", "Spot", "Deposit", "BRL", "100"}});
    String id = corpo(receber(perfil, arquivo).andExpect(status().isCreated())).path("arquivos").get(0).path("id").asText();
    doAnswer(invocacao -> {
      invocacao.callRealMethod();
      throw new IllegalStateException("Falha de teste após exclusão do arquivo");
    }).when(arquivosRecebidos).excluir(org.mockito.ArgumentMatchers.any());
    assertThatThrownBy(() -> mvc.perform(delete("/api/v1/importacoes/recebimentos/" + id)
        .header("Authorization", bearer(perfil))).andReturn())
        .isInstanceOf(jakarta.servlet.ServletException.class);
    assertThat(timeline(perfil).path("total").asInt()).isEqualTo(1);
    assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM arquivos_recebidos WHERE perfil_ref = ?", Long.class, perfil)).isEqualTo(1);
  }

  @Test
  void excluirArquivoConfirmadoRemoveSeuEfeitoSemApagarOutroArquivo() throws Exception {
    String perfil = "exclusao-confirmado-" + UUID.randomUUID();
    byte[] primeiro = planilha(new String[][] {{"2024-05-02 10:00:00", "Spot", "Deposit", "BRL", "100"}});
    byte[] segundo = planilha(new String[][] {{"2024-05-03 10:00:00", "Spot", "Deposit", "BRL", "50"}});
    String id = corpo(receber(perfil, primeiro).andExpect(status().isCreated()))
        .path("arquivos").get(0).path("id").asText();
    receber(perfil, segundo).andExpect(status().isCreated());
    aplicar(perfil);
    mvc.perform(delete("/api/v1/importacoes/recebimentos/" + id)
        .header("Authorization", bearer(perfil))).andExpect(status().isNoContent());
    JsonNode restante = timeline(perfil);
    assertThat(restante.path("total").asInt()).isEqualTo(1);
    assertThat(restante.path("itens").get(0).path("quantidade").asText()).isEqualTo("50");
    assertThat(restante.path("itens").get(0).path("aplicado").asBoolean()).isTrue();
    assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM arquivos_recebidos WHERE perfil_ref = ?", Long.class, perfil)).isEqualTo(1);
  }

  @Test
  void excluirUmaPernaDeGrupoPreservaOutraParaConferencia() throws Exception {
    String perfil = "exclusao-grupo-" + UUID.randomUUID();
    byte[] primeiro = planilha(new String[][] {{"2024-06-01 10:00:00", "Spot", "Buy Crypto With Fiat", "BRL", "-100"}});
    byte[] segundo = planilha(new String[][] {{"2024-06-01 10:00:01", "Spot", "Buy Crypto With Fiat", "BTC", "1"}});
    String id = corpo(receber(perfil, primeiro).andExpect(status().isCreated())).path("arquivos").get(0).path("id").asText();
    receber(perfil, segundo).andExpect(status().isCreated());
    aplicar(perfil);
    assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM grupos_associacao WHERE perfil_ref = ?", Long.class, perfil)).isEqualTo(1);
    mvc.perform(delete("/api/v1/importacoes/recebimentos/" + id).header("Authorization", bearer(perfil)))
        .andExpect(status().isNoContent());
    JsonNode restante = timeline(perfil);
    assertThat(restante.path("total").asInt()).isEqualTo(1);
    assertThat(restante.path("itens").get(0).path("ativoTicker").asText()).isEqualTo("BTC");
    assertThat(restante.path("itens").get(0).path("aplicado").asBoolean()).isFalse();
  }

  @Test
  void listaArquivosRecebidosSomenteDoProprietario() throws Exception {
    String perfil = "lista-" + UUID.randomUUID();
    byte[] arquivo = planilha(new String[][] {{"2024-05-02 10:00:00", "Spot", "Deposit", "BRL", "100"}});
    JsonNode recebido = corpo(receber(perfil, arquivo).andExpect(status().isCreated()));
    JsonNode lista = corpo(mvc.perform(get("/api/v1/importacoes/recebimentos")
        .header("Authorization", bearer(perfil))).andExpect(status().isOk()));
    assertThat(lista.path("itens").size()).isEqualTo(1);
    assertThat(lista.path("itens").get(0).path("id").asText()).isEqualTo(recebido.path("arquivos").get(0).path("id").asText());
    assertThat(lista.path("itens").get(0).path("nomeArquivo").asText()).isEqualTo("sintetico.xlsx");
    assertThat(lista.path("itens").get(0).path("registros").asInt()).isEqualTo(1);
    assertThat(lista.path("itens").get(0).path("criadoEm").asText()).isNotBlank();
    JsonNode outro = corpo(mvc.perform(get("/api/v1/importacoes/recebimentos")
        .header("Authorization", bearer("outro-" + UUID.randomUUID()))).andExpect(status().isOk()));
    assertThat(outro.path("itens").size()).isZero();
  }

  @Test
  void excluirArquivoRecebidoRemoveRegistrosEPermiteReimportacao() throws Exception {
    String perfil = "exclusao-" + UUID.randomUUID();
    byte[] arquivo = planilha(new String[][] {{"2024-05-02 10:00:00", "Spot", "Deposit", "BRL", "100"}});
    JsonNode recebido = corpo(receber(perfil, arquivo).andExpect(status().isCreated()));
    String id = recebido.path("arquivos").get(0).path("id").asText();
    mvc.perform(delete("/api/v1/importacoes/recebimentos/" + id)
            .header("Authorization", bearer("outro-" + UUID.randomUUID())))
        .andExpect(status().isNotFound());
    mvc.perform(delete("/api/v1/importacoes/recebimentos/" + id)
            .header("Authorization", bearer(perfil)))
        .andExpect(status().isNoContent());
    assertThat(timeline(perfil).path("total").asInt()).isZero();
    assertThat(corpo(receber(perfil, arquivo).andExpect(status().isCreated()))
            .path("arquivos").get(0).path("reconhecido").asBoolean())
        .isFalse();
  }

  @Test
  void recebimentoCadastraAtivoNovoEReutilizaNoMesmoArquivo() throws Exception {
    String perfil = "ativo-novo-" + UUID.randomUUID();
    String ticker = "NOVO" + UUID.randomUUID().toString().replace("-", "").substring(0, 12).toUpperCase();
    receber(
            perfil,
            planilha(
                new String[][] {
                  {"2024-07-19 10:00:00", "Spot", "Airdrop Assets", ticker, "0.00677182"},
                  {"2024-07-20 10:00:00", "Spot", "Airdrop Assets", ticker, "0.1"}
                }))
        .andExpect(status().isCreated());
    assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM ativos WHERE ticker = ?", Long.class, ticker))
        .isEqualTo(1);
    assertThat(jdbc.queryForObject("SELECT nome FROM ativos WHERE ticker = ?", String.class, ticker))
        .isNull();
    assertThat(jdbc.queryForObject(
            "SELECT COUNT(DISTINCT JSON_UNQUOTE(JSON_EXTRACT(proposta_json, '$.ativoId'))) FROM registros_conferencia WHERE perfil_ref = ?",
            Long.class, perfil))
        .isEqualTo(1);
    assertThat(timeline(perfil).toString()).contains(ticker);
  }

  @Test
  void importacoesConcorrentesReutilizamAtivoCriado() throws Exception {
    String ticker = "N" + UUID.randomUUID().toString().replace("-", "").substring(0, 12).toUpperCase();
    byte[] arquivo = planilha(new String[][] {{"2024-07-19 10:00:00", "Spot", "Deposit", ticker, "1"}});
    try (var executor = java.util.concurrent.Executors.newFixedThreadPool(2)) {
      var primeira = executor.submit(() -> receber("a-" + UUID.randomUUID(), arquivo).andReturn().getResponse().getStatus());
      var segunda = executor.submit(() -> receber("b-" + UUID.randomUUID(), arquivo).andReturn().getResponse().getStatus());
      assertThat(primeira.get()).isEqualTo(201);
      assertThat(segunda.get()).isEqualTo(201);
    }
    assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM ativos WHERE ticker = ?", Long.class, ticker))
        .isEqualTo(1);
  }

  @Test
  void recebePreservaInvalidaSugereAjusteEEncerraComEntradaReal() throws Exception {
    byte[] arquivo =
        planilha(
            new String[][] {
              {"2024-05-02 10:00:00", "Spot", "Withdraw", "BTC", "-0.03"},
              {"data inválida", "Spot", "Deposit", "BRL", "100"}
            });
    JsonNode recebido = corpo(receber("fluxo", arquivo).andExpect(status().isCreated()));
    assertThat(recebido.path("recebidos").asInt()).isEqualTo(2);
    JsonNode timeline = timeline("fluxo");
    assertThat(timeline.path("total").asInt()).isEqualTo(2);
    JsonNode previa = previa("fluxo", timeline.path("revisao").asText());
    assertThat(previa.path("ajustes").size()).isEqualTo(1);
    assertThat(previa.path("ajustes").get(0).path("quantidadeSugerida").asText()).isEqualTo("0.03");
    String chave = UUID.randomUUID().toString();
    var comando =
        Map.of(
            "revisao",
            previa.path("revisao").asText(),
            "previaId",
            previa.path("previaId").asText(),
            "aceitarLacunaIds",
            List.of(previa.path("ajustes").get(0).path("lacunaId").asText()),
            "aceitarCorrespondenciaIds",
            List.of());
    confirmar("fluxo", comando, chave).andExpect(status().isOk());
    confirmar("fluxo", comando, chave).andExpect(status().isOk());
    assertThat(timeline("fluxo").path("resumo").path("ajustesAtivos").asInt()).isEqualTo(1);
    assertThat(timeline("outro").path("total").asInt()).isZero();
    byte[] origem =
        planilha(new String[][] {{"2024-05-01 10:00:00", "Spot", "Deposit", "BTC", "0.03"}});
    receber("fluxo", origem).andExpect(status().isCreated());
    JsonNode nova = previa("fluxo", timeline("fluxo").path("revisao").asText());
    assertThat(nova.path("ajustes").get(0).path("acao").asText()).isEqualTo("ENCERRAR");
    confirmar(
            "fluxo",
            Map.of(
                "revisao",
                nova.path("revisao").asText(),
                "previaId",
                nova.path("previaId").asText(),
                "aceitarLacunaIds",
                List.of(nova.path("ajustes").get(0).path("lacunaId").asText()),
                "aceitarCorrespondenciaIds",
                List.of()),
            UUID.randomUUID().toString())
        .andExpect(status().isOk());
    assertThat(timeline("fluxo").path("resumo").path("ajustesAtivos").asInt()).isZero();
    assertThat(
            jdbc.queryForObject(
                "SELECT COUNT(*) FROM ajustes_conciliatorios WHERE perfil_ref='fluxo'", Long.class))
        .isEqualTo(1);
  }

  @Test
  void csvPreservaAsDuasPernasSemTransformarAvaliacaoEmCusto() throws Exception {
    String perfil = "csv-" + UUID.randomUUID();
    String csv =
        "id,datetime_tz_BRT,type,label,market_model_type,order_type,sent_amount,sent_currency,sent_value_BRL,sent_address,received_amount,received_currency,received_value_BRL,received_address,fee_amount,fee_currency,fee_value_BRL\n"
            + "evento,2024-05-02-10:00:00,Buy,,CONVERT,,100,BRL,100,,0.001,BTC,100,,,,\n";
    receber(perfil, csv.getBytes(StandardCharsets.UTF_8)).andExpect(status().isCreated());
    JsonNode previa = previa(perfil, timeline(perfil).path("revisao").asText());
    assertThat(previa.path("ajustes").size()).isEqualTo(1);
    assertThat(previa.path("ajustes").get(0).path("ativoTicker").asText()).isEqualTo("BRL");
    assertThat(previa.path("ajustes").get(0).path("quantidadeSugerida").asText()).isEqualTo("100");
  }

  @Test
  void confirmacoesConcorrentesReutilizamMesmoComandoSemDuplicarAjuste() throws Exception {
    String perfil = "concorrente-" + UUID.randomUUID();
    receber(
            perfil,
            planilha(new String[][] {{"2024-05-02 10:00:00", "Spot", "Withdraw", "BTC", "-1"}}))
        .andExpect(status().isCreated());
    JsonNode previa = previa(perfil, timeline(perfil).path("revisao").asText());
    Map<String, Object> comando =
        Map.of(
            "revisao",
            previa.path("revisao").asText(),
            "previaId",
            previa.path("previaId").asText(),
            "aceitarLacunaIds",
            List.of(previa.path("ajustes").get(0).path("lacunaId").asText()),
            "aceitarCorrespondenciaIds",
            List.of());
    String chave = UUID.randomUUID().toString();
    try (var executor = java.util.concurrent.Executors.newFixedThreadPool(2)) {
      var a =
          executor.submit(
              () -> confirmar(perfil, comando, chave).andReturn().getResponse().getStatus());
      var b =
          executor.submit(
              () -> confirmar(perfil, comando, chave).andReturn().getResponse().getStatus());
      assertThat(a.get()).isEqualTo(200);
      assertThat(b.get()).isEqualTo(200);
    }
    assertThat(timeline(perfil).path("resumo").path("ajustesAtivos").asInt()).isEqualTo(1);
  }

  @Test
  void sobreposicaoMantemMultiplicidadeSemAplicarOcorrenciasAmbiguas() throws Exception {
    String perfil = "sobreposicao-" + UUID.randomUUID();
    String[] repetida = {"2024-05-02 10:00:00", "Spot", "Deposit", "BTC", "1"};
    receber(perfil, planilha(new String[][] {repetida, repetida})).andExpect(status().isCreated());
    receber(
            perfil,
            planilha(
                new String[][] {repetida, {"2024-05-03 10:00:00", "Spot", "Deposit", "ETH", "2"}}))
        .andExpect(status().isCreated());
    JsonNode atual = timeline(perfil);
    assertThat(atual.path("total").asInt()).isEqualTo(4);
    JsonNode previa = previa(perfil, atual.path("revisao").asText());
    assertThat(previa.path("registrosParaAplicar").asInt()).isEqualTo(1);
  }

  @Test
  void recebimentoAnteriorNaoAlteraDataDaAberturaConfirmada() throws Exception {
    String perfil = "abertura-" + UUID.randomUUID();
    long inst =
        jdbc.queryForObject("SELECT id FROM instituicoes WHERE codigo='binance'", Long.class);
    long btc = jdbc.queryForObject("SELECT id FROM ativos WHERE codigo='cripto:BTC'", Long.class);
    jdbc.update(
        "INSERT INTO"
            + " pontos_partida(perfil_ref,data_referencia,criado_em,criado_por,alterado_em,alterado_por)"
            + " VALUES (?,'2024-06-01',UTC_TIMESTAMP(6),?,UTC_TIMESTAMP(6),?)",
        perfil,
        perfil,
        perfil);
    jdbc.update(
        "INSERT INTO"
            + " operacoes(perfil_ref,instituicao_id,ativo_id,tipo,data_referencia,ocorrida_em,quantidade,custo_total_brl,situacao,chave_requisicao,fingerprint,criado_em,criado_por,alterado_em,alterado_por,ordem_livro)"
            + " VALUES (?,?,?,'SALDO_INICIAL','2024-06-01','2024-06-01"
            + " 03:00:00',1,100,'CONFIRMADA',UUID(),REPEAT('a',64),UTC_TIMESTAMP(6),?,UTC_TIMESTAMP(6),?,1)",
        perfil,
        inst,
        btc,
        perfil,
        perfil);
    receber(
            perfil,
            planilha(new String[][] {{"2024-05-01 10:00:00", "Spot", "Deposit", "BTC", "1"}}))
        .andExpect(status().isCreated());
    assertThat(
            jdbc.queryForObject(
                "SELECT data_referencia FROM pontos_partida WHERE perfil_ref=?",
                String.class,
                perfil))
        .isEqualTo("2024-06-01");
    assertThat(
            previa(perfil, timeline(perfil).path("revisao").asText())
                .path("registrosParaAplicar")
                .asInt())
        .isZero();
  }

  @Test
  void propostaIlegivelNaoEncerraAjusteDeSaidaAindaAplicada() throws Exception {
    String perfil = "ilegivel-" + UUID.randomUUID();
    receber(
            perfil,
            planilha(new String[][] {{"2024-05-02 10:00:00", "Spot", "Withdraw", "BTC", "-1"}}))
        .andExpect(status().isCreated());
    aplicar(perfil);
    JsonNode t = timeline(perfil), registro = porTipo(t, "Withdraw");
    var campos = new LinkedHashMap<String, Object>();
    campos.put("quantidade", null);
    editarRegistro(perfil, t, registro, campos);
    JsonNode p = previa(perfil, timeline(perfil).path("revisao").asText());
    assertThat(p.path("ajustes").get(0).path("acao").asText()).isEqualTo("MANTER");
  }

  @Test
  void lacunaEncerradaPodeReabrirEmNovaVersao() throws Exception {
    String perfil = "reabertura-" + UUID.randomUUID();
    receber(
            perfil,
            planilha(new String[][] {{"2024-05-02 10:00:00", "Spot", "Withdraw", "BTC", "-1"}}))
        .andExpect(status().isCreated());
    aplicar(perfil);
    receber(
            perfil,
            planilha(new String[][] {{"2024-05-01 10:00:00", "Spot", "Deposit", "BTC", "1"}}))
        .andExpect(status().isCreated());
    aplicar(perfil);
    JsonNode t = timeline(perfil);
    editarRegistro(perfil, t, porTipo(t, "Deposit"), Map.of("quantidade", "0.5"));
    aplicar(perfil);
    JsonNode depois = timeline(perfil);
    assertThat(depois.path("resumo").path("ajustesAtivos").asInt()).isEqualTo(1);
    JsonNode ajuste = porTipo(depois, "AJUSTE");
    assertThat(ajuste.path("versao").asText()).isEqualTo("2");
    assertThat(ajuste.path("aceitoPor").asText()).isEqualTo(perfil);
  }

  @Test
  void referenciaReexportadaNaoDuplicaMesmoComOutrosCamposDiferentes() throws Exception {
    String perfil = "referencia-" + UUID.randomUUID();
    receber(perfil, csvDeposito("referencia-unica", "primeiro")).andExpect(status().isCreated());
    aplicar(perfil);
    receber(perfil, csvDeposito("referencia-unica", "alterado")).andExpect(status().isCreated());
    JsonNode t = timeline(perfil);
    assertThat(t.path("total").asInt()).isEqualTo(2);
    assertThat(previa(perfil, t.path("revisao").asText()).path("registrosParaAplicar").asInt())
        .isZero();
    mvc.perform(
            multipart("/api/v1/importacoes/previa")
                .file(
                    new MockMultipartFile(
                        "arquivo",
                        "sintetico.csv",
                        "text/csv",
                        csvDeposito("referencia-unica", "terceiro")))
                .param("aportesProprios", "true")
                .param("taxasIncluidas", "true")
                .param("posicaoAnteriorZerada", "true")
                .header("Authorization", bearer(perfil)))
        .andExpect(status().isConflict());
  }

  @Test
  void transferenciaComDuasSaidasCandidatasNaoEscolhePrimeira() throws Exception {
    String perfil = "ambiguo-" + UUID.randomUUID();
    String op = "Transfer Between Spot and Funding";
    receber(
            perfil,
            planilha(
                new String[][] {
                  {"2024-05-01 10:00:00", "Spot", op, "BTC", "-1"},
                  {"2024-05-01 10:00:01", "Spot", op, "BTC", "-1"},
                  {"2024-05-01 10:00:02", "Funding", op, "BTC", "1"}
                }))
        .andExpect(status().isCreated());
    assertThat(
            previa(perfil, timeline(perfil).path("revisao").asText())
                .path("correspondencias")
                .size())
        .isZero();
  }

  @Test
  void vendaSemInformacaoDeTaxaContinuaPendenteComEstoqueECustoConhecidos() throws Exception {
    String perfil = "taxa-" + UUID.randomUUID();
    receber(
            perfil,
            planilha(new String[][] {{"2024-05-01 10:00:00", "Spot", "Deposit", "BTC", "2"}}))
        .andExpect(status().isCreated());
    JsonNode t = timeline(perfil);
    editarRegistro(
        perfil,
        t,
        porTipo(t, "Deposit"),
        Map.of("natureza", "SALDO_ANTERIOR", "custoTotalBrl", "20"));
    aplicar(perfil);
    receber(
            perfil,
            planilha(new String[][] {{"2024-05-02 10:00:00", "Spot", "Withdraw", "BTC", "-1"}}))
        .andExpect(status().isCreated());
    t = timeline(perfil);
    String brl =
        jdbc.queryForObject("SELECT id FROM ativos WHERE codigo='fiat:BRL'", Long.class).toString();
    editarRegistro(
        perfil,
        t,
        porTipo(t, "Withdraw"),
        Map.of(
            "natureza",
            "VENDA",
            "quantidade",
            "1",
            "moedaCotacaoId",
            brl,
            "totalBruto",
            "100",
            "cotacaoBrl",
            "1",
            "taxaInformada",
            false));
    aplicar(perfil);
    assertThat(porTipo(timeline(perfil), "Withdraw").path("estado").asText()).isEqualTo("PENDENTE");
  }

  @Test
  void falhaAoSalvarComandoReverteAjustesAplicacaoERevisao() throws Exception {
    String perfil = "rollback-" + UUID.randomUUID();
    receber(
            perfil,
            planilha(new String[][] {{"2024-05-02 10:00:00", "Spot", "Withdraw", "BTC", "-1"}}))
        .andExpect(status().isCreated());
    JsonNode antes = timeline(perfil), p = previa(perfil, antes.path("revisao").asText());
    String gatilho = "falhar_comando_conciliacao";
    jdbc.execute(
        "ALTER TABLE comandos_conciliacao ADD CONSTRAINT "
            + gatilho
            + " CHECK (perfil_ref <> '"
            + perfil
            + "')");
    try {
      var comando =
          Map.of(
              "revisao",
              p.path("revisao").asText(),
              "previaId",
              p.path("previaId").asText(),
              "aceitarLacunaIds",
              List.of(p.path("ajustes").get(0).path("lacunaId").asText()),
              "aceitarCorrespondenciaIds",
              List.of());
      assertThatThrownBy(() -> confirmar(perfil, comando, UUID.randomUUID().toString()))
          .isInstanceOf(jakarta.servlet.ServletException.class);
      JsonNode depois = timeline(perfil);
      assertThat(depois.path("revisao").asText()).isEqualTo(antes.path("revisao").asText());
      assertThat(depois.path("resumo").path("ajustesAtivos").asInt()).isZero();
      assertThat(depois.path("itens").get(0).path("aplicado").asBoolean()).isFalse();
      assertThat(
              jdbc.queryForObject(
                  "SELECT COUNT(*) FROM comandos_conciliacao WHERE perfil_ref=?",
                  Long.class,
                  perfil))
          .isZero();
    } finally {
      jdbc.execute("ALTER TABLE comandos_conciliacao DROP CHECK " + gatilho);
    }
  }

  @Test
  void linhaXlsxClassificadaNaoDuplicaContrapartidaObservada() throws Exception {
    String perfil = "pernas-" + UUID.randomUUID();
    receber(
            perfil,
            planilha(
                new String[][] {
                  {"2024-05-01 10:00:00", "Spot", "Deposit", "BRL", "300"},
                  {"2024-05-02 10:00:00", "Spot", "Buy", "BTC", "1"},
                  {"2024-05-02 10:00:00", "Spot", "Buy", "BRL", "-100"}
                }))
        .andExpect(status().isCreated());
    JsonNode t = timeline(perfil), btc = null;
    for (JsonNode r : t.path("itens")) if (r.path("ativoTicker").asText().equals("BTC")) btc = r;
    String brl =
        jdbc.queryForObject("SELECT id FROM ativos WHERE codigo='fiat:BRL'", Long.class).toString();
    editarRegistro(
        perfil,
        t,
        btc,
        Map.of(
            "natureza",
            "COMPRA",
            "moedaCotacaoId",
            brl,
            "totalBruto",
            "100",
            "cotacaoBrl",
            "1",
            "taxaInformada",
            true,
            "taxaQuantidade",
            "0"));
    aplicar(perfil);
    JsonNode posicoes =
        corpo(
            mvc.perform(get("/api/v1/posicoes").header("Authorization", bearer(perfil)))
                .andExpect(status().isOk()));
    for (JsonNode p : posicoes.path("itens"))
      if (p.path("ativoTicker").asText().equals("BRL"))
        assertThat(p.path("quantidade").asText()).isEqualTo("200");
    assertThat(timeline(perfil).path("resumo").path("ajustesAtivos").asInt()).isZero();
  }

  @Test
  void csvParcialAntesDoPrimeiroAceitePreservaAsDuasQuantidades() throws Exception {
    String perfil = "primeiro-parcial-" + UUID.randomUUID();
    receber(perfil, csvNegociacao("Buy", "BRL", "100", "BTC", "1")).andExpect(status().isCreated());
    assertThat(timeline(perfil).path("linhasPorGrupo").iterator().next()).hasSize(2);
    JsonNode p = previa(perfil, timeline(perfil).path("revisao").asText());
    assertThat(p.path("grupos")).hasSize(1);
    assertThat(p.path("grupos").get(0).path("linhas")).hasSize(2);
    assertThat(new java.math.BigDecimal(p.path("grupos").get(0).path("principalBrl").asText()))
        .isEqualByComparingTo("100");
    assertThat(p.path("ajustes").get(0).path("quantidadeSugerida").asText()).isEqualTo("100");
    aplicar(perfil);
    JsonNode posicoes =
        corpo(
            mvc.perform(get("/api/v1/posicoes").header("Authorization", bearer(perfil)))
                .andExpect(status().isOk()));
    assertThat(posicoes.path("itens").get(0).path("ativoTicker").asText()).isEqualTo("BTC");
    assertThat(posicoes.path("itens").get(0).path("quantidade").asText()).isEqualTo("1");
    assertThat(
            previa(perfil, timeline(perfil).path("revisao").asText())
                .path("registrosParaAplicar")
                .asInt())
        .isZero();
  }

  @Test
  void contrapartidaDeMoedaDesconhecidaTornaCoberturaParcial() throws Exception {
    String perfil = "moeda-desconhecida-" + UUID.randomUUID();
    receber(perfil, csvNegociacao("Sell", "ZEC_DESCONHECIDA", "10", "BRL", "100"))
        .andExpect(status().isCreated());
    aplicar(perfil);
    JsonNode painel =
        corpo(
            mvc.perform(get("/api/v1/visao-geral").header("Authorization", bearer(perfil)))
                .andExpect(status().isOk()));
    assertThat(painel.path("disponibilidade").path("quantidadesCompletas").asBoolean()).isFalse();
    assertThat(painel.path("resumo").path("custoSaldosBrl").isNull()).isTrue();
    assertThat(painel.path("disponibilidade").path("subtotalCustoBrl").asText()).isEqualTo("100");
  }

  @Test
  void pernaEnviadaConhecidaEhAproveitadaMesmoSemAtivoRecebidoIdentificado() throws Exception {
    String perfil = "perna-conhecida-" + UUID.randomUUID();
    receber(perfil, csvNegociacao("Buy", "BRL", "100", "ATIVO_DESCONHECIDO", "1"))
        .andExpect(status().isCreated());
    JsonNode p = previa(perfil, timeline(perfil).path("revisao").asText());
    assertThat(p.path("registrosParaAplicar").asInt()).isEqualTo(1);
    assertThat(p.path("ajustes").get(0).path("ativoTicker").asText()).isEqualTo("BRL");
    aplicar(perfil);
    assertThat(timeline(perfil).path("itens").get(1).path("aplicado").asBoolean()).isTrue();
  }

  byte[] csvNegociacao(
      String tipo, String enviada, String valorEnviado, String recebida, String valorRecebido) {
    String[] c = new String[17];
    Arrays.fill(c, "");
    c[0] = "evento";
    c[1] = "2024-05-02-10:00:00";
    c[2] = tipo;
    c[4] = "CONVERT";
    c[6] = valorEnviado;
    c[7] = enviada;
    c[10] = valorRecebido;
    c[11] = recebida;
    c[14] = "0";
    String h =
        "id,datetime_tz_BRT,type,label,market_model_type,order_type,sent_amount,sent_currency,sent_value_BRL,sent_address,received_amount,received_currency,received_value_BRL,received_address,fee_amount,fee_currency,fee_value_BRL";
    return (h + "\n" + String.join(",", c) + "\n").getBytes(StandardCharsets.UTF_8);
  }

  byte[] csvDeposito(String id, String label) {
    String[] c = new String[17];
    Arrays.fill(c, "");
    c[0] = id;
    c[1] = "2024-05-01-10:00:00";
    c[2] = "Deposit";
    c[3] = label;
    c[4] = "FIAT";
    c[10] = "100";
    c[11] = "BRL";
    c[14] = "0";
    String h =
        "id,datetime_tz_BRT,type,label,market_model_type,order_type,sent_amount,sent_currency,sent_value_BRL,sent_address,received_amount,received_currency,received_value_BRL,received_address,fee_amount,fee_currency,fee_value_BRL";
    return (h + "\n" + String.join(",", c) + "\n").getBytes(StandardCharsets.UTF_8);
  }

  JsonNode porTipo(JsonNode t, String tipo) {
    for (JsonNode r : t.path("itens")) if (r.path("tipo").asText().equals(tipo)) return r;
    throw new AssertionError("Tipo não encontrado: " + tipo);
  }

  void aplicar(String perfil) throws Exception {
    JsonNode p = previa(perfil, timeline(perfil).path("revisao").asText());
    List<String> ids = new ArrayList<>();
    p.path("ajustes").forEach(a -> ids.add(a.path("lacunaId").asText()));
    confirmar(
            perfil,
            Map.of(
                "revisao",
                p.path("revisao").asText(),
                "previaId",
                p.path("previaId").asText(),
                "aceitarLacunaIds",
                ids,
                "aceitarCorrespondenciaIds",
                List.of()),
            UUID.randomUUID().toString())
        .andExpect(status().isOk());
  }

  void editarRegistro(String perfil, JsonNode t, JsonNode r, Map<String, Object> campos)
      throws Exception {
    var body = new LinkedHashMap<String, Object>();
    body.put("revisao", t.path("revisao").asText());
    body.put("versao", r.path("versao").asText());
    for (String c : List.of("data", "contaId", "ativoId", "quantidade", "custoTotalBrl"))
      body.put(c, r.path(c).isNull() ? null : r.path(c).asText());
    body.put("natureza", "OBSERVADA");
    body.put("motivo", "Conferência sintética");
    body.putAll(campos);
    mvc.perform(
            put("/api/v1/conciliacao/registros/" + r.path("id").asText())
                .header("Authorization", bearer(perfil))
                .header("Idempotency-Key", UUID.randomUUID().toString())
                .contentType("application/json")
                .content(json.writeValueAsString(body)))
        .andExpect(status().isOk());
  }

  ResultActions receber(String perfil, byte[] bytes) throws Exception {
    return mvc.perform(
        multipart("/api/v1/importacoes/recebimentos")
            .file(
                new MockMultipartFile(
                    "arquivos",
                    "sintetico.xlsx",
                    "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet",
                    bytes))
            .header("Authorization", bearer(perfil)));
  }

  JsonNode timeline(String perfil) throws Exception {
    return corpo(
        mvc.perform(get("/api/v1/conciliacao/timeline").header("Authorization", bearer(perfil)))
            .andExpect(status().isOk()));
  }

  JsonNode previa(String perfil, String revisao) throws Exception {
    return corpo(
        enviar(perfil, "/api/v1/conciliacao/previa", Map.of("revisao", revisao), null)
            .andExpect(status().isOk()));
  }

  ResultActions confirmar(String perfil, Object body, String chave) throws Exception {
    return enviar(perfil, "/api/v1/conciliacao/confirmacoes", body, chave);
  }

  ResultActions enviar(String perfil, String rota, Object body, String chave) throws Exception {
    var req =
        post(rota)
            .header("Authorization", bearer(perfil))
            .contentType("application/json")
            .content(json.writeValueAsString(body));
    if (chave != null) req.header("Idempotency-Key", chave);
    return mvc.perform(req);
  }

  JsonNode corpo(ResultActions acao) throws Exception {
    return json.readTree(acao.andReturn().getResponse().getContentAsString());
  }

  String bearer(String perfil) {
    var b = Base64.getUrlEncoder().withoutPadding();
    return "Bearer "
        + b.encodeToString("{\"alg\":\"none\"}".getBytes(StandardCharsets.UTF_8))
        + "."
        + b.encodeToString(
            ("{\"sub\":\"" + perfil + "\",\"name\":\"Teste\",\"role\":\"acesso_total\"}")
                .getBytes(StandardCharsets.UTF_8))
        + ".";
  }

  byte[] planilha(String[][] dados) throws Exception {
    try (var wb = new XSSFWorkbook();
        var out = new ByteArrayOutputStream()) {
      var sh = wb.createSheet("Movimentos");
      sh.createRow(0).createCell(0).setCellValue("Período (UTC--3)");
      var h = sh.createRow(1);
      String[] nomes = {"Tempo", "Conta", "Operação", "Moeda", "Alterar"};
      for (int i = 0; i < nomes.length; i++) h.createCell(i).setCellValue(nomes[i]);
      for (int i = 0; i < dados.length; i++) {
        var r = sh.createRow(i + 2);
        for (int j = 0; j < dados[i].length; j++) r.createCell(j).setCellValue(dados[i][j]);
      }
      wb.write(out);
      return out.toByteArray();
    }
  }
}
