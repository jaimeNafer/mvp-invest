package io.github.jaimenafer.lastro.operacoes.application.usecase.impl;

import io.github.jaimenafer.lastro.operacoes.application.dto.DadosSaldoInicial;
import io.github.jaimenafer.lastro.operacoes.application.dto.EntradaSaldoInicial;
import io.github.jaimenafer.lastro.operacoes.domain.exception.SaldoInicialInvalidoException;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.ZoneId;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class ValidacaoSaldoInicial {
  private static final ZoneId FUSO = ZoneId.of("America/Fortaleza");

  public static DadosSaldoInicial normalizar(EntradaSaldoInicial entrada, boolean correcao) {
    LocalDate data;
    try {
      data = LocalDate.parse(entrada.dataReferencia() == null ? "" : entrada.dataReferencia());
    } catch (java.time.format.DateTimeParseException erro) {
      throw new SaldoInicialInvalidoException("Informe uma data válida.");
    }
    validarDataPermitida(data);
    var quantidade = decimal(entrada.quantidade(), 18, 20, "quantidade");
    var custo = decimal(entrada.custoTotalBrl(), 8, 30, "custo total");
    validarQuantidadePositiva(quantidade);
    var justificativa =
        texto(entrada.justificativaCustoZero(), false, "Informe a justificativa do custo zero.");
    validarDeclaracaoDeCustoZero(custo, entrada.custoZeroConfirmado(), justificativa);
    var motivo = texto(entrada.motivo(), correcao, "Informe o motivo da correção.");
    validarMotivoExclusivoDeCorrecao(correcao, motivo);
    return new DadosSaldoInicial(
        entrada.instituicaoId(), entrada.ativoId(), data, quantidade, custo, justificativa, motivo);
  }

  private static BigDecimal decimal(String valor, int escala, int inteiros, String campo) {
    if (valor == null
        || valor.length() > 160
        || !valor.matches("(?:0|[1-9][0-9]*)(?:\\.[0-9]+)?")) {
      throw new SaldoInicialInvalidoException("Informe " + campo + " em formato decimal válido.");
    }
    var numero = new BigDecimal(valor).stripTrailingZeros();
    if (Math.max(0, numero.scale()) > escala
        || Math.max(0, numero.precision() - numero.scale()) > inteiros) {
      throw new SaldoInicialInvalidoException(
          "O valor de " + campo + " excede a precisão permitida.");
    }
    return numero;
  }

  public static String texto(String valor, boolean obrigatorio, String mensagem) {
    var texto = valor == null ? "" : valor.strip();
    if (obrigatorio && texto.isEmpty())
      throw new SaldoInicialInvalidoException(mensagem + " Limite: 300 caracteres.");
    return texto;
  }

  public static void idValido(Long id) {
    if (id == null || id <= 0)
      throw new SaldoInicialInvalidoException("Informe identificadores válidos.");
  }

  private static void validarDataPermitida(LocalDate data) {
    if (data.getYear() < 1000 || data.isAfter(LocalDate.now(FUSO)))
      throw new SaldoInicialInvalidoException("Informe uma data válida, até hoje.");
  }

  private static void validarQuantidadePositiva(BigDecimal quantidade) {
    if (quantidade.signum() <= 0)
      throw new SaldoInicialInvalidoException("A quantidade deve ser maior que zero.");
  }

  private static void validarDeclaracaoDeCustoZero(
      BigDecimal custo, Boolean confirmado, String justificativa) {
    if (custo.signum() == 0 && (!Boolean.TRUE.equals(confirmado) || justificativa.isEmpty())) {
      throw new SaldoInicialInvalidoException("Confirme o custo zero e informe sua justificativa.");
    }
    if (custo.signum() > 0 && (Boolean.TRUE.equals(confirmado) || !justificativa.isEmpty())) {
      throw new SaldoInicialInvalidoException(
          "Confirmação e justificativa de custo zero exigem custo igual a zero.");
    }
  }

  private static void validarMotivoExclusivoDeCorrecao(boolean correcao, String motivo) {
    if (!correcao && !motivo.isEmpty())
      throw new SaldoInicialInvalidoException("Motivo de alteração é exclusivo da correção.");
  }
}
