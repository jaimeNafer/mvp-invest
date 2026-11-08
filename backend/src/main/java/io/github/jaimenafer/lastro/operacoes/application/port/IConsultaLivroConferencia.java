package io.github.jaimenafer.lastro.operacoes.application.port;

import io.github.jaimenafer.lastro.operacoes.application.dto.RetratoConferencia;
import io.github.jaimenafer.lastro.operacoes.domain.model.Operacao;
import io.github.jaimenafer.lastro.operacoes.domain.model.ResultadoLivro;
import java.time.Instant;
import java.util.List;

public interface IConsultaLivroConferencia {
  RetratoConferencia carregar(Instant ate);

  ResultadoLivro validarCandidato(List<Operacao> candidato);
}
