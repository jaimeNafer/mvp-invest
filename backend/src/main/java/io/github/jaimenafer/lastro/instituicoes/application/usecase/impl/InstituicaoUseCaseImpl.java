package io.github.jaimenafer.lastro.instituicoes.application.usecase.impl;

import io.github.jaimenafer.lastro.instituicoes.application.dto.InstituicaoResponse;
import io.github.jaimenafer.lastro.instituicoes.application.mapper.InstituicaoMapper;
import io.github.jaimenafer.lastro.instituicoes.application.usecase.IInstituicaoUseCase;
import io.github.jaimenafer.lastro.instituicoes.domain.repository.IInstituicaoRepository;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class InstituicaoUseCaseImpl implements IInstituicaoUseCase {
  private final IInstituicaoRepository instituicoes;

  @Override
  @Transactional(readOnly = true)
  public List<InstituicaoResponse> listar() {
    return instituicoes.findAllByOrderByNomeAscIdAsc().stream().map(InstituicaoMapper::de).toList();
  }
}
