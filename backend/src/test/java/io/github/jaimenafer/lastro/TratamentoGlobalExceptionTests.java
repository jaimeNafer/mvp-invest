package io.github.jaimenafer.lastro;

import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import io.github.jaimenafer.lastro.acesso.domain.exception.AcessoInvalidoException;
import io.github.jaimenafer.lastro.exceptions.TratamentoGlobalException;
import io.github.jaimenafer.lastro.operacoes.domain.exception.*;
import jakarta.servlet.ServletException;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import org.junit.jupiter.api.Test;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.bind.annotation.*;

class TratamentoGlobalExceptionTests {
  private final org.springframework.test.web.servlet.MockMvc mvc =
      MockMvcBuilders.standaloneSetup(new FalhasController())
          .setControllerAdvice(new TratamentoGlobalException())
          .build();

  @Test
  void preservaMensagemDaOrigemComStatusDeNegocioEAcesso() throws Exception {
    for (int status : new int[] {400, 401, 404, 409}) {
      var resposta =
          mvc.perform(get("/falhas/" + status))
              .andExpect(status().is(status))
              .andExpect(content().contentTypeCompatibleWith("application/problem+json"));
      if (status == 401)
        resposta.andExpect(header().doesNotExist("WWW-Authenticate"))
            .andExpect(jsonPath("$.detail").value(new AcessoInvalidoException().getMessage()));
      else
        resposta.andExpect(jsonPath("$.detail").value("Mensagem descritiva da origem " + status));
    }
  }

  @Test
  void validacaoUsaMensagemDefinidaNoParametro() throws Exception {
    mvc.perform(post("/falhas/cadastro").contentType("application/json").content("{\"nome\":\"\"}"))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.detail").value("Informe o nome da carteira."));
    mvc.perform(get("/falhas/invalido")).andExpect(status().isBadRequest());
    mvc.perform(post("/falhas/400"))
        .andExpect(status().isMethodNotAllowed())
        .andExpect(header().exists("Allow"));
    mvc.perform(post("/falhas/cadastro").contentType("text/plain").content("texto"))
        .andExpect(status().isUnsupportedMediaType());
  }

  @Test
  void falhasTecnicasNaoSaoInterceptadasPorCatchGenerico() {
    var erro = assertThrows(ServletException.class, () -> mvc.perform(get("/falhas/500")));
    assertInstanceOf(IllegalStateException.class, erro.getCause());
  }

  @Test
  void registraUmaVezStatusEMensagemDaOrigemEmError() throws Exception {
    var logger =
        (ch.qos.logback.classic.Logger)
            org.slf4j.LoggerFactory.getLogger(TratamentoGlobalException.class);
    var eventos =
        new ch.qos.logback.core.read.ListAppender<ch.qos.logback.classic.spi.ILoggingEvent>();
    eventos.start();
    logger.addAppender(eventos);
    try {
      mvc.perform(get("/falhas/409"))
          .andExpect(status().isConflict())
          .andExpect(jsonPath("$.detail").value("Mensagem descritiva da origem 409"));
      assertEquals(1, eventos.list.size());
      assertEquals(
          "Ocorreu um erro, status: 409 CONFLICT, mensagem: Mensagem descritiva da origem 409",
          eventos.list.getFirst().getFormattedMessage());
      assertEquals(ch.qos.logback.classic.Level.ERROR, eventos.list.getFirst().getLevel());
    } finally {
      logger.detachAppender(eventos);
      eventos.stop();
    }
  }

  @Test
  void tratamentosRetornamProblemDetailTipadoSemCatchGenerico() {
    for (var metodo : TratamentoGlobalException.class.getDeclaredMethods()) {
      var handler = metodo.getAnnotation(ExceptionHandler.class);
      if (handler == null) continue;
      var retorno = (java.lang.reflect.ParameterizedType) metodo.getGenericReturnType();
      assertEquals(org.springframework.http.ResponseEntity.class, retorno.getRawType());
      assertArrayEquals(
          new Class<?>[] {org.springframework.http.ProblemDetail.class},
          retorno.getActualTypeArguments());
      for (var tipo : handler.value()) {
        assertNotEquals(Exception.class, tipo);
        assertNotEquals(RuntimeException.class, tipo);
      }
    }
  }

  @RestController
  static class FalhasController {
    @GetMapping("/falhas/{status}")
    String falhar(@PathVariable int status) {
      switch (status) {
        case 400:
          throw new SaldoInicialInvalidoException("Mensagem descritiva da origem 400");
        case 401:
          throw new AcessoInvalidoException();
        case 404:
          throw new SaldoInicialNaoEncontradoException("Mensagem descritiva da origem 404");
        case 409:
          throw new SaldoInicialConflitanteException("Mensagem descritiva da origem 409");
        default:
          throw new IllegalStateException("Detalhe técnico interno");
      }
    }

    @PostMapping(value = "/falhas/cadastro", consumes = "application/json")
    String cadastro(@Valid @RequestBody Cadastro entrada) {
      return "ok";
    }
  }

  record Cadastro(@NotBlank(message = "Informe o nome da carteira.") String nome) {}
}
