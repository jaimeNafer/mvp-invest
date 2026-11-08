package io.github.jaimenafer.lastro.operacoes.application.mapper;

import io.github.jaimenafer.lastro.ativos.application.contract.AtivoResumo;
import io.github.jaimenafer.lastro.instituicoes.application.contract.InstituicaoResumo;
import io.github.jaimenafer.lastro.operacoes.application.dto.*;
import io.github.jaimenafer.lastro.operacoes.domain.model.*;
import java.math.BigDecimal;
import java.util.*;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class OperacoesMapper {
  public static String numero(BigDecimal valor) {
    return valor == null ? null : valor.stripTrailingZeros().toPlainString();
  }

  private static String id(Long valor) {
    return valor == null ? null : valor.toString();
  }

  public static CalculoLivroResponse calculo(CalculoOperacao calculo) {
    return calculo == null
        ? null
        : new CalculoLivroResponse(
            numero(calculo.valorBrutoBrl()),
            numero(calculo.taxaBrl()),
            numero(calculo.custoBaixadoBrl()),
            numero(calculo.resultadoNegociacaoBrl()),
            numero(calculo.resultadoMoedaBrl()),
            numero(calculo.resultadoTaxaBrl()),
            numero(calculo.resultadoTotalBrl()));
  }

  private static String ticker(Long id, Map<Long, AtivoResumo> ativos) {
    return id == null ? null : ativos.get(id).ticker();
  }

  public static OperacaoResponse de(
      Operacao operacao,
      CalculoOperacao calculo,
      Map<Long, AtivoResumo> ativos,
      Map<Long, InstituicaoResumo> instituicoes) {
    return new OperacaoResponse(
        id(operacao.getId()),
        operacao.getTipo(),
        id(operacao.getInstituicaoId()),
        instituicoes.get(operacao.getInstituicaoId()).nome(),
        id(operacao.getAtivoId()),
        ticker(operacao.getAtivoId(), ativos),
        operacao.getOcorridaEm().toString(),
        numero(operacao.getQuantidade()),
        numero(operacao.getCustoTotalBrl()),
        id(operacao.getMoedaCotacaoId()),
        ticker(operacao.getMoedaCotacaoId(), ativos),
        numero(operacao.getTotalBruto()),
        numero(operacao.precoUnitario()),
        numero(operacao.getCotacaoBrl()),
        id(operacao.getTaxaAtivoId()),
        ticker(operacao.getTaxaAtivoId(), ativos),
        numero(operacao.getTaxaQuantidade()),
        operacao.getReferencia(),
        operacao.getSituacao(),
        id(operacao.getSubstituiId()),
        operacao.getMotivoAlteracao(),
        operacao.getCriadoEm().toString(),
        operacao.getCriadoPor(),
        operacao.getAlteradoEm().toString(),
        operacao.getAlteradoPor(),
        calculo(calculo));
  }

  public static List<PosicaoLivroResponse> posicoes(
      ResultadoLivro livro,
      Map<Long, AtivoResumo> ativos,
      Map<Long, InstituicaoResumo> instituicoes) {
    return livro.posicoes().stream()
        .map(posicao ->
            new PosicaoLivroResponse(
                id(posicao.ativoId()),
                ticker(posicao.ativoId(), ativos),
                numero(posicao.quantidade()),
                numero(posicao.custoTotalBrl()),
                numero(posicao.custoMedioBrl()),
                posicao.instituicoes().stream()
                    .map(custodia ->
                        new CustodiaLivroResponse(
                            id(custodia.instituicaoId()),
                            instituicoes.get(custodia.instituicaoId()).nome(),
                            numero(custodia.quantidade())))
                    .sorted(
                        Comparator.comparing(CustodiaLivroResponse::instituicaoNome)
                            .thenComparing(CustodiaLivroResponse::instituicaoId))
                    .toList(),
                true,
                false,
                List.of()))
        .sorted(
            Comparator.comparing(PosicaoLivroResponse::ativoTicker)
                .thenComparing(PosicaoLivroResponse::ativoId))
        .toList();
  }
}
