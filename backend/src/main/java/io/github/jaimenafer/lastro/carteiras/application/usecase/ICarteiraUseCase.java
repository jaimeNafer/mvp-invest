package io.github.jaimenafer.lastro.carteiras.application.usecase;

import io.github.jaimenafer.lastro.carteiras.application.dto.CadastroCarteira;
import io.github.jaimenafer.lastro.carteiras.application.dto.CarteiraResponse;
import java.util.List;

public interface ICarteiraUseCase {
  List<CarteiraResponse> listar(Boolean ativo);

  CarteiraResponse cadastrar(CadastroCarteira entrada);

  CarteiraResponse alterar(long id, CadastroCarteira entrada);

  CarteiraResponse alterarSituacao(long id, boolean ativo);
}
