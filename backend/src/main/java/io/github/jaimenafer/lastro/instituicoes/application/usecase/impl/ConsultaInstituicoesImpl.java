package io.github.jaimenafer.lastro.instituicoes.application.usecase.impl;

import io.github.jaimenafer.lastro.instituicoes.application.contract.*;
import io.github.jaimenafer.lastro.instituicoes.domain.model.Instituicao;
import io.github.jaimenafer.lastro.instituicoes.domain.repository.IInstituicaoRepository;
import java.util.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class ConsultaInstituicoesImpl implements IConsultaInstituicoes {
  private final IInstituicaoRepository instituicoes;

  @Override
  @Transactional(readOnly = true)
  public Optional<InstituicaoResumo> buscarPorCodigo(String codigo) {
    return instituicoes.findByCodigo(codigo).map(this::resumir);
  }

  @Override
  @Transactional(readOnly = true)
  public Optional<InstituicaoResumo> buscar(Long id) {
    return instituicoes.findById(id).map(this::resumir);
  }

  @Override
  @Transactional(readOnly = true)
  public List<InstituicaoResumo> buscarPorIds(Collection<Long> ids) {
    return ids.isEmpty()
        ? List.of()
        : instituicoes.findAllByIdIn(ids).stream().map(this::resumir).toList();
  }

  private InstituicaoResumo resumir(Instituicao instituicao) {
    return new InstituicaoResumo(instituicao.getId(), instituicao.getNome(), instituicao.isAtivo());
  }
}
