package io.github.jaimenafer.lastro.operacoes.application.dto;

import io.github.jaimenafer.lastro.operacoes.domain.model.*;
import jakarta.validation.constraints.*;
import java.util.*;

public record PreviaConferenciaRequest(
    @NotBlank(message = "Informe a revisão do histórico.") String revisao) {}
