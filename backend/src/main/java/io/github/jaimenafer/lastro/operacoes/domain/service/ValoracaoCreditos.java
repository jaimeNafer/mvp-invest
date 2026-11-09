package io.github.jaimenafer.lastro.operacoes.domain.service;

import io.github.jaimenafer.lastro.operacoes.domain.model.CotacaoHistorica;
import io.github.jaimenafer.lastro.operacoes.domain.model.ItemConferenciaLivro;
import io.github.jaimenafer.lastro.operacoes.domain.model.Valoracao;
import java.util.List;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class ValoracaoCreditos {
  public static List<ItemConferenciaLivro> valorar(List<ItemConferenciaLivro> itens, Long brlId,
      List<CotacaoHistorica> cotacoes) {
    return itens.stream().map(item -> valorar(item, brlId, cotacoes)).toList();
  }

  private static ItemConferenciaLivro valorar(ItemConferenciaLivro item, Long brlId,
      List<CotacaoHistorica> cotacoes) {
    if (!item.creditoAutomatico() || item.custoEntradaBrl() != null) return item;
    Valoracao valor = MotorValoracao.valorar(item.quantidade(), item.ativoId(), brlId,
        item.ocorridaEm(), null, null, cotacoes);
    if (valor.valorTotalBrl() == null) return item;
    return new ItemConferenciaLivro(item.id(), item.contaId(), item.instituicaoId(), item.ativoId(),
        item.ocorridaEm(), item.ordem(), item.quantidade(), valor.valorTotalBrl(), item.operacao(),
        item.ajuste(), item.ancoraId(), item.transferenciaRef(), true, valor);
  }
}
