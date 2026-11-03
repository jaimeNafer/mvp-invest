package io.github.jaimenafer.lastro.ativos.application.mapper;

import io.github.jaimenafer.lastro.ativos.application.dto.AtivoResponse;
import io.github.jaimenafer.lastro.ativos.domain.model.Ativo;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class AtivoMapper {

  public static AtivoResponse de(Ativo ativo) {
    return new AtivoResponse(
        ativo.getId().toString(),
        ativo.getCodigo(),
        ativo.getTicker(),
        ativo.getNome(),
        ativo.getTipo(),
        ativo.getCasasDecimais(),
        ativo.isAtivo());
  }
}
