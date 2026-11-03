package io.github.jaimenafer.lastro.operacoes.application.mapper;

import io.github.jaimenafer.lastro.ativos.application.contract.AtivoResumo;
import io.github.jaimenafer.lastro.instituicoes.application.contract.InstituicaoResumo;
import io.github.jaimenafer.lastro.operacoes.application.dto.*;
import io.github.jaimenafer.lastro.operacoes.domain.exception.SaldoInicialInvalidoException;
import io.github.jaimenafer.lastro.operacoes.domain.model.Operacao;
import io.github.jaimenafer.lastro.operacoes.domain.model.PosicaoInicial;
import io.github.jaimenafer.lastro.operacoes.domain.model.SituacaoOperacao;
import java.math.BigDecimal;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import tools.jackson.databind.JsonNode;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class SaldoInicialMapper {

  public static SituacaoOperacao situacaoDoFiltro(String situacao) {
    if ("TODAS".equals(situacao)) return null;
    for (SituacaoOperacao valor : SituacaoOperacao.values()) {
      if (valor.name().equals(situacao)) return valor;
    }
    throw new SaldoInicialInvalidoException("Verifique os filtros e a paginação.");
  }

  public static SaldoInicialResponse de(
      Operacao operacao, InstituicaoResumo instituicao, AtivoResumo ativo) {
    return new SaldoInicialResponse(
        operacao.getId().toString(),
        operacao.getInstituicaoId().toString(),
        instituicao.nome(),
        operacao.getAtivoId().toString(),
        ativo.ticker(),
        operacao.getDataReferencia(),
        numero(operacao.getQuantidade()),
        numero(operacao.getCustoTotalBrl()),
        operacao.getSituacao(),
        operacao.getSubstituiId() == null ? null : operacao.getSubstituiId().toString(),
        operacao.getJustificativaCustoZero(),
        operacao.getMotivoAlteracao(),
        operacao.getCriadoEm().toString(),
        operacao.getAlteradoEm().toString());
  }

  public static PosicaoInicialResponse posicao(PosicaoInicial posicao) {
    return new PosicaoInicialResponse(
        posicao.ativoId().toString(),
        posicao.ticker(),
        numero(posicao.quantidade()),
        numero(posicao.custo()),
        numero(posicao.medio()),
        posicao.instituicoes().stream()
            .map(custodia ->
                new CustodiaInicialResponse(
                    custodia.instituicaoId().toString(),
                    custodia.instituicaoNome(),
                    numero(custodia.quantidade()),
                    numero(custodia.custo())))
            .toList());
  }

  public static EntradaSaldoInicial entrada(CadastroSaldoInicial cadastro) {
    return new EntradaSaldoInicial(
        cadastro.instituicaoId(),
        cadastro.ativoId(),
        cadastro.dataReferencia(),
        decimal(cadastro.quantidade()),
        decimal(cadastro.custoTotalBrl()),
        cadastro.custoZeroConfirmado(),
        cadastro.justificativaCustoZero(),
        cadastro.motivo());
  }

  private static String numero(BigDecimal valor) {
    return valor.stripTrailingZeros().toPlainString();
  }

  private static String decimal(JsonNode valor) {
    if (valor == null || !valor.isString())
      throw new SaldoInicialInvalidoException("Envie quantidades e custos como textos decimais.");
    return valor.stringValue();
  }
}
