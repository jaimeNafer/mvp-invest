package io.github.jaimenafer.lastro;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import java.time.LocalDate;
import java.time.ZoneId;
import java.util.*;
import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.mysql.MySQLContainer;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

@SpringBootTest(properties = "spring.config.import=")
@AutoConfigureMockMvc
@Testcontainers
class SaldosIniciaisIntegrationTests {
  @Container @ServiceConnection
  static final MySQLContainer MYSQL = new MySQLContainer("mysql:8.0.42");

  @Autowired MockMvc mvc;
  @Autowired JdbcTemplate jdbc;
  @Autowired JsonMapper json;
  private long binance;
  private long bitget;
  private final String data = LocalDate.now(ZoneId.of("America/Fortaleza")).minusDays(7).toString();

  @BeforeEach
  void preparar() {
    jdbc.update("UPDATE operacoes SET substitui_id = NULL");
    jdbc.update("DELETE FROM operacoes");
    jdbc.update("DELETE FROM pontos_partida");
    jdbc.update("DELETE FROM contas");
    jdbc.update("DELETE FROM carteiras");
    jdbc.update("UPDATE ativos SET ativo = TRUE");
    jdbc.update("UPDATE instituicoes SET ativo = TRUE");
    binance = jdbc.queryForObject("SELECT id FROM instituicoes WHERE codigo='binance'", Long.class);
    bitget = jdbc.queryForObject("SELECT id FROM instituicoes WHERE codigo='bitget'", Long.class);
  }

