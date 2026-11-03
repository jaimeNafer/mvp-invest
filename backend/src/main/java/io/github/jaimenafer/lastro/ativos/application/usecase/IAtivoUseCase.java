package io.github.jaimenafer.lastro.ativos.application.usecase;

import io.github.jaimenafer.lastro.ativos.application.dto.AtivoResponse;
import java.util.List;

public interface IAtivoUseCase {
  List<AtivoResponse> listar();
}
