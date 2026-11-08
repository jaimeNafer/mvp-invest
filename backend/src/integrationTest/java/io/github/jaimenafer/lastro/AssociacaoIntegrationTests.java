package io.github.jaimenafer.lastro;

import static org.assertj.core.api.Assertions.*;
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
import org.testcontainers.junit.jupiter.*;
import org.testcontainers.mysql.MySQLContainer;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

@SpringBootTest(properties = "spring.config.import=")
@AutoConfigureMockMvc
@Testcontainers
class AssociacaoIntegrationTests {
  @Container @ServiceConnection
  static final MySQLContainer MYSQL = new MySQLContainer("mysql:8.0.42");

  @Autowired MockMvc mvc;
  @Autowired JdbcTemplate jdbc;
  @Autowired JsonMapper json;

  @Test
  void conversaoCsvUsaProvenienciaDaLinhaEValorEmBrlConsistente() throws Exception {
    String perfil = "convert-csv-" + UUID.randomUUID();
    byte[] arquivo = csvRelatorio(new String[][] {
        {"123456789", "2024-06-11-07:54:00", "Convert", "", "", "", "0.5", "BNB", "1000", "", "2", "SOL", "1000", "", "0", "", ""},
        {"123456789", "2024-06-11-07:54:00", "Convert", "", "", "", "0.1", "BNB", "200", "", "0.4", "SOL", "200", "", "0", "", ""}
    });
    receber(perfil, arquivo).andExpect(status().isCreated());
    JsonNode p = previa(perfil, timeline(perfil).path("revisao").asText());
    assertThat(p.path("grupos").size()).isEqualTo(2);
    assertThat(p.path("grupos").get(0).path("completo").asBoolean()).isTrue();
    assertThat(p.path("grupos").get(1).path("completo").asBoolean()).isTrue();
    assertThat(p.path("grupos").get(0).path("linhas").size()).isEqualTo(2);
    assertThat(p.path("grupos").get(1).path("linhas").size()).isEqualTo(2);
    assertThat(registro(perfil, "-0.5").path("principalConhecidoBrl").asText()).isEqualTo("1000");
    assertThat(registro(perfil, "-0.1").path("principalConhecidoBrl").asText()).isEqualTo("200");
  }

  @Test
  void conversaoNaoPodeSerInventadaEmUmaLinhaIndividual() throws Exception {
    String perfil = "convert-linha-" + UUID.randomUUID();
    receber(perfil, planilha(new String[][] {
        {"2024-06-11 07:54:00", "Spot", "Binance Convert", "BNB", "-0.5"}
    })).andExpect(status().isCreated());
    JsonNode linha = registro(perfil, "-0.5");
    Map<String, Object> edicao = new LinkedHashMap<>();
    edicao.put("revisao", timeline(perfil).path("revisao").asText());
    edicao.put("versao", linha.path("versao").asText());
    edicao.put("data", linha.path("data").asText());
    edicao.put("contaId", linha.path("contaId").asText());
    edicao.put("ativoId", linha.path("ativoId").asText());
    edicao.put("quantidade", "-0.5");
    edicao.put("natureza", "CONVERSAO");
    edicao.put("motivo", "Conferência");
    mvc.perform(put("/api/v1/conciliacao/registros/" + linha.path("id").asText())
            .header("Authorization", bearer(perfil))
            .header("Idempotency-Key", UUID.randomUUID().toString())
            .contentType("application/json").content(json.writeValueAsString(edicao)))
        .andExpect(status().isBadRequest());
  }

  @Test
  void conversaoCsvComTaxaSemMoedaNaoIgnoraATaxa() throws Exception {
    String perfil = "convert-csv-taxa-invalida-" + UUID.randomUUID();
    receber(perfil, csvRelatorio(new String[][] {
        {"123456789", "2024-06-11-07:54:00", "Convert", "", "", "", "0.5", "BNB", "1000", "", "2", "SOL", "1000", "", "0.01", "", ""}
    })).andExpect(status().isCreated());
    assertThat(previa(perfil, timeline(perfil).path("revisao").asText()).path("grupos").size()).isZero();
  }

  @Test
  void conversaoCsvMantemTaxaDaMesmaLinhaSemPresumirValorFinanceiro() throws Exception {
    String perfil = "convert-csv-taxa-" + UUID.randomUUID();
    receber(perfil, csvRelatorio(new String[][] {
        {"123456789", "2024-06-11-07:54:00", "Convert", "", "", "", "0.5", "BNB", "1000", "", "2", "SOL", "1000", "", "0.01", "SOL", "5"}
    })).andExpect(status().isCreated());
    JsonNode grupos = previa(perfil, timeline(perfil).path("revisao").asText()).path("grupos");
    assertThat(grupos.size()).isEqualTo(1);
    JsonNode grupo = grupos.get(0);
    assertThat(grupo.path("linhas").size()).isEqualTo(3);
    assertThat(grupo.path("taxas").get(0).path("quantidade").asText()).isEqualTo("0.01");
    assertThat(grupo.path("principalBrl").isNull()).isTrue();
  }

  @Test
  void conversaoCsvParaStablecoinDerivaCotacaoDaMoedaRecebida() throws Exception {
    String perfil = "convert-stable-" + UUID.randomUUID();
    receber(perfil, csvRelatorio(new String[][] {
        {"123456789", "2024-06-11-07:54:00", "Convert", "", "", "", "0.5", "BNB", "1000", "", "200", "USDT", "1000", "", "0", "", ""}
    })).andExpect(status().isCreated());
    JsonNode grupo = previa(perfil, timeline(perfil).path("revisao").asText()).path("grupos").get(0);
    assertThat(grupo.path("natureza").asText()).isEqualTo("VENDA");
    assertThat(new java.math.BigDecimal(grupo.path("cotacaoBrl").asText())).isEqualByComparingTo("5");
    assertThat(new java.math.BigDecimal(grupo.path("principalBrl").asText())).isEqualByComparingTo("1000");
    assertThat(grupo.path("completo").asBoolean()).isTrue();
  }

  @Test
  void conversaoCsvComValoresEmBrlContraditoriosNaoInventaCotacao() throws Exception {
    String perfil = "convert-csv-incerto-" + UUID.randomUUID();
    receber(perfil, csvRelatorio(new String[][] {
        {"123456789", "2024-06-11-07:54:00", "Convert", "", "", "", "0.5", "BNB", "1000", "", "2", "SOL", "900", "", "0", "", ""}
    })).andExpect(status().isCreated());
    JsonNode grupo = previa(perfil, timeline(perfil).path("revisao").asText()).path("grupos").get(0);
    assertThat(grupo.path("principalBrl").isNull()).isTrue();
    assertThat(grupo.path("completo").asBoolean()).isFalse();
    assertThat(grupo.path("pendencias").isEmpty()).isTrue();
  }

  @Test
  void conversaoComMultiplasExecucoesPreservaLinhasEQuantidades() throws Exception {
    String perfil = "convert-multiplas-" + UUID.randomUUID();
    receber(perfil, planilha(new String[][] {
        {"2024-06-01 10:00:00", "Spot", "Megadrop Rewards", "BNB", "3"},
        {"2024-06-11 07:54:00", "Spot", "Binance Convert", "BNB", "-1", "Order/abc"},
        {"2024-06-11 07:54:00", "Spot", "Binance Convert", "BNB", "-1", "Order/abc"},
        {"2024-06-11 07:54:00", "Spot", "Binance Convert", "SOL", "0.5", "Order/abc"},
        {"2024-06-11 07:54:00", "Spot", "Binance Convert", "SOL", "1.5", "Order/abc"}
    })).andExpect(status().isCreated());
    JsonNode p = previa(perfil, timeline(perfil).path("revisao").asText());
    assertThat(p.path("grupos").size()).isEqualTo(1);
    assertThat(p.path("grupos").get(0).path("linhas").size()).isEqualTo(4);
    assertThat(p.path("grupos").get(0).path("pagamentos").get(0).path("quantidade").asText()).isEqualTo("2");
    assertThat(p.path("grupos").get(0).path("recebimentos").get(0).path("quantidade").asText()).isEqualTo("2");
    confirmarTudo(perfil, p);
    JsonNode posicoes = corpo(mvc.perform(get("/api/v1/posicoes")
        .header("Authorization", bearer(perfil))).andExpect(status().isOk()));
    JsonNode bnb = java.util.stream.StreamSupport.stream(posicoes.path("itens").spliterator(), false)
        .filter(v -> v.path("ativoTicker").asText().equals("BNB")).findFirst().orElseThrow();
    JsonNode sol = java.util.stream.StreamSupport.stream(posicoes.path("itens").spliterator(), false)
        .filter(v -> v.path("ativoTicker").asText().equals("SOL")).findFirst().orElseThrow();
    assertThat(new java.math.BigDecimal(bnb.path("quantidade").asText())).isEqualByComparingTo("1");
    assertThat(new java.math.BigDecimal(sol.path("quantidade").asText())).isEqualByComparingTo("2");
    assertThat(timeline(perfil).path("linhasPorGrupo").iterator().next().size()).isEqualTo(4);
  }

  @Test
  void conversaoComContaAusenteNaoEAssociadaAutomaticamente() throws Exception {
    String perfil = "convert-sem-conta-" + UUID.randomUUID();
    receber(perfil, planilha(new String[][] {
        {"2024-06-11 07:54:00", "", "Binance Convert", "BNB", "-0.5"},
        {"2024-06-11 07:54:00", "", "Binance Convert", "SOL", "2"}
    })).andExpect(status().isCreated());
    assertThat(previa(perfil, timeline(perfil).path("revisao").asText()).path("grupos").size()).isZero();
    assertThat(registro(perfil, "-0.5").path("grupoAutomatico").isNull()).isTrue();
  }

