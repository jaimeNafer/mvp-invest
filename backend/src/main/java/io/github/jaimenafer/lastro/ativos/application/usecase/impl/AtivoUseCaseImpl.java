package io.github.jaimenafer.lastro.ativos.application.usecase.impl;

import io.github.jaimenafer.lastro.ativos.application.dto.AtivoResponse;
import io.github.jaimenafer.lastro.ativos.application.mapper.AtivoMapper;
import io.github.jaimenafer.lastro.ativos.application.usecase.IAtivoUseCase;
import io.github.jaimenafer.lastro.ativos.domain.repository.IAtivoRepository;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class AtivoUseCaseImpl implements IAtivoUseCase {
  private final IAtivoRepository ativos;

  @Override
  @Transactional(readOnly = true)
  public List<AtivoResponse> listar() {
    return ativos.findAllByOrderByTickerAscIdAsc().stream().map(AtivoMapper::de).toList();
  }
}
