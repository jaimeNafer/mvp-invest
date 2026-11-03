package io.github.jaimenafer.lastro;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.web.servlet.MockMvc;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.mysql.MySQLContainer;
import tools.jackson.databind.json.JsonMapper;

import java.util.Base64;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest(properties = "spring.config.import=")
@AutoConfigureMockMvc
@Testcontainers
class CarteirasIntegrationTests {
    @Container @ServiceConnection
    static final MySQLContainer MYSQL = new MySQLContainer("mysql:8.0.42");

    @Autowired MockMvc mvc;
    @Autowired JdbcTemplate jdbc;
    @Autowired JsonMapper json;
    private long binance;
    private long bitget;

    @BeforeEach void preparar() {
        jdbc.update("DELETE FROM contas");
        jdbc.update("DELETE FROM carteiras");
        jdbc.update("UPDATE instituicoes SET ativo = TRUE");
        binance = jdbc.queryForObject("SELECT id FROM instituicoes WHERE codigo = 'binance'", Long.class);
        bitget = jdbc.queryForObject("SELECT id FROM instituicoes WHERE codigo = 'bitget'", Long.class);
    }

    @Test void limiteDoNomeConsideraSomenteTextoNormalizado() throws Exception {
        String nome = "a".repeat(160);
        mvc.perform(post("/api/v1/carteiras").header("Authorization", bearer("a"))
            .contentType("application/json").content(json.writeValueAsString(Map.of("nome", "  " + nome + "  ", "instituicaoIds", List.of(Long.toString(binance))))))
            .andExpect(status().isCreated()).andExpect(jsonPath("$.nome").value(nome));
    }

    @Test void catalogoMantidoPeloSistemaERotasAntigasRemovidas() throws Exception {
        jdbc.update("UPDATE instituicoes SET ativo = FALSE WHERE id = ?", bitget);
        mvc.perform(get("/api/v1/instituicoes").header("Authorization", bearer("a")))
            .andExpect(status().isOk()).andExpect(jsonPath("$.length()").value(2))
            .andExpect(jsonPath("$[0].nome").value("Binance")).andExpect(jsonPath("$[1].nome").value("BitGet"))
            .andExpect(jsonPath("$[0].id").isString()).andExpect(jsonPath("$[1].ativo").value(false));
        mvc.perform(post("/api/v1/instituicoes").header("Authorization", bearer("a")))
            .andExpect(status().isMethodNotAllowed());
        mvc.perform(get("/api/v1/titulares").header("Authorization", bearer("a"))).andExpect(status().isNotFound());
    }

    @Test void exigeBearerESubDefineProprietario() throws Exception {
        for (String rota : new String[]{"/api/v1/carteiras", "/api/v1/instituicoes"}) {
            mvc.perform(get(rota).header("X-Perfil-Id", "a")).andExpect(status().isUnauthorized());
            mvc.perform(get(rota).header("Authorization", "Bearer abc.def.")).andExpect(status().isUnauthorized());
        }
        mvc.perform(post("/api/v1/carteiras").header("Authorization", bearer("a")).header("X-Perfil-Id", "b")
            .contentType("application/json").content(json.writeValueAsString(Map.of(
                "nome", "  Principal  ", "instituicaoIds", List.of(Long.toString(binance), Long.toString(bitget)), "perfilRef", "b"))))
            .andExpect(status().isCreated()).andExpect(jsonPath("$.nome").value("Principal"))
            .andExpect(jsonPath("$.id").isString()).andExpect(jsonPath("$.ativo").value(true))
            .andExpect(jsonPath("$.contas.length()").value(2)).andExpect(jsonPath("$.contas[0].id").isString());
        assertThat(jdbc.queryForObject("SELECT perfil_ref FROM carteiras", String.class)).isEqualTo("a");
        assertThat(jdbc.queryForList("SELECT DISTINCT ca.perfil_ref FROM contas co JOIN carteiras ca ON ca.id = co.carteira_id", String.class)).containsExactly("a");
        mvc.perform(get("/api/v1/carteiras").header("Authorization", bearer("b"))).andExpect(content().json("[]"));
    }

    @Test void contasPertencemAUmaUnicaCarteiraMesmoComAMesmaInstituicao() throws Exception {
        var principal = cadastrar("a", "Principal", binance, bitget);
        var recorte = cadastrar("a", "Binance", binance);
        var outraPessoa = cadastrar("b", "Principal", binance);
        assertThat(principal.get("contas").get(0).get("id").stringValue())
            .isNotEqualTo(recorte.get("contas").get(0).get("id").stringValue())
            .isNotEqualTo(outraPessoa.get("contas").get(0).get("id").stringValue());
        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM contas", Integer.class)).isEqualTo(4);
        assertThat(jdbc.queryForObject("SELECT COUNT(DISTINCT carteira_id) FROM contas", Integer.class)).isEqualTo(3);
        mvc.perform(get("/api/v1/carteiras").header("Authorization", bearer("a")))
            .andExpect(jsonPath("$.length()").value(2)).andExpect(jsonPath("$[0].nome").value("Binance"));
    }

