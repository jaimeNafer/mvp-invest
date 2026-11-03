package io.github.jaimenafer.lastro.operacoes.application.usecase;

import io.github.jaimenafer.lastro.operacoes.application.dto.*;
import java.util.List;

public interface IOperacoesUseCase {
  ListaOperacoesResponse listar(
      String tipo,
      String situacao,
      Long instituicaoId,
      Long ativoId,
      List<Long> carteiraIds,
      int pagina,
      int tamanho);

  OperacaoResponse buscar(long id);

  PreviaOperacaoResponse prever(SolicitacaoPrevia solicitacao);

  GravacaoOperacaoResponse gravar(ConfirmacaoOperacao confirmacao, String chave, Long anteriorId);

  OperacaoResponse cancelar(long id, CancelamentoOperacao cancelamento);

  PosicoesLivroResponse posicoes();
}
