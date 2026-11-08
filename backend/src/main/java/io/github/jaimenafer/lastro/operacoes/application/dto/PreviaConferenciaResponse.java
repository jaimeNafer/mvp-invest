package io.github.jaimenafer.lastro.operacoes.application.dto;

import io.github.jaimenafer.lastro.operacoes.domain.model.*;
import jakarta.validation.constraints.*;
import java.util.*;

public record PreviaConferenciaResponse(
    String revisao,
    String previaId,
    int registrosParaAplicar,
    List<AjusteConferenciaResponse> ajustes,
    List<CorrespondenciaConferenciaResponse> correspondencias,
    ResumoConferenciaResponse resumo,
    List<GrupoAssociacaoResponse> grupos) {}
