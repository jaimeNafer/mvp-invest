package io.github.jaimenafer.lastro.carteiras.application.mapper;

import io.github.jaimenafer.lastro.carteiras.application.dto.CarteiraResponse;
import io.github.jaimenafer.lastro.carteiras.application.dto.ContaResponse;
import io.github.jaimenafer.lastro.carteiras.domain.model.Carteira;
import io.github.jaimenafer.lastro.instituicoes.application.contract.InstituicaoResumo;
import java.util.Comparator;
import java.util.Map;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class CarteiraMapper {

  public static CarteiraResponse de(Carteira carteira, Map<Long, InstituicaoResumo> instituicoes) {
    var contas =
        carteira.contasVinculadas().stream()
            .map(conta ->
                new ContaResponse(
                    conta.getId().toString(),
                    conta.getInstituicaoId().toString(),
                    instituicoes.get(conta.getInstituicaoId()).nome()))
            .sorted(
                Comparator.comparing(ContaResponse::instituicaoNome)
                    .thenComparing(ContaResponse::id))
            .toList();
    return new CarteiraResponse(
        carteira.getId().toString(), carteira.getNome(), carteira.isAtivo(), contas);
  }
}
