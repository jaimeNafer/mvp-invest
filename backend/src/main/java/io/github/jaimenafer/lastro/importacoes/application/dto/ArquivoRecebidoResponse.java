package io.github.jaimenafer.lastro.importacoes.application.dto;

public record ArquivoRecebidoResponse(
    String id, String nomeArquivo, String hash, int registros, boolean reconhecido, String erro) {}
