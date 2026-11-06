package io.github.jaimenafer.lastro.operacoes.application.contract;

import java.util.List;

public record ResultadoLoteOperacoes(String revisao, List<ReconhecimentoOperacao> eventos) {}
