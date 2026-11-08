package io.github.jaimenafer.lastro.operacoes.domain.model;

import java.math.BigDecimal;
import java.util.List;

public record ResumoGrupo(
    NaturezaRegistro natureza,
    List<ParcelaGrupo> recebimentos,
    List<ParcelaGrupo> pagamentos,
    List<ParcelaGrupo> taxas,
    BigDecimal principalBrl,
    boolean completo,
    List<String> pendencias) {}
