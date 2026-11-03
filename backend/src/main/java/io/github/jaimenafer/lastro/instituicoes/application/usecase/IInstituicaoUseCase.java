package io.github.jaimenafer.lastro.instituicoes.application.usecase;

import io.github.jaimenafer.lastro.instituicoes.application.dto.InstituicaoResponse;
import java.util.List;

public interface IInstituicaoUseCase {
  List<InstituicaoResponse> listar();
}
