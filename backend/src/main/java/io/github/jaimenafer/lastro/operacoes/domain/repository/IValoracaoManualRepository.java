package io.github.jaimenafer.lastro.operacoes.domain.repository;

import io.github.jaimenafer.lastro.operacoes.domain.model.ValoracaoManual;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

public interface IValoracaoManualRepository {
  List<ValoracaoManual> listar(String perfil);
  void salvar(String perfil, Long registroId, Long grupoId, BigDecimal valor, String identidade, Instant agora);
}
