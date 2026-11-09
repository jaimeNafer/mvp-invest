package io.github.jaimenafer.lastro.operacoes.infrastructure.persistence;

import io.github.jaimenafer.lastro.operacoes.domain.model.ComandoConciliacao;
import java.util.Optional;
import java.util.Collection;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;

public interface IComandoConciliacaoJpaRepository extends JpaRepository<ComandoConciliacao, Long> {
  Optional<ComandoConciliacao> findByPerfilRefAndChave(String perfilRef, String chave);

  @Modifying
  @Query(value = "DELETE FROM comandos_conciliacao WHERE perfil_ref=:perfil AND JSON_UNQUOTE(JSON_EXTRACT(resposta_json, '$.id')) IN (:ids)", nativeQuery = true)
  void excluirRespostasDeRegistros(String perfil, Collection<String> ids);

  @Modifying
  @Query(value = "DELETE FROM comandos_conciliacao WHERE perfil_ref=:perfil AND JSON_UNQUOTE(JSON_EXTRACT(resposta_json, '$.numero')) IN (:numeros)", nativeQuery = true)
  void excluirRespostasDeGrupos(String perfil, Collection<String> numeros);
}
