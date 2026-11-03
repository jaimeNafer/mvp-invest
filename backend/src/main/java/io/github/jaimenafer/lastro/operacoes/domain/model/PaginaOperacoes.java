package io.github.jaimenafer.lastro.operacoes.domain.model;

import java.util.List;

public record PaginaOperacoes(List<Operacao> itens, long total) {}
