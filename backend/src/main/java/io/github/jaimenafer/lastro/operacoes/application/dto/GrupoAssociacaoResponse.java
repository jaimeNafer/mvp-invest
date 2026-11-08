package io.github.jaimenafer.lastro.operacoes.application.dto;

import io.github.jaimenafer.lastro.operacoes.domain.model.NaturezaRegistro;
import java.util.List;

public record GrupoAssociacaoResponse(
    String revisao,
    String previaId,
    String numero,
    String versao,
    List<LinhaAssociacaoResponse> linhas,
    NaturezaRegistro natureza,
    List<ParcelaGrupoResponse> recebimentos,
    List<ParcelaGrupoResponse> pagamentos,
    List<ParcelaGrupoResponse> taxas,
    String principalBrl,
    String custoFinalBrl,
    List<String> pendencias,
    List<String> resolvidas,
    boolean completo,
    boolean desfazer,
    boolean taxaZeroConfirmada,
    String cotacaoBrl) {}
