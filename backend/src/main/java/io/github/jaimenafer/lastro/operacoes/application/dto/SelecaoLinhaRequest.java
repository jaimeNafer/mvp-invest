package io.github.jaimenafer.lastro.operacoes.application.dto;

import io.github.jaimenafer.lastro.operacoes.domain.model.PapelLinha;
import jakarta.validation.constraints.*;

public record SelecaoLinhaRequest(
    @NotBlank String id, @NotBlank String versao, @NotNull PapelLinha papel) {}
