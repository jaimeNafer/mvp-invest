package io.github.jaimenafer.lastro.operacoes.api.controller;

import io.github.jaimenafer.lastro.operacoes.application.dto.*;
import io.github.jaimenafer.lastro.operacoes.application.usecase.IOperacoesUseCase;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Positive;
import java.net.URI;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
public class OperacoesController {
  private final IOperacoesUseCase operacoes;

  @GetMapping("/api/v1/operacoes")
  public ListaOperacoesResponse listar(
      @RequestParam(required = false) String tipo,
      @RequestParam(defaultValue = "CONFIRMADA") String situacao,
      @RequestParam(required = false) Long instituicaoId,
      @RequestParam(required = false) Long ativoId,
      @RequestParam(required = false) List<Long> carteiraIds,
      @RequestParam(defaultValue = "0") int pagina,
      @RequestParam(defaultValue = "50") int tamanho) {
    return operacoes.listar(tipo, situacao, instituicaoId, ativoId, carteiraIds, pagina, tamanho);
  }

  @GetMapping("/api/v1/operacoes/{id}")
  public OperacaoResponse buscar(@PathVariable @Positive long id) {
    return operacoes.buscar(id);
  }

  @GetMapping("/api/v1/posicoes")
  public PosicoesLivroResponse posicoes() {
    return operacoes.posicoes();
  }

  @PostMapping("/api/v1/operacoes/previa")
  public PreviaOperacaoResponse prever(@Valid @RequestBody SolicitacaoPrevia solicitacao) {
    return operacoes.prever(solicitacao);
  }

  @PostMapping("/api/v1/operacoes")
  public ResponseEntity<OperacaoResponse> gravar(
      @Valid @RequestBody ConfirmacaoOperacao confirmacao,
      @RequestHeader(value = "Idempotency-Key", required = false) String chave) {
    return responder(operacoes.gravar(confirmacao, chave, null));
  }

  @PostMapping("/api/v1/operacoes/{id}/substituicoes")
  public ResponseEntity<OperacaoResponse> substituir(
      @PathVariable @Positive long id,
      @Valid @RequestBody ConfirmacaoOperacao confirmacao,
      @RequestHeader(value = "Idempotency-Key", required = false) String chave) {
    return responder(operacoes.gravar(confirmacao, chave, id));
  }

  @PostMapping("/api/v1/operacoes/{id}/cancelamento")
  public OperacaoResponse cancelar(
      @PathVariable @Positive long id, @Valid @RequestBody CancelamentoOperacao cancelamento) {
    return operacoes.cancelar(id, cancelamento);
  }

  private ResponseEntity<OperacaoResponse> responder(GravacaoOperacaoResponse resultado) {
    return resultado.repetida()
        ? ResponseEntity.ok(resultado.operacao())
        : ResponseEntity.created(URI.create("/api/v1/operacoes/" + resultado.operacao().id()))
            .body(resultado.operacao());
  }
}
