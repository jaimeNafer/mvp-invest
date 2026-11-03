package io.github.jaimenafer.lastro.carteiras.application.usecase.impl;

import io.github.jaimenafer.lastro.acesso.application.contract.IUsuarioAtual;
import io.github.jaimenafer.lastro.carteiras.application.dto.CadastroCarteira;
import io.github.jaimenafer.lastro.carteiras.application.dto.CarteiraResponse;
import io.github.jaimenafer.lastro.carteiras.application.mapper.CarteiraMapper;
import io.github.jaimenafer.lastro.carteiras.application.usecase.ICarteiraUseCase;
import io.github.jaimenafer.lastro.carteiras.domain.exception.CarteiraInvalidaException;
import io.github.jaimenafer.lastro.carteiras.domain.model.Carteira;
import io.github.jaimenafer.lastro.carteiras.domain.repository.ICarteiraRepository;
import io.github.jaimenafer.lastro.instituicoes.application.contract.IConsultaInstituicoes;
import io.github.jaimenafer.lastro.instituicoes.application.contract.InstituicaoResumo;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class CarteiraUseCaseImpl implements ICarteiraUseCase {
  private final ICarteiraRepository carteiras;
  private final IConsultaInstituicoes instituicoes;
  private final IUsuarioAtual usuarioAtual;

  @Transactional(readOnly = true)
  public List<CarteiraResponse> listar(Boolean ativo) {
    List<Carteira> carteirasEncontradas =
        ativo == null
            ? carteiras.findByPerfilRefOrderByNomeAscIdAsc(usuarioAtual.id())
            : carteiras.findByPerfilRefAndAtivoOrderByNomeAscIdAsc(usuarioAtual.id(), ativo);
    return mapearCarteiras(carteirasEncontradas);
  }

  @Transactional
  public CarteiraResponse cadastrar(CadastroCarteira cadastro) {
    String nome = cadastro.nome();
    var instituicaoIds = cadastro.instituicaoIds();
    Carteira carteira = Carteira.criar(usuarioAtual.id(), nome);
    carteira.sincronizarInstituicoes(selecionarInstituicoes(instituicaoIds, Set.of()));
    return mapearCarteira(carteiras.saveAndFlush(carteira));
  }

  @Transactional
  public CarteiraResponse alterar(long id, CadastroCarteira cadastro) {
    String nome = cadastro.nome();
    var instituicaoIds = cadastro.instituicaoIds();
    Carteira carteira = buscar(id);
    carteira.validarEdicaoPermitida();
    carteira.alterar(
        nome, selecionarInstituicoes(instituicaoIds, carteira.instituicaoIdsVinculadas()));
    return mapearCarteira(carteiras.saveAndFlush(carteira));
  }

  @Transactional
  public CarteiraResponse alterarSituacao(long id, boolean ativo) {
    Carteira carteira = buscar(id);
    carteira.alterarSituacao(ativo);
    return mapearCarteira(carteira);
  }

  private List<Long> selecionarInstituicoes(List<Long> instituicaoIds, Set<Long> vinculadas) {
    validarSelecaoSemRepeticoes(instituicaoIds);
    List<InstituicaoResumo> instituicoesSelecionadas = instituicoes.buscarPorIds(instituicaoIds);
    validarInstituicoesDisponiveis(instituicaoIds, instituicoesSelecionadas, vinculadas);
    return instituicoesSelecionadas.stream().map(InstituicaoResumo::id).toList();
  }

  private void validarSelecaoSemRepeticoes(List<Long> instituicaoIds) {
    if (new HashSet<>(instituicaoIds).size() != instituicaoIds.size()) {
      throw new CarteiraInvalidaException("Selecione pelo menos uma instituição, sem repetições.");
    }
  }

  private void validarInstituicoesDisponiveis(
      List<Long> instituicaoIds,
      List<InstituicaoResumo> instituicoesSelecionadas,
      Set<Long> vinculadas) {
    boolean possuiNovoVinculoInativo =
        instituicoesSelecionadas.stream()
            .anyMatch(
                instituicao -> !instituicao.ativo() && !vinculadas.contains(instituicao.id()));
    if (instituicoesSelecionadas.size() != instituicaoIds.size() || possuiNovoVinculoInativo) {
      throw new CarteiraInvalidaException("Selecione instituições disponíveis no catálogo.");
    }
  }

  private Carteira buscar(long carteiraId) {
    return carteiras.buscarObrigatoria(carteiraId, usuarioAtual.id());
  }

  private CarteiraResponse mapearCarteira(Carteira carteira) {
    return CarteiraMapper.de(carteira, carregarInstituicoes(carteira.instituicaoIdsVinculadas()));
  }

  private List<CarteiraResponse> mapearCarteiras(List<Carteira> carteirasEncontradas) {
    Set<Long> instituicaoIds =
        carteirasEncontradas.stream()
            .flatMap(carteira -> carteira.instituicaoIdsVinculadas().stream())
            .collect(Collectors.toSet());
    Map<Long, InstituicaoResumo> instituicoesPorId = carregarInstituicoes(instituicaoIds);
    return carteirasEncontradas.stream()
        .map(carteira -> CarteiraMapper.de(carteira, instituicoesPorId))
        .toList();
  }

  private Map<Long, InstituicaoResumo> carregarInstituicoes(Collection<Long> instituicaoIds) {
    return instituicoes.buscarPorIds(instituicaoIds).stream()
        .collect(Collectors.toMap(InstituicaoResumo::id, Function.identity()));
  }
}
