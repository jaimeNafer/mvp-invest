package io.github.jaimenafer.lastro.operacoes.domain.model;

import java.util.Set;

public record FiltroOperacoes(
    String perfilRef,
    Long instituicaoId,
    Long ativoId,
    Set<Long> instituicaoIds,
    SituacaoOperacao situacao,
    int pagina,
    int tamanho) {}
