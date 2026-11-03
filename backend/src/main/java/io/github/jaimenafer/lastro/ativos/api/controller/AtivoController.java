package io.github.jaimenafer.lastro.ativos.api.controller;

import io.github.jaimenafer.lastro.ativos.application.dto.AtivoResponse;
import io.github.jaimenafer.lastro.ativos.application.usecase.IAtivoUseCase;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
public class AtivoController {
  private final IAtivoUseCase useCase;

  @GetMapping("/api/v1/ativos")
  public List<AtivoResponse> listar() {
    return useCase.listar();
  }
}