  @Test
  void converteBrlEmBnbComCustoConhecido() throws Exception {
    String perfil = "convert-brl-" + UUID.randomUUID();
    byte[] arquivo = planilha(new String[][] {
        {"2024-06-01 09:00:00", "Spot", "Deposit", "BRL", "1000"},
        {"2024-06-11 07:54:00", "Spot", "Binance Convert", "BRL", "-1000"},
        {"2024-06-11 07:54:00", "Spot", "Binance Convert", "BNB", "0.27536352"}
    });
    receber(perfil, arquivo).andExpect(status().isCreated());
    assertThat(registro(perfil, "-1000").path("grupoAutomatico").asText()).isNotBlank()
        .isEqualTo(registro(perfil, "0.27536352").path("grupoAutomatico").asText());
    confirmarTudo(perfil, previa(perfil, timeline(perfil).path("revisao").asText()));
    receber(perfil, arquivo).andExpect(status().isCreated());
    confirmarTudo(perfil, previa(perfil, timeline(perfil).path("revisao").asText()));
    JsonNode posicoes = corpo(mvc.perform(get("/api/v1/posicoes")
        .header("Authorization", bearer(perfil))).andExpect(status().isOk()));
    JsonNode bnb = java.util.stream.StreamSupport.stream(posicoes.path("itens").spliterator(), false)
        .filter(p -> p.path("ativoTicker").asText().equals("BNB")).findFirst().orElseThrow();
    assertThat(new java.math.BigDecimal(bnb.path("quantidade").asText())).isEqualByComparingTo("0.27536352");
    assertThat(new java.math.BigDecimal(bnb.path("custoTotalBrl").asText())).isEqualByComparingTo("1000");
  }

  @Test
  void conversaoCriptoComCotacaoInformadaCalculaCustoEResultado() throws Exception {
    String perfil = "convert-valor-" + UUID.randomUUID();
    receber(perfil, planilha(new String[][] {
        {"2024-06-01 09:00:00", "Spot", "Deposit", "BRL", "1000"},
        {"2024-06-01 10:00:00", "Spot", "Buy Crypto With Fiat", "BRL", "-1000"},
        {"2024-06-01 10:00:01", "Spot", "Buy Crypto With Fiat", "BNB", "1"},
        {"2024-06-11 07:54:00", "Spot", "Binance Convert", "BNB", "-0.5"},
        {"2024-06-11 07:54:01", "Spot", "Binance Convert", "SOL", "2"}
    })).andExpect(status().isCreated());
    var pedido = pedido(perfil, List.of(registro(perfil, "-0.5"), registro(perfil, "2")), null, null, true, false);
    pedido.put("cotacaoBrl", "2000");
    var grupo = corpo(enviar(perfil, "/api/v1/conciliacao/associacoes/previa", pedido, null)
        .andExpect(status().isOk()));
    assertThat(grupo.path("natureza").asText()).isEqualTo("CONVERSAO");
    assertThat(grupo.path("completo").asBoolean()).isTrue();
    assertThat(new java.math.BigDecimal(grupo.path("principalBrl").asText())).isEqualByComparingTo("1000");
    var salvo = salvarGrupo(perfil, pedido);
    confirmarTudo(perfil, previa(perfil, timeline(perfil).path("revisao").asText()));
    assertThat(registro(perfil, "-0.5").path("grupoNumero").asText())
        .isEqualTo(salvo.path("numero").asText());
    assertThat(previa(perfil, timeline(perfil).path("revisao").asText()).path("grupos").size()).isZero();
    JsonNode posicoes = corpo(mvc.perform(get("/api/v1/posicoes")
        .header("Authorization", bearer(perfil))).andExpect(status().isOk()));
    JsonNode sol = java.util.stream.StreamSupport.stream(posicoes.path("itens").spliterator(), false)
        .filter(p -> p.path("ativoTicker").asText().equals("SOL")).findFirst().orElseThrow();
    assertThat(new java.math.BigDecimal(sol.path("custoTotalBrl").asText())).isEqualByComparingTo("1000");
    JsonNode painel = corpo(mvc.perform(get("/api/v1/visao-geral")
        .header("Authorization", bearer(perfil))).andExpect(status().isOk()));
    assertThat(new java.math.BigDecimal(painel.path("resumo").path("resultadoRealizadoBrl").asText()))
        .isEqualByComparingTo("500");
  }

  @Test
  void converteBnbEmSolSemInventarValorEConservaQuantidadeUmaVez() throws Exception {
    String perfil = "convert-cripto-" + UUID.randomUUID();
    byte[] arquivo = planilha(new String[][] {
        {"2024-06-01 09:00:00", "Spot", "Deposit", "BRL", "1000"},
        {"2024-06-01 10:00:00", "Spot", "Buy Crypto With Fiat", "BRL", "-1000"},
        {"2024-06-01 10:00:01", "Spot", "Buy Crypto With Fiat", "BNB", "1"},
        {"2024-06-11 07:54:00", "Spot", "Binance Convert", "BNB", "-0.14536437"},
        {"2024-06-11 07:54:02", "Spot", "Binance Convert", "SOL", "0.57296862"}
    });
    receber(perfil, arquivo).andExpect(status().isCreated());
    JsonNode saida = registro(perfil, "-0.14536437");
    JsonNode entrada = registro(perfil, "0.57296862");
    assertThat(saida.path("grupoAutomatico").asText()).isNotBlank()
        .isEqualTo(entrada.path("grupoAutomatico").asText());
    assertThat(saida.path("estado").asText()).isEqualTo("COERENTE");
    assertThat(entrada.path("estado").asText()).isEqualTo("COERENTE");
    assertThat(entrada.path("pendencias").size()).isZero();
    confirmarTudo(perfil, previa(perfil, timeline(perfil).path("revisao").asText()));
    receber(perfil, arquivo).andExpect(status().isCreated());
    confirmarTudo(perfil, previa(perfil, timeline(perfil).path("revisao").asText()));
    JsonNode posicoes = corpo(mvc.perform(get("/api/v1/posicoes")
        .header("Authorization", bearer(perfil))).andExpect(status().isOk()));
    JsonNode bnb = java.util.stream.StreamSupport.stream(posicoes.path("itens").spliterator(), false)
        .filter(p -> p.path("ativoTicker").asText().equals("BNB")).findFirst().orElseThrow();
    JsonNode sol = java.util.stream.StreamSupport.stream(posicoes.path("itens").spliterator(), false)
        .filter(p -> p.path("ativoTicker").asText().equals("SOL")).findFirst().orElseThrow();
    assertThat(new java.math.BigDecimal(bnb.path("quantidade").asText())).isEqualByComparingTo("0.85463563");
    assertThat(new java.math.BigDecimal(bnb.path("custoTotalBrl").asText())).isEqualByComparingTo("854.63563");
    assertThat(new java.math.BigDecimal(sol.path("quantidade").asText())).isEqualByComparingTo("0.57296862");
    assertThat(sol.path("custoTotalBrl").isNull()).isTrue();
    JsonNode painel = corpo(mvc.perform(get("/api/v1/visao-geral")
        .header("Authorization", bearer(perfil))).andExpect(status().isOk()));
    assertThat(painel.path("resumo").path("resultadoRealizadoBrl").isNull()).isTrue();
  }

  @Test
  void movimentacaoEarnNaoAlteraAssociacaoDeBinanceConvert() throws Exception {
    String semEarn = "convert-sem-earn-" + UUID.randomUUID();
    String comEarn = "convert-com-earn-" + UUID.randomUUID();
    String[] saida = {"2024-06-09 10:00:00", "Spot", "Binance Convert", "BRL", "-1000"};
    String[] entrada = {"2024-06-09 10:00:01", "Spot", "Binance Convert", "BNB", "0.27536352"};
    receber(semEarn, planilha(new String[][] {saida, entrada})).andExpect(status().isCreated());
    receber(comEarn, planilha(new String[][] {saida, entrada,
        {"2024-06-09 10:00:01", "Spot", "Simple Earn Locked Subscription", "BNB", "-0.13"}}))
        .andExpect(status().isCreated());
    JsonNode previaSem = previa(semEarn, timeline(semEarn).path("revisao").asText());
    JsonNode previaCom = previa(comEarn, timeline(comEarn).path("revisao").asText());
    assertThat(previaCom.path("grupos").size()).isEqualTo(previaSem.path("grupos").size());
    assertThat(registro(comEarn, "-1000").path("natureza").asText())
        .isEqualTo(registro(semEarn, "-1000").path("natureza").asText());
    assertThat(registro(comEarn, "0.27536352").path("natureza").asText())
        .isEqualTo(registro(semEarn, "0.27536352").path("natureza").asText());
  }

  @Test
  void compraDeBnbEAplicacaoPreservamQuantidadeECusto() throws Exception {
    String perfil = "earn-compra-" + UUID.randomUUID();
    receber(perfil, planilha(new String[][] {
        {"2024-06-01 09:00:00", "Spot", "Deposit", "BRL", "1000"},
        {"2024-06-09 10:00:00", "Spot", "Buy Crypto With Fiat", "BRL", "-1000"},
        {"2024-06-09 10:00:01", "Spot", "Buy Crypto With Fiat", "BNB", "0.27536352"},
        {"2024-06-09 11:00:00", "Spot", "Simple Earn Locked Subscription", "BNB", "-0.13"}
    })).andExpect(status().isCreated());
    confirmarTudo(perfil, previa(perfil, timeline(perfil).path("revisao").asText()));
    JsonNode posicoes = corpo(mvc.perform(get("/api/v1/posicoes")
        .header("Authorization", bearer(perfil))).andExpect(status().isOk()));
    JsonNode bnb = java.util.stream.StreamSupport.stream(posicoes.path("itens").spliterator(), false)
        .filter(p -> p.path("ativoTicker").asText().equals("BNB")).findFirst().orElseThrow();
    assertThat(new java.math.BigDecimal(bnb.path("quantidade").asText())).isEqualByComparingTo("0.27536352");
    assertThat(new java.math.BigDecimal(bnb.path("custoTotalBrl").asText())).isEqualByComparingTo("1000");
    assertThat(new java.math.BigDecimal(bnb.path("custoMedioBrl").asText()))
        .isEqualByComparingTo(new java.math.BigDecimal("1000")
            .divide(new java.math.BigDecimal("0.27536352"), 36, java.math.RoundingMode.HALF_EVEN));
  }

