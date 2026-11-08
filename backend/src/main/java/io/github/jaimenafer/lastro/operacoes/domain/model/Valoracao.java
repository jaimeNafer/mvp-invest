package io.github.jaimenafer.lastro.operacoes.domain.model;

import java.math.BigDecimal;
import java.util.List;

public record Valoracao(EstadoValoracao estado, BigDecimal valorTotalBrl, List<CotacaoHistorica> caminho) {}
