package io.github.jaimenafer.lastro.operacoes.application.usecase.impl;

import io.github.jaimenafer.lastro.acesso.application.contract.IUsuarioAtual;
import io.github.jaimenafer.lastro.ativos.application.contract.*;
import io.github.jaimenafer.lastro.instituicoes.application.contract.*;
import io.github.jaimenafer.lastro.operacoes.application.dto.*;
import io.github.jaimenafer.lastro.operacoes.application.mapper.*;
import io.github.jaimenafer.lastro.operacoes.application.usecase.IVisaoGeralUseCase;
import io.github.jaimenafer.lastro.operacoes.domain.model.*;
import io.github.jaimenafer.lastro.operacoes.domain.repository.*;
import io.github.jaimenafer.lastro.operacoes.domain.service.*;
import java.time.LocalDate;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;
import java.time.temporal.ChronoUnit;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class VisaoGeralUseCaseImpl implements IVisaoGeralUseCase {
  private final IOperacaoRepository operacoes;
  private final IPontoPartidaRepository pontos;
  private final IConsultaAtivos ativos;
  private final IConsultaInstituicoes instituicoes;
  private final IUsuarioAtual usuario;
  private final IHistoricoCotacaoRepository cotacoes;
  private final io.github.jaimenafer.lastro.operacoes.application.port.IConsultaLivroConferencia
      conferencia;

  @Override
  @Transactional(readOnly = true, isolation = Isolation.REPEATABLE_READ)
  public VisaoGeralResponse consultar(FiltroVisaoGeral filtro) {
    var ponto = pontos.findById(usuario.id());
    LocalDate referencia = ponto.map(PontoPartida::getDataReferencia).orElse(null);
    PeriodoVisaoGeral periodo =
        PeriodoVisaoGeral.definir(
            VisaoGeralMapper.data(filtro.dataInicio()),
            VisaoGeralMapper.data(filtro.dataFim()),
            referencia,
            LocalDate.now(ZoneId.of("America/Fortaleza")));
    List<Operacao> historico =
        operacoes.historico(usuario.id()).stream()
            .filter(Operacao::confirmada)
            .filter(o -> periodo.ateFim(o.getOcorridaEm()))
            .toList();
    var retrato =
        conferencia.carregar(
            periodo
                .fim()
                .plusDays(1)
                .atStartOfDay(ZoneId.of("America/Fortaleza"))
                .toInstant()
                .minusNanos(1));
    return VisaoGeralConferenciaMapper.de(
        ponto.map(PontoPartida::getRevisao).orElse(0L),
        referencia,
        periodo,
        retrato,
        ConferenciaRegistros.selecionar(historico, TipoAvisoConferencia.CUSTO_ZERO_DECLARADO)
            .size(),
        ConferenciaRegistros.selecionar(historico, TipoAvisoConferencia.REFERENCIA_REPETIDA)
            .size());
  }

  @Override
  @Transactional(readOnly = true, isolation = Isolation.REPEATABLE_READ)
  public List<PatrimonioDiarioResponse> patrimonioDiario(FiltroVisaoGeral filtro) {
    var ponto = pontos.findById(usuario.id());
    PeriodoVisaoGeral periodo = PeriodoVisaoGeral.definir(VisaoGeralMapper.data(filtro.dataInicio()),
        VisaoGeralMapper.data(filtro.dataFim()), ponto.map(PontoPartida::getDataReferencia).orElse(null),
        LocalDate.now(ZoneId.of("America/Fortaleza")));
    ZoneId fuso = ZoneId.of("America/Fortaleza");
    Instant limite = periodo.fim().plusDays(1).atStartOfDay(fuso).toInstant().minusNanos(1);
    RetratoConferencia retrato = conferencia.carregar(limite);
    Long brl = ativos.buscarPorCodigo("fiat:BRL").orElseThrow().id();
    List<CotacaoHistorica> precos = cotacoes.buscar(
        periodo.inicio().atStartOfDay(fuso).toInstant().minus(Duration.ofHours(24)),
        limite.plusNanos(1), "DIA");
    Map<Long, String> codigos = retrato.ativos().values().stream()
        .collect(Collectors.toMap(AtivoResumo::id, AtivoResumo::codigo));
    List<PatrimonioDiarioResponse> resultado = new ArrayList<>();
    Instant agora = Instant.now();
    for (LocalDate dia = periodo.inicio(); !dia.isAfter(periodo.fim()); dia = dia.plusDays(1)) {
      Instant fimDia = dia.plusDays(1).atStartOfDay(fuso).toInstant().minusNanos(1);
      Instant fimCotacao = fimDia;
      if (fimCotacao.isAfter(agora)) fimCotacao = agora;
      fimCotacao = fimCotacao.truncatedTo(ChronoUnit.DAYS);
      List<ItemConferenciaLivro> anteriores = retrato.itens().stream()
          .filter(i -> i.ocorridaEm() != null && !i.ocorridaEm().isAfter(fimDia)).toList();
      Set<String> ancoras = anteriores.stream().map(ItemConferenciaLivro::id).collect(Collectors.toSet());
      List<ItemConferenciaLivro> itens = new ArrayList<>(anteriores);
      retrato.itens().stream().filter(i -> i.ajuste() && ancoras.contains(i.ancoraId())).forEach(itens::add);
      ResultadoConferenciaLivro livro = MotorLivro.conferir(itens, codigos);
      BigDecimal conhecido = BigDecimal.ZERO;
      int comPreco = 0, semPreco = 0;
      boolean quantidadesCompletas = !retrato.incertezaSemData()
          && (retrato.primeiraDataPendente() == null || dia.isBefore(retrato.primeiraDataPendente()))
          && livro.posicoes().stream().allMatch(PosicaoConferenciaLivro::quantidadeDisponivel);
      for (PosicaoConferenciaLivro posicao : livro.posicoes()) {
        if (posicao.quantidade() == null || posicao.quantidade().signum() == 0) continue;
        var preco = ResolvedorCotacao.resolver(posicao.ativoId(), brl, fimCotacao, precos);
        if (preco.isEmpty()) semPreco++;
        else {
          conhecido = conhecido.add(posicao.quantidade().multiply(preco.get().preco()));
          comPreco++;
        }
      }
      String subtotal = conhecido.setScale(8, RoundingMode.HALF_EVEN).toPlainString();
      resultado.add(new PatrimonioDiarioResponse(dia,
          semPreco == 0 && quantidadesCompletas ? subtotal : null,
          subtotal, comPreco, semPreco, quantidadesCompletas));
    }
    return List.copyOf(resultado);
  }

  @Override
  @Transactional(readOnly = true, isolation = Isolation.REPEATABLE_READ)
  public AvisosConferenciaResponse avisos(FiltroAvisosConferencia filtro) {
    var ponto = pontos.findById(usuario.id());
    LocalDate fim = VisaoGeralMapper.data(filtro.dataFim());
    PeriodoVisaoGeral periodo =
        PeriodoVisaoGeral.definir(fim, fim, null, LocalDate.now(ZoneId.of("America/Fortaleza")));
    List<Operacao> historico =
        operacoes.historico(usuario.id()).stream()
            .filter(Operacao::confirmada)
            .filter(o -> periodo.ateFim(o.getOcorridaEm()))
            .toList();
    List<Operacao> selecionadas = ConferenciaRegistros.selecionar(historico, filtro.tipo());
    Map<Long, AtivoResumo> ativosPorId =
        ativos
            .buscarPorIds(
                selecionadas.stream().map(Operacao::getAtivoId).collect(Collectors.toSet()))
            .stream()
            .collect(Collectors.toMap(AtivoResumo::id, Function.identity()));
    Map<Long, InstituicaoResumo> instituicoesPorId =
        instituicoes
            .buscarPorIds(
                selecionadas.stream().map(Operacao::getInstituicaoId).collect(Collectors.toSet()))
            .stream()
            .collect(Collectors.toMap(InstituicaoResumo::id, Function.identity()));
    return VisaoGeralMapper.avisos(
        ponto.map(PontoPartida::getRevisao).orElse(0L),
        selecionadas,
        filtro,
        ativosPorId,
        instituicoesPorId);
  }
}
