package io.github.jaimenafer.lastro.operacoes.application.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import java.util.List;
import tools.jackson.databind.JsonNode;

public record AssociacaoRequest(
    @NotBlank String revisao,
    String numero,
    String versao,
    @NotNull @Size(max = 10000) List<@NotNull @Valid SelecaoLinhaRequest> linhas,
    boolean taxaZeroConfirmada,
    JsonNode cotacaoBrl,
    boolean desfazer,
    String previaId) {}
