package io.github.jaimenafer.lastro;

import static org.assertj.core.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import java.util.*;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.web.servlet.*;
import org.testcontainers.junit.jupiter.*;
import org.testcontainers.mysql.MySQLContainer;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

@SpringBootTest(properties = "spring.config.import=")
@AutoConfigureMockMvc
@Testcontainers
class NegociacoesIntegrationTests {
  @Container @ServiceConnection
  static final MySQLContainer MYSQL = new MySQLContainer("mysql:8.0.42");

  @Autowired MockMvc mvc;
  @Autowired JdbcTemplate jdbc;
  @Autowired JsonMapper json;
  long binance, bitget, brl, btc, usdt;

  @BeforeEach
  void preparar() {
    jdbc.update("UPDATE operacoes SET substitui_id=NULL");
    jdbc.update("DELETE FROM operacoes");
    jdbc.update("DELETE FROM pontos_partida");
    jdbc.update("DELETE FROM contas");
    jdbc.update("DELETE FROM carteiras");
    jdbc.update("UPDATE ativos SET ativo=TRUE");
    jdbc.update("UPDATE instituicoes SET ativo=TRUE");
    binance = instituicao("binance");
    bitget = instituicao("bitget");
    brl = ativo("BRL");
    btc = ativo("BTC");
    usdt = ativo("USDT");
  }

  long ativo(String ticker) {
    return jdbc.queryForObject("SELECT id FROM ativos WHERE ticker=?", Long.class, ticker);
  }

  long instituicao(String codigo) {
    return jdbc.queryForObject("SELECT id FROM instituicoes WHERE codigo=?", Long.class, codigo);
  }

  Map<String, Object> aporte(String quantidade) {
    return new HashMap<>(
        Map.of(
            "tipo",
            "APORTE",
            "instituicaoId",
            Long.toString(binance),
            "ativoId",
            Long.toString(brl),
            "ocorridaEm",
            "2026-10-05T10:00:00-03:00",
            "quantidade",
            quantidade));
  }

  Map<String, Object> negocio(String tipo, String quantidade, String total) {
    return new HashMap<>(
        Map.of(
            "tipo",
            tipo,
            "instituicaoId",
            Long.toString(binance),
            "ativoId",
            Long.toString(btc),
            "ocorridaEm",
            "2026-10-05T11:00:00-03:00",
            "quantidade",
            quantidade,
            "moedaCotacaoId",
            Long.toString(brl),
            "totalBruto",
            total,
            "cotacaoBrl",
            "1"));
  }

  String bearer(String perfil) {
    var encoder = Base64.getUrlEncoder().withoutPadding();
    return "Bearer "
        + encoder.encodeToString(
            "{\"alg\":\"none\"}".getBytes(java.nio.charset.StandardCharsets.UTF_8))
        + "."
        + encoder.encodeToString(
            ("{\"sub\":\"" + perfil + "\",\"name\":\"Teste\",\"role\":\"acesso_total\"}")
                .getBytes(java.nio.charset.StandardCharsets.UTF_8))
        + ".";
  }

  ResultActions postJson(String perfil, String rota, Object corpo) throws Exception {
    return mvc.perform(
        post(rota)
            .header("Authorization", bearer(perfil))
            .contentType("application/json")
            .content(json.writeValueAsString(corpo)));
  }

  JsonNode corpo(ResultActions resultado) throws Exception {
    return json.readTree(resultado.andReturn().getResponse().getContentAsString());
  }

  JsonNode previa(String perfil, Map<String, Object> cadastro) throws Exception {
    return corpo(
        postJson(perfil, "/api/v1/operacoes/previa", Map.of("acao", "CRIAR", "operacao", cadastro))
            .andExpect(status().isOk()));
  }

  ResultActions gravar(String perfil, Map<String, Object> cadastro, String revisao, String chave)
      throws Exception {
    return mvc.perform(
        post("/api/v1/operacoes")
            .header("Authorization", bearer(perfil))
            .header("Idempotency-Key", chave)
            .contentType("application/json")
            .content(json.writeValueAsString(Map.of("revisao", revisao, "operacao", cadastro))));
  }

  JsonNode criar(String perfil, Map<String, Object> cadastro) throws Exception {
    return corpo(
        gravar(
                perfil,
                cadastro,
                previa(perfil, cadastro).get("revisao").stringValue(),
                UUID.randomUUID().toString())
            .andExpect(status().isCreated()));
  }

