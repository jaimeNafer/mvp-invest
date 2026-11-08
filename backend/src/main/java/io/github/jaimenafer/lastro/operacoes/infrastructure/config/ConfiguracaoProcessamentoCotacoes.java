package io.github.jaimenafer.lastro.operacoes.infrastructure.config;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;

@Configuration
@EnableScheduling
@ConditionalOnProperty(prefix = "lastro.valoracao", name = "processamento-ativo", havingValue = "true")
public class ConfiguracaoProcessamentoCotacoes {}
