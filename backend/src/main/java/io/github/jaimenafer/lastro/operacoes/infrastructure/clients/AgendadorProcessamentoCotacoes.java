package io.github.jaimenafer.lastro.operacoes.infrastructure.clients;

import io.github.jaimenafer.lastro.operacoes.application.usecase.IProcessamentoCotacoes;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@ConditionalOnProperty(prefix = "lastro.valoracao", name = "processamento-ativo", havingValue = "true")
public class AgendadorProcessamentoCotacoes {
  private final IProcessamentoCotacoes processamento;

  @Scheduled(fixedDelayString = "${lastro.valoracao.intervalo-processamento:PT10S}")
  public void executar() {
    processamento.processarProxima();
  }
}
