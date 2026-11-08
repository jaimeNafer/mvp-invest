package io.github.jaimenafer.lastro.operacoes;

import static org.assertj.core.api.Assertions.assertThat;

import io.github.jaimenafer.lastro.operacoes.domain.model.NaturezaRegistro;
import io.github.jaimenafer.lastro.operacoes.domain.service.ClassificacaoMovimentacao;
import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

class ClassificacaoMovimentacaoTests {
  @Test
  void movimentacoesEarnExigemDescricaoESinalExatos() {
    for (String tipo : List.of("Simple Earn Locked Subscription", "Simple Earn Flexible Subscription")) {
      assertThat(ClassificacaoMovimentacao.classificarMovimentacaoEarn(tipo, new BigDecimal("-1")))
          .isEqualTo(NaturezaRegistro.MOVIMENTACAO_INTERNA);
      assertThat(ClassificacaoMovimentacao.classificarMovimentacaoEarn(tipo, BigDecimal.ONE))
          .isEqualTo(NaturezaRegistro.OBSERVADA);
    }
    for (String tipo : List.of("Simple Earn Locked Redemption", "Simple Earn Flexible Redemption")) {
      assertThat(ClassificacaoMovimentacao.classificarMovimentacaoEarn(tipo, BigDecimal.ONE))
          .isEqualTo(NaturezaRegistro.MOVIMENTACAO_INTERNA);
      assertThat(ClassificacaoMovimentacao.classificarMovimentacaoEarn(tipo, BigDecimal.ZERO))
          .isEqualTo(NaturezaRegistro.OBSERVADA);
    }
    assertThat(ClassificacaoMovimentacao.classificarMovimentacaoEarn("Simple Earn Locked Rewards", BigDecimal.ONE))
        .isEqualTo(NaturezaRegistro.OBSERVADA);
    assertThat(ClassificacaoMovimentacao.classificarMovimentacaoEarn("Earn Unknown Subscription", new BigDecimal("-1")))
        .isEqualTo(NaturezaRegistro.OBSERVADA);
  }

  @Test
  void reconheceSomenteSaidaNegativaDeCripto() {
    assertThat(ClassificacaoMovimentacao.classificarSaidaCripto("Withdraw", new BigDecimal("-10"), true))
        .isEqualTo(NaturezaRegistro.SAIDA_CRIPTO);
    assertThat(ClassificacaoMovimentacao.classificarSaidaCripto("Withdraw", BigDecimal.ONE, true))
        .isEqualTo(NaturezaRegistro.OBSERVADA);
    assertThat(ClassificacaoMovimentacao.classificarSaidaCripto("Deposit", new BigDecimal("-10"), true))
        .isEqualTo(NaturezaRegistro.OBSERVADA);
    assertThat(ClassificacaoMovimentacao.classificarSaidaCripto("Withdraw", new BigDecimal("-10"), false))
        .isEqualTo(NaturezaRegistro.OBSERVADA);
  }

  @Test
  void classificaSomenteMovimentosBrlComSinalCompativel() {
    assertThat(ClassificacaoMovimentacao.classificar("Deposit", "BRL", new BigDecimal("1000"), Map.of(), false))
        .isEqualTo(NaturezaRegistro.APORTE);
    assertThat(ClassificacaoMovimentacao.classificar("Withdraw", "BRL", new BigDecimal("-100"), Map.of(), false))
        .isEqualTo(NaturezaRegistro.RETIRADA);
    assertThat(ClassificacaoMovimentacao.classificar("Deposit", "BTC", BigDecimal.ONE, Map.of(), false))
        .isEqualTo(NaturezaRegistro.OBSERVADA);
    assertThat(ClassificacaoMovimentacao.classificar("Withdraw", "BRL", BigDecimal.ONE, Map.of(), false))
        .isEqualTo(NaturezaRegistro.OBSERVADA);
  }

  @Test
  void preservaAmbiguidadesEIndicacoesDeTransferencia() {
    assertThat(ClassificacaoMovimentacao.classificar("Deposit", "BRL", BigDecimal.ONE, Map.of(), true))
        .isEqualTo(NaturezaRegistro.OBSERVADA);
    assertThat(ClassificacaoMovimentacao.classificar("Deposit", "BRL", BigDecimal.ONE, Map.of("Remark", "Internal transfer"), false))
        .isEqualTo(NaturezaRegistro.OBSERVADA);
    assertThat(ClassificacaoMovimentacao.classificar("Deposit", "BRL", BigDecimal.ONE, Map.of("Remark", "Transferência bancária"), false))
        .isEqualTo(NaturezaRegistro.APORTE);
  }
}