  @Test
  void previaNaoGravaECicloCompletoPreservaContrato() throws Exception {
    var aporte = aporte("1000");
    previa("a", aporte);
    assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM operacoes", Integer.class)).isZero();
    assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM pontos_partida", Integer.class)).isZero();
    criar("a", aporte);
    criar("a", negocio("COMPRA", "0.01", "100"));
    var venda = negocio("VENDA", "0.005", "75");
    venda.put("ocorridaEm", "2026-10-05T12:00:00-03:00");
    var confirmada = criar("a", venda);
    assertThat(confirmada.get("calculo").get("resultadoTotalBrl").stringValue()).isEqualTo("25");
    assertThat(confirmada.get("criadoPor").stringValue()).isEqualTo("a");
    assertThat(confirmada.get("alteradoPor").stringValue()).isEqualTo("a");
    mvc.perform(get("/api/v1/posicoes").header("Authorization", bearer("a")))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.itens[0].ativoTicker").value("BRL"))
        .andExpect(jsonPath("$.itens[0].quantidade").value("975"))
        .andExpect(jsonPath("$.itens[1].quantidade").value("0.005"))
        .andExpect(jsonPath("$.itens[1].custoTotalBrl").value("50"));
    mvc.perform(get("/api/v1/saldos-iniciais").header("Authorization", bearer("a")))
        .andExpect(jsonPath("$.itens").isEmpty())
        .andExpect(jsonPath("$.possuiSaldosConfirmados").value(false));
    mvc.perform(
            get("/api/v1/saldos-iniciais/" + confirmada.get("id").stringValue())
                .header("Authorization", bearer("a")))
        .andExpect(status().isNotFound());
    mvc.perform(get("/api/v1/operacoes").header("Authorization", bearer("b")))
        .andExpect(jsonPath("$.itens").isEmpty());
    mvc.perform(
            get("/api/v1/operacoes/" + confirmada.get("id").stringValue())
                .header("Authorization", bearer("b")))
        .andExpect(status().isNotFound());
  }

  @Test
  void revisaoERepeticaoNaoDuplicamLancamentos() throws Exception {
    var entrada = aporte("1000");
    String revisao = previa("a", entrada).get("revisao").stringValue();
    String chave = UUID.randomUUID().toString();
    var primeira = corpo(gravar("a", entrada, revisao, chave).andExpect(status().isCreated()));
    gravar("a", entrada, revisao, chave)
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.id").value(primeira.get("id").stringValue()));
    gravar("a", entrada, revisao, UUID.randomUUID().toString()).andExpect(status().isConflict());
    assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM operacoes", Integer.class)).isEqualTo(1);
    assertThat(jdbc.queryForObject("SELECT revisao FROM pontos_partida", Long.class)).isEqualTo(1L);
  }

  @Test
  void cancelamentoRetroativoInvalidoReverteTudo() throws Exception {
    var origem = criar("a", aporte("1000"));
    criar("a", negocio("COMPRA", "0.01", "100"));
    String id = origem.get("id").stringValue();
    postJson(
            "a",
            "/api/v1/operacoes/previa",
            Map.of("acao", "CANCELAR", "operacaoId", id, "motivo", "Conferência"))
        .andExpect(status().isConflict());
    postJson(
            "a",
            "/api/v1/operacoes/" + id + "/cancelamento",
            Map.of("revisao", "2", "motivo", "Conferência"))
        .andExpect(status().isConflict());
    assertThat(jdbc.queryForObject("SELECT situacao FROM operacoes WHERE id=?", String.class, id))
        .isEqualTo("CONFIRMADA");
    assertThat(jdbc.queryForObject("SELECT revisao FROM pontos_partida", Long.class)).isEqualTo(2L);
  }

  @Test
  void stablecoinETaxasMantemValorHistorico() throws Exception {
    var saldo =
        Map.of(
            "instituicaoId",
            Long.toString(binance),
            "ativoId",
            Long.toString(usdt),
            "dataReferencia",
            "2026-10-01",
            "quantidade",
            "100",
            "custoTotalBrl",
            "500",
            "custoZeroConfirmado",
            false);
    mvc.perform(
            post("/api/v1/saldos-iniciais")
                .header("Authorization", bearer("a"))
                .header("Idempotency-Key", UUID.randomUUID().toString())
                .contentType("application/json")
                .content(json.writeValueAsString(saldo)))
        .andExpect(status().isCreated());
    var compra = negocio("COMPRA", "0.001", "100");
    compra.put("moedaCotacaoId", Long.toString(usdt));
    compra.put("cotacaoBrl", "5.2");
    var confirmada = criar("a", compra);
    assertThat(confirmada.get("calculo").get("resultadoMoedaBrl").stringValue()).isEqualTo("20");
    var aberturaId =
        jdbc.queryForObject("SELECT id FROM operacoes WHERE tipo='SALDO_INICIAL'", Long.class);
    postJson(
            "a",
            "/api/v1/saldos-iniciais/" + aberturaId + "/cancelamento",
            Map.of("motivo", "Revisar abertura"))
        .andExpect(status().isConflict());
    assertThat(
            jdbc.queryForObject(
                "SELECT COUNT(*) FROM operacoes WHERE situacao='CONFIRMADA'", Integer.class))
        .isEqualTo(2);
  }

