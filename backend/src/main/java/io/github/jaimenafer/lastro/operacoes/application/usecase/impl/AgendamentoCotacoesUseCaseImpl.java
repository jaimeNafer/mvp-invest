package io.github.jaimenafer.lastro.operacoes.application.usecase.impl;

import static io.github.jaimenafer.lastro.operacoes.application.mapper.ConferenciaMapper.instante;
import static io.github.jaimenafer.lastro.operacoes.application.mapper.ConferenciaMapper.proposta;

import io.github.jaimenafer.lastro.acesso.application.contract.IUsuarioAtual;
import io.github.jaimenafer.lastro.operacoes.application.dto.PropostaRegistro;
import io.github.jaimenafer.lastro.operacoes.application.usecase.IAgendamentoCotacoesUseCase;
import io.github.jaimenafer.lastro.operacoes.domain.model.ParCotacao;
import io.github.jaimenafer.lastro.operacoes.domain.model.RegistroConferencia;
import io.github.jaimenafer.lastro.operacoes.domain.repository.ICargaCotacaoRepository;
import io.github.jaimenafer.lastro.operacoes.domain.repository.IHistoricoCotacaoRepository;
import io.github.jaimenafer.lastro.operacoes.domain.repository.IRegistroConferenciaRepository;
import java.time.Instant;
import java.time.YearMonth;
import java.time.ZoneOffset;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class AgendamentoCotacoesUseCaseImpl implements IAgendamentoCotacoesUseCase {
  private final IUsuarioAtual usuario;
  private final IRegistroConferenciaRepository registros;
  private final ICargaCotacaoRepository cargas;
  private final IHistoricoCotacaoRepository cotacoes;

  @Override
  @Transactional(readOnly = true)
  public Instant ultimaAtualizacao() {
    return cotacoes.ultimaAtualizacao();
  }

  @Override
  @Transactional(isolation = Isolation.READ_COMMITTED)
  public int solicitar() {
    List<RegistroConferencia> historico = registros.listar(usuario.id());
    if (historico.isEmpty()) return 0;
    cargas.prepararParesConhecidos();
    List<ParCotacao> pares = cargas.listarPares();
    Set<String> periodos = new HashSet<>();
    Instant agora = Instant.now();
    for (RegistroConferencia registro : historico) {
      PropostaRegistro p = proposta(registro, true);
      if (p == null || p.data() == null || p.ativoId() == null) continue;
      Instant data = instante(p.data());
      if (data == null || data.isAfter(agora)) continue;
      YearMonth mes = YearMonth.from(data.atZone(ZoneOffset.UTC));
      Set<Long> ativos = new HashSet<>();
      adicionarAtivo(ativos, p.ativoId());
      adicionarAtivo(ativos, p.moedaCotacaoId());
      adicionarAtivo(ativos, p.taxaAtivoId());
      for (ParCotacao par : pares)
        if (ativos.contains(par.ativoBaseId()) || par.simbolo().equals("USDTBRL")
            || par.simbolo().equals("USDCUSDT")) {
          agendar(par, mes, "HORA", agora, periodos);
          if (data.atZone(ZoneOffset.UTC).getDayOfMonth() == 1)
            agendar(par, mes.minusMonths(1), "HORA", agora, periodos);
          YearMonth mesAtual = YearMonth.from(agora.atZone(ZoneOffset.UTC));
          for (YearMonth diario = mes; !diario.isAfter(mesAtual); diario = diario.plusMonths(1))
            agendar(par, diario, "DIA", agora, periodos);
        }
    }
    return periodos.size();
  }

  private void adicionarAtivo(Set<Long> ativos, String id) {
    if (id != null) ativos.add(Long.valueOf(id));
  }

  private void agendar(ParCotacao par, YearMonth mes, String granularidade, Instant agora,
      Set<String> periodos) {
    String chave = par.id() + ":" + mes + ":" + granularidade;
    if (!periodos.add(chave)) return;
    Instant inicio = mes.atDay(1).atStartOfDay(ZoneOffset.UTC).toInstant();
    Instant fim = mes.plusMonths(1).atDay(1).atStartOfDay(ZoneOffset.UTC).toInstant();
    cargas.agendar(par.id(), granularidade, inicio, fim, agora);
  }
}
