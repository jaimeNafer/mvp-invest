package io.github.jaimenafer.lastro.operacoes.api.controller;

import io.github.jaimenafer.lastro.operacoes.application.dto.FiltroVisaoGeral;
import io.github.jaimenafer.lastro.operacoes.application.dto.VisaoGeralResponse;
import io.github.jaimenafer.lastro.operacoes.application.usecase.IVisaoGeralUseCase;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
public class VisaoGeralController {
  private final IVisaoGeralUseCase visaoGeral;

  @GetMapping("/api/v1/visao-geral")
  public VisaoGeralResponse consultar(@Valid @ModelAttribute FiltroVisaoGeral filtro) {
    return visaoGeral.consultar(filtro);
  }
}
