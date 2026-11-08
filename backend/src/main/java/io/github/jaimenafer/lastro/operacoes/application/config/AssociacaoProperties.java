package io.github.jaimenafer.lastro.operacoes.application.config;

import java.time.Duration;
import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties("lastro.conciliacao")
public record AssociacaoProperties(Duration janelaAssociacao) implements IConfiguracaoAssociacao {
  public AssociacaoProperties {
    if (janelaAssociacao == null || janelaAssociacao.isNegative()) {
      throw new IllegalArgumentException("A janela de associação deve ser positiva ou zero.");
    }
  }
}
