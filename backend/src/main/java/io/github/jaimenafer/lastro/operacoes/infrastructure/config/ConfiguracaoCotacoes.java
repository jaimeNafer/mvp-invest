package io.github.jaimenafer.lastro.operacoes.infrastructure.config;

import io.github.jaimenafer.lastro.operacoes.infrastructure.clients.ClienteCandlesBinance;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class ConfiguracaoCotacoes {
  @Bean
  ClienteCandlesBinance clienteCandlesBinance() {
    return new ClienteCandlesBinance();
  }
}
