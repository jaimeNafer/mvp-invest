package io.github.jaimenafer.lastro.operacoes.application.dto;

import io.github.jaimenafer.lastro.operacoes.domain.model.SituacaoOperacao;
import io.github.jaimenafer.lastro.operacoes.domain.model.TipoOperacao;

public record OperacaoResponse(
    String id,
    TipoOperacao tipo,
    String instituicaoId,
    String instituicaoNome,
    String ativoId,
    String ativoTicker,
    String ocorridaEm,
    String quantidade,
    String custoTotalBrl,
    String moedaCotacaoId,
    String moedaCotacaoTicker,
    String totalBruto,
    String precoUnitario,
    String cotacaoBrl,
    String taxaAtivoId,
    String taxaAtivoTicker,
    String taxaQuantidade,
    String referencia,
    SituacaoOperacao situacao,
    String substituiId,
    String motivoAlteracao,
    String criadoEm,
    String criadoPor,
    String alteradoEm,
    String alteradoPor,
    CalculoLivroResponse calculo) {}
