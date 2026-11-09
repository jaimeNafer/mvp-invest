package io.github.jaimenafer.lastro.operacoes;

import static org.assertj.core.api.Assertions.*;

import io.github.jaimenafer.lastro.operacoes.domain.model.CotacaoHistorica;
import io.github.jaimenafer.lastro.operacoes.domain.model.ItemConferenciaLivro;
import io.github.jaimenafer.lastro.operacoes.domain.model.EstadoValoracao;
import io.github.jaimenafer.lastro.operacoes.domain.service.ValoracaoCreditos;
import io.github.jaimenafer.lastro.operacoes.domain.service.MotorLivro;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

class ValoracaoCreditosTests {
  @Test
  void atribuiCustoEstimadoAoCreditoERecalculaPosicao() {
    Instant data = Instant.parse("2024-06-11T10:30:00Z");
    var credito = new ItemConferenciaLivro("registro:1",1L,1L,2L,data,1L,
        new BigDecimal("0.5"),null,null,false,null,null,true);
    var cotacoes = List.of(new CotacaoHistorica(2L,1L,data.minusSeconds(1800), new BigDecimal("200")));
    var itens = ValoracaoCreditos.valorar(List.of(credito), 1L, cotacoes);
    assertThat(itens.getFirst().valoracao()).isNotNull();
    assertThat(itens.getFirst().valoracao().estado()).isEqualTo(EstadoValoracao.ESTIMADO);
    assertThat(itens.getFirst().valoracao().caminho()).hasSize(1);
    var posicao = MotorLivro.conferir(itens, Map.of(1L,"fiat:BRL",2L,"cripto:BNB")).posicoes().getFirst();
    assertThat(posicao.quantidade()).isEqualByComparingTo("0.5");
    assertThat(posicao.custoTotalBrl()).isEqualByComparingTo("100");
  }

  @Test
  void preservaCustoManualEIgnoraPrecoPosterior() {
    Instant data = Instant.parse("2024-06-11T10:30:00Z");
    var manual = new ItemConferenciaLivro("registro:1",1L,1L,2L,data,1L,
        new BigDecimal("0.5"),new BigDecimal("40"),null,false,null,null,true);
    var semCusto = new ItemConferenciaLivro("registro:2",1L,1L,3L,data,2L,
        new BigDecimal("1"),null,null,false,null,null,true);
    var cotacoes = List.of(new CotacaoHistorica(2L,1L,data.minusSeconds(1),new BigDecimal("200")),
        new CotacaoHistorica(3L,1L,data.plusSeconds(1),new BigDecimal("100")));
    var itens = ValoracaoCreditos.valorar(List.of(manual,semCusto),1L,cotacoes);
    assertThat(itens.getFirst().custoEntradaBrl()).isEqualByComparingTo("40");
    assertThat(itens.getLast().custoEntradaBrl()).isNull();
  }
}
