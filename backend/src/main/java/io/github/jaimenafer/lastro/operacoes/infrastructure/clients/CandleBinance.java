package io.github.jaimenafer.lastro.operacoes.infrastructure.clients;

import java.math.BigDecimal;
import java.time.Instant;

public record CandleBinance(
    Instant abertoEm,
    BigDecimal aberto,
    BigDecimal maximo,
    BigDecimal minimo,
    BigDecimal fechado,
    BigDecimal volume,
    Instant fechadoEm,
    BigDecimal volumeCotacao,
    long numeroNegociacoes,
    BigDecimal volumeCompradoTaker,
    BigDecimal volumeCotacaoCompradaTaker) {}
