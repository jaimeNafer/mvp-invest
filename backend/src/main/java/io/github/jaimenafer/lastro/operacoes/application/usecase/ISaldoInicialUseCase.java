package io.github.jaimenafer.lastro.operacoes.application.usecase;

import io.github.jaimenafer.lastro.operacoes.application.dto.*;
import java.util.List;

public interface ISaldoInicialUseCase {
  ListaSaldosIniciais listar(
      Long instituicaoId,
      Long ativoId,
      List<Long> carteiraIds,
      String situacao,
      int pagina,
      int tamanho);

  SaldoInicialResponse buscar(long id);

  List<PosicaoInicialResponse> posicoes();

  ResultadoGravacaoSaldo gravar(CadastroSaldoInicial entrada, String chave, Long anteriorId);

  SaldoInicialResponse cancelar(long id, String motivo);
}