  @Test
  void eventosEarnIsoladosNaoCriamNemConsomemPosicao() throws Exception {
    for (String[] caso : List.of(
        new String[] {"Simple Earn Locked Subscription", "-0.13"},
        new String[] {"Simple Earn Flexible Redemption", "0.13"})) {
      String perfil = "earn-isolado-" + UUID.randomUUID();
      receber(perfil, planilha(new String[][] {
          {"2024-06-09 10:00:00", "Spot", caso[0], "BNB", caso[1]}
      })).andExpect(status().isCreated());
      JsonNode evento = registro(perfil, caso[1]);
      assertThat(evento.path("estado").asText()).isEqualTo("COERENTE");
      JsonNode p = previa(perfil, timeline(perfil).path("revisao").asText());
      assertThat(p.path("ajustes").size()).isZero();
      confirmarTudo(perfil, p);
      JsonNode posicoes = corpo(mvc.perform(get("/api/v1/posicoes")
          .header("Authorization", bearer(perfil))).andExpect(status().isOk()));
      assertThat(posicoes.path("itens").toString()).doesNotContain("BNB");
      assertThat(registro(perfil, caso[1]).path("estado").asText()).isEqualTo("COERENTE");
    }
  }

  @Test
  void earnDesconhecidoOuComSinalContraditorioNaoEhClassificadoComoInterno() throws Exception {
    String perfil = "earn-invalido-" + UUID.randomUUID();
    receber(perfil, planilha(new String[][] {
        {"2024-06-09 10:00:00", "Spot", "Earn Unknown", "BNB", "-0.01"},
        {"2024-06-09 10:01:00", "Spot", "Simple Earn Locked Subscription", "BNB", "0.03"}
    })).andExpect(status().isCreated());
    assertThat(registro(perfil, "-0.01").path("natureza").asText()).isEqualTo("OBSERVADA");
    assertThat(registro(perfil, "0.03").path("natureza").asText()).isEqualTo("OBSERVADA");
    assertThat(registro(perfil, "0.03").path("estado").asText()).isNotEqualTo("COERENTE");
  }

  @Test
  void earnJaAplicadoAguardaConfirmacaoParaSubstituirEfeito() throws Exception {
    String perfil = "earn-legado-" + UUID.randomUUID();
    receber(perfil, planilha(new String[][] {
        {"2024-06-09 10:00:00", "Spot", "Megadrop Rewards", "BNB", "1"},
        {"2024-06-09 10:01:00", "Spot", "Simple Earn Locked Subscription", "BNB", "-0.13"}
    })).andExpect(status().isCreated());
    jdbc.update("UPDATE registros_conferencia SET aplicada_json=proposta_json WHERE perfil_ref=?", perfil);
    JsonNode anterior = corpo(mvc.perform(get("/api/v1/posicoes")
        .header("Authorization", bearer(perfil))).andExpect(status().isOk()));
    assertThat(anterior.path("itens").toString()).contains("0.87");
    assertThat(registro(perfil, "-0.13").path("aguardaConfirmacao").asBoolean()).isTrue();
    confirmarTudo(perfil, previa(perfil, timeline(perfil).path("revisao").asText()));
    JsonNode depois = corpo(mvc.perform(get("/api/v1/posicoes")
        .header("Authorization", bearer(perfil))).andExpect(status().isOk()));
    assertThat(depois.path("itens").toString()).contains("\"quantidade\":\"1\"");
    assertThat(registro(perfil, "-0.13").path("aguardaConfirmacao").asBoolean()).isFalse();
  }

  @Test
  void aplicacaoEResgateEarnSaoNeutrosERendimentoContinuaCreditado() throws Exception {
    String perfil = "earn-neutro-" + UUID.randomUUID();
    byte[] arquivo = planilha(new String[][] {
        {"2024-06-09 10:00:00", "Spot", "Megadrop Rewards", "BNB", "0.27536352"},
        {"2024-06-09 10:01:00", "Spot", "Simple Earn Locked Subscription", "BNB", "-0.13"},
        {"2024-06-09 10:02:00", "Spot", "Simple Earn Flexible Subscription", "BNB", "-0.02"},
        {"2024-06-10 10:01:00", "Spot", "Simple Earn Locked Redemption", "BNB", "0.13"},
        {"2024-06-10 10:02:00", "Spot", "Simple Earn Flexible Redemption", "BNB", "0.02"},
        {"2024-06-10 10:03:00", "Spot", "Simple Earn Locked Rewards", "BNB", "0.00000053"}
    });
    receber(perfil, arquivo).andExpect(status().isCreated());
    for (String q : List.of("-0.13", "-0.02", "0.13", "0.02")) {
      JsonNode evento = registro(perfil, q);
      assertThat(evento.path("natureza").asText()).isEqualTo("MOVIMENTACAO_INTERNA");
      assertThat(evento.path("estado").asText()).isEqualTo("COERENTE");
      assertThat(evento.path("pendencias").size()).isZero();
    }
    assertThat(previa(perfil, timeline(perfil).path("revisao").asText()).path("ajustes").size()).isZero();
    confirmarTudo(perfil, previa(perfil, timeline(perfil).path("revisao").asText()));
    receber(perfil, arquivo).andExpect(status().isCreated());
    confirmarTudo(perfil, previa(perfil, timeline(perfil).path("revisao").asText()));
    JsonNode posicoes = corpo(mvc.perform(get("/api/v1/posicoes")
        .header("Authorization", bearer(perfil))).andExpect(status().isOk()));
    JsonNode bnb = java.util.stream.StreamSupport.stream(posicoes.path("itens").spliterator(), false)
        .filter(p -> p.path("ativoTicker").asText().equals("BNB")).findFirst().orElseThrow();
    assertThat(new java.math.BigDecimal(bnb.path("quantidade").asText())).isEqualByComparingTo("0.27536405");
    assertThat(bnb.path("custoTotalBrl").isNull()).isTrue();
  }

  @Test
  void recompensasValidasSaoCoerentesSemCustoEAplicadasUmaVez() throws Exception {
    String perfil = "creditos-" + UUID.randomUUID();
    byte[] arquivo = planilha(new String[][] {
        {"2024-06-10 10:00:00", "Spot", "Megadrop Rewards", "BNB", "0.1"},
        {"2024-06-10 10:01:00", "Spot", "Launchpool Airdrop - System Distribution", "BNB", "0.2"},
        {"2024-06-10 10:02:00", "Spot", "Simple Earn Locked Rewards", "BNB", "0.3"}
    });
    receber(perfil, arquivo).andExpect(status().isCreated());
    jdbc.update("UPDATE registros_conferencia SET proposta_json = JSON_SET(proposta_json, '$.ativoId', NULL) WHERE perfil_ref = ?", perfil);
    for (String quantidade : List.of("0.1", "0.2", "0.3")) {
      JsonNode linha = registro(perfil, quantidade);
      assertThat(linha.path("natureza").asText()).isEqualTo("CREDITO_AUTOMATICO");
      assertThat(linha.path("estado").asText()).isEqualTo("COERENTE");
      assertThat(linha.path("pendencias").size()).isZero();
    }
    JsonNode resumo = timeline(perfil).path("resumo");
    assertThat(resumo.path("creditosAutomaticos").asInt()).isEqualTo(3);
    assertThat(resumo.path("pendentes").asInt()).isZero();
    assertThat(registro(perfil, "0.1").path("subtipoCredito").asText()).isEqualTo("BONIFICACAO");
    assertThat(registro(perfil, "0.2").path("subtipoCredito").asText()).isEqualTo("AIRDROP");
    assertThat(registro(perfil, "0.3").path("subtipoCredito").asText()).isEqualTo("RENDIMENTO");
    confirmarTudo(perfil, previa(perfil, timeline(perfil).path("revisao").asText()));
    receber(perfil, arquivo).andExpect(status().isCreated());
    confirmarTudo(perfil, previa(perfil, timeline(perfil).path("revisao").asText()));
    JsonNode posicoes = corpo(mvc.perform(get("/api/v1/posicoes")
        .header("Authorization", bearer(perfil))).andExpect(status().isOk()));
    JsonNode bnb = java.util.stream.StreamSupport.stream(posicoes.path("itens").spliterator(), false)
        .filter(p -> p.path("ativoTicker").asText().equals("BNB")).findFirst().orElseThrow();
    assertThat(new java.math.BigDecimal(bnb.path("quantidade").asText())).isEqualByComparingTo("0.6");
    assertThat(bnb.path("custoTotalBrl").isNull()).isTrue();
  }

