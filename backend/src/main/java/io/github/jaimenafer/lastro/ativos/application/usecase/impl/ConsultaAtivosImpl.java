package io.github.jaimenafer.lastro.ativos.application.usecase.impl;

import io.github.jaimenafer.lastro.ativos.application.contract.*;
import io.github.jaimenafer.lastro.ativos.domain.model.Ativo;
import io.github.jaimenafer.lastro.ativos.domain.repository.IAtivoRepository;
import java.util.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class ConsultaAtivosImpl implements IConsultaAtivos {
  private final IAtivoRepository ativos;

  @Override
  @Transactional(readOnly = true)
  public Optional<AtivoResumo> buscar(Long id) {
    return ativos.findById(id).map(this::resumir);
  }

  @Override
  @Transactional(readOnly = true)
  public List<AtivoResumo> buscarPorIds(Collection<Long> ids) {
    return ids.isEmpty()
        ? List.of()
        : ativos.findAllByIdIn(ids).stream().map(this::resumir).toList();
  }

  private AtivoResumo resumir(Ativo ativo) {
    return new AtivoResumo(
        ativo.getId(),
        ativo.getCodigo(),
        ativo.getTicker(),
        ativo.getCasasDecimais(),
        ativo.isAtivo(),
        ativo.permiteSaldoInicial(),
        ativo.cripto());
  }
}
