package io.github.jaimenafer.lastro.operacoes.application.mapper;

import static io.github.jaimenafer.lastro.operacoes.application.mapper.ConferenciaMapper.*;

import io.github.jaimenafer.lastro.operacoes.application.dto.*;
import io.github.jaimenafer.lastro.operacoes.domain.model.*;
import io.github.jaimenafer.lastro.operacoes.domain.service.AgrupamentoNegociacoes;
import java.math.*;
import java.util.*;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class AssociacaoMapper {
  public static LinhaAssociacaoResponse linha(
      LinhaAssociacao l, String numero, OrigemRegistro origem) {
    return new LinhaAssociacaoResponse(
        "registro:" + l.registroId(),
        Long.toString(l.versao()),
        l.data() == null ? null : l.data().toString(),
        l.instituicaoId().toString(),
        l.contaId() == null ? null : l.contaId().toString(),
        l.ticker(),
        decimal(l.quantidade()),
        l.operacao(),
        l.referencia(),
        l.papel(),
        numero,
        origem.campos().getOrDefault("arquivo", origem.arquivoRef()),
        origem.linha(),
        origem.aba());
  }

  public static GrupoAssociacaoResponse grupo(
      GrupoEmConferencia g, String revisao, String versao, Map<Long, OrigemRegistro> origens) {
    String numero = g.id() == null ? null : "OP-" + g.id();
    ResumoGrupo r = g.resumo();
    BigDecimal custo = AgrupamentoNegociacoes.custoFinal(r, g.proposta().cotacaoBrl());
    return new GrupoAssociacaoResponse(
        revisao,
        hash(revisao + "|" + numero + "|" + versao + "|" + escrever(g.proposta())),
        numero,
        versao,
        g.proposta().linhas().stream()
            .map(l -> linha(l, numero, origens.get(l.registroId())))
            .toList(),
        r.natureza(),
        parcelas(r.recebimentos()),
        parcelas(r.pagamentos()),
        parcelas(r.taxas()),
        decimal(r.principalBrl()),
        decimal(custo),
        r.pendencias(),
        g.proposta().desfazer() || r.natureza() == null
            ? List.of()
            : List.of(
                "A confirmação vinculará as contrapartidas sem repetir os efeitos das linhas."),
        r.completo(),
        g.proposta().desfazer(),
        g.proposta().taxaZeroConfirmada(),
        decimal(g.proposta().cotacaoBrl()));
  }

  private static List<ParcelaGrupoResponse> parcelas(List<ParcelaGrupo> parcelas) {
    return parcelas.stream()
        .map(p -> new ParcelaGrupoResponse(p.ticker(), decimal(p.quantidade())))
        .toList();
  }
}
