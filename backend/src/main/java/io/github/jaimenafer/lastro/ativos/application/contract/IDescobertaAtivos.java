package io.github.jaimenafer.lastro.ativos.application.contract;

import java.util.Optional;

public interface IDescobertaAtivos {
  Optional<AtivoResumo> identificarNaBinance(String simbolo);
}
