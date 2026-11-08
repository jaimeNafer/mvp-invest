package io.github.jaimenafer.lastro.operacoes.application.usecase;

import io.github.jaimenafer.lastro.operacoes.application.dto.*;
import java.util.List;

public interface IConferenciaUseCase {
  TimelineConferenciaResponse listar(
      int pagina, int tamanho, String estado, Long ativoId, Long contaId);

  List<ContaConferenciaResponse> contas();

  PreviaConferenciaResponse prever(PreviaConferenciaRequest request);

  RevisaoConferenciaResponse confirmar(ConfirmacaoConferenciaRequest request, String chave);

  RegistroTimelineResponse editar(String id, EdicaoRegistroRequest request, String chave);

  RegistroTimelineResponse revisar(String id, RevisaoRegistroRequest request, String chave);

  RevisaoConferenciaResponse saldoAnterior(SaldoAnteriorConferenciaRequest request, String chave);
}
