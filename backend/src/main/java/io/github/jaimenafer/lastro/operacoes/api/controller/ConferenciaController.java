package io.github.jaimenafer.lastro.operacoes.api.controller;

import io.github.jaimenafer.lastro.operacoes.application.dto.*;
import io.github.jaimenafer.lastro.operacoes.application.usecase.IConferenciaUseCase;
import io.github.jaimenafer.lastro.operacoes.application.usecase.IAgendamentoCotacoesUseCase;
import io.github.jaimenafer.lastro.operacoes.application.usecase.IValoracaoManualUseCase;
import jakarta.validation.Valid;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/conciliacao")
@RequiredArgsConstructor
public class ConferenciaController {
  private final IConferenciaUseCase usecase;
  private final IAgendamentoCotacoesUseCase cotacoes;
  private final IValoracaoManualUseCase valoracoes;

  @PutMapping("/registros/{id}/valoracao")
  public RevisaoConferenciaResponse valorarRegistro(
      @PathVariable String id, @Valid @RequestBody ValoracaoManualRequest request,
      @RequestHeader("Idempotency-Key") String chave) {
    return valoracoes.registrarLinha(id, request, chave);
  }

  @PostMapping("/cotacoes/atualizacoes")
  public AgendamentoCotacoesResponse atualizarCotacoes() {
    return new AgendamentoCotacoesResponse(cotacoes.solicitar());
  }

  @GetMapping("/cotacoes/atualizacao")
  public AtualizacaoCotacoesResponse ultimaAtualizacaoCotacoes() {
    return new AtualizacaoCotacoesResponse(cotacoes.ultimaAtualizacao());
  }

  @GetMapping("/timeline")
  public TimelineConferenciaResponse listar(
      @RequestParam(defaultValue = "0") int pagina,
      @RequestParam(defaultValue = "50") int tamanho,
      @RequestParam(required = false) String estado,
      @RequestParam(required = false) Long ativoId,
      @RequestParam(required = false) Long contaId) {
    return usecase.listar(pagina, tamanho, estado, ativoId, contaId);
  }

  @GetMapping("/contas")
  public List<ContaConferenciaResponse> contas() {
    return usecase.contas();
  }

  @PostMapping("/previa")
  public PreviaConferenciaResponse prever(@Valid @RequestBody PreviaConferenciaRequest r) {
    return usecase.prever(r);
  }

  @PostMapping("/confirmacoes")
  public RevisaoConferenciaResponse confirmar(
      @Valid @RequestBody ConfirmacaoConferenciaRequest r,
      @RequestHeader("Idempotency-Key") String chave) {
    return usecase.confirmar(r, chave);
  }

  @PutMapping("/registros/{id}")
  public RegistroTimelineResponse editar(
      @PathVariable String id,
      @Valid @RequestBody EdicaoRegistroRequest r,
      @RequestHeader("Idempotency-Key") String chave) {
    return usecase.editar(id, r, chave);
  }

  @PostMapping("/registros/{id}/revisao")
  public RegistroTimelineResponse revisar(
      @PathVariable String id,
      @Valid @RequestBody RevisaoRegistroRequest r,
      @RequestHeader("Idempotency-Key") String chave) {
    return usecase.revisar(id, r, chave);
  }

  @PostMapping("/saldos-anteriores")
  public RevisaoConferenciaResponse saldo(
      @Valid @RequestBody SaldoAnteriorConferenciaRequest r,
      @RequestHeader("Idempotency-Key") String chave) {
    return usecase.saldoAnterior(r, chave);
  }
}
