package io.github.jaimenafer.lastro.operacoes.application.dto;

import io.github.jaimenafer.lastro.operacoes.domain.model.*;
import jakarta.validation.constraints.*;
import java.util.*;

public record CorrespondenciaConferenciaResponse(
    String id,
    List<String> registroIds,
    String descricao,
    String contaOrigem,
    String contaDestino,
    String quantidade,
    String ativoTicker) {}
