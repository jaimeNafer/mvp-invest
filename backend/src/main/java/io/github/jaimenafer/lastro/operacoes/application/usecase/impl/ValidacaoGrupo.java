package io.github.jaimenafer.lastro.operacoes.application.usecase.impl;

import static io.github.jaimenafer.lastro.operacoes.application.mapper.ConferenciaMapper.*;

import io.github.jaimenafer.lastro.ativos.application.contract.AtivoResumo;
import io.github.jaimenafer.lastro.operacoes.application.dto.CadastroOperacao;
import io.github.jaimenafer.lastro.operacoes.domain.exception.OperacaoInvalidaException;
import io.github.jaimenafer.lastro.operacoes.domain.model.*;
import io.github.jaimenafer.lastro.operacoes.domain.service.*;
import java.util.*;
import java.time.Duration;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class ValidacaoGrupo {
  public static ResumoGrupo resumir(PropostaGrupo proposta, Map<Long, AtivoResumo> catalogo) {
    return resumir(proposta, catalogo, Duration.ofSeconds(2));
  }

  public static ResumoGrupo resumir(
      PropostaGrupo proposta, Map<Long, AtivoResumo> catalogo, Duration janela) {
    ResumoGrupo resumo =
        AgrupamentoNegociacoes.resumir(
            proposta.linhas(), proposta.taxaZeroConfirmada(), proposta.cotacaoBrl(), janela);
    if (!resumo.completo()) return resumo;
    FatoLivro fato =
        ProjecaoGrupo.projetar("grupo:validacao", proposta, resumo).getFirst().operacao();
    try {
      CadastroOperacao cadastro =
          new CadastroOperacao(
              fato.tipo(),
              fato.instituicaoId(),
              fato.ativoId(),
              fato.ocorridaEm().toString(),
              texto(decimal(fato.quantidade())),
              fato.moedaCotacaoId(),
              texto(decimal(fato.totalBruto())),
              texto(
                  decimal(
                      proposta.cotacaoBrl() == null ? fato.cotacaoBrl() : proposta.cotacaoBrl())),
              fato.taxaAtivoId(),
              texto(decimal(fato.taxaQuantidade())),
              "",
              "");
      if (resumo.natureza() == NaturezaRegistro.CONVERSAO)
        ValidacaoNegociacao.normalizarConversao(cadastro, catalogo);
      else ValidacaoNegociacao.normalizar(cadastro, catalogo, false);
      return resumo;
    } catch (OperacaoInvalidaException erro) {
      return new ResumoGrupo(
          resumo.natureza(),
          resumo.recebimentos(),
          resumo.pagamentos(),
          resumo.taxas(),
          resumo.principalBrl(),
          false,
          List.of(erro.getMessage()));
    }
  }
}
