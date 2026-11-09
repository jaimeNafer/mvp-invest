package io.github.jaimenafer.lastro.operacoes.domain.model;

import java.math.BigDecimal;
import java.util.List;

public record ResultadoCotacao(BigDecimal preco, boolean aproximado, List<CotacaoHistorica> caminho) {}
