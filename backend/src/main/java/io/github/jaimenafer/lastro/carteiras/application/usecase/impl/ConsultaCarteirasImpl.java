package io.github.jaimenafer.lastro.carteiras.application.usecase.impl;

import io.github.jaimenafer.lastro.acesso.application.contract.IUsuarioAtual;
import io.github.jaimenafer.lastro.carteiras.application.contract.IConsultaCarteiras;
import io.github.jaimenafer.lastro.carteiras.domain.repository.ICarteiraRepository;
import java.util.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class ConsultaCarteirasImpl implements IConsultaCarteiras {
  private final ICarteiraRepository carteiras;
  private final IUsuarioAtual usuario;

  @Override
  @Transactional(readOnly = true)
  public Optional<Set<Long>> instituicoesVinculadas(List<Long> carteiraIds) {
    var selecionadas = new HashSet<Long>();
    for (var id : carteiraIds) {
      var carteira = carteiras.findByIdAndPerfilRef(id, usuario.id());
      if (carteira.isEmpty()) return Optional.empty();
      carteira
          .get()
          .contasVinculadas()
          .forEach(conta -> selecionadas.add(conta.getInstituicaoId()));
    }
    return Optional.of(Set.copyOf(selecionadas));
  }
}
