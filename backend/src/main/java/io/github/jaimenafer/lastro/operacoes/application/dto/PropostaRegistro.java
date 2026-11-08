package io.github.jaimenafer.lastro.operacoes.application.dto;

import io.github.jaimenafer.lastro.operacoes.domain.model.*;
import jakarta.validation.constraints.*;
import java.util.*;

public record PropostaRegistro(
    String data,
    String contaId,
    String ativoId,
    String quantidade,
    String custoTotalBrl,
    NaturezaRegistro natureza,
    String motivo,
    String moedaCotacaoId,
    String totalBruto,
    String cotacaoBrl,
    String taxaAtivoId,
    String taxaQuantidade,
    boolean taxaInformada,
    String transferenciaRef,
    FinalidadeCripto finalidadeCripto) {
  public PropostaRegistro(
      String data, String contaId, String ativoId, String quantidade, String custoTotalBrl,
      NaturezaRegistro natureza, String motivo, String moedaCotacaoId, String totalBruto,
      String cotacaoBrl, String taxaAtivoId, String taxaQuantidade, boolean taxaInformada,
      String transferenciaRef) {
    this(data, contaId, ativoId, quantidade, custoTotalBrl, natureza, motivo, moedaCotacaoId,
        totalBruto, cotacaoBrl, taxaAtivoId, taxaQuantidade, taxaInformada, transferenciaRef, null);
  }
  public PropostaRegistro {
    data = opcional(data);
    contaId = opcional(contaId);
    ativoId = opcional(ativoId);
    moedaCotacaoId = opcional(moedaCotacaoId);
    taxaAtivoId = opcional(taxaAtivoId);
  }

  private static String opcional(String valor) {
    return valor == null || valor.isBlank() ? null : valor.strip();
  }
}
