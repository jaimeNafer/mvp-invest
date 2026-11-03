package io.github.jaimenafer.lastro;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import io.github.jaimenafer.lastro.acesso.application.contract.IUsuarioAtual;
import io.github.jaimenafer.lastro.ativos.application.usecase.impl.ConsultaAtivosImpl;
import io.github.jaimenafer.lastro.ativos.domain.model.Ativo;
import io.github.jaimenafer.lastro.ativos.domain.repository.IAtivoRepository;
import io.github.jaimenafer.lastro.carteiras.application.usecase.impl.ConsultaCarteirasImpl;
import io.github.jaimenafer.lastro.carteiras.domain.model.Carteira;
import io.github.jaimenafer.lastro.carteiras.domain.repository.ICarteiraRepository;
import io.github.jaimenafer.lastro.instituicoes.application.usecase.impl.ConsultaInstituicoesImpl;
import io.github.jaimenafer.lastro.instituicoes.domain.model.Instituicao;
import io.github.jaimenafer.lastro.instituicoes.domain.repository.IInstituicaoRepository;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import org.junit.jupiter.api.Test;

class ContratosContextosTests {
  @Test
  void selecaoDeCarteirasUsaIdentidadeAtualUneInstituicoesENaoExponheEntidades() {
    var repository = mock(ICarteiraRepository.class);
    var usuario = mock(IUsuarioAtual.class);
    when(usuario.id()).thenReturn("jaime");
    var primeira = Carteira.criar("jaime", "Principal");
    primeira.alterar("Principal", List.of(1L, 2L));
    var segunda = Carteira.criar("jaime", "Binance");
    segunda.alterar("Binance", List.of(1L));
    when(repository.findByIdAndPerfilRef(10L, "jaime")).thenReturn(Optional.of(primeira));
    when(repository.findByIdAndPerfilRef(11L, "jaime")).thenReturn(Optional.of(segunda));
    when(repository.findByIdAndPerfilRef(99L, "jaime")).thenReturn(Optional.empty());
    var consulta = new ConsultaCarteirasImpl(repository, usuario);
    var ids = consulta.instituicoesVinculadas(List.of(10L, 11L)).orElseThrow();
    assertEquals(Set.of(1L, 2L), ids);
    assertThrows(UnsupportedOperationException.class, () -> ids.add(3L));
    assertTrue(consulta.instituicoesVinculadas(List.of(10L, 99L)).isEmpty());
  }

  @Test
  void catalogoDeInstituicoesIncluiInativasParaHistoricoEFazConsultaEmLote() {
    var repository = mock(IInstituicaoRepository.class);
    var entidade = mock(Instituicao.class);
    when(entidade.getId()).thenReturn(1L);
    when(entidade.getNome()).thenReturn("Binance");
    when(entidade.isAtivo()).thenReturn(false);
    when(repository.findAllByIdIn(Set.of(1L))).thenReturn(List.of(entidade));
    var resumo = new ConsultaInstituicoesImpl(repository).buscarPorIds(Set.of(1L)).getFirst();
    assertEquals(1L, resumo.id());
    assertEquals("Binance", resumo.nome());
    assertFalse(resumo.ativo());
    verify(repository, times(1)).findAllByIdIn(Set.of(1L));
    verify(repository, never()).findById(any());
  }

  @Test
  void catalogoDeAtivosPreservaMetadadosDeHistoricoSemLiberarNovaAbertura() {
    var repository = mock(IAtivoRepository.class);
    var entidade = mock(Ativo.class);
    when(entidade.getId()).thenReturn(1L);
    when(entidade.getCodigo()).thenReturn("cripto:OUTRO");
    when(entidade.getTicker()).thenReturn("OUTRO");
    when(entidade.getCasasDecimais()).thenReturn((byte) 18);
    when(entidade.isAtivo()).thenReturn(false);
    when(repository.findById(1L)).thenReturn(Optional.of(entidade));
    var resumo = new ConsultaAtivosImpl(repository).buscar(1L).orElseThrow();
    assertEquals("OUTRO", resumo.ticker());
    assertFalse(resumo.ativo());
    assertFalse(resumo.permiteSaldoInicial());
  }
}
