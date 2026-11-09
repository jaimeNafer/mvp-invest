package io.github.jaimenafer.lastro.operacoes.application.port;

import io.github.jaimenafer.lastro.operacoes.domain.model.LotePrecos;
import java.time.YearMonth;

public interface IProvedorCotacoes {
  LotePrecos consultar(String simbolo, String granularidade, YearMonth mes);
}
