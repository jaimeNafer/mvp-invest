package io.github.jaimenafer.lastro.operacoes.application.dto;

import io.github.jaimenafer.lastro.operacoes.domain.model.TipoAvisoConferencia;
import jakarta.validation.constraints.*;

public record FiltroAvisosConferencia(
    @NotNull(message = "Informe a data final da conferência.")
        @Pattern(
            regexp = "\\d{4}-\\d{2}-\\d{2}",
            message = "Informe a data final no formato AAAA-MM-DD.")
        String dataFim,
    @NotNull(message = "Informe o tipo de aviso.") TipoAvisoConferencia tipo,
    @Min(value = 0, message = "A página não pode ser negativa.") Integer pagina,
    @Min(value = 1, message = "O tamanho mínimo da página é 1.")
        @Max(value = 50, message = "O tamanho máximo da página é 50.")
        Integer tamanho,
    @Positive Long carteiraId) {

  public int paginaEfetiva() {
    return pagina == null ? 0 : pagina;
  }

  public int tamanhoEfetivo() {
    return tamanho == null ? 10 : tamanho;
  }
}
