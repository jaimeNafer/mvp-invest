package io.github.jaimenafer.lastro.operacoes.api.controller;

import io.github.jaimenafer.lastro.operacoes.application.dto.*;
import io.github.jaimenafer.lastro.operacoes.application.usecase.ISaldoInicialUseCase;
import jakarta.validation.Valid;
import java.net.URI;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
public class SaldoInicialController {
  private final ISaldoInicialUseCase saldos;

  @GetMapping("/api/v1/saldos-iniciais")
  public ListaSaldosIniciais listar(
      @RequestParam(required = false) Long instituicaoId,
      @RequestParam(required = false) Long ativoId,
      @RequestParam(required = false) List<Long> carteiraIds,
      @RequestParam(defaultValue = "CONFIRMADA") String situacao,
      @RequestParam(defaultValue = "0") int pagina,
      @RequestParam(defaultValue = "50") int tamanho) {
    return saldos.listar(instituicaoId, ativoId, carteiraIds, situacao, pagina, tamanho);
  }

  @GetMapping("/api/v1/saldos-iniciais/{id}")
  public SaldoInicialResponse buscar(@PathVariable long id) {
    return saldos.buscar(id);
  }

  @PostMapping("/api/v1/saldos-iniciais")
  public ResponseEntity<SaldoInicialResponse> criar(
      @Valid @RequestBody CadastroSaldoInicial entrada,
      @RequestHeader(value = "Idempotency-Key", required = false) String chave) {
    return responder(saldos.gravar(entrada, chave, null));
  }

  @PostMapping("/api/v1/saldos-iniciais/{id}/substituicoes")
  public ResponseEntity<SaldoInicialResponse> substituir(
      @PathVariable long id,
      @Valid @RequestBody CadastroSaldoInicial entrada,
      @RequestHeader(value = "Idempotency-Key", required = false) String chave) {
    return responder(saldos.gravar(entrada, chave, id));
  }

  @PostMapping("/api/v1/saldos-iniciais/{id}/cancelamento")
  public SaldoInicialResponse cancelar(
      @PathVariable long id, @Valid @RequestBody CancelamentoSaldoInicial entrada) {
    return saldos.cancelar(id, entrada.motivo());
  }

  @GetMapping("/api/v1/posicoes-iniciais")
  public List<PosicaoInicialResponse> posicoes() {
    return saldos.posicoes();
  }

  private ResponseEntity<SaldoInicialResponse> responder(ResultadoGravacaoSaldo resultado) {
    var corpo = resultado.operacao();
    return resultado.repetida()
        ? ResponseEntity.ok(corpo)
        : ResponseEntity.created(URI.create("/api/v1/saldos-iniciais/" + corpo.id())).body(corpo);
  }
}