  @Test
  void creditoObservadoJaAplicadoSoMudaClassificacaoNaConfirmacao() throws Exception {
    String perfil = "credito-anterior-" + UUID.randomUUID();
    receber(perfil, planilha(new String[][] {
        {"2024-06-10 10:00:00", "Spot", "Megadrop Rewards", "BNB", "0.1"}
    })).andExpect(status().isCreated());
    jdbc.update("UPDATE registros_conferencia SET aplicada_json=proposta_json WHERE perfil_ref=?", perfil);
    JsonNode linha = registro(perfil, "0.1");
    assertThat(linha.path("natureza").asText()).isEqualTo("CREDITO_AUTOMATICO");
    assertThat(linha.path("aplicado").asBoolean()).isTrue();
    assertThat(linha.path("aguardaConfirmacao").asBoolean()).isTrue();
    confirmarTudo(perfil, previa(perfil, timeline(perfil).path("revisao").asText()));
    assertThat(registro(perfil, "0.1").path("aguardaConfirmacao").asBoolean()).isFalse();
    JsonNode posicoes = corpo(mvc.perform(get("/api/v1/posicoes")
        .header("Authorization", bearer(perfil))).andExpect(status().isOk()));
    JsonNode bnb = java.util.stream.StreamSupport.stream(posicoes.path("itens").spliterator(), false)
        .filter(p -> p.path("ativoTicker").asText().equals("BNB")).findFirst().orElseThrow();
    assertThat(new java.math.BigDecimal(bnb.path("quantidade").asText())).isEqualByComparingTo("0.1");
  }

  @Test
  void sobreposicaoDeArquivosNaoDuplicaCreditoAnterior() throws Exception {
    String perfil = "creditos-sobrepostos-" + UUID.randomUUID();
    String[] credito = {"2024-06-10 10:00:00", "Spot", "Megadrop Rewards", "BNB", "0.1"};
    receber(perfil, planilha(new String[][] {credito})).andExpect(status().isCreated());
    confirmarTudo(perfil, previa(perfil, timeline(perfil).path("revisao").asText()));
    receber(perfil, planilha(new String[][] {credito,
        {"2024-06-11 10:00:00", "Spot", "Megadrop Rewards", "BNB", "0.2"}}))
        .andExpect(status().isCreated());
    confirmarTudo(perfil, previa(perfil, timeline(perfil).path("revisao").asText()));
    JsonNode posicoes = corpo(mvc.perform(get("/api/v1/posicoes")
        .header("Authorization", bearer(perfil))).andExpect(status().isOk()));
    JsonNode bnb = java.util.stream.StreamSupport.stream(posicoes.path("itens").spliterator(), false)
        .filter(p -> p.path("ativoTicker").asText().equals("BNB")).findFirst().orElseThrow();
    assertThat(new java.math.BigDecimal(bnb.path("quantidade").asText())).isEqualByComparingTo("0.3");
    assertThat(timeline(perfil).path("resumo").path("pendentes").asInt()).isEqualTo(1);
  }

  @Test
  void reconheceAtivosDosExemplosSemClassificarCreditoComSinalInvalido() throws Exception {
    String perfil = "creditos-origem-" + UUID.randomUUID();
    receber(perfil, planilha(new String[][] {
        {"2024-06-20 04:11:00", "Spot", "Megadrop Rewards", "LISTA", "0.23007999"},
        {"2024-06-10 21:36:00", "Spot", "Launchpool Airdrop - System Distribution", "IO", "0.0308032"},
        {"2024-06-10 22:42:00", "Spot", "Simple Earn Locked Rewards", "BNB", "-0.00000085"}
    })).andExpect(status().isCreated());
    assertThat(registro(perfil, "0.23007999").path("estado").asText()).isEqualTo("COERENTE");
    assertThat(registro(perfil, "0.0308032").path("estado").asText()).isEqualTo("COERENTE");
    assertThat(registro(perfil, "-0.00000085").path("natureza").asText()).isEqualTo("OBSERVADA");
    assertThat(registro(perfil, "-0.00000085").path("estado").asText()).isNotEqualTo("COERENTE");
    confirmarTudo(perfil, previa(perfil, timeline(perfil).path("revisao").asText()));
    JsonNode posicoes = corpo(mvc.perform(get("/api/v1/posicoes")
        .header("Authorization", bearer(perfil))).andExpect(status().isOk()));
    assertThat(posicoes.path("itens").toString()).doesNotContain("BNB");
  }

  @Test
  void saidaCriptoIdentificadaPermiteFinalidadeSemPendenciaFiscal() throws Exception {
    String perfil = "saida-cripto-" + UUID.randomUUID();
    receber(perfil, planilha(new String[][] {
        {"2024-05-01 10:00:00", "Spot", "Deposit", "USDT", "20"},
        {"2024-05-28 22:53:41", "Spot", "Withdraw", "USDT", "-10", "Withdraw fee is included"}
    })).andExpect(status().isCreated());
    JsonNode linha = registro(perfil, "-10");
    assertThat(linha.path("natureza").asText()).isEqualTo("SAIDA_CRIPTO");
    assertThat(linha.path("estado").asText()).isEqualTo("COERENTE");
    assertThat(linha.path("finalidadeCripto").asText()).isEqualTo("DESCONHECIDA");
    assertThat(linha.has("estadoFiscal")).isFalse();
    assertThat(linha.path("taxaDetalhamentoPendente").asBoolean()).isTrue();
    assertThat(linha.path("camposOriginais").toString()).contains("Withdraw fee is included");
    confirmarTudo(perfil, previa(perfil, timeline(perfil).path("revisao").asText()));
    for (String finalidade : List.of("CARTEIRA_PROPRIA", "TERCEIROS", "OUTRA")) {
      linha = registro(perfil, "-10");
      Map<String, Object> edicao = new LinkedHashMap<>();
      edicao.put("revisao", timeline(perfil).path("revisao").asText());
      edicao.put("versao", linha.path("versao").asText());
      edicao.put("data", linha.path("data").asText());
      edicao.put("contaId", linha.path("contaId").asText());
      edicao.put("ativoId", linha.path("ativoId").asText());
      edicao.put("quantidade", "-10");
      edicao.put("natureza", "SAIDA_CRIPTO");
      edicao.put("finalidadeCripto", finalidade);
      edicao.put("motivo", "Finalidade informada");
      mvc.perform(put("/api/v1/conciliacao/registros/" + linha.path("id").asText())
              .header("Authorization", bearer(perfil))
              .header("Idempotency-Key", UUID.randomUUID().toString())
              .contentType("application/json").content(json.writeValueAsString(edicao)))
          .andExpect(status().isOk());
      assertThat(registro(perfil, "-10").path("finalidadeCripto").asText()).isEqualTo(finalidade);
      confirmarTudo(perfil, previa(perfil, timeline(perfil).path("revisao").asText()));
      assertThat(registro(perfil, "-10").has("estadoFiscal")).isFalse();
    }
    linha = registro(perfil, "-10");
    Map<String, Object> taxa = new LinkedHashMap<>();
    taxa.put("revisao", timeline(perfil).path("revisao").asText());
    taxa.put("versao", linha.path("versao").asText());
    taxa.put("data", linha.path("data").asText());
    taxa.put("contaId", linha.path("contaId").asText());
    taxa.put("ativoId", linha.path("ativoId").asText());
    taxa.put("quantidade", "-10");
    taxa.put("natureza", "SAIDA_CRIPTO");
    taxa.put("finalidadeCripto", "OUTRA");
    taxa.put("taxaInformada", true);
    taxa.put("taxaAtivoId", linha.path("ativoId").asText());
    taxa.put("taxaQuantidade", "0.1");
    taxa.put("motivo", "Taxa conferida");
    mvc.perform(put("/api/v1/conciliacao/registros/" + linha.path("id").asText())
            .header("Authorization", bearer(perfil))
            .header("Idempotency-Key", UUID.randomUUID().toString())
            .contentType("application/json").content(json.writeValueAsString(taxa)))
        .andExpect(status().isOk());
    confirmarTudo(perfil, previa(perfil, timeline(perfil).path("revisao").asText()));
    assertThat(registro(perfil, "-10").path("taxaDetalhamentoPendente").asBoolean()).isFalse();
    JsonNode posicoes = corpo(mvc.perform(get("/api/v1/posicoes")
        .header("Authorization", bearer(perfil))).andExpect(status().isOk()));
    assertThat(posicoes.toString()).contains("10");
    JsonNode painel = corpo(mvc.perform(get("/api/v1/visao-geral")
        .header("Authorization", bearer(perfil))).andExpect(status().isOk()));
    assertThat(painel.path("resumo").path("resultadoRealizadoBrl").isNull()).isTrue();
  }

  @Test
  void saidaCriptoSemSaldoAnteriorEIdentificadaComAvisoSeparado() throws Exception {
    String perfil = "saida-sem-cobertura-" + UUID.randomUUID();
    receber(perfil, planilha(new String[][] {
        {"2024-05-28 22:53:41", "Spot", "Withdraw", "USDT", "-10", "Withdraw fee is included"}
    })).andExpect(status().isCreated());
    JsonNode linha = registro(perfil, "-10");
    assertThat(linha.path("estado").asText()).isEqualTo("COERENTE");
    assertThat(linha.path("saldoAnteriorPendente").asBoolean()).isTrue();
    assertThat(linha.has("estadoFiscal")).isFalse();
  }

