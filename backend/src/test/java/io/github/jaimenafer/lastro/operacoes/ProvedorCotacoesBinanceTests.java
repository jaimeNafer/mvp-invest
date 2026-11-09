package io.github.jaimenafer.lastro.operacoes;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import io.github.jaimenafer.lastro.operacoes.infrastructure.clients.CandleBinance;
import io.github.jaimenafer.lastro.operacoes.infrastructure.clients.ClienteCandlesBinance;
import io.github.jaimenafer.lastro.operacoes.infrastructure.clients.LeituraCandlesBinance;
import io.github.jaimenafer.lastro.operacoes.infrastructure.clients.ProvedorCotacoesBinance;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.YearMonth;
import java.util.List;
import org.junit.jupiter.api.Test;

class ProvedorCotacoesBinanceTests {
  @Test
  void preservaChecksumApenasDoArquivoEIgnoraCandlesSemNegociacoes() {
    ClienteCandlesBinance cliente = mock(ClienteCandlesBinance.class);
    YearMonth mes = YearMonth.of(2024, 6);
    CandleBinance negociado = candle(1);
    CandleBinance semNegociacao = candle(0);
    when(cliente.ler("BTCUSDT", "1h", mes))
        .thenReturn(new LeituraCandlesBinance("a".repeat(64), List.of(negociado, semNegociacao), true));
    var provedor = new ProvedorCotacoesBinance(cliente);
    var lote = provedor.consultar("BTCUSDT", "HORA", mes);
    assertThat(lote.checksumArquivo()).isEqualTo("a".repeat(64));
    assertThat(lote.precos()).singleElement().satisfies(p -> {
      assertThat(p.instante()).isEqualTo(negociado.abertoEm());
      assertThat(p.preco()).isEqualByComparingTo("12");
    });
  }

  private CandleBinance candle(long negociacoes) {
    Instant data = Instant.parse("2024-06-01T00:00:00Z");
    BigDecimal valor = new BigDecimal("12");
    return new CandleBinance(data, valor, valor, valor, valor, BigDecimal.ONE,
        data.plusSeconds(3599), BigDecimal.ONE, negociacoes, BigDecimal.ONE, BigDecimal.ONE);
  }
}
