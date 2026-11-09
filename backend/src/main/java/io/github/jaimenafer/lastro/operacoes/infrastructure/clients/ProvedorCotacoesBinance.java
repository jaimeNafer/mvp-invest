package io.github.jaimenafer.lastro.operacoes.infrastructure.clients;

import io.github.jaimenafer.lastro.operacoes.application.port.IProvedorCotacoes;
import io.github.jaimenafer.lastro.operacoes.domain.model.LotePrecos;
import io.github.jaimenafer.lastro.operacoes.domain.model.PrecoCandle;
import java.time.YearMonth;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class ProvedorCotacoesBinance implements IProvedorCotacoes {
  private final ClienteCandlesBinance cliente;

  @Override
  public LotePrecos consultar(String simbolo, String granularidade, YearMonth mes) {
    String intervalo = switch (granularidade) {
      case "HORA" -> "1h";
      case "DIA" -> "1d";
      default -> throw new IllegalArgumentException("Granularidade de cotação inválida.");
    };
    LeituraCandlesBinance leitura = cliente.ler(simbolo, intervalo, mes);
    var precos = leitura.candles().stream()
        .filter(c -> c.numeroNegociacoes() > 0 && c.aberto().signum() > 0)
        .map(c -> new PrecoCandle(c.abertoEm(), c.aberto()))
        .toList();
    return new LotePrecos(precos, leitura.arquivoValidado() ? leitura.sha256() : null);
  }
}
