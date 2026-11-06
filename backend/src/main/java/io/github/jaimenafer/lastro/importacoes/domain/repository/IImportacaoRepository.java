package io.github.jaimenafer.lastro.importacoes.domain.repository;

import io.github.jaimenafer.lastro.importacoes.domain.model.Importacao;
import io.github.jaimenafer.lastro.importacoes.domain.model.ResumoImportacao;
import java.util.List;
import java.util.Optional;

public interface IImportacaoRepository {
  Optional<Importacao> buscarPorChave(String perfil, String chave);

  Optional<Importacao> buscarPorHash(String perfil, String hash);

  Importacao buscarObrigatoria(String perfil, long id);

  List<ResumoImportacao> listar(String perfil, int pagina, int tamanho);

  long contar(String perfil);

  java.util.Map<String, Long> origensComprovadas(
      String perfil, java.util.Collection<String> referencias);

  Importacao salvar(Importacao importacao);
}
