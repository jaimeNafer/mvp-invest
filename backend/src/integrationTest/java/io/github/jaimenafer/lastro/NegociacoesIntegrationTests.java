package io.github.jaimenafer.lastro;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.doAnswer;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import io.github.jaimenafer.lastro.operacoes.domain.repository.IOperacaoRepository;
import java.util.*;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import org.junit.jupiter.api.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean;
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
  @MockitoSpyBean IOperacaoRepository repositorio;
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
  void visaoGeralCalculaPeriodoSemPerderCustoAnterior() throws Exception {
    criar("a", aporte("1000"));
    var compra = negocio("COMPRA", "2", "200");
    compra.put("taxaAtivoId", Long.toString(brl));
    compra.put("taxaQuantidade", "2");
    criar("a", compra);
    var venda = negocio("VENDA", "1", "150");
    venda.put("ocorridaEm", "2026-10-06T12:00:00-03:00");
    venda.put("taxaAtivoId", Long.toString(brl));
    venda.put("taxaQuantidade", "1");
    criar("a", venda);
    criar("b", aporte("9999"));
    mvc.perform(
            get("/api/v1/visao-geral")
                .header("Authorization", bearer("a"))
                .param("dataInicio", "2026-10-06")
                .param("dataFim", "2026-10-06"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.revisao").value("3"))
        .andExpect(jsonPath("$.dataReferencia").value("2026-10-05"))
        .andExpect(jsonPath("$.possuiRegistrosAteFim").value(true))
        .andExpect(jsonPath("$.resumo.custoSaldosBrl").value("1048"))
        .andExpect(jsonPath("$.resumo.resultadoRealizadoBrl").value("48"))
        .andExpect(jsonPath("$.resumo.taxasBrl").value("1"))
        .andExpect(jsonPath("$.resumo.aportesBrl").value("0"))
        .andExpect(jsonPath("$.resumo.operacoesNoPeriodo").value(1))
        .andExpect(jsonPath("$.resultado.negociacaoBrl").value("49"))
        .andExpect(jsonPath("$.resultado.moedaBrl").value("0"))
        .andExpect(jsonPath("$.resultado.taxaBrl").value("-1"))
        .andExpect(jsonPath("$.posicoes[?(@.ativoTicker == 'BTC')].custoTotalBrl").value("101"))
        .andExpect(jsonPath("$.posicoes[?(@.ativoTicker == 'BTC')].ativoNome").value("Bitcoin"));
    mvc.perform(
            get("/api/v1/visao-geral")
                .header("Authorization", bearer("a"))
                .param("dataInicio", "2026-10-05")
                .param("dataFim", "2026-10-05"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.resumo.custoSaldosBrl").value("1000"))
        .andExpect(jsonPath("$.resumo.taxasBrl").value("2"))
        .andExpect(jsonPath("$.resumo.aportesBrl").value("1000"))
        .andExpect(
            jsonPath("$.posicoes[?(@.ativoTicker == 'BTC')].participacaoPercentual").value("20.2"));
  }

  @Test
  void visaoGeralRespeitaLimiteDoDiaESaldoSemMovimentoNoPeriodo() throws Exception {
    var ultimo = aporte("10");
    ultimo.put("ocorridaEm", "2026-10-06T02:59:59.999999Z");
    criar("a", ultimo);
    var seguinte = aporte("20");
    seguinte.put("ocorridaEm", "2026-10-06T03:00:00Z");
    criar("a", seguinte);
    mvc.perform(
            get("/api/v1/visao-geral")
                .header("Authorization", bearer("a"))
                .param("dataInicio", "2026-10-01")
                .param("dataFim", "2026-10-05"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.resumo.custoSaldosBrl").value("10"))
        .andExpect(jsonPath("$.resumo.operacoesNoPeriodo").value(1));
    mvc.perform(
            get("/api/v1/visao-geral")
                .header("Authorization", bearer("a"))
                .param("dataInicio", "2026-10-07")
                .param("dataFim", "2026-10-07"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.resumo.custoSaldosBrl").value("30"))
        .andExpect(jsonPath("$.resumo.operacoesNoPeriodo").value(0));
  }

  @Test
  void visaoGeralSemHistoricoUsaHojeENaoExigeCarteira() throws Exception {
    String hoje = java.time.LocalDate.now(java.time.ZoneId.of("America/Fortaleza")).toString();
    mvc.perform(get("/api/v1/visao-geral").header("Authorization", bearer("a")))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.dataInicio").value(hoje))
        .andExpect(jsonPath("$.dataFim").value(hoje))
        .andExpect(jsonPath("$.dataReferencia").isEmpty())
        .andExpect(jsonPath("$.possuiRegistrosAteFim").value(false))
        .andExpect(jsonPath("$.resumo.custoSaldosBrl").value("0"))
        .andExpect(jsonPath("$.posicoes").isEmpty());
    mvc.perform(get("/api/v1/visao-geral")).andExpect(status().isUnauthorized());
  }

  @ParameterizedTest
  @CsvSource({
    "2026-10-01,2026-09-30",
    "2026-02-30,2026-10-01",
    "2026-1-01,2026-10-01",
    "2999-01-01,2999-01-02",
    "2026-10-01,''",
    "'',2026-10-01",
    "0999-01-01,2026-10-01"
  })
  void visaoGeralRejeitaPeriodoInvalido(String inicio, String fim) throws Exception {
    mvc.perform(
            get("/api/v1/visao-geral")
                .header("Authorization", bearer("a"))
                .param("dataInicio", inicio)
                .param("dataFim", fim))
        .andExpect(status().isBadRequest());
  }

  @Test
  void visaoGeralMantemSnapshotMesmoComEscritaEntreLeituras() throws Exception {
    criar("a", aporte("100"));
    CountDownLatch leuRevisao = new CountDownLatch(1);
    CountDownLatch gravou = new CountDownLatch(1);
    AtomicBoolean primeiraLeitura = new AtomicBoolean(true);
    doAnswer(
            chamada -> {
              if (primeiraLeitura.compareAndSet(true, false)) {
                leuRevisao.countDown();
                assertThat(gravou.await(20, TimeUnit.SECONDS)).isTrue();
              }
              return chamada.callRealMethod();
            })
        .when(repositorio)
        .historico("a");
    try (var executor = java.util.concurrent.Executors.newSingleThreadExecutor()) {
      var consulta =
          executor.submit(() ->
              corpo(
                  mvc.perform(get("/api/v1/visao-geral").header("Authorization", bearer("a")))
                      .andExpect(status().isOk())));
      assertThat(leuRevisao.await(20, TimeUnit.SECONDS)).isTrue();
      try {
        criar("a", aporte("10"));
      } finally {
        gravou.countDown();
      }
      var anterior = consulta.get(20, TimeUnit.SECONDS);
      assertThat(anterior.get("revisao").stringValue()).isEqualTo("1");
      assertThat(anterior.get("resumo").get("custoSaldosBrl").stringValue()).isEqualTo("100");
    }
    mvc.perform(get("/api/v1/visao-geral").header("Authorization", bearer("a")))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.revisao").value("2"))
        .andExpect(jsonPath("$.resumo.custoSaldosBrl").value("110"));
  }

  @Test
  void visaoGeralContaAberturaSemConfundirComAporteEMantemCustoZero() throws Exception {
    mvc.perform(
            post("/api/v1/saldos-iniciais")
                .header("Authorization", bearer("a"))
                .header("Idempotency-Key", UUID.randomUUID().toString())
                .contentType("application/json")
                .content(
                    json.writeValueAsString(
                        Map.of(
                            "instituicaoId",
                            Long.toString(binance),
                            "ativoId",
                            Long.toString(btc),
                            "dataReferencia",
                            "2026-10-01",
                            "quantidade",
                            "1",
                            "custoTotalBrl",
                            "0",
                            "custoZeroConfirmado",
                            true,
                            "justificativaCustoZero",
                            "Origem comprovada"))))
        .andExpect(status().isCreated());
    jdbc.update("UPDATE ativos SET ativo=FALSE WHERE id=?", btc);
    mvc.perform(get("/api/v1/visao-geral").header("Authorization", bearer("a")))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.dataInicio").value("2026-10-01"))
        .andExpect(jsonPath("$.resumo.operacoesNoPeriodo").value(1))
        .andExpect(jsonPath("$.resumo.aportesBrl").value("0"))
        .andExpect(jsonPath("$.posicoes[0].quantidade").value("1"))
        .andExpect(jsonPath("$.posicoes[0].participacaoPercentual").isEmpty());
    mvc.perform(
            get("/api/v1/visao-geral")
                .header("Authorization", bearer("a"))
                .param("dataInicio", "2026-09-01")
                .param("dataFim", "2026-09-30"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.dataReferencia").value("2026-10-01"))
        .andExpect(jsonPath("$.possuiRegistrosAteFim").value(false))
        .andExpect(jsonPath("$.posicoes").isEmpty());
  }

  @Test
  void visaoGeralConsideraSomenteVersoesVigentes() throws Exception {
    var original = criar("a", aporte("100"));
    var revisada = aporte("150");
    revisada.put("motivo", "Valor conferido");
    var nova =
        corpo(
            mvc.perform(
                    post("/api/v1/operacoes/" + original.get("id").stringValue() + "/substituicoes")
                        .header("Authorization", bearer("a"))
                        .header("Idempotency-Key", UUID.randomUUID().toString())
                        .contentType("application/json")
                        .content(
                            json.writeValueAsString(Map.of("revisao", "1", "operacao", revisada))))
                .andExpect(status().isCreated()));
    mvc.perform(get("/api/v1/visao-geral").header("Authorization", bearer("a")))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.resumo.custoSaldosBrl").value("150"))
        .andExpect(jsonPath("$.resumo.operacoesNoPeriodo").value(1));
    postJson(
            "a",
            "/api/v1/operacoes/" + nova.get("id").stringValue() + "/cancelamento",
            Map.of("revisao", "2", "motivo", "Evento não ocorreu"))
        .andExpect(status().isOk());
    mvc.perform(get("/api/v1/visao-geral").header("Authorization", bearer("a")))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.resumo.custoSaldosBrl").value("0"))
        .andExpect(jsonPath("$.resumo.operacoesNoPeriodo").value(0));
  }

  @Test
  void criptoAdicionadaAoCatalogoPermiteAberturaENegociacaoSemListaFixa() throws Exception {
    jdbc.update(
        "INSERT INTO ativos(codigo,ticker,nome,tipo,casas_decimais,ativo) VALUES"
            + " ('cripto:TESTE','TESTE','Cripto de teste','CRIPTO',18,TRUE)");
    long extra = ativo("TESTE");
    mvc.perform(get("/api/v1/ativos").header("Authorization", bearer("a")))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$[?(@.ticker == 'TESTE')].id").value(Long.toString(extra)));
    validarCicloDoCatalogo("TESTE");
  }

  @ParameterizedTest
  @ValueSource(strings = {"SOL", "LINK", "NEAR", "LDO", "ARKM", "RONIN", "PYTH", "TIA"})
  void ativosNovosPermitemAberturaCompraVendaEPosicao(String ticker) throws Exception {
    validarCicloDoCatalogo(ticker);
  }

  private void validarCicloDoCatalogo(String ticker) throws Exception {
    long extra = ativo(ticker);
    mvc.perform(
            post("/api/v1/saldos-iniciais")
                .header("Authorization", bearer("a"))
                .header("Idempotency-Key", UUID.randomUUID().toString())
                .contentType("application/json")
                .content(
                    json.writeValueAsString(
                        Map.of(
                            "instituicaoId",
                            Long.toString(binance),
                            "ativoId",
                            Long.toString(extra),
                            "dataReferencia",
                            "2026-10-05",
                            "quantidade",
                            "1",
                            "custoTotalBrl",
                            "100",
                            "custoZeroConfirmado",
                            false))))
        .andExpect(status().isCreated());
    criar("a", aporte("1000"));
    var compra = negocio("COMPRA", "1", "200");
    compra.put("ativoId", Long.toString(extra));
    criar("a", compra);
    var venda = negocio("VENDA", "1", "250");
    venda.put("ativoId", Long.toString(extra));
    venda.put("ocorridaEm", "2026-10-05T12:00:00-03:00");
    assertThat(criar("a", venda).get("calculo").get("resultadoTotalBrl").stringValue())
        .isEqualTo("100");
    mvc.perform(get("/api/v1/posicoes").header("Authorization", bearer("a")))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.itens[?(@.ativoTicker == '" + ticker + "')].quantidade").value("1"))
        .andExpect(
            jsonPath("$.itens[?(@.ativoTicker == '" + ticker + "')].custoTotalBrl").value("150"));
  }

  @Test
  void ativoNovoPreservaTaxasHistoricoERepeticaoAposInativacao() throws Exception {
    criar("a", aporte("1000"));
    long sol = ativo("SOL");
    var compra = negocio("COMPRA", "2", "200");
    compra.put("ativoId", Long.toString(sol));
    compra.put("taxaAtivoId", Long.toString(brl));
    compra.put("taxaQuantidade", "2");
    String revisao = previa("a", compra).get("revisao").stringValue();
    String chave = UUID.randomUUID().toString();
    var criada = corpo(gravar("a", compra, revisao, chave).andExpect(status().isCreated()));
    mvc.perform(get("/api/v1/posicoes").header("Authorization", bearer("a")))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.itens[?(@.ativoTicker == 'SOL')].custoTotalBrl").value("202"));
    jdbc.update("UPDATE ativos SET ativo=FALSE WHERE id=?", sol);
    var repetida = corpo(gravar("a", compra, revisao, chave).andExpect(status().isOk()));
    assertThat(repetida.get("id").stringValue()).isEqualTo(criada.get("id").stringValue());
    postJson("a", "/api/v1/operacoes/previa", Map.of("acao", "CRIAR", "operacao", compra))
        .andExpect(status().isBadRequest());
    mvc.perform(
            get("/api/v1/operacoes/" + criada.get("id").stringValue())
                .header("Authorization", bearer("a")))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.ativoTicker").value("SOL"));
    assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM operacoes", Integer.class)).isEqualTo(2);
  }

  @Test
  void catalogoNaoLiberaMoedaFiatComoBaseNemNovasCotacoes() throws Exception {
    criar("a", aporte("1000"));
    var compra = negocio("COMPRA", "1", "100");
    compra.put("ativoId", Long.toString(ativo("USD")));
    postJson("a", "/api/v1/operacoes/previa", Map.of("acao", "CRIAR", "operacao", compra))
        .andExpect(status().isBadRequest());
    compra.put("ativoId", Long.toString(ativo("SOL")));
    compra.put("moedaCotacaoId", Long.toString(ativo("USD")));
    postJson("a", "/api/v1/operacoes/previa", Map.of("acao", "CRIAR", "operacao", compra))
        .andExpect(status().isBadRequest());
    compra.put("moedaCotacaoId", compra.get("ativoId"));
    postJson("a", "/api/v1/operacoes/previa", Map.of("acao", "CRIAR", "operacao", compra))
        .andExpect(status().isBadRequest());
    compra.put("moedaCotacaoId", Long.toString(brl));
    compra.put("quantidade", "0.0000000000000000001");
    postJson("a", "/api/v1/operacoes/previa", Map.of("acao", "CRIAR", "operacao", compra))
        .andExpect(status().isBadRequest());
    compra.put("quantidade", "1");
    compra.put("ativoId", "9223372036854775807");
    postJson("a", "/api/v1/operacoes/previa", Map.of("acao", "CRIAR", "operacao", compra))
        .andExpect(status().isBadRequest());
    var venda = negocio("VENDA", "1", "100");
    venda.put("ativoId", Long.toString(ativo("SOL")));
    postJson("a", "/api/v1/operacoes/previa", Map.of("acao", "CRIAR", "operacao", venda))
        .andExpect(status().isConflict());
    assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM operacoes", Integer.class)).isEqualTo(1);
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
