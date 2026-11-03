package io.github.jaimenafer.lastro.operacoes;

import io.github.jaimenafer.lastro.operacoes.domain.model.FatoPosicaoInicial;
import io.github.jaimenafer.lastro.operacoes.domain.service.CalculoPosicaoInicial;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class CalculoPosicaoInicialTests {
    @Test void somaCustosExatamenteEDivideSemLimiteGlobalDeDigitos() {
        var p = CalculoPosicaoInicial.calcular(List.of(f("0.01","4000"), f("0.02","10000"))).getFirst();
        assertThat(p.quantidade()).isEqualByComparingTo("0.03");
        assertThat(p.custo()).isEqualByComparingTo("14000");
        assertThat(p.medio().toPlainString()).isEqualTo("466666.666666666666666666666666666666666667");
        var grande = CalculoPosicaoInicial.calcular(List.of(f("0.000000000000000001","999999999999999999999999999999.99999999"))).getFirst();
        assertThat(grande.medio().toBigInteger().toString()).hasSize(48);
    }
    @Test void divisaoPeriodicaNaoAlteraTotalECustoZeroNaoPerdeQuantidade() {
        var p = CalculoPosicaoInicial.calcular(List.of(f("3","1"))).getFirst();
        assertThat(p.medio().toPlainString()).isEqualTo("0.333333333333333333333333333333333333");
        assertThat(p.custo()).isEqualByComparingTo("1");
        assertThat(CalculoPosicaoInicial.calcular(List.of(f("1","0"))).getFirst().quantidade()).isEqualByComparingTo("1");
        assertThat(CalculoPosicaoInicial.calcular(List.of())).isEmpty();
    }
    private FatoPosicaoInicial f(String q,String c) { return new FatoPosicaoInicial(1L,"BTC",2L,"Binance",new BigDecimal(q),new BigDecimal(c)); }
}
