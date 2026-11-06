package io.github.jaimenafer.lastro.operacoes.domain.repository;

import io.github.jaimenafer.lastro.operacoes.domain.model.FiltroOperacoes;
import io.github.jaimenafer.lastro.operacoes.domain.model.Operacao;
import io.github.jaimenafer.lastro.operacoes.domain.model.PaginaOperacoes;
import io.github.jaimenafer.lastro.operacoes.domain.model.SituacaoOperacao;
import java.util.List;
import java.util.Optional;

public interface IOperacaoRepository {
  Operacao buscarObrigatoria(Long operacaoId, String perfilRef);

  Optional<Operacao> findByPerfilRefAndChaveRequisicao(String perfilRef, String chave);

  List<Operacao> findByPerfilRefAndSituacaoOrderByIdAsc(
      String perfilRef, SituacaoOperacao situacao);

  boolean existsByPerfilRefAndSituacao(String perfilRef, SituacaoOperacao situacao);

  Operacao saveAndFlush(Operacao operacao);

  void salvarLote(List<Operacao> operacoes);

  List<Operacao> historico(String perfilRef);

  void flush();

  PaginaOperacoes consultar(FiltroOperacoes filtro);
}
