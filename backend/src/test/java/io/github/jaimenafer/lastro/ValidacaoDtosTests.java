package io.github.jaimenafer.lastro;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import io.github.jaimenafer.lastro.carteiras.application.dto.CadastroCarteira;
import io.github.jaimenafer.lastro.exceptions.TratamentoGlobalException;
import io.github.jaimenafer.lastro.operacoes.api.controller.SaldoInicialController;
import io.github.jaimenafer.lastro.operacoes.application.dto.CadastroSaldoInicial;
import io.github.jaimenafer.lastro.operacoes.application.dto.CancelamentoSaldoInicial;
import io.github.jaimenafer.lastro.operacoes.application.usecase.ISaldoInicialUseCase;
import jakarta.validation.Validation;
import java.util.Arrays;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

class ValidacaoDtosTests {
  @Test
  void validaNomeNormalizadoEElementosDasInstituicoes() {
    try (var factory = Validation.buildDefaultValidatorFactory()) {
      var validator = factory.getValidator();
      assertTrue(
          validator
              .validate(new CadastroCarteira("  " + "a".repeat(160) + "  ", List.of(1L)))
              .isEmpty());
      assertFalse(validator.validate(new CadastroCarteira("a".repeat(161), List.of(1L))).isEmpty());
      assertFalse(
          validator.validate(new CadastroCarteira("Nome", Arrays.asList(null, 0L))).isEmpty());
    }
  }

  @Test
  void validaIdsDoSaldoEMotivoDoCancelamentoAntesDoUsecase() {
    try (var factory = Validation.buildDefaultValidatorFactory()) {
      var validator = factory.getValidator();
      var cadastro = new CadastroSaldoInicial(null, 0L, "2026-01-01", null, null, false, "", "");
      var campos =
          validator.validate(cadastro).stream()
              .map(violacao -> violacao.getPropertyPath().toString())
              .toList();
      assertTrue(campos.containsAll(List.of("instituicaoId", "ativoId")));
      assertFalse(validator.validate(new CancelamentoSaldoInicial(" ")).isEmpty());
      assertTrue(
          validator
              .validate(new CancelamentoSaldoInicial("  " + "a".repeat(300) + "  "))
              .isEmpty());
      assertFalse(validator.validate(new CancelamentoSaldoInicial("a".repeat(301))).isEmpty());
    }
  }

  @Test
  void endpointsRejeitamEntradaEstruturalAntesDeChamarUsecase() throws Exception {
    var usecase = mock(ISaldoInicialUseCase.class);
    var mvc =
        MockMvcBuilders.standaloneSetup(new SaldoInicialController(usecase))
            .setControllerAdvice(new TratamentoGlobalException())
            .build();
    for (String rota :
        List.of("/api/v1/saldos-iniciais", "/api/v1/saldos-iniciais/1/substituicoes")) {
      mvc.perform(post(rota).contentType("application/json").content("{}"))
          .andExpect(status().isBadRequest());
    }
    mvc.perform(
            post("/api/v1/saldos-iniciais/1/cancelamento")
                .contentType("application/json")
                .content("{}"))
        .andExpect(status().isBadRequest())
        .andExpect(
            jsonPath("$.detail")
                .value("Informe o motivo do cancelamento. Limite: 300 caracteres."));
    verifyNoInteractions(usecase);
  }
}
