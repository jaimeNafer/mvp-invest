package io.github.jaimenafer.lastro.importacoes.infrastructure.persistence;

import static org.hibernate.exception.ConstraintViolationException.ConstraintKind.UNIQUE;

import io.github.jaimenafer.lastro.importacoes.domain.exception.*;
import io.github.jaimenafer.lastro.importacoes.domain.model.Importacao;
import io.github.jaimenafer.lastro.importacoes.domain.model.ResumoImportacao;
import io.github.jaimenafer.lastro.importacoes.domain.repository.IImportacaoRepository;
import java.util.List;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.hibernate.exception.ConstraintViolationException;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Repository;

@Repository
@RequiredArgsConstructor
public class ImportacaoPersistenceAdapter implements IImportacaoRepository {
  private final IImportacaoJpaRepository jpa;

  public List<Long> operacoesCompartilhadas(String perfil, long importacaoId, java.util.Collection<Long> ids) {
    return ids.isEmpty() ? List.of() : jpa.operacoesCompartilhadas(perfil, importacaoId, ids);
  }

  public void excluir(Importacao importacao) {
    jpa.delete(importacao);
    jpa.flush();
  }

  public Optional<Importacao> buscarPorChave(String perfil, String chave) {
    return jpa.findByPerfilRefAndChaveRequisicao(perfil, chave);
  }

  public Optional<Importacao> buscarPorHash(String perfil, String hash) {
    return jpa.findByPerfilRefAndHash(perfil, hash);
  }

  public Importacao buscarObrigatoria(String perfil, long id) {
    return jpa.findByPerfilRefAndId(perfil, id)
        .orElseThrow(() -> new ImportacaoNaoEncontradaException("Importação não encontrada."));
  }

  public List<ResumoImportacao> listar(String perfil, int pagina, int tamanho) {
    return jpa.listarResumos(perfil, PageRequest.of(pagina, tamanho));
  }

  public long contar(String perfil) {
    return jpa.countByPerfilRef(perfil);
  }

  public java.util.Map<String, Long> origensComprovadas(
      String perfil, java.util.Collection<String> referencias) {
    java.util.Map<String, Long> origens = new java.util.HashMap<>();
    if (!referencias.isEmpty())
      jpa.buscarOrigensComprovadas(perfil, referencias)
          .forEach(evento -> origens.putIfAbsent(evento.getReferencia(), evento.getOperacaoId()));
    return java.util.Map.copyOf(origens);
  }

  public Importacao salvar(Importacao importacao) {
    try {
      return jpa.saveAndFlush(importacao);
    } catch (DataIntegrityViolationException erro) {
      if (erro.getCause() instanceof ConstraintViolationException violacao
          && violacao.getKind() == UNIQUE)
        throw new ImportacaoConflitanteException(
            "O arquivo ou a chave de confirmação já foi registrado. Atualize o histórico e confira"
                + " o recibo.",
            erro);
      throw erro;
    }
  }
}
