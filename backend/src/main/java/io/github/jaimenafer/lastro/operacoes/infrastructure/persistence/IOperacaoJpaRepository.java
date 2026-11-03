package io.github.jaimenafer.lastro.operacoes.infrastructure.persistence;

import io.github.jaimenafer.lastro.operacoes.domain.model.Operacao;
import io.github.jaimenafer.lastro.operacoes.domain.model.SituacaoOperacao;
import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

public interface IOperacaoJpaRepository
    extends JpaRepository<Operacao, Long>, JpaSpecificationExecutor<Operacao> {
  Optional<Operacao> findByIdAndPerfilRef(Long id, String perfilRef);

  Optional<Operacao> findByPerfilRefAndChaveRequisicao(String perfilRef, String chave);

  @Override
  Page<Operacao> findAll(Specification<Operacao> filtro, Pageable pagina);

  List<Operacao> findByPerfilRefAndSituacaoOrderByIdAsc(
      String perfilRef, SituacaoOperacao situacao);

  boolean existsByPerfilRefAndSituacao(String perfilRef, SituacaoOperacao situacao);
}
