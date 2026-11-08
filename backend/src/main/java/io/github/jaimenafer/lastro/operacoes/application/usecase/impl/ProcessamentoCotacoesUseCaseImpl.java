package io.github.jaimenafer.lastro.operacoes.application.usecase.impl;

import io.github.jaimenafer.lastro.operacoes.application.port.IProvedorCotacoes;
import io.github.jaimenafer.lastro.operacoes.application.usecase.IProcessamentoCotacoes;
import io.github.jaimenafer.lastro.operacoes.domain.exception.CotacaoInvalidaException;
import io.github.jaimenafer.lastro.operacoes.domain.exception.LimiteCotacaoException;
import io.github.jaimenafer.lastro.operacoes.domain.model.CargaCotacao;
import io.github.jaimenafer.lastro.operacoes.domain.model.LotePrecos;
import io.github.jaimenafer.lastro.operacoes.domain.model.PrecoCandle;
import io.github.jaimenafer.lastro.operacoes.domain.repository.ICargaCotacaoRepository;
import io.github.jaimenafer.lastro.operacoes.domain.repository.IHistoricoCotacaoRepository;
import java.time.Duration;
import java.time.Instant;
import java.time.YearMonth;
import java.time.ZoneOffset;
import java.util.HashSet;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

@Service
@RequiredArgsConstructor
public class ProcessamentoCotacoesUseCaseImpl implements IProcessamentoCotacoes {
  private static final Duration PRAZO_RESERVA = Duration.ofMinutes(15);
  private static final int MAXIMO_TENTATIVAS = 3;
  private final ICargaCotacaoRepository cargas;
  private final IHistoricoCotacaoRepository cotacoes;
  private final IProvedorCotacoes provedor;
  private final PlatformTransactionManager transacoes;

  @Override
  public boolean processarProxima() {
    Instant agora = Instant.now();
    String token = UUID.randomUUID().toString();
    var reserva = transacao().execute(status -> cargas.reservar(agora, agora.plus(PRAZO_RESERVA), token));
    if (reserva == null || reserva.isEmpty()) return false;
    CargaCotacao carga = reserva.orElseThrow();
    LotePrecos lote;
    try {
      lote = provedor.consultar(carga.simbolo(), carga.granularidade(),
          YearMonth.from(carga.inicio().atZone(ZoneOffset.UTC)));
      validarLote(carga, lote);
    } catch (LimiteCotacaoException erro) {
      registrarFalha(carga, Duration.ofMillis(erro.getEsperaMillis()));
      return true;
    } catch (CotacaoInvalidaException | IllegalStateException | IllegalArgumentException erro) {
      registrarFalha(carga, Duration.ofMinutes(5));
      return true;
    }
    transacao().executeWithoutResult(status -> aplicarLote(carga, lote, agora));
    return true;
  }

  private void validarLote(CargaCotacao carga, LotePrecos lote) {
    var instantes = new HashSet<Instant>();
    for (PrecoCandle preco : lote.precos()) {
      if (preco.instante() == null || preco.preco() == null || preco.preco().signum() <= 0
          || preco.instante().isBefore(carga.inicio()) || !preco.instante().isBefore(carga.fim())
          || !instantes.add(preco.instante()) || preco.preco().precision() > 38
          || preco.preco().precision() - preco.preco().scale() > 20
          || Math.max(0, preco.preco().stripTrailingZeros().scale()) > 18)
        throw new CotacaoInvalidaException("O período contém cotações inválidas ou incompatíveis.");
    }
  }

  private void aplicarLote(CargaCotacao carga, LotePrecos lote, Instant inicioConsulta) {
    Instant agora = Instant.now();
    if (!cargas.concluir(carga.id(), carga.reservaToken(), lote.precos().isEmpty(), inicioConsulta, agora)) return;
    for (PrecoCandle preco : lote.precos())
      cotacoes.registrar(carga.parId(), carga.granularidade(), preco.instante(), preco.preco(),
          lote.checksumArquivo(), agora);
  }

  private void registrarFalha(CargaCotacao carga, Duration espera) {
    Instant agora = Instant.now();
    Instant proxima = carga.tentativas() < MAXIMO_TENTATIVAS ? agora.plus(espera) : null;
    transacao().executeWithoutResult(status -> cargas.falhar(carga.id(), carga.reservaToken(), agora, proxima));
  }

  private TransactionTemplate transacao() {
    return new TransactionTemplate(transacoes);
  }
}