    @Test void edicaoPreservaContaMantidaERemoveSomenteContaDaCarteiraEditada() throws Exception {
        var primeira = cadastrar("a", "Primeira", binance);
        var outra = cadastrar("a", "Outra", binance);
        var contaOriginal = primeira.get("contas").get(0).get("id").stringValue();
        var contaOutra = outra.get("contas").get(0).get("id").stringValue();
        var id = primeira.get("id").stringValue();
        mvc.perform(put("/api/v1/carteiras/" + id).header("Authorization", bearer("a"))
            .contentType("application/json").content(cadastro("Renomeada", binance, bitget)))
            .andExpect(status().isOk()).andExpect(jsonPath("$.contas[0].id").value(contaOriginal));
        mvc.perform(put("/api/v1/carteiras/" + id).header("Authorization", bearer("a"))
            .contentType("application/json").content(cadastro("Alterada", bitget)))
            .andExpect(status().isOk()).andExpect(jsonPath("$.contas[0].instituicaoNome").value("BitGet"));
        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM contas", Integer.class)).isEqualTo(2);
        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM contas WHERE id = ?", Integer.class, contaOriginal)).isZero();
        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM contas WHERE id = ? AND carteira_id = ?", Integer.class,
            contaOutra, outra.get("id").stringValue())).isEqualTo(1);
        mvc.perform(put("/api/v1/carteiras/" + id).header("Authorization", bearer("a"))
            .contentType("application/json").content(cadastro("Alterada", binance)))
            .andExpect(status().isOk()).andExpect(jsonPath("$.contas[0].instituicaoNome").value("Binance"));
        assertThat(jdbc.queryForObject("SELECT id FROM contas WHERE carteira_id = ?", String.class, id))
            .isNotEqualTo(contaOriginal).isNotEqualTo(contaOutra);
        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM contas", Integer.class)).isEqualTo(2);
    }

    @Test void arquivamentoPreservaContasDaCarteiraEReativacao() throws Exception {
        var id = cadastrar("a", "A", binance).get("id").stringValue();
        cadastrar("a", "B", binance);
        mvc.perform(patch("/api/v1/carteiras/" + id + "/situacao").header("Authorization", bearer("a"))
            .contentType("application/json").content("{\"ativo\":false}"))
            .andExpect(status().isOk()).andExpect(jsonPath("$.ativo").value(false));
        mvc.perform(get("/api/v1/carteiras").header("Authorization", bearer("a")))
            .andExpect(jsonPath("$.length()").value(2)).andExpect(jsonPath("$[0].ativo").value(false))
            .andExpect(jsonPath("$[1].ativo").value(true));
        mvc.perform(get("/api/v1/carteiras").header("Authorization", bearer("a")).param("ativo", "false"))
            .andExpect(jsonPath("$.length()").value(1));
        mvc.perform(put("/api/v1/carteiras/" + id).header("Authorization", bearer("a"))
            .contentType("application/json").content(cadastro("Inválida", bitget))).andExpect(status().isBadRequest());
        mvc.perform(patch("/api/v1/carteiras/" + id + "/situacao").header("Authorization", bearer("a"))
            .contentType("application/json").content("{\"ativo\":true}"))
            .andExpect(status().isOk()).andExpect(jsonPath("$.ativo").value(true));
        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM contas", Integer.class)).isEqualTo(2);
    }

    @Test void recusaCadastrosInvalidosSemGravacaoParcial() throws Exception {
        for (String entrada : new String[]{"{}", cadastro(" ", binance), cadastro("a".repeat(161), binance),
                cadastro("Carteira"), cadastro("Carteira", binance, binance), cadastro("Carteira", 999999L),
                cadastro("Carteira", 0L), "{\"nome\":\"Carteira\",\"instituicaoIds\":[null]}"}) {
            mvc.perform(post("/api/v1/carteiras").header("Authorization", bearer("a"))
                .contentType("application/json").content(entrada)).andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.detail").isString());
        }
        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM carteiras", Integer.class)).isZero();
        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM contas", Integer.class)).isZero();
        cadastrar("a", "a".repeat(160), binance);
    }

    @Test void instituicaoInativaPermiteManterOuRemoverVinculoMasNaoAdicionar() throws Exception {
        var id = cadastrar("a", "Existente", binance).get("id").stringValue();
        jdbc.update("UPDATE instituicoes SET ativo = FALSE WHERE id = ?", binance);
        mvc.perform(post("/api/v1/carteiras").header("Authorization", bearer("a"))
            .contentType("application/json").content(cadastro("Nova", binance))).andExpect(status().isBadRequest());
        mvc.perform(put("/api/v1/carteiras/" + id).header("Authorization", bearer("a"))
            .contentType("application/json").content(cadastro("Existente", binance, bitget))).andExpect(status().isOk());
        mvc.perform(put("/api/v1/carteiras/" + id).header("Authorization", bearer("a"))
            .contentType("application/json").content(cadastro("Existente", bitget))).andExpect(status().isOk());
        mvc.perform(put("/api/v1/carteiras/" + id).header("Authorization", bearer("a"))
            .contentType("application/json").content(cadastro("Existente", binance))).andExpect(status().isBadRequest());
        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM contas", Integer.class)).isEqualTo(1);
    }

    @Test void conflitoDeNomeReverteCarteiraEContas() throws Exception {
        cadastrar("a", "Principal", binance);
        var outra = cadastrar("a", "Outra", binance).get("id").stringValue();
        mvc.perform(post("/api/v1/carteiras").header("Authorization", bearer("a"))
            .contentType("application/json").content(cadastro("principal", bitget))).andExpect(status().isConflict());
        mvc.perform(put("/api/v1/carteiras/" + outra).header("Authorization", bearer("a"))
            .contentType("application/json").content(cadastro("Principal", bitget))).andExpect(status().isConflict());
        assertThat(jdbc.queryForObject("SELECT nome FROM carteiras WHERE id = ?", String.class, outra)).isEqualTo("Outra");
        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM carteiras", Integer.class)).isEqualTo(2);
        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM contas", Integer.class)).isEqualTo(2);
        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM contas WHERE instituicao_id = ?", Integer.class, bitget)).isZero();
    }

    @Test void carteiraDeOutroUsuarioNaoPodeSerAlteradaOuArquivada() throws Exception {
        var id = cadastrar("a", "Privada", binance).get("id").stringValue();
        mvc.perform(put("/api/v1/carteiras/" + id).header("Authorization", bearer("b"))
            .contentType("application/json").content(cadastro("Outro", bitget))).andExpect(status().isNotFound());
        mvc.perform(patch("/api/v1/carteiras/" + id + "/situacao").header("Authorization", bearer("b"))
            .contentType("application/json").content("{\"ativo\":false}")).andExpect(status().isNotFound());
        mvc.perform(put("/api/v1/carteiras/999999").header("Authorization", bearer("a"))
            .contentType("application/json").content(cadastro("Outro", bitget))).andExpect(status().isNotFound());
        mvc.perform(put("/api/v1/carteiras/0").header("Authorization", bearer("a"))
            .contentType("application/json").content(cadastro("Outro", bitget))).andExpect(status().isBadRequest());
    }

    @Test void contextoIsoladoEntreRequisicoesSimultaneas() throws Exception {
        cadastrar("a", "A", binance);
        cadastrar("b", "B", binance);
        var tokenA = bearer("a");
        var tokenB = bearer("b");
        try (var executor = Executors.newVirtualThreadPerTaskExecutor()) {
            var tarefas = java.util.stream.IntStream.range(0, 20).mapToObj(indice -> executor.submit(() -> {
                mvc.perform(get("/api/v1/carteiras").header("Authorization", indice % 2 == 0 ? tokenA : tokenB))
                    .andExpect(status().isOk()).andExpect(jsonPath("$.length()").value(1))
                    .andExpect(jsonPath("$[0].nome").value(indice % 2 == 0 ? "A" : "B"));
                return true;
            })).toList();
            for (var tarefa : tarefas) assertThat(tarefa.get(15, TimeUnit.SECONDS)).isTrue();
        }
        mvc.perform(get("/api/v1/carteiras")).andExpect(status().isUnauthorized());
    }

    @Test void concorrenciaDeNomeNaoGravaCarteiraOuContaParcial() throws Exception {
        var inicio = new CountDownLatch(1);
        var token = bearer("a");
        try (var executor = Executors.newVirtualThreadPerTaskExecutor()) {
            var tarefas = java.util.stream.IntStream.range(0, 2).mapToObj(indice -> executor.submit(() -> {
                inicio.await(5, TimeUnit.SECONDS);
                var resposta = mvc.perform(post("/api/v1/carteiras").header("Authorization", token)
                    .contentType("application/json").content(cadastro("Concorrente", binance))).andReturn();
                return resposta.getResponse().getStatus();
            })).toList();
            inicio.countDown();
            assertThat(List.of(tarefas.get(0).get(15, TimeUnit.SECONDS), tarefas.get(1).get(15, TimeUnit.SECONDS)))
                .containsExactlyInAnyOrder(201, 409);
        }
        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM contas", Integer.class)).isEqualTo(1);
        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM carteiras", Integer.class)).isEqualTo(1);
    }

    private tools.jackson.databind.JsonNode cadastrar(String perfil, String nome, Long... ids) throws Exception {
        var resposta = mvc.perform(post("/api/v1/carteiras").header("Authorization", bearer(perfil))
            .contentType("application/json").content(cadastro(nome, ids))).andExpect(status().isCreated()).andReturn();
        return json.readTree(resposta.getResponse().getContentAsByteArray());
    }

    private String cadastro(String nome, Long... ids) {
        return json.writeValueAsString(Map.of("nome", nome, "instituicaoIds", List.of(ids)));
    }

    private String bearer(String perfil) {
        var encoder = Base64.getUrlEncoder().withoutPadding();
        return "Bearer " + encoder.encodeToString(json.writeValueAsBytes(Map.of("alg", "none"))) + "."
            + encoder.encodeToString(json.writeValueAsBytes(Map.of("sub", perfil, "name", "Nome " + perfil, "role", "acesso_total"))) + ".";
    }
}