  @Test
  void enumsLeemValoresTextuaisExistentesSemAlterarColunas() throws Exception {
    var saldo = criar("a", entrada("BTC", binance, "1", "100"));
    String operacaoId = id(saldo);
    assertThat(
            jdbc.queryForObject(
                "SELECT tipo FROM operacoes WHERE id = ?", String.class, operacaoId))
        .isEqualTo("SALDO_INICIAL");
    for (String situacao : List.of("CONFIRMADA", "SUBSTITUIDA", "CANCELADA")) {
      jdbc.update("UPDATE operacoes SET situacao = ? WHERE id = ?", situacao, operacaoId);
      mvc.perform(get("/api/v1/saldos-iniciais/" + operacaoId).header("Authorization", bearer("a")))
          .andExpect(status().isOk())
          .andExpect(jsonPath("$.situacao").value(situacao));
      mvc.perform(
              get("/api/v1/saldos-iniciais")
                  .header("Authorization", bearer("a"))
                  .param("situacao", situacao))
          .andExpect(status().isOk())
          .andExpect(jsonPath("$.itens.length()").value(1));
    }
    mvc.perform(
            get("/api/v1/saldos-iniciais")
                .header("Authorization", bearer("a"))
                .param("situacao", "TODAS"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.itens.length()").value(1));
    mvc.perform(
            get("/api/v1/saldos-iniciais")
                .header("Authorization", bearer("a"))
                .param("situacao", "confirmada"))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.detail").value("Verifique os filtros e a paginação."));
    assertThat(
            jdbc.queryForList(
                "SELECT DISTINCT DATA_TYPE FROM information_schema.COLUMNS WHERE TABLE_SCHEMA ="
                    + " DATABASE() AND ((TABLE_NAME = 'ativos' AND COLUMN_NAME = 'tipo') OR"
                    + " (TABLE_NAME = 'operacoes' AND COLUMN_NAME IN ('tipo', 'situacao')))",
                String.class))
        .containsExactly("varchar");
    mvc.perform(get("/api/v1/ativos").header("Authorization", bearer("a")))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$[0].tipo").value("FIAT"))
        .andExpect(jsonPath("$[1].tipo").value("CRIPTO"));
  }

  @Test
  void catalogoSomenteLeituraEContextoObrigatorio() throws Exception {
    mvc.perform(get("/api/v1/ativos")).andExpect(status().isUnauthorized());
    mvc.perform(get("/api/v1/ativos").header("Authorization", bearer("a")))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.length()").value(6))
        .andExpect(jsonPath("$[0].id").isString())
        .andExpect(jsonPath("$[0].ticker").value("BRL"));
    mvc.perform(post("/api/v1/ativos").header("Authorization", bearer("a")))
        .andExpect(status().isMethodNotAllowed());
    mvc.perform(get("/api/v1/saldos-iniciais").header("Authorization", bearer("a")))
        .andExpect(jsonPath("$.itens").isEmpty())
        .andExpect(jsonPath("$.possuiSaldosConfirmados").value(false));
  }

  @Test
  void aberturaPrecisaEPosicaoGlobalSemDuplicarPorCarteiras() throws Exception {
    var entrada = entrada("BTC", binance, "0.01", "4000.00000000");
    entrada.put("perfilRef", "outro");
    var original = criar("a", entrada);
    criar("a", entrada("BTC", bitget, "0.02", "10000"));
    criar("b", entrada("BTC", binance, "0.005", "1500"));
    jdbc.update(
        "INSERT INTO carteiras(id,perfil_ref,nome) VALUES"
            + " (101,'a','Principal'),(102,'a','Recorte'),(103,'b','Privada')");
    jdbc.update(
        "INSERT INTO contas(carteira_id,instituicao_id) VALUES (101,?),(102,?),(103,?)",
        binance,
        binance,
        binance);
    mvc.perform(
            get("/api/v1/saldos-iniciais")
                .header("Authorization", bearer("a"))
                .param("carteiraIds", "101", "102")
                .param("tamanho", "1"))
        .andExpect(jsonPath("$.total").value(1))
        .andExpect(jsonPath("$.itens[0].id").value(id(original)))
        .andExpect(jsonPath("$.dataReferencia").value(data));
    mvc.perform(get("/api/v1/posicoes-iniciais").header("Authorization", bearer("a")))
        .andExpect(jsonPath("$[0].quantidade").value("0.03"))
        .andExpect(jsonPath("$[0].custoTotalBrl").value("14000"))
        .andExpect(
            jsonPath("$[0].custoMedioBrl").value("466666.666666666666666666666666666666666667"));
    mvc.perform(get("/api/v1/posicoes-iniciais").header("Authorization", bearer("b")))
        .andExpect(jsonPath("$[0].custoMedioBrl").value("300000"));
    assertThat(
            jdbc.queryForObject(
                "SELECT perfil_ref FROM operacoes WHERE id=?", String.class, id(original)))
        .isEqualTo("a");
    assertThat(
            jdbc.queryForObject(
                "SELECT CAST(ocorrida_em AS CHAR) FROM operacoes WHERE id=?",
                String.class,
                id(original)))
        .startsWith(data + " 03:00:00");
    jdbc.update("DELETE FROM contas WHERE carteira_id=101");
    jdbc.update("UPDATE carteiras SET ativo=FALSE WHERE id=102");
    mvc.perform(
            get("/api/v1/saldos-iniciais")
                .header("Authorization", bearer("a"))
                .param("carteiraIds", "102"))
        .andExpect(jsonPath("$.itens[0].id").value(id(original)));
    assertThat(contar("operacoes")).isEqualTo(3);
  }

  @Test
  void custoBrlIgualQuantidadeEMoedasNaoAssumemParidade() throws Exception {
    criar("a", entrada("BRL", binance, "1234.56", "1234.560"));
    criar("a", entrada("USD", binance, "100", "500"));
    criar("a", entrada("USDT", binance, "100", "520"));
    enviar("a", entrada("BRL", bitget, "10", "11"), UUID.randomUUID().toString(), null)
        .andExpect(status().isBadRequest());
    mvc.perform(get("/api/v1/posicoes-iniciais").header("Authorization", bearer("a")))
        .andExpect(jsonPath("$[0].custoMedioBrl").value("1"))
        .andExpect(jsonPath("$[1].custoMedioBrl").value("5"))
        .andExpect(jsonPath("$[2].custoMedioBrl").value("5.2"));
  }

  @Test
  void custoZeroExplicitoSemGravacaoParcialQuandoDesconhecido() throws Exception {
    var entrada = entrada("ETH", binance, "3", "0");
    enviar("a", entrada, UUID.randomUUID().toString(), null).andExpect(status().isBadRequest());
    assertThat(contar("pontos_partida")).isZero();
    entrada.put("custoZeroConfirmado", true);
    entrada.put("justificativaCustoZero", "  Origem documentada sem custo  ");
    var criado = criar("a", entrada);
    assertThat(criado.get("justificativaCustoZero").stringValue())
        .isEqualTo("Origem documentada sem custo");
    mvc.perform(get("/api/v1/posicoes-iniciais").header("Authorization", bearer("a")))
        .andExpect(jsonPath("$[0].custoTotalBrl").value("0"))
        .andExpect(jsonPath("$[0].custoMedioBrl").value("0"));
  }

  @Test
  void decimaisCanonicosEscalaCapacidadeECalendarioValidados() throws Exception {
    for (String q :
        List.of(
            "0", "-1", "0.0000000000000000001", "100000000000000000000", "1e3", "NaN", "1,2", "")) {
      enviar("a", entrada("BTC", binance, q, "1"), UUID.randomUUID().toString(), null)
          .andExpect(status().isBadRequest());
    }
    for (String c : List.of("-1", "0.000000001", "1000000000000000000000000000000", "1e3")) {
      enviar("a", entrada("BTC", binance, "1", c), UUID.randomUUID().toString(), null)
          .andExpect(status().isBadRequest());
    }
    var numeroJson = entrada("BTC", binance, "1", "1");
    numeroJson.put("quantidade", 1);
    enviar("a", numeroJson, UUID.randomUUID().toString(), null).andExpect(status().isBadRequest());
    enviar("a", entrada("USD", binance, "1.001", "1"), UUID.randomUUID().toString(), null)
        .andExpect(status().isBadRequest());
    for (String d : List.of("2026-02-30", LocalDate.now().plusDays(2).toString(), "")) {
      var e = entrada("BTC", binance, "1", "1");
      e.put("dataReferencia", d);
      enviar("a", e, UUID.randomUUID().toString(), null).andExpect(status().isBadRequest());
    }
    assertThat(contar("operacoes")).isZero();
    assertThat(contar("pontos_partida")).isZero();
    criar("a", entrada("BTC", binance, "0.000000000000000001", "0.00000001"));
  }

  @Test
  void idempotenciaNormalizaValoresERetornaMesmoIdMesmoAposCancelamentoECatalogoInativo()
      throws Exception {
    var chave = UUID.randomUUID().toString();
    var entrada = entrada("BTC", binance, "0.01", "4000.00");
    var original =
        node(enviar("a", entrada, chave, null).andExpect(status().isCreated()).andReturn());
    entrada.put("quantidade", "0.0100");
    entrada.put("custoTotalBrl", "4000.000");
    enviar("a", entrada, chave, null)
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.id").value(id(original)));
    entrada.put("custoTotalBrl", "4001");
    enviar("a", entrada, chave, null).andExpect(status().isConflict());
    entrada.put("custoTotalBrl", "4000");
    enviar("a", entrada, UUID.randomUUID().toString(), null).andExpect(status().isConflict());
    cancelar("a", id(original), "Revisar").andExpect(status().isOk());
    jdbc.update("UPDATE instituicoes SET ativo=FALSE WHERE id=?", binance);
    enviar("a", entrada, chave, null)
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.situacao").value("CANCELADA"));
    assertThat(contar("operacoes")).isEqualTo(1);
  }

  @Test
  void dataUnicaPodeSerRedefinidaSomenteSemConfirmados() throws Exception {
    var original = criar("a", entrada("BTC", binance, "1", "10"));
    var outraData = entrada("ETH", bitget, "1", "20");
    outraData.put("dataReferencia", LocalDate.parse(data).plusDays(1).toString());
    enviar("a", outraData, UUID.randomUUID().toString(), null).andExpect(status().isConflict());
    cancelar("a", id(original), "Data incorreta").andExpect(status().isOk());
    criar("a", outraData);
    assertThat(
            jdbc.queryForObject(
                "SELECT data_referencia FROM pontos_partida WHERE perfil_ref='a'", String.class))
        .isEqualTo(outraData.get("dataReferencia"));
    assertThat(
            jdbc.queryForObject(
                "SELECT data_referencia FROM operacoes WHERE id=?", String.class, id(original)))
        .isEqualTo(data);
  }

  @Test
  void correcaoPreservaFatoAnteriorERepeticaoNaoCriaOutraVersao() throws Exception {
    var original = criar("a", entrada("BTC", binance, "0.01", "4000"));
    var e = entrada("BTC", binance, "0.01", "4200");
    e.put("motivo", "Extrato conferido");
    var chave = UUID.randomUUID().toString();
    var corrigido =
        node(enviar("a", e, chave, id(original)).andExpect(status().isCreated()).andReturn());
    assertThat(id(corrigido)).isNotEqualTo(id(original));
    assertThat(corrigido.get("substituiId").stringValue()).isEqualTo(id(original));
    enviar("a", e, chave, id(original))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.id").value(id(corrigido)));
    mvc.perform(get("/api/v1/saldos-iniciais/" + id(original)).header("Authorization", bearer("a")))
        .andExpect(jsonPath("$.situacao").value("SUBSTITUIDA"))
        .andExpect(jsonPath("$.custoTotalBrl").value("4000"));
    mvc.perform(get("/api/v1/posicoes-iniciais").header("Authorization", bearer("a")))
        .andExpect(jsonPath("$[0].custoMedioBrl").value("420000"));
    assertThat(contar("operacoes")).isEqualTo(2);
    assertThat(
            jdbc.queryForObject(
                "SELECT criado_por FROM operacoes WHERE id=?", String.class, id(corrigido)))
        .isEqualTo("a");
  }

  @Test
  void conflitoDeCorrecaoReverteInclusiveSituacaoAnterior() throws Exception {
    var original = criar("a", entrada("BTC", binance, "1", "10"));
    criar("a", entrada("ETH", binance, "1", "20"));
    var e = entrada("ETH", binance, "2", "30");
    e.put("motivo", "Revisão");
    enviar("a", e, UUID.randomUUID().toString(), id(original)).andExpect(status().isConflict());
    assertThat(
            jdbc.queryForObject(
                "SELECT situacao FROM operacoes WHERE id=?", String.class, id(original)))
        .isEqualTo("CONFIRMADA");
    assertThat(contar("operacoes")).isEqualTo(2);
  }

  @Test
  void cancelamentoIdempotenteNaoApagaHistorico() throws Exception {
    var original = criar("a", entrada("BTC", binance, "1", "10"));
    cancelar("a", id(original), "  Revisão  ").andExpect(status().isOk());
    cancelar("a", id(original), "Revisão").andExpect(status().isOk());
    cancelar("a", id(original), "Outro motivo").andExpect(status().isConflict());
    mvc.perform(get("/api/v1/posicoes-iniciais").header("Authorization", bearer("a")))
        .andExpect(content().json("[]"));
    mvc.perform(
            get("/api/v1/saldos-iniciais")
                .header("Authorization", bearer("a"))
                .param("situacao", "TODAS"))
        .andExpect(jsonPath("$.total").value(1))
        .andExpect(jsonPath("$.possuiSaldosConfirmados").value(false));
  }

  @Test
  void outroUsuarioNaoConsultaCorrigeCancelaOuFiltraCarteiraAlheia() throws Exception {
    var original = criar("a", entrada("BTC", binance, "1", "10"));
    mvc.perform(get("/api/v1/saldos-iniciais/" + id(original)).header("Authorization", bearer("b")))
        .andExpect(status().isNotFound());
    cancelar("b", id(original), "Motivo").andExpect(status().isNotFound());
    var e = entrada("BTC", binance, "1", "11");
    e.put("motivo", "Revisão");
    enviar("b", e, UUID.randomUUID().toString(), id(original)).andExpect(status().isNotFound());
    assertThat(
            jdbc.queryForObject(
                "SELECT COUNT(*) FROM pontos_partida WHERE perfil_ref='b'", Integer.class))
        .isZero();
    jdbc.update("INSERT INTO carteiras(id,perfil_ref,nome) VALUES (101,'a','Privada')");
    mvc.perform(
            get("/api/v1/saldos-iniciais")
                .header("Authorization", bearer("b"))
                .param("carteiraIds", "101"))
        .andExpect(status().isNotFound());
  }

  @Test
  void chavesMotivosCatalogoEFiltrosInvalidosNaoGravam() throws Exception {
    var e = entrada("BTC", binance, "1", "10");
    enviar("a", e, null, null).andExpect(status().isBadRequest());
    enviar("a", e, "invalida", null).andExpect(status().isBadRequest());
    e.put("instituicaoId", "99999");
    enviar("a", e, UUID.randomUUID().toString(), null).andExpect(status().isBadRequest());
    e.put("instituicaoId", Long.toString(binance));
    jdbc.update("UPDATE ativos SET ativo=FALSE WHERE ticker='BTC'");
    enviar("a", e, UUID.randomUUID().toString(), null).andExpect(status().isBadRequest());
    assertThat(contar("pontos_partida")).isZero();
    mvc.perform(
            get("/api/v1/saldos-iniciais")
                .header("Authorization", bearer("a"))
                .param("pagina", "-1"))
        .andExpect(status().isBadRequest());
    mvc.perform(
            get("/api/v1/saldos-iniciais")
                .header("Authorization", bearer("a"))
                .param("situacao", "INVALIDA"))
        .andExpect(status().isBadRequest());
  }

  @Test
  void primeirasGravacoesConcorrentesNaoCriamDatasDiferentes() throws Exception {
    var a = entrada("BTC", binance, "1", "10");
    var b = entrada("ETH", bitget, "1", "20");
    b.put("dataReferencia", LocalDate.parse(data).plusDays(1).toString());
    var resultados =
        concorrer(
            () -> enviar("a", a, UUID.randomUUID().toString(), null).andReturn(),
            () -> enviar("a", b, UUID.randomUUID().toString(), null).andReturn());
    assertThat(resultados).containsExactlyInAnyOrder(201, 409);
    assertThat(contar("operacoes")).isEqualTo(1);
    assertThat(
            jdbc.queryForObject(
                "SELECT COUNT(*) FROM operacoes o JOIN pontos_partida p ON"
                    + " o.perfil_ref=p.perfil_ref WHERE o.data_referencia<>p.data_referencia",
                Integer.class))
        .isZero();
  }

  @Test
  void repeticoesConcorrentesNaoDuplicamERequisicaoPodeSerRetomada() throws Exception {
    var e = entrada("BTC", binance, "1", "10");
    var chave = UUID.randomUUID().toString();
    var resultados =
        concorrer(
            () -> enviar("a", e, chave, null).andReturn(),
            () -> enviar("a", e, chave, null).andReturn());
    assertThat(resultados).allMatch(s -> s == 201 || s == 200 || s == 409).contains(201);
    enviar("a", e, chave, null).andExpect(status().isOk());
    assertThat(contar("operacoes")).isEqualTo(1);
  }

  @Test
  void correcaoECancelamentoSimultaneosNaoAlteramDuasVezes() throws Exception {
    var original = criar("a", entrada("BTC", binance, "1", "10"));
    var e = entrada("BTC", binance, "1", "20");
    e.put("motivo", "Revisão");
    var resultados =
        concorrer(
            () -> enviar("a", e, UUID.randomUUID().toString(), id(original)).andReturn(),
            () -> cancelar("a", id(original), "Cancelar").andReturn());
    assertThat(resultados).contains(409);
    assertThat(resultados).anyMatch(s -> s == 200 || s == 201);
  }

  private List<Integer> concorrer(Callable<MvcResult> a, Callable<MvcResult> b) throws Exception {
    var inicio = new CountDownLatch(1);
    try (var executor = Executors.newVirtualThreadPerTaskExecutor()) {
      var tarefas =
          List.of(a, b).stream()
              .map(t ->
                  executor.submit(() -> {
                    inicio.await();
                    return t.call().getResponse().getStatus();
                  }))
              .toList();
      inicio.countDown();
      return List.of(
          tarefas.get(0).get(20, TimeUnit.SECONDS), tarefas.get(1).get(20, TimeUnit.SECONDS));
    }
  }

  private Map<String, Object> entrada(
      String ativo, long instituicao, String quantidade, String custo) {
    var e = new HashMap<String, Object>();
    e.put("instituicaoId", Long.toString(instituicao));
    e.put(
        "ativoId",
        jdbc.queryForObject(
            "SELECT CAST(id AS CHAR) FROM ativos WHERE ticker=?", String.class, ativo));
    e.put("dataReferencia", data);
    e.put("quantidade", quantidade);
    e.put("custoTotalBrl", custo);
    e.put("custoZeroConfirmado", false);
    return e;
  }

  private org.springframework.test.web.servlet.ResultActions enviar(
      String perfil, Map<String, Object> e, String chave, String anterior) throws Exception {
    var request =
        post(anterior == null
                ? "/api/v1/saldos-iniciais"
                : "/api/v1/saldos-iniciais/" + anterior + "/substituicoes")
            .header("Authorization", bearer(perfil))
            .contentType("application/json")
            .content(json.writeValueAsString(e));
    if (chave != null) request.header("Idempotency-Key", chave);
    return mvc.perform(request);
  }

  private JsonNode criar(String perfil, Map<String, Object> e) throws Exception {
    return node(
        enviar(perfil, e, UUID.randomUUID().toString(), null)
            .andExpect(status().isCreated())
            .andReturn());
  }

  private org.springframework.test.web.servlet.ResultActions cancelar(
      String perfil, String id, String motivo) throws Exception {
    return mvc.perform(
        post("/api/v1/saldos-iniciais/" + id + "/cancelamento")
            .header("Authorization", bearer(perfil))
            .contentType("application/json")
            .content(json.writeValueAsString(Map.of("motivo", motivo))));
  }

  private String bearer(String perfil) {
    var encoder = Base64.getUrlEncoder().withoutPadding();
    return "Bearer "
        + encoder.encodeToString(json.writeValueAsBytes(Map.of("alg", "none")))
        + "."
        + encoder.encodeToString(
            json.writeValueAsBytes(Map.of("sub", perfil, "name", "Teste", "role", "acesso_total")))
        + ".";
  }

  private JsonNode node(MvcResult r) {
    return json.readTree(r.getResponse().getContentAsByteArray());
  }

  private String id(JsonNode n) {
    return n.get("id").stringValue();
  }

  private int contar(String tabela) {
    return jdbc.queryForObject("SELECT COUNT(*) FROM " + tabela, Integer.class);
  }
}
