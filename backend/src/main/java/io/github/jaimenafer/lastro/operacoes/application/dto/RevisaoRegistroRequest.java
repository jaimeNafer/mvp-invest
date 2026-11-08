package io.github.jaimenafer.lastro.operacoes.application.dto;

import io.github.jaimenafer.lastro.operacoes.domain.model.*;
import jakarta.validation.constraints.*;
import java.util.*;

public record RevisaoRegistroRequest(
    @NotBlank(message = "Informe a revisão do histórico.") String revisao,
    @NotBlank(message = "Informe a versão do registro.") String versao) {}
