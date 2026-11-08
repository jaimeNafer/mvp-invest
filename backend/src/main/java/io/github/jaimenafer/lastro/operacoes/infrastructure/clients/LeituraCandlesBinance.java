package io.github.jaimenafer.lastro.operacoes.infrastructure.clients;

import java.util.List;

public record LeituraCandlesBinance(String sha256, List<CandleBinance> candles, boolean arquivoValidado) {
  public LeituraCandlesBinance(String sha256, List<CandleBinance> candles) {
    this(sha256, candles, false);
  }
  public LeituraCandlesBinance {
    candles = List.copyOf(candles);
  }
}
