package io.github.jaimenafer.lastro;

import static org.junit.jupiter.api.Assertions.*;

import io.github.jaimenafer.lastro.ativos.application.dto.AtivoResponse;
import io.github.jaimenafer.lastro.ativos.domain.model.Ativo;
import io.github.jaimenafer.lastro.ativos.domain.model.TipoAtivo;
import io.github.jaimenafer.lastro.operacoes.application.mapper.SaldoInicialMapper;
import io.github.jaimenafer.lastro.operacoes.domain.exception.SaldoInicialInvalidoException;
import io.github.jaimenafer.lastro.operacoes.domain.model.Operacao;
import io.github.jaimenafer.lastro.operacoes.domain.model.SituacaoOperacao;
import io.github.jaimenafer.lastro.operacoes.domain.model.TipoOperacao;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import org.junit.jupiter.api.Test;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.json.JsonMapper;

class EnumsDominioTests {
  @Test
  void classificacoesFechadasSaoEnumsPersistidosPorNome() throws Exception {
    for (var campo :
        new java.lang.reflect.Field[] {
          Ativo.class.getDeclaredField("tipo"),
          Operacao.class.getDeclaredField("tipo"),
          Operacao.class.getDeclaredField("situacao")
        }) {
      assertTrue(campo.getType().isEnum(), campo.toString());
      assertEquals(EnumType.STRING, campo.getAnnotation(Enumerated.class).value());
    }
  }

  @Test
  void filtroPreservaValoresExternosEMensagemDeErro() {
    assertNull(SaldoInicialMapper.situacaoDoFiltro("TODAS"));
    for (var situacao : SituacaoOperacao.values()) {
      assertEquals(situacao, SaldoInicialMapper.situacaoDoFiltro(situacao.name()));
    }
    for (String invalida : new String[] {"confirmada", "ATIVA", "", " CONFIRMADA "}) {
      var erro =
          assertThrows(
              SaldoInicialInvalidoException.class,
              () -> SaldoInicialMapper.situacaoDoFiltro(invalida));
      assertEquals("Verifique os filtros e a paginação.", erro.getMessage());
    }
  }

  @Test
  void jsonPreservaNomesERejeitaClassificacaoDesconhecida() {
    var json = JsonMapper.builder().build();
    for (var tipo : TipoAtivo.values()) {
      assertEquals(tipo, json.readValue(json.writeValueAsString(tipo), TipoAtivo.class));
    }
    for (var situacao : SituacaoOperacao.values()) {
      assertEquals(
          situacao, json.readValue(json.writeValueAsString(situacao), SituacaoOperacao.class));
    }
    assertEquals("\"SALDO_INICIAL\"", json.writeValueAsString(TipoOperacao.SALDO_INICIAL));
    var resposta = new AtivoResponse("1", "fiat:BRL", "BRL", "Real", TipoAtivo.FIAT, 2, true);
    assertEquals(
        "FIAT", json.readTree(json.writeValueAsString(resposta)).get("tipo").stringValue());
    assertThrows(JacksonException.class, () -> json.readValue("\"OUTRO\"", TipoAtivo.class));
    assertThrows(JacksonException.class, () -> json.readValue("\"TODAS\"", SituacaoOperacao.class));
  }
}
