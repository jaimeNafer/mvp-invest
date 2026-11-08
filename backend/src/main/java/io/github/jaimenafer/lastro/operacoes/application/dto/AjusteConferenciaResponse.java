package io.github.jaimenafer.lastro.operacoes.application.dto;

import io.github.jaimenafer.lastro.operacoes.domain.model.*;
import jakarta.validation.constraints.*;
import java.util.*;

public record AjusteConferenciaResponse(
    String lacunaId,
    String ancoraId,
    String contaId,
    String contaNome,
    String instituicaoNome,
    String ativoId,
    String ativoTicker,
    String quantidadeSugerida,
    String custoTotalBrl,
    String acao,
    String quantidadeAnterior) {}
