package io.github.jaimenafer.lastro.operacoes.application.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.*;

public record ConfirmacaoOperacao(
    @NotNull(message = "Atualize a prévia antes de confirmar.")
        @PositiveOrZero(message = "Informe uma revisão válida.")
        Long revisao,
    @NotNull(message = "Informe os dados da operação.") @Valid CadastroOperacao operacao) {}
