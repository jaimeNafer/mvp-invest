package io.github.jaimenafer.lastro.importacoes.application.usecase;

import io.github.jaimenafer.lastro.importacoes.application.dto.ArquivoParaRecebimento;
import io.github.jaimenafer.lastro.importacoes.application.dto.HistoricoRecebimentosResponse;
import io.github.jaimenafer.lastro.importacoes.application.dto.RecebimentoResponse;
import java.util.List;

public interface IRecebimentoUseCase {
  void excluir(String id);
  RecebimentoResponse receber(List<ArquivoParaRecebimento> arquivos);

  HistoricoRecebimentosResponse listar(int pagina, int tamanho);
}