  @Test
  void depositoAnteriorAplicadoEhReclassificadoSomenteNaConfirmacao() throws Exception {
    String perfil = "legado-deposito-" + UUID.randomUUID();
    receber(perfil, planilha(new String[][] {
        {"2024-05-01 10:00:00", "Spot", "Deposit", "BRL", "1000"}
    })).andExpect(status().isCreated());
    jdbc.update(
        "UPDATE registros_conferencia SET aplicada_json = proposta_json WHERE perfil_ref = ?",
        perfil);
    JsonNode linha = registro(perfil, "1000");
    assertThat(linha.path("natureza").asText()).isEqualTo("APORTE");
    assertThat(linha.path("aplicado").asBoolean()).isTrue();
    assertThat(linha.path("aguardaConfirmacao").asBoolean()).isTrue();
    JsonNode posicoesAntes = corpo(mvc.perform(get("/api/v1/posicoes")
        .header("Authorization", bearer(perfil))).andExpect(status().isOk()));
    assertThat(posicoesAntes.toString()).contains("1000");
    JsonNode previa = previa(perfil, timeline(perfil).path("revisao").asText());
    assertThat(previa.path("registrosParaAplicar").asInt()).isEqualTo(1);
    confirmarTudo(perfil, previa);
    assertThat(registro(perfil, "1000").path("aguardaConfirmacao").asBoolean()).isFalse();
    JsonNode painel = corpo(mvc.perform(get("/api/v1/visao-geral")
        .header("Authorization", bearer(perfil))).andExpect(status().isOk()));
    assertThat(new java.math.BigDecimal(painel.path("resumo").path("aportesBrl").asText()))
        .isEqualByComparingTo("1000");
    JsonNode posicoesDepois = corpo(mvc.perform(get("/api/v1/posicoes")
        .header("Authorization", bearer(perfil))).andExpect(status().isOk()));
    assertThat(posicoesDepois.toString()).contains("1000");
    assertThat(previa(perfil, timeline(perfil).path("revisao").asText())
        .path("registrosParaAplicar").asInt()).isZero();
  }

  @Test
  void classificacaoManualPrevaleceSobreOReconhecimentoAutomatico() throws Exception {
    String perfil = "manual-deposito-" + UUID.randomUUID();
    receber(perfil, planilha(new String[][] {
        {"2024-05-01 10:00:00", "Spot", "Deposit", "BRL", "1000"}
    })).andExpect(status().isCreated());
    JsonNode linha = registro(perfil, "1000");
    JsonNode pagina = timeline(perfil);
    Map<String, Object> edicao = new LinkedHashMap<>();
    edicao.put("revisao", pagina.path("revisao").asText());
    edicao.put("versao", linha.path("versao").asText());
    edicao.put("data", linha.path("data").asText());
    edicao.put("contaId", linha.path("contaId").asText());
    edicao.put("ativoId", linha.path("ativoId").asText());
    edicao.put("quantidade", "1000");
    edicao.put("natureza", "OBSERVADA");
    edicao.put("motivo", "Origem em conferência");
    mvc.perform(put("/api/v1/conciliacao/registros/" + linha.path("id").asText())
            .header("Authorization", bearer(perfil))
            .header("Idempotency-Key", UUID.randomUUID().toString())
            .contentType("application/json").content(json.writeValueAsString(edicao)))
        .andExpect(status().isOk());
    assertThat(registro(perfil, "1000").path("natureza").asText()).isEqualTo("OBSERVADA");
  }

  @Test
  void retiradaNaoGeraResultadoNemAfetaOValorDosAportes() throws Exception {
    String perfil = "retirada-resultado-" + UUID.randomUUID();
    receber(perfil, planilha(new String[][] {
        {"2024-05-01 10:00:00", "Spot", "Deposit", "BRL", "1000"},
        {"2024-05-02 10:00:00", "Spot", "Withdraw", "BRL", "-250"}
    })).andExpect(status().isCreated());
    JsonNode antes = timeline(perfil);
    assertThat(jdbc.queryForObject(
        "SELECT COUNT(*) FROM registros_conferencia WHERE perfil_ref=? AND aplicada_json IS NOT NULL",
        Long.class, perfil)).isZero();
    confirmarTudo(perfil, previa(perfil, antes.path("revisao").asText()));
    JsonNode painel = corpo(mvc.perform(get("/api/v1/visao-geral").header("Authorization", bearer(perfil)))
        .andExpect(status().isOk()));
    assertThat(new java.math.BigDecimal(painel.path("resumo").path("aportesBrl").asText()))
        .isEqualByComparingTo("1000");
    assertThat(new java.math.BigDecimal(painel.path("resumo").path("resultadoRealizadoBrl").asText()))
        .isEqualByComparingTo("0");
    assertThat(new java.math.BigDecimal(painel.path("resumo").path("custoSaldosBrl").asText()))
        .isEqualByComparingTo("750");
  }

  @Test
  void classificaAporteERetiradaEmBrlSemAnteciparEfeitos() throws Exception {
    String perfil = "deposito-retirada-" + UUID.randomUUID();
    receber(perfil, planilha(new String[][] {
        {"2024-05-01 10:00:00", "Spot", "Deposit", "BRL", "1000"},
        {"2024-05-02 10:00:00", "Spot", "Withdraw", "BRL", "-250"},
        {"2024-05-03 10:00:00", "Spot", "Deposit", "BTC", "1"}
    })).andExpect(status().isCreated());
    JsonNode antes = timeline(perfil);
    JsonNode deposito = registro(perfil, "1000");
    JsonNode retirada = registro(perfil, "-250");
    JsonNode cripto = registro(perfil, "1");
    assertThat(deposito.path("natureza").asText()).isEqualTo("APORTE");
    assertThat(deposito.path("estado").asText()).isEqualTo("COERENTE");
    assertThat(deposito.path("aplicado").asBoolean()).isFalse();
    assertThat(retirada.path("natureza").asText()).isEqualTo("RETIRADA");
    assertThat(retirada.path("estado").asText()).isEqualTo("COERENTE");
    assertThat(cripto.path("natureza").asText()).isEqualTo("OBSERVADA");
    JsonNode previa = previa(perfil, antes.path("revisao").asText());
    confirmarTudo(perfil, previa);
    JsonNode posicoes = corpo(mvc.perform(get("/api/v1/posicoes").header("Authorization", bearer(perfil)))
        .andExpect(status().isOk()));
    assertThat(posicoes.toString()).contains("750");
  }

  @Test
  void entradasESaidasCorrespondentesEntreContasPermanecemParaConferencia() throws Exception {
    String perfil = "transferencia-brl-" + UUID.randomUUID();
    receber(perfil, planilha(new String[][] {
        {"2024-05-01 10:00:00", "Spot", "Withdraw", "BRL", "-100"},
        {"2024-05-01 10:00:01", "Funding", "Deposit", "BRL", "100"}
    })).andExpect(status().isCreated());
    assertThat(registro(perfil, "-100").path("natureza").asText()).isEqualTo("OBSERVADA");
    assertThat(registro(perfil, "100").path("natureza").asText()).isEqualTo("OBSERVADA");
  }

  @Test
  void alteracaoEconomicaMantemPropostaVisivelParaRevisaoSemAplicaLa() throws Exception {
    String perfil = "grupo-invalido-" + UUID.randomUUID();
    receber(perfil, planilha(new String[][] {
        {"2024-05-26 19:05:32", "Spot", "Buy Crypto With Fiat", "SOL", "1"},
        {"2024-05-26 19:05:32", "Spot", "Buy Crypto With Fiat", "BRL", "-100"}
    })).andExpect(status().isCreated());
    salvarGrupo(perfil, pedido(perfil, List.of(registro(perfil, "1"), registro(perfil, "-100")),
        null, null, true, false));
    JsonNode linha = registro(perfil, "1");
    Map<String, Object> edicao = new LinkedHashMap<>();
    edicao.put("revisao", timeline(perfil).path("revisao").asText());
    for (String campo : List.of("versao", "data", "contaId", "ativoId", "natureza"))
      edicao.put(campo, linha.path(campo).asText());
    edicao.put("quantidade", "2");
    edicao.put("motivo", "Quantidade conferida");
    mvc.perform(put("/api/v1/conciliacao/registros/" + linha.path("id").asText())
        .header("Authorization", bearer(perfil))
        .header("Idempotency-Key", UUID.randomUUID().toString())
        .contentType("application/json").content(json.writeValueAsString(edicao)))
        .andExpect(status().isOk());
    JsonNode pagina = timeline(perfil);
    assertThat(pagina.path("linhasPorGrupo").iterator().next()).hasSize(2);
    assertThat(registro(perfil, "2").path("pendencias").toString()).contains("grupo_desatualizado");
    assertThat(previa(perfil, pagina.path("revisao").asText()).path("grupos")).isEmpty();
  }

