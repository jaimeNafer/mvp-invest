package io.github.jaimenafer.lastro.importacoes.api.controller;

import io.github.jaimenafer.lastro.importacoes.application.dto.*;
import io.github.jaimenafer.lastro.importacoes.application.usecase.IImportacaoUseCase;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Positive;
import java.io.IOException;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api/v1/importacoes")
@RequiredArgsConstructor
public class ImportacaoController {
  private final IImportacaoUseCase usecase;

  @PostMapping(value = "/previa", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
  public PreviaImportacaoResponse prever(
      @RequestPart MultipartFile arquivo,
      @RequestParam boolean aportesProprios,
      @RequestParam boolean taxasIncluidas,
      @RequestParam boolean posicaoAnteriorZerada)
      throws IOException {
    return usecase.prever(
        arquivo.getBytes(),
        new DecisoesImportacao(aportesProprios, taxasIncluidas, posicaoAnteriorZerada));
  }

  @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
  @ResponseStatus(HttpStatus.CREATED)
  public ReciboImportacaoResponse confirmar(
      @RequestPart MultipartFile arquivo,
      @RequestParam boolean aportesProprios,
      @RequestParam boolean taxasIncluidas,
      @RequestParam boolean posicaoAnteriorZerada,
      @RequestParam String hash,
      @RequestParam String revisao,
      @RequestParam String previaId,
      @RequestHeader("Idempotency-Key") String chave)
      throws IOException {
    return usecase.confirmar(
        arquivo.getBytes(),
        arquivo.getOriginalFilename(),
        new DecisoesImportacao(aportesProprios, taxasIncluidas, posicaoAnteriorZerada),
        hash,
        revisao,
        previaId,
        chave);
  }

  @GetMapping
  public PaginaImportacoesResponse listar(
      @RequestParam(defaultValue = "0") @Min(0) int pagina,
      @RequestParam(defaultValue = "20") @Min(1) @Max(100) int tamanho) {
    return usecase.listar(pagina, tamanho);
  }

  @GetMapping("/{id}")
  public ReciboImportacaoResponse consultar(@PathVariable @Positive long id) {
    return usecase.consultar(id);
  }
}
