package io.github.jaimenafer.lastro.operacoes.application.usecase;

import java.time.Instant;

public interface IAgendamentoCotacoesUseCase {
  int solicitar();
  Instant ultimaAtualizacao();
}
