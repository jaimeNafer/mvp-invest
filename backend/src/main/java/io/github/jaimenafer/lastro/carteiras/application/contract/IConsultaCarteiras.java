package io.github.jaimenafer.lastro.carteiras.application.contract;

import java.util.*;

public interface IConsultaCarteiras {
  Optional<Set<Long>> instituicoesVinculadas(List<Long> carteiraIds);
}