  @Test
  void saldoAnteriorPreservaCompraAutomaticamenteAssociada() throws Exception {
    String perfil = "saldo-grupo-" + UUID.randomUUID();
    byte[] arquivo = planilha(new String[][] {
        {"2024-05-26 19:05:32", "Spot", "Buy Crypto With Fiat", "SOL", "0.12345678", "Via CashBalance"},
        {"2024-05-26 19:05:32", "Spot", "Buy Crypto With Fiat", "BRL", "-100", "Via CashBalance"}
    });
    receber(perfil, arquivo).andExpect(status().isCreated());
    JsonNode antes = timeline(perfil);
    assertThat(antes.path("linhasPorGrupo").iterator().next()).hasSize(2);
    salvarGrupo(perfil, pedido(perfil, List.of(registro(perfil, "0.12345678"),
        registro(perfil, "-100")), null, null, true, false));
    for (String quantidade : List.of("0.12345678", "-100")) {
      JsonNode linha = registro(perfil, quantidade);
      Map<String, Object> edicao = new LinkedHashMap<>();
      edicao.put("revisao", timeline(perfil).path("revisao").asText());
      for (String campo : List.of("versao", "data", "contaId", "ativoId", "quantidade", "natureza"))
        edicao.put(campo, linha.path(campo).asText());
      edicao.put("motivo", "Conferência");
      mvc.perform(put("/api/v1/conciliacao/registros/" + linha.path("id").asText())
          .header("Authorization", bearer(perfil))
          .header("Idempotency-Key", UUID.randomUUID().toString())
          .contentType("application/json").content(json.writeValueAsString(edicao)))
          .andExpect(status().isOk());
    }
    antes = timeline(perfil);
    JsonNode brl = registro(perfil, "-100");
    enviar(perfil, "/api/v1/conciliacao/saldos-anteriores", Map.of(
        "revisao", antes.path("revisao").asText(), "ancoraId", brl.path("id").asText(),
        "contaId", brl.path("contaId").asText(), "ativoId", brl.path("ativoId").asText(),
        "data", "2024-05-25T12:00:00Z", "quantidade", "100", "custoTotalBrl", "100",
        "motivo", "Saldo anterior"), UUID.randomUUID().toString()).andExpect(status().isOk());
    JsonNode depois = timeline(perfil);
    assertThat(depois.path("linhasPorGrupo").iterator().next()).hasSize(2);
    JsonNode saldo = registro(perfil, "100");
    Map<String, Object> ajuste = new LinkedHashMap<>();
    ajuste.put("revisao", depois.path("revisao").asText());
    for (String campo : List.of("versao", "data", "contaId", "ativoId", "natureza"))
      ajuste.put(campo, saldo.path(campo).asText());
    ajuste.put("quantidade", "150");
    ajuste.put("custoTotalBrl", "150");
    ajuste.put("motivo", "Saldo atualizado");
    mvc.perform(put("/api/v1/conciliacao/registros/" + saldo.path("id").asText())
        .header("Authorization", bearer(perfil))
        .header("Idempotency-Key", UUID.randomUUID().toString())
        .contentType("application/json").content(json.writeValueAsString(ajuste)))
        .andExpect(status().isOk());
    depois = timeline(perfil);
    JsonNode proposta = previa(perfil, depois.path("revisao").asText());
    assertThat(proposta.path("grupos")).hasSize(1);
    assertThat(new java.math.BigDecimal(proposta.path("grupos").get(0).path("custoFinalBrl").asText()))
        .isEqualByComparingTo("100");
    confirmarTudo(perfil, proposta);
    JsonNode posicoes = corpo(mvc.perform(get("/api/v1/posicoes")
        .header("Authorization", bearer(perfil))).andExpect(status().isOk()));
    JsonNode sol = java.util.stream.StreamSupport.stream(posicoes.path("itens").spliterator(), false)
        .filter(item -> item.path("ativoTicker").asText().equals("SOL"))
        .findFirst().orElseThrow();
    assertThat(new java.math.BigDecimal(sol.path("quantidade").asText())).isEqualByComparingTo("0.12345678");
    assertThat(new java.math.BigDecimal(sol.path("custoTotalBrl").asText())).isEqualByComparingTo("100");
    receber(perfil, arquivo).andExpect(status().isCreated());
    assertThat(timeline(perfil).path("linhasPorGrupo").iterator().next()).hasSize(2);
  }

  @Test
  void agrupaCompraSemTaxaInformadaEAplicaUmaVez() throws Exception {
    String perfil = "grupo-" + UUID.randomUUID();
    receber(
            perfil,
            planilha(
                new String[][] {
                  {"2024-05-01 10:00:00", "Spot", "Deposit", "BRL", "200"},
                  {"2024-05-26 19:05:32", "Spot", "Buy Crypto With Fiat", "SOL", "0.12345678"},
                  {"2024-05-26 19:05:32", "Spot", "Buy Crypto With Fiat", "BRL", "-100"}
                }))
        .andExpect(status().isCreated());
    JsonNode pagina = timeline(perfil);
    assertThat(pagina.path("itens")).hasSize(2);
    JsonNode operacao = pagina.path("itens").get(1);
    assertThat(operacao.path("estado").asText()).isEqualTo("COERENTE");
    assertThat(operacao.path("pendencias")).isEmpty();
    assertThat(operacao.path("aplicado").asBoolean()).isFalse();
    assertThat(operacao.path("grupoNumero").isNull()).isTrue();
    assertThat(pagina.path("linhasPorGrupo").path(operacao.path("grupoAutomatico").asText()))
        .hasSize(2);
    JsonNode previa = previa(perfil, pagina.path("revisao").asText());
    assertThat(previa.path("grupos").size()).isEqualTo(1);
    assertThat(previa.path("grupos").get(0).path("principalBrl").asText()).isEqualTo("100");
    assertThat(new java.math.BigDecimal(previa.path("grupos").get(0).path("custoFinalBrl").asText()))
        .isEqualByComparingTo("100");
    assertThat(previa.path("grupos").get(0).path("completo").asBoolean()).isTrue();
    confirmarTudo(perfil, previa);
    assertThat(
            jdbc.queryForObject(
                "SELECT COUNT(*) FROM grupos_associacao WHERE perfil_ref=?", Long.class, perfil))
        .isEqualTo(1);
    JsonNode segunda = previa(perfil, timeline(perfil).path("revisao").asText());
    assertThat(segunda.path("grupos").size()).isZero();
    JsonNode candidatos =
        corpo(
            mvc.perform(
                    get("/api/v1/conciliacao/associacoes/candidatos")
                        .param("registroId", registro(perfil, "0.12345678").path("id").asText())
                        .header("Authorization", bearer(perfil)))
                .andExpect(status().isOk()));
    assertThat(candidatos.path("grupo").path("numero").asText()).startsWith("OP-");
    assertThat(candidatos.path("grupo").path("linhas").size()).isEqualTo(2);
    JsonNode posicoes =
        corpo(
            mvc.perform(get("/api/v1/posicoes").header("Authorization", bearer(perfil)))
                .andExpect(status().isOk()));
    assertThat(posicoes.toString()).contains("0.12345678");
  }

  @Test
  void importaCsvComIdRecorrenteEExecucoesParciaisEmUmaUnicaOperacao() throws Exception {
    String perfil = "csv-parcial-" + UUID.randomUUID();
    byte[] arquivo =
        csvRelatorio(
            new String[][] {
              {
                "123456789", "2024-05-26-19:05:32", "Buy", "", "OCBS", "", "100.12", "BRL", "",
                "", "0.1", "SOL", "", "", "0.12", "BRL", ""
              },
              {
                "123456789", "2024-05-26-19:05:33", "Buy", "", "OCBS", "", "0", "BRL", "", "",
                "0.02345678", "SOL", "", "", "0", "BRL", ""
              }
            });

    receber(perfil, arquivo).andExpect(status().isCreated());
    JsonNode timeline = timeline(perfil);
    assertThat(timeline.path("total").asInt()).isEqualTo(1);
    assertThat(
            jdbc.queryForObject(
                "SELECT COUNT(DISTINCT origem_ref) FROM registros_conferencia WHERE perfil_ref=?",
                Long.class,
                perfil))
        .isEqualTo(4);
    JsonNode previa = previa(perfil, timeline.path("revisao").asText());
    assertThat(previa.path("grupos")).hasSize(1);
    JsonNode grupo = previa.path("grupos").get(0);
    assertThat(grupo.path("linhas")).hasSize(4);
    assertThat(grupo.path("recebimentos").get(0).path("quantidade").asText())
        .isEqualTo("0.12345678");
    assertThat(new java.math.BigDecimal(grupo.path("principalBrl").asText()))
        .isEqualByComparingTo("100");
    assertThat(new java.math.BigDecimal(grupo.path("custoFinalBrl").asText()))
        .isEqualByComparingTo("100.12");
    confirmarTudo(perfil, previa);

    JsonNode posicoes =
        corpo(
            mvc.perform(get("/api/v1/posicoes").header("Authorization", bearer(perfil)))
                .andExpect(status().isOk()));
    assertThat(posicoes.toString()).contains("0.12345678", "100.12");
    receber(perfil, arquivo).andExpect(status().isCreated());
    JsonNode depois = timeline(perfil);
    assertThat(depois.path("linhasPorGrupo").size()).isEqualTo(1);
    assertThat(depois.path("linhasPorGrupo").iterator().next()).hasSize(4);
    assertThat(
        java.util.stream.StreamSupport.stream(depois.path("itens").spliterator(), false)
            .filter(item -> item.path("grupoNumero").isTextual())
            .count()).isEqualTo(1);
  }

  JsonNode registro(String perfil, String quantidade) throws Exception {
    JsonNode pagina = timeline(perfil);
    for (var linhas : pagina.path("linhasPorGrupo"))
      for (var r : linhas)
        if (r.path("quantidade").asText().equals(quantidade)) return r;
    for (var r : pagina.path("itens"))
      if (r.path("id").asText().startsWith("registro:")
          && r.path("quantidade").asText().equals(quantidade)) return r;
    throw new AssertionError("Registro não encontrado: " + quantidade);
  }

  Map<String, Object> pedido(
      String perfil,
      List<JsonNode> linhas,
      String numero,
      String versao,
      boolean zero,
      boolean desfazer)
      throws Exception {
    Map<String, Object> r = new LinkedHashMap<>();
    r.put("revisao", timeline(perfil).path("revisao").asText());
    r.put("numero", numero);
    r.put("versao", versao);
    r.put("taxaZeroConfirmada", zero);
    r.put("desfazer", desfazer);
    r.put(
        "linhas",
        linhas.stream()
            .map(l ->
                Map.of(
                    "id",
                    l.path("id").asText(),
                    "versao",
                    l.path("versao").asText(),
                    "papel",
                    l.path("papel").isMissingNode()
                        ? (l.path("quantidade").asText().startsWith("-")
                            ? "PAGAMENTO"
                            : "RECEBIMENTO")
                        : l.path("papel").asText()))
            .toList());
    return r;
  }

  JsonNode salvarGrupo(String perfil, Map<String, Object> pedido) throws Exception {
    JsonNode preview =
        corpo(
            enviar(perfil, "/api/v1/conciliacao/associacoes/previa", pedido, null)
                .andExpect(status().isOk()));
    pedido.put("previaId", preview.path("previaId").asText());
    String chave = UUID.randomUUID().toString();
    JsonNode salvo =
        corpo(
            enviar(perfil, "/api/v1/conciliacao/associacoes/propostas", pedido, chave)
                .andExpect(status().isOk()));
    enviar(perfil, "/api/v1/conciliacao/associacoes/propostas", pedido, chave)
        .andExpect(status().isOk());
    return salvo;
  }

