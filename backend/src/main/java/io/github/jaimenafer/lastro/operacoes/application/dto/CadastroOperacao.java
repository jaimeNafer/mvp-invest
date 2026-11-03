package io.github.jaimenafer.lastro.operacoes.application.dto;

import io.github.jaimenafer.lastro.operacoes.domain.model.TipoOperacao;
import jakarta.validation.constraints.*;
import tools.jackson.databind.JsonNode;

public record CadastroOperacao(
    @NotNull(message = "Informe o tipo da operação.") TipoOperacao tipo,
    @NotNull(message = "Informe a instituição.")
        @Positive(message = "Informe uma instituição válida.")
        Long instituicaoId,
    @NotNull(message = "Informe o ativo.") @Positive(message = "Informe um ativo válido.")
        Long ativoId,
    @NotBlank(message = "Informe data e hora da operação.") String ocorridaEm,
    @NotNull(message = "Informe a quantidade como texto decimal.") JsonNode quantidade,
    @Positive(message = "Informe uma moeda de cotação válida.") Long moedaCotacaoId,
    JsonNode totalBruto,
    JsonNode cotacaoBrl,
    @Positive(message = "Informe o ativo da taxa válido.") Long taxaAtivoId,
    JsonNode taxaQuantidade,
    @Size(max = 160, message = "A referência aceita até 160 caracteres.") String referencia,
    @Size(max = 300, message = "O motivo aceita até 300 caracteres.") String motivo) {
  public CadastroOperacao {
    referencia = referencia == null ? "" : referencia.strip();
    motivo = motivo == null ? "" : motivo.strip();
  }
}
