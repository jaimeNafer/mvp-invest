package io.github.jaimenafer.lastro.importacoes.application.usecase;

import io.github.jaimenafer.lastro.importacoes.application.dto.*;

public interface IImportacaoUseCase {
  PreviaImportacaoResponse prever(byte[] arquivo, DecisoesImportacao decisoes);

  ReciboImportacaoResponse confirmar(
      byte[] arquivo,
      String nome,
      DecisoesImportacao decisoes,
      String hash,
      String revisao,
      String previaId,
      String chave);

  PaginaImportacoesResponse listar(int pagina, int tamanho);

  ReciboImportacaoResponse consultar(long id);
}
