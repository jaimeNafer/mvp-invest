package io.github.jaimenafer.lastro.operacoes.application.port;

import io.github.jaimenafer.lastro.operacoes.application.dto.PreviaConferenciaResponse;

public interface IPreparacaoConciliacao {
  PreviaConferenciaResponse preparar(String revisao);
}