  @Test
  void manualAteDoisSegundosConfirmaTaxaZeroSemDuplicarEDesfaz() throws Exception {
    String perfil = "manual-" + UUID.randomUUID();
    receber(
            perfil,
            planilha(
                new String[][] {
                  {"2024-05-01 10:00:00", "Spot", "Deposit", "BRL", "200"},
                  {"2024-05-26 19:05:32", "Spot", "Buy Crypto With Fiat", "SOL", "1"},
                  {"2024-05-26 19:05:34", "Spot", "Buy Crypto With Fiat", "BRL", "-100"}
                }))
        .andExpect(status().isCreated());
    JsonNode p = previa(perfil, timeline(perfil).path("revisao").asText());
    assertThat(p.path("grupos").size()).isZero();
    confirmarTudo(perfil, p);
    var pedido =
        pedido(
            perfil,
            List.of(registro(perfil, "1"), registro(perfil, "-100")),
            null,
            null,
            true,
            false);
    var salvo = salvarGrupo(perfil, pedido);
    String numero = salvo.path("numero").asText();
    assertThat(
            jdbc.queryForObject(
                "SELECT aplicada_json FROM grupos_associacao WHERE id=?",
                String.class,
                Long.valueOf(numero.substring(3))))
        .isNull();
    confirmarTudo(perfil, previa(perfil, timeline(perfil).path("revisao").asText()));
    JsonNode painel =
        corpo(
            mvc.perform(get("/api/v1/visao-geral").header("Authorization", bearer(perfil)))
                .andExpect(status().isOk()));
    assertThat(painel.toString()).contains("100");
    JsonNode grupo =
        corpo(
            mvc.perform(
                    get("/api/v1/conciliacao/associacoes/" + numero)
                        .header("Authorization", bearer(perfil)))
                .andExpect(status().isOk()));
    assertThat(grupo.path("completo").asBoolean()).isTrue();
    var desfazer = pedido(perfil, List.of(), numero, grupo.path("versao").asText(), false, true);
    salvarGrupo(perfil, desfazer);
    confirmarTudo(perfil, previa(perfil, timeline(perfil).path("revisao").asText()));
    assertThat(
            jdbc.queryForObject(
                "SELECT COUNT(*) FROM linhas_grupos_associacao WHERE grupo_id=?",
                Long.class,
                Long.valueOf(numero.substring(3))))
        .isZero();
    assertThat(
            jdbc.queryForObject(
                "SELECT COUNT(*) FROM historico_grupos_associacao WHERE grupo_id=?",
                Long.class,
                Long.valueOf(numero.substring(3))))
        .isEqualTo(1);
  }

  @Test
  void rejeitaOutroUsuarioEJanelaAcimaDeDoisSegundos() throws Exception {
    String perfil = "limite-" + UUID.randomUUID();
    receber(
            perfil,
            planilha(
                new String[][] {
                  {"2024-05-26 19:05:32", "Spot", "Buy Crypto With Fiat", "SOL", "1"},
                  {"2024-05-26 19:05:35", "Spot", "Buy Crypto With Fiat", "BRL", "-100"}
                }))
        .andExpect(status().isCreated());
    var pedido =
        pedido(
            perfil,
            List.of(registro(perfil, "1"), registro(perfil, "-100")),
            null,
            null,
            false,
            false);
    enviar(perfil, "/api/v1/conciliacao/associacoes/previa", pedido, null)
        .andExpect(status().isBadRequest());
    mvc.perform(
            get("/api/v1/conciliacao/associacoes/candidatos")
                .param("registroId", registro(perfil, "1").path("id").asText())
                .header("Authorization", bearer("outro-" + perfil)))
        .andExpect(status().isNotFound());
  }

  @Test
  void duasComprasNoMesmoSegundoNaoSaoUnidas() throws Exception {
    String perfil = "ambiguo-" + UUID.randomUUID();
    receber(
            perfil,
            planilha(
                new String[][] {
                  {
                    "2024-05-26 19:05:32",
                    "Spot",
                    "Buy Crypto With Fiat",
                    "SOL",
                    "1",
                    "Compra por saldo"
                  },
                  {
                    "2024-05-26 19:05:32",
                    "Spot",
                    "Buy Crypto With Fiat",
                    "BRL",
                    "-100",
                    "Compra por saldo"
                  },
                  {
                    "2024-05-26 19:05:32",
                    "Spot",
                    "Buy Crypto With Fiat",
                    "SOL",
                    "2",
                    "Compra por saldo"
                  },
                  {
                    "2024-05-26 19:05:32",
                    "Spot",
                    "Buy Crypto With Fiat",
                    "BRL",
                    "-200",
                    "Compra por saldo"
                  }
                }))
        .andExpect(status().isCreated());
    assertThat(previa(perfil, timeline(perfil).path("revisao").asText()).path("grupos").size())
        .isZero();
  }

  @Test
  void adicionaTaxaAoGrupoPeloNumeroPreservandoDebitoUnico() throws Exception {
    String perfil = "taxa-" + UUID.randomUUID();
    receber(
            perfil,
            planilha(
                new String[][] {
                  {"2024-05-26 19:05:32", "Spot", "Buy Crypto With Fiat", "SOL", "1", "Wallet/abc"},
                  {
                    "2024-05-26 19:05:32",
                    "Spot",
                    "Buy Crypto With Fiat",
                    "BRL",
                    "-100",
                    "Wallet/abc"
                  }
                }))
        .andExpect(status().isCreated());
    confirmarTudo(perfil, previa(perfil, timeline(perfil).path("revisao").asText()));
    String numero =
        "OP-"
            + jdbc.queryForObject(
                "SELECT id FROM grupos_associacao WHERE perfil_ref=?", Long.class, perfil);
    receber(
            perfil,
            planilha(
                new String[][] {
                  {"2024-05-26 19:05:33", "Spot", "Transaction Fee", "ETH", "-0.001", "Wallet/abc"}
                }))
        .andExpect(status().isCreated());
    JsonNode grupo =
        corpo(
            mvc.perform(
                    get("/api/v1/conciliacao/associacoes/" + numero)
                        .header("Authorization", bearer(perfil)))
                .andExpect(status().isOk()));
    List<JsonNode> linhas = new ArrayList<>();
    for (var l : grupo.path("linhas")) linhas.add(l);
    JsonNode taxa = registro(perfil, "-0.001");
    var taxaMap = new LinkedHashMap<String, Object>();
    taxaMap.put("id", taxa.path("id").asText());
    taxaMap.put("versao", taxa.path("versao").asText());
    taxaMap.put("papel", "TAXA");
    linhas.add(json.readTree(json.writeValueAsString(taxaMap)));
    var pedido = pedido(perfil, linhas, numero, grupo.path("versao").asText(), false, false);
    var previa =
        corpo(
            enviar(perfil, "/api/v1/conciliacao/associacoes/previa", pedido, null)
                .andExpect(status().isOk()));
    assertThat(previa.path("taxas").get(0).path("ativoTicker").asText()).isEqualTo("ETH");
    assertThat(previa.path("completo").asBoolean()).isFalse();
    salvarGrupo(perfil, pedido);
    confirmarTudo(perfil, previa(perfil, timeline(perfil).path("revisao").asText()));
    assertThat(
            jdbc.queryForObject(
                "SELECT COUNT(*) FROM linhas_grupos_associacao WHERE grupo_id=?",
                Long.class,
                Long.valueOf(numero.substring(3))))
        .isEqualTo(3);
    assertThat(
            jdbc.queryForObject(
                "SELECT COUNT(*) FROM grupos_associacao WHERE perfil_ref=?", Long.class, perfil))
        .isEqualTo(1);
  }

  @Test
  void falhaNaAplicacaoReverteGruposAjustesEComando() throws Exception {
    String perfil = "rollback-" + UUID.randomUUID();
    receber(
            perfil,
            planilha(
                new String[][] {
                  {"2024-05-26 19:05:32", "Spot", "Buy Crypto With Fiat", "SOL", "1"},
                  {"2024-05-26 19:05:32", "Spot", "Buy Crypto With Fiat", "BRL", "-100"}
                }))
        .andExpect(status().isCreated());
    JsonNode anterior = timeline(perfil);
    JsonNode previa = previa(perfil, anterior.path("revisao").asText());
    jdbc.execute(
        "ALTER TABLE grupos_associacao ADD CONSTRAINT ck_falha_aplicacao CHECK(perfil_ref <> '"
            + perfil
            + "' OR aplicada_json IS NULL)");
    try {
      assertThatThrownBy(() -> confirmarTudo(perfil, previa)).isInstanceOf(Exception.class);
      assertThat(
              jdbc.queryForObject(
                  "SELECT COUNT(*) FROM grupos_associacao WHERE perfil_ref=?", Long.class, perfil))
          .isZero();
      assertThat(
              jdbc.queryForObject(
                  "SELECT COUNT(*) FROM ajustes_conciliatorios WHERE perfil_ref=?",
                  Long.class,
                  perfil))
          .isZero();
      assertThat(timeline(perfil).path("revisao").asText())
          .isEqualTo(anterior.path("revisao").asText());
    } finally {
      jdbc.execute("ALTER TABLE grupos_associacao DROP CHECK ck_falha_aplicacao");
    }
  }

