package io.github.jaimenafer.lastro.exceptions;

import io.github.jaimenafer.lastro.acesso.domain.exception.AcessoInvalidoException;
import io.github.jaimenafer.lastro.carteiras.domain.exception.CarteiraConflitanteException;
import io.github.jaimenafer.lastro.carteiras.domain.exception.CarteiraInvalidaException;
import io.github.jaimenafer.lastro.carteiras.domain.exception.CarteiraNaoEncontradaException;
import io.github.jaimenafer.lastro.importacoes.domain.exception.*;
import io.github.jaimenafer.lastro.operacoes.domain.exception.*;
import java.util.stream.Collectors;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.MessageSourceResolvable;
import org.springframework.http.*;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.method.annotation.HandlerMethodValidationException;

@Slf4j
@RestControllerAdvice
public class TratamentoGlobalException {
  @ExceptionHandler({
    CarteiraInvalidaException.class,
    SaldoInicialInvalidoException.class,
    ImportacaoInvalidaException.class,
    OperacaoInvalidaException.class
  })
  public ResponseEntity<ProblemDetail> entrada(RuntimeException erro) {
    return responder(erro.getMessage(), HttpStatus.BAD_REQUEST);
  }

  @ExceptionHandler({
    CarteiraNaoEncontradaException.class,
    SaldoInicialNaoEncontradoException.class,
    ImportacaoNaoEncontradaException.class,
    OperacaoNaoEncontradaException.class
  })
  public ResponseEntity<ProblemDetail> ausente(RuntimeException erro) {
    return responder(erro.getMessage(), HttpStatus.NOT_FOUND);
  }

  @ExceptionHandler({
    CarteiraConflitanteException.class,
    SaldoInicialConflitanteException.class,
    ImportacaoConflitanteException.class,
    OperacaoConflitanteException.class
  })
  public ResponseEntity<ProblemDetail> conflito(RuntimeException erro) {
    return responder(erro.getMessage(), HttpStatus.CONFLICT);
  }

  @ExceptionHandler(AcessoInvalidoException.class)
  public ResponseEntity<ProblemDetail> acesso(AcessoInvalidoException erro) {
    return responder(erro.getMessage(), HttpStatus.UNAUTHORIZED);
  }

  @ExceptionHandler(MethodArgumentNotValidException.class)
  public ResponseEntity<ProblemDetail> validarEntrada(MethodArgumentNotValidException erro) {
    var mensagem =
        erro.getBindingResult().getAllErrors().stream()
            .map(MessageSourceResolvable::getDefaultMessage)
            .distinct()
            .collect(Collectors.joining("; "));
    return responder(mensagem, HttpStatus.BAD_REQUEST);
  }

  @ExceptionHandler(HandlerMethodValidationException.class)
  public ResponseEntity<ProblemDetail> validarParametros(HandlerMethodValidationException erro) {
    var mensagem =
        erro.getAllErrors().stream()
            .map(MessageSourceResolvable::getDefaultMessage)
            .distinct()
            .collect(Collectors.joining("; "));
    return responder(mensagem, HttpStatus.BAD_REQUEST);
  }

  private ResponseEntity<ProblemDetail> responder(String mensagem, HttpStatus status) {
    log.error(String.format("Ocorreu um erro, status: %s, mensagem: %s", status, mensagem));
    return ResponseEntity.status(status).body(ProblemDetail.forStatusAndDetail(status, mensagem));
  }
}
