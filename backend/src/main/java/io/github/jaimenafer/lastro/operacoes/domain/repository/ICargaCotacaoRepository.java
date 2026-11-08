package io.github.jaimenafer.lastro.operacoes.domain.repository;

import io.github.jaimenafer.lastro.operacoes.domain.model.CargaCotacao;
import io.github.jaimenafer.lastro.operacoes.domain.model.ParCotacao;
import java.time.Instant;
import java.util.List;
import java.util.Optional;

public interface ICargaCotacaoRepository {
  void prepararParesConhecidos();
  List<ParCotacao> listarPares();
  void agendar(Long parId, String granularidade, Instant inicio, Instant fim, Instant agora);
  Optional<CargaCotacao> reservar(Instant agora, Instant reservaAte, String token);
  boolean concluir(Long id, String token, boolean semDados, Instant agora);
  boolean concluir(Long id, String token, boolean semDados, Instant inicioConsulta, Instant agora);
  boolean falhar(Long id, String token, Instant agora, Instant proximaTentativa);
}
