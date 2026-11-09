package io.github.jaimenafer.lastro.importacoes.infrastructure.persistence;

import io.github.jaimenafer.lastro.importacoes.domain.model.ArquivoRecebido;
import io.github.jaimenafer.lastro.importacoes.domain.model.ResumoArquivoRecebido;
import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

public interface IArquivoRecebidoJpaRepository extends JpaRepository<ArquivoRecebido, Long> {
  Optional<ArquivoRecebido> findByPerfilRefAndId(String perfilRef, Long id);
  Optional<ArquivoRecebido> findByPerfilRefAndHash(String perfilRef, String hash);

  @Query("""
      select new io.github.jaimenafer.lastro.importacoes.domain.model.ResumoArquivoRecebido(
          a.id, a.nomeArquivo, a.criadoEm, a.registros)
      from ArquivoRecebido a where a.perfilRef = :perfilRef
      order by a.criadoEm desc, a.id desc
      """)
  List<ResumoArquivoRecebido> listar(String perfilRef, Pageable pagina);

  long countByPerfilRef(String perfilRef);
}