  @Test
  void confirmacoesConcorrentesComMesmaChaveCriamUmGrupo() throws Exception {
    String perfil = "concorrencia-" + UUID.randomUUID();
    receber(
            perfil,
            planilha(
                new String[][] {
                  {"2024-05-26 19:05:32", "Spot", "Buy Crypto With Fiat", "SOL", "1"},
                  {"2024-05-26 19:05:32", "Spot", "Buy Crypto With Fiat", "BRL", "-100"}
                }))
        .andExpect(status().isCreated());
    JsonNode previa = previa(perfil, timeline(perfil).path("revisao").asText());
    List<String> lacunas = new ArrayList<>();
    for (var a : previa.path("ajustes")) lacunas.add(a.path("lacunaId").asText());
    var corpo =
        Map.of(
            "revisao",
            previa.path("revisao").asText(),
            "previaId",
            previa.path("previaId").asText(),
            "aceitarLacunaIds",
            lacunas,
            "aceitarCorrespondenciaIds",
            List.of());
    String chave = UUID.randomUUID().toString();
    try (var executor = java.util.concurrent.Executors.newFixedThreadPool(2)) {
      var a =
          executor.submit(() ->
              confirmar(perfil, corpo, chave)
                  .andExpect(status().isOk())
                  .andReturn()
                  .getResponse()
                  .getContentAsString());
      var b =
          executor.submit(() ->
              confirmar(perfil, corpo, chave)
                  .andExpect(status().isOk())
                  .andReturn()
                  .getResponse()
                  .getContentAsString());
      assertThat(a.get()).isEqualTo(b.get());
    }
    assertThat(
            jdbc.queryForObject(
                "SELECT COUNT(*) FROM grupos_associacao WHERE perfil_ref=?", Long.class, perfil))
        .isEqualTo(1);
  }

  @Test
  void associacaoDeTaxaAnteriorEncerraAjusteSemRepetirQuantidades() throws Exception {
    String perfil = "encerra-" + UUID.randomUUID();
    receber(
            perfil,
            planilha(
                new String[][] {
                  {"2024-05-01 10:00:00", "Spot", "Deposit", "BRL", "200"},
                  {"2024-05-26 19:05:32", "Spot", "Transaction Fee", "SOL", "-0.01", "Wallet/abc"},
                  {"2024-05-26 19:05:33", "Spot", "Buy Crypto With Fiat", "SOL", "1", "Wallet/abc"},
                  {
                    "2024-05-26 19:05:33",
                    "Spot",
                    "Buy Crypto With Fiat",
                    "BRL",
                    "-100",
                    "Wallet/abc"
                  }
                }))
        .andExpect(status().isCreated());
    confirmarTudo(perfil, previa(perfil, timeline(perfil).path("revisao").asText()));
    assertThat(timeline(perfil).path("resumo").path("ajustesAtivos").asInt()).isEqualTo(1);
    String numero =
        "OP-"
            + jdbc.queryForObject(
                "SELECT id FROM grupos_associacao WHERE perfil_ref=?", Long.class, perfil);
    JsonNode grupo =
        corpo(
            mvc.perform(
                    get("/api/v1/conciliacao/associacoes/" + numero)
                        .header("Authorization", bearer(perfil)))
                .andExpect(status().isOk()));
    List<JsonNode> linhas = new ArrayList<>();
    for (var l : grupo.path("linhas")) linhas.add(l);
    var taxa = registro(perfil, "-0.01");
    linhas.add(
        json.readTree(
            json.writeValueAsString(
                Map.of(
                    "id",
                    taxa.path("id").asText(),
                    "versao",
                    taxa.path("versao").asText(),
                    "papel",
                    "TAXA"))));
    salvarGrupo(
        perfil, pedido(perfil, linhas, numero, grupo.path("versao").asText(), false, false));
    assertThat(timeline(perfil).path("resumo").path("ajustesAtivos").asInt()).isEqualTo(1);
    var previa = previa(perfil, timeline(perfil).path("revisao").asText());
    assertThat(previa.path("ajustes").get(0).path("acao").asText()).isEqualTo("ENCERRAR");
    confirmarTudo(perfil, previa);
    assertThat(timeline(perfil).path("resumo").path("ajustesAtivos").asInt()).isZero();
    var posicoes =
        corpo(
            mvc.perform(get("/api/v1/posicoes").header("Authorization", bearer(perfil)))
                .andExpect(status().isOk()));
    var sol =
        java.util.stream.StreamSupport.stream(posicoes.path("itens").spliterator(), false)
            .filter(l -> l.path("ativoTicker").asText().equals("SOL"))
            .findFirst()
            .orElseThrow();
    assertThat(sol.path("quantidade").asText()).isEqualTo("0.99");
    assertThat(new java.math.BigDecimal(sol.path("custoTotalBrl").asText()))
        .isEqualByComparingTo("100");
  }

  @Test
  void editarLinhaNaoMudaGrupoAplicadoAntesDeReavaliar() throws Exception {
    String perfil = "versao-" + UUID.randomUUID();
    receber(
            perfil,
            planilha(
                new String[][] {
                  {"2024-05-01 10:00:00", "Spot", "Deposit", "BRL", "200"},
                  {"2024-05-26 19:05:32", "Spot", "Buy Crypto With Fiat", "SOL", "1"},
                  {"2024-05-26 19:05:33", "Spot", "Buy Crypto With Fiat", "BRL", "-100"}
                }))
        .andExpect(status().isCreated());
    confirmarTudo(perfil, previa(perfil, timeline(perfil).path("revisao").asText()));
    var salvo =
        salvarGrupo(
            perfil,
            pedido(
                perfil,
                List.of(registro(perfil, "1"), registro(perfil, "-100")),
                null,
                null,
                true,
                false));
    confirmarTudo(perfil, previa(perfil, timeline(perfil).path("revisao").asText()));
    var linha = registro(perfil, "1");
    var body = new LinkedHashMap<String, Object>();
    body.put("revisao", timeline(perfil).path("revisao").asText());
    body.put("versao", linha.path("versao").asText());
    body.put("data", linha.path("data").asText());
    body.put("contaId", linha.path("contaId").asText());
    body.put("ativoId", linha.path("ativoId").asText());
    body.put("quantidade", "2");
    body.put("natureza", "OBSERVADA");
    body.put("motivo", "Quantidade conferida");
    mvc.perform(
            put("/api/v1/conciliacao/registros/" + linha.path("id").asText())
                .header("Authorization", bearer(perfil))
                .header("Idempotency-Key", UUID.randomUUID().toString())
                .contentType("application/json")
                .content(json.writeValueAsString(body)))
        .andExpect(status().isOk());
    confirmarTudo(perfil, previa(perfil, timeline(perfil).path("revisao").asText()));
    var posicoes =
        corpo(
            mvc.perform(get("/api/v1/posicoes").header("Authorization", bearer(perfil)))
                .andExpect(status().isOk()));
    assertThat(
            java.util.stream.StreamSupport.stream(posicoes.path("itens").spliterator(), false)
                .filter(l -> l.path("ativoTicker").asText().equals("SOL"))
                .findFirst()
                .orElseThrow()
                .path("quantidade")
                .asText())
        .isEqualTo("1");
    assertThat(registro(perfil, "2").path("pendencias").toString()).contains("grupo_desatualizado");
    String numero = salvo.path("numero").asText();
    var grupo =
        corpo(
            mvc.perform(
                    get("/api/v1/conciliacao/associacoes/" + numero)
                        .header("Authorization", bearer(perfil)))
                .andExpect(status().isOk()));
    salvarGrupo(
        perfil,
        pedido(
            perfil,
            List.of(registro(perfil, "2"), registro(perfil, "-100")),
            numero,
            grupo.path("versao").asText(),
            true,
            false));
    confirmarTudo(perfil, previa(perfil, timeline(perfil).path("revisao").asText()));
    var depois =
        corpo(
            mvc.perform(get("/api/v1/posicoes").header("Authorization", bearer(perfil)))
                .andExpect(status().isOk()));
    assertThat(
            java.util.stream.StreamSupport.stream(depois.path("itens").spliterator(), false)
                .filter(l -> l.path("ativoTicker").asText().equals("SOL"))
                .findFirst()
                .orElseThrow()
                .path("quantidade")
                .asText())
        .isEqualTo("2");
  }

  void confirmarTudo(String perfil, JsonNode previa) throws Exception {
    List<String> ajustes = new ArrayList<>();
    for (var a : previa.path("ajustes")) ajustes.add(a.path("lacunaId").asText());
    confirmar(
            perfil,
            Map.of(
                "revisao",
                previa.path("revisao").asText(),
                "previaId",
                previa.path("previaId").asText(),
                "aceitarLacunaIds",
                ajustes,
                "aceitarCorrespondenciaIds",
                List.of()),
            UUID.randomUUID().toString())
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
      String[] nomes = {"Tempo", "Conta", "Operação", "Moeda", "Alterar", "Observação"};
      for (int i = 0; i < nomes.length; i++) h.createCell(i).setCellValue(nomes[i]);
      for (int i = 0; i < dados.length; i++) {
        var r = sh.createRow(i + 2);
        for (int j = 0; j < dados[i].length; j++) r.createCell(j).setCellValue(dados[i][j]);
      }
      wb.write(out);
      return out.toByteArray();
    }
  }

  byte[] csvRelatorio(String[][] linhas) {
    String cabecalho =
        "id,datetime_tz_BRT,type,label,market_model_type,order_type,sent_amount,sent_currency,sent_value_BRL,sent_address,received_amount,received_currency,received_value_BRL,received_address,fee_amount,fee_currency,fee_value_BRL";
    return (cabecalho
            + "\n"
            + java.util.Arrays.stream(linhas)
                .map(linha -> String.join(",", linha))
                .collect(java.util.stream.Collectors.joining("\n")))
        .getBytes(StandardCharsets.UTF_8);
  }
}
