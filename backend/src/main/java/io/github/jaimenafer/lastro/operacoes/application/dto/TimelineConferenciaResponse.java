package io.github.jaimenafer.lastro.operacoes.application.dto;

import io.github.jaimenafer.lastro.operacoes.domain.model.*;
import jakarta.validation.constraints.*;
import java.util.*;

public record TimelineConferenciaResponse(
    List<RegistroTimelineResponse> itens,
    long total,
    int pagina,
    int tamanho,
    String revisao,
    ResumoConferenciaResponse resumo,
    String proximaPendenciaId,
    Integer proximaPendenciaPagina,
    Map<String, List<RegistroTimelineResponse>> linhasPorGrupo) {}
