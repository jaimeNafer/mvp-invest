package io.github.jaimenafer.lastro.carteiras.api.controller;

import io.github.jaimenafer.lastro.carteiras.application.dto.CadastroCarteira;
import io.github.jaimenafer.lastro.carteiras.application.dto.CarteiraResponse;
import io.github.jaimenafer.lastro.carteiras.application.dto.SituacaoCarteira;
import io.github.jaimenafer.lastro.carteiras.application.usecase.ICarteiraUseCase;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Positive;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/carteiras")
@RequiredArgsConstructor
public class CarteiraController {
  private final ICarteiraUseCase carteiras;

  @GetMapping
  public List<CarteiraResponse> listar(@RequestParam(required = false) Boolean ativo) {
    return carteiras.listar(ativo);
  }

  @PostMapping
  public ResponseEntity<CarteiraResponse> cadastrar(@Valid @RequestBody CadastroCarteira entrada) {
    var carteira = carteiras.cadastrar(entrada);
    return ResponseEntity.status(HttpStatus.CREATED).body(carteira);
  }

  @PutMapping("/{carteiraId}")
  public CarteiraResponse alterar(
      @PathVariable @Positive long carteiraId, @Valid @RequestBody CadastroCarteira entrada) {
    return carteiras.alterar(carteiraId, entrada);
  }

  @PatchMapping("/{carteiraId}/situacao")
  public CarteiraResponse alterarSituacao(
      @PathVariable @Positive long carteiraId, @Valid @RequestBody SituacaoCarteira entrada) {
    return carteiras.alterarSituacao(carteiraId, entrada.ativo());
  }
}
