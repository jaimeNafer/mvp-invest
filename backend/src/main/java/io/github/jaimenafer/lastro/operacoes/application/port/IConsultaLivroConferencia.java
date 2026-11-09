package io.github.jaimenafer.lastro.operacoes.application.port;

import io.github.jaimenafer.lastro.operacoes.application.dto.RetratoConferencia;
import io.github.jaimenafer.lastro.operacoes.domain.model.Operacao;
import io.github.jaimenafer.lastro.operacoes.domain.model.ResultadoLivro;
import java.time.Instant;
import java.util.List;
import java.util.Set;

public interface IConsultaLivroConferencia {
  RetratoConferencia carregar(Instant ate);

  RetratoConferencia carregar(Instant ate, Set<Long> instituicoes);

  ResultadoLivro validarCandidato(List<Operacao> candidato);
}
