package io.github.jaimenafer.lastro.operacoes.api.controller;

import io.github.jaimenafer.lastro.operacoes.application.dto.*;
import io.github.jaimenafer.lastro.operacoes.application.usecase.IAssociacaoUseCase;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/conciliacao/associacoes")
public class AssociacaoController {
  private final IAssociacaoUseCase usecase;

  @GetMapping("/candidatos")
  public CandidatosAssociacaoResponse candidatos(@RequestParam String registroId) {
    return usecase.candidatos(registroId);
  }

  @GetMapping("/{numero}")
  public GrupoAssociacaoResponse buscar(@PathVariable String numero) {
    return usecase.buscar(numero);
  }

  @PostMapping("/previa")
  public GrupoAssociacaoResponse prever(@Valid @RequestBody AssociacaoRequest request) {
    return usecase.prever(request);
  }

  @PostMapping("/propostas")
  public RevisaoAssociacaoResponse salvar(
      @Valid @RequestBody AssociacaoRequest request,
      @RequestHeader("Idempotency-Key") String chave) {
    return usecase.salvar(request, chave);
  }
}
