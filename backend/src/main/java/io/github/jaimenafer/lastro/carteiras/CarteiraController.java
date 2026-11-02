package io.github.jaimenafer.lastro.carteiras;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import java.util.List;
import java.util.Comparator;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.dao.ConcurrencyFailureException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.method.annotation.HandlerMethodValidationException;

@RestController
@RequestMapping("/api/v1/carteiras")
public class CarteiraController {
    private final CarteiraService carteiras;

    public CarteiraController(CarteiraService carteiras) {
        this.carteiras = carteiras;
    }

    @GetMapping
    public List<Resposta> listar(@RequestParam(required = false) Boolean ativo) {
        return carteiras.listar(ativo).stream().map(Resposta::de).toList();
    }

    @PostMapping
    public ResponseEntity<Resposta> cadastrar(@Valid @RequestBody Cadastro entrada) {
        var carteira = carteiras.cadastrar(entrada.nome(), entrada.instituicaoIds());
        return ResponseEntity.status(HttpStatus.CREATED).body(Resposta.de(carteira));
    }

    @PutMapping("/{carteiraId}")
    public Resposta alterar(@PathVariable @Positive long carteiraId, @Valid @RequestBody Cadastro entrada) {
        return Resposta.de(carteiras.alterar(carteiraId, entrada.nome(), entrada.instituicaoIds()));
    }

    @PatchMapping("/{carteiraId}/situacao")
    public Resposta alterarSituacao(@PathVariable @Positive long carteiraId,
                                    @Valid @RequestBody Situacao entrada) {
        return Resposta.de(carteiras.alterarSituacao(carteiraId, entrada.ativo()));
    }

    @ExceptionHandler(CarteiraService.NaoEncontrada.class)
    @ResponseStatus(HttpStatus.NOT_FOUND)
    public ProblemDetail naoEncontrado(CarteiraService.NaoEncontrada erro) {
        return ProblemDetail.forStatusAndDetail(HttpStatus.NOT_FOUND, erro.getMessage());
    }

    @ExceptionHandler(CarteiraService.DadosInvalidos.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public ProblemDetail cadastroInvalido(CarteiraService.DadosInvalidos erro) {
        return ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST, erro.getMessage());
    }

    @ExceptionHandler({DataIntegrityViolationException.class, ConcurrencyFailureException.class})
    @ResponseStatus(HttpStatus.CONFLICT)
    public ProblemDetail conflitoDeCadastro() {
        return ProblemDetail.forStatusAndDetail(HttpStatus.CONFLICT,
            "Nome de carteira já utilizado ou cadastro alterado. Atualize e tente novamente.");
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public ProblemDetail entradaInvalida(MethodArgumentNotValidException erro) {
        var campo = erro.getBindingResult().getFieldError();
        return ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST,
            campo == null ? "Verifique os dados informados." : campo.getDefaultMessage());
    }

    @ExceptionHandler(HandlerMethodValidationException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public ProblemDetail parametrosInvalidos(HandlerMethodValidationException erro) {
        var mensagem = erro.getAllErrors().isEmpty()
            ? "Verifique os dados informados." : erro.getAllErrors().getFirst().getDefaultMessage();
        return ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST, mensagem);
    }

    public record Cadastro(
        @NotBlank(message = "Informe o nome da carteira.")
        @Size(max = 160, message = "O nome deve ter até 160 caracteres.") String nome,
        @NotEmpty(message = "Selecione pelo menos uma instituição.")
        List<@NotNull(message = "Informe uma instituição válida.")
             @Positive(message = "Informe uma instituição válida.") Long> instituicaoIds) {
        public Cadastro { nome = nome == null ? null : nome.strip(); }
    }

    public record Situacao(@NotNull(message = "Informe a situação da carteira.") Boolean ativo) {}

    public record Resposta(String id, String nome, boolean ativo, List<ContaResposta> contas) {
        static Resposta de(Carteira carteira) {
            var contas = carteira.getContas().stream().map(conta -> new ContaResposta(
                conta.getId().toString(), conta.getInstituicao().getId().toString(), conta.getInstituicao().getNome()))
                .sorted(Comparator.comparing(ContaResposta::instituicaoNome).thenComparing(ContaResposta::id)).toList();
            return new Resposta(carteira.getId().toString(), carteira.getNome(), carteira.isAtivo(), contas);
        }
    }

    public record ContaResposta(String id, String instituicaoId, String instituicaoNome) {}
}