  @Test
  void saldoPorInstituicaoETerceiraMoedaSaoRecusados() throws Exception {
    criar("a", aporte("1000"));
    var compra = negocio("COMPRA", "1", "100");
    compra.put("instituicaoId", Long.toString(bitget));
    postJson("a", "/api/v1/operacoes/previa", Map.of("acao", "CRIAR", "operacao", compra))
        .andExpect(status().isConflict());
    compra.put("instituicaoId", Long.toString(binance));
    compra.put("taxaAtivoId", Long.toString(usdt));
    compra.put("taxaQuantidade", "1");
    postJson("a", "/api/v1/operacoes/previa", Map.of("acao", "CRIAR", "operacao", compra))
        .andExpect(status().isBadRequest());
    assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM operacoes", Integer.class)).isEqualTo(1);
  }

  @Test
  void correcaoPreservaOrdemEHistoricoECancelamentoRepete() throws Exception {
    criar("a", aporte("1000"));
    var original = criar("a", negocio("COMPRA", "0.01", "100"));
    String id = original.get("id").stringValue();
    var revisada = negocio("COMPRA", "0.02", "100");
    revisada.put("motivo", "Quantidade do extrato");
    var previa =
        corpo(
            postJson(
                    "a",
                    "/api/v1/operacoes/previa",
                    Map.of("acao", "SUBSTITUIR", "operacaoId", id, "operacao", revisada))
                .andExpect(status().isOk()));
    var nova =
        corpo(
            mvc.perform(
                    post("/api/v1/operacoes/" + id + "/substituicoes")
                        .header("Authorization", bearer("a"))
                        .header("Idempotency-Key", UUID.randomUUID().toString())
                        .contentType("application/json")
                        .content(
                            json.writeValueAsString(
                                Map.of(
                                    "revisao",
                                    previa.get("revisao").stringValue(),
                                    "operacao",
                                    revisada))))
                .andExpect(status().isCreated()));
    assertThat(nova.get("substituiId").stringValue()).isEqualTo(id);
    assertThat(
            jdbc.queryForObject(
                "SELECT COUNT(DISTINCT ordem_livro) FROM operacoes WHERE tipo='COMPRA'",
                Integer.class))
        .isEqualTo(1);
    String novaId = nova.get("id").stringValue();
    postJson(
            "a",
            "/api/v1/operacoes/" + novaId + "/cancelamento",
            Map.of("revisao", "3", "motivo", "Não ocorreu"))
        .andExpect(status().isOk());
    postJson(
            "a",
            "/api/v1/operacoes/" + novaId + "/cancelamento",
            Map.of("revisao", "3", "motivo", "Não ocorreu"))
        .andExpect(status().isOk());
    assertThat(jdbc.queryForObject("SELECT revisao FROM pontos_partida", Long.class)).isEqualTo(4L);
  }

  @Test
  void confirmacoesConcorrentesDaMesmaRevisaoNaoSeSobrepoem() throws Exception {
    criar("a", aporte("100"));
    var executor = java.util.concurrent.Executors.newFixedThreadPool(2);
    var inicio = new java.util.concurrent.CountDownLatch(1);
    try {
      var tarefas = new ArrayList<java.util.concurrent.Future<Integer>>();
      for (int i = 0; i < 2; i++)
        tarefas.add(
            executor.submit(() -> {
              inicio.await();
              return gravar("a", aporte("10"), "1", UUID.randomUUID().toString())
                  .andReturn()
                  .getResponse()
                  .getStatus();
            }));
      inicio.countDown();
      var status = new ArrayList<Integer>();
      for (var tarefa : tarefas) status.add(tarefa.get(20, java.util.concurrent.TimeUnit.SECONDS));
      assertThat(status).containsExactlyInAnyOrder(201, 409);
      assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM operacoes", Integer.class)).isEqualTo(2);
      assertThat(jdbc.queryForObject("SELECT revisao FROM pontos_partida", Long.class))
          .isEqualTo(2L);
    } finally {
      executor.shutdownNow();
    }
  }

  @Test
  void carteirasSobrepostasNaoDuplicamERepeticaoPreservaCatalogoHistorico() throws Exception {
    var cadastro = aporte("1000");
    String chave = UUID.randomUUID().toString();
    var primeira = corpo(gravar("a", cadastro, "0", chave).andExpect(status().isCreated()));
    var compra = negocio("COMPRA", "0.01", "100");
    criar("a", compra);
    var ids = new ArrayList<String>();
    for (String nome : List.of("Principal", "Recorte"))
      ids.add(
          corpo(
                  postJson(
                          "a",
                          "/api/v1/carteiras",
                          Map.of("nome", nome, "instituicaoIds", List.of(Long.toString(binance))))
                      .andExpect(status().isCreated()))
              .get("id")
              .stringValue());
    mvc.perform(
            get("/api/v1/operacoes")
                .header("Authorization", bearer("a"))
                .param("carteiraIds", ids.toArray(String[]::new)))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.total").value(2));
    jdbc.update("UPDATE instituicoes SET ativo=FALSE WHERE id=?", binance);
    jdbc.update("UPDATE ativos SET ativo=FALSE WHERE id=?", brl);
    gravar("a", cadastro, "0", chave)
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.id").value(primeira.get("id").stringValue()));
    assertThat(jdbc.queryForObject("SELECT revisao FROM pontos_partida", Long.class)).isEqualTo(2L);
  }
}
