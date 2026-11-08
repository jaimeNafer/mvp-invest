package io.github.jaimenafer.lastro.operacoes.application.dto;

import io.github.jaimenafer.lastro.operacoes.domain.model.*;

public record GrupoEmConferencia(
    Long id,
    String identificador,
    PropostaGrupo proposta,
    ResumoGrupo resumo,
    boolean pendente,
    boolean automatico) {}
