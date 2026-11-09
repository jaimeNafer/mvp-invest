package io.github.jaimenafer.lastro.operacoes.application.dto;

import jakarta.validation.constraints.*;
import java.util.List;

public record FiltroHistoricoOperacoes(
    String tipo, String situacao, @Positive Long instituicaoId, @Positive Long ativoId,
    List<@NotNull @Positive Long> carteiraIds, @Positive Long arquivoId,
    @Min(0) Integer pagina, @Min(1) @Max(100) Integer tamanho) {
  public int paginaEfetiva() { return pagina == null ? 0 : pagina; }
  public int tamanhoEfetivo() { return tamanho == null ? 50 : tamanho; }
}
