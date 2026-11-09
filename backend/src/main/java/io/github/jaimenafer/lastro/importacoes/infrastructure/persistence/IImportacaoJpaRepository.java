package io.github.jaimenafer.lastro.importacoes.infrastructure.persistence;

import io.github.jaimenafer.lastro.importacoes.domain.model.EventoImportado;
import io.github.jaimenafer.lastro.importacoes.domain.model.Importacao;
import io.github.jaimenafer.lastro.importacoes.domain.model.ResumoImportacao;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

public interface IImportacaoJpaRepository extends JpaRepository<Importacao, Long> {
  Optional<Importacao> findByPerfilRefAndChaveRequisicao(String perfil, String chave);

  Optional<Importacao> findByPerfilRefAndHash(String perfil, String hash);

  Optional<Importacao> findByPerfilRefAndId(String perfil, long id);

  @Query(
      "select new io.github.jaimenafer.lastro.importacoes.domain.model.ResumoImportacao(i.id,"
          + " i.hash, i.nomeArquivo, i.criadoEm, i.novos, i.reconhecidos) from Importacao i where"
          + " i.perfilRef = :perfil order by i.criadoEm desc, i.id desc")
  List<ResumoImportacao> listarResumos(String perfil, Pageable pagina);

  long countByPerfilRef(String perfil);

  @Query("select distinct e.operacaoId from EventoImportado e where e.importacao.perfilRef = :perfil and e.importacao.id <> :importacaoId and e.operacaoId in :operacaoIds")
  List<Long> operacoesCompartilhadas(String perfil, long importacaoId, Collection<Long> operacaoIds);


  @Query(
      "select evento from EventoImportado evento where evento.importacao.perfilRef = :perfil and"
          + " evento.importacao.versaoLeitor = 'binance-brl-v1' and evento.referencia in"
          + " :referencias order by evento.id")
  List<EventoImportado> buscarOrigensComprovadas(String perfil, Collection<String> referencias);
}
