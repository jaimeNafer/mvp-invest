package io.github.jaimenafer.lastro.operacoes.domain.model;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "linhas_grupos_associacao")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class VinculoGrupoAssociacao {
  @Id private Long registroId;
  private Long grupoId;

  public static VinculoGrupoAssociacao vincular(Long registro, Long grupo) {
    var v = new VinculoGrupoAssociacao();
    v.registroId = registro;
    v.grupoId = grupo;
    return v;
  }
}
