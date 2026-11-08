package io.github.jaimenafer.lastro.importacoes.application.dto;

import io.github.jaimenafer.lastro.importacoes.domain.model.DocumentoMovimentacoes;

public record DocumentoParaRecebimento(
    ArquivoParaRecebimento arquivo, DocumentoMovimentacoes documento, String erro) {}
