package io.github.jaimenafer.lastro.operacoes.application.port;

import io.github.jaimenafer.lastro.operacoes.application.dto.*;
import io.github.jaimenafer.lastro.operacoes.domain.model.*;
import java.util.*;

public interface IAssociacoesLivro {
  List<GrupoEmConferencia> grupos(boolean propostas, Map<Long, String> impedimentos);

  List<GrupoEmConferencia> gruposParaTimeline(Map<Long, String> impedimentos);

  List<ItemConferenciaLivro> projetar(
      List<ItemConferenciaLivro> itens, boolean propostas, Map<Long, String> impedimentos);

  boolean aplicar(Map<Long, String> impedimentos);

  List<LinhaAssociacao> linhas();

  List<LinhaAssociacaoResponse> linhasResponse(
      List<LinhaAssociacao> linhas, Map<Long, String> numeros);

  GrupoAssociacaoResponse response(GrupoEmConferencia grupo, String revisao);

  List<GrupoAssociacaoResponse> respostas(List<GrupoEmConferencia> grupos, String revisao);

  void reservar(GrupoAssociacao grupo);
}
