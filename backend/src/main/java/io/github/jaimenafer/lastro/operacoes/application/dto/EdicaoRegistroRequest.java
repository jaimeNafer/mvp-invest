package io.github.jaimenafer.lastro.operacoes.application.dto;

import io.github.jaimenafer.lastro.operacoes.domain.model.*;
import jakarta.validation.constraints.*;
import tools.jackson.databind.JsonNode;

public record EdicaoRegistroRequest(
    @NotBlank(message = "Informe a revisão do histórico.") String revisao,
    @NotBlank(message = "Informe a versão do registro.") String versao,
    String data,
    @NotBlank(message = "Informe a conta de custódia.") String contaId,
    String ativoId,
    JsonNode quantidade,
    JsonNode custoTotalBrl,
    @NotNull(message = "Informe a natureza do registro.") NaturezaRegistro natureza,
    @Size(max = 500, message = "O motivo aceita até 500 caracteres.") String motivo,
    String moedaCotacaoId,
    JsonNode totalBruto,
    JsonNode cotacaoBrl,
    String taxaAtivoId,
    JsonNode taxaQuantidade,
    Boolean taxaInformada,
    FinalidadeCripto finalidadeCripto) {}
