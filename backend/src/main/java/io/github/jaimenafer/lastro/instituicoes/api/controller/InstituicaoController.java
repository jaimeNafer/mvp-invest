package io.github.jaimenafer.lastro.instituicoes.api.controller;

import io.github.jaimenafer.lastro.instituicoes.application.dto.InstituicaoResponse;
import io.github.jaimenafer.lastro.instituicoes.application.usecase.IInstituicaoUseCase;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
public class InstituicaoController {
  private final IInstituicaoUseCase useCase;

  @GetMapping("/api/v1/instituicoes")
  public List<InstituicaoResponse> listar() {
    return useCase.listar();
  }
}
