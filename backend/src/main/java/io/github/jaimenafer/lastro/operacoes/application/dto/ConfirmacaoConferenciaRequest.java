package io.github.jaimenafer.lastro.operacoes.application.dto;

import io.github.jaimenafer.lastro.operacoes.domain.model.*;
import jakarta.validation.constraints.*;
import java.util.*;

public record ConfirmacaoConferenciaRequest(
    @NotBlank(message = "Informe a revisão do histórico.") String revisao,
    @NotBlank(message = "Informe a identificação da prévia.") String previaId,
    @NotNull(message = "Informe a seleção de ajustes.")
        List<@NotBlank(message = "Identificador de ajuste vazio.") String> aceitarLacunaIds,
    @NotNull(message = "Informe a seleção de correspondências.")
        List<@NotBlank(message = "Identificador de correspondência vazio.") String>
            aceitarCorrespondenciaIds) {}
