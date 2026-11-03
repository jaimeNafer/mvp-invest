package io.github.jaimenafer.lastro.instituicoes.application.mapper;

import io.github.jaimenafer.lastro.instituicoes.application.dto.InstituicaoResponse;
import io.github.jaimenafer.lastro.instituicoes.domain.model.Instituicao;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class InstituicaoMapper {

  public static InstituicaoResponse de(Instituicao instituicao) {
    return new InstituicaoResponse(
        instituicao.getId().toString(), instituicao.getNome(), instituicao.isAtivo());
  }
}
