package io.github.jaimenafer.lastro.operacoes.application.usecase.impl;

import io.github.jaimenafer.lastro.acesso.application.contract.IUsuarioAtual;
import io.github.jaimenafer.lastro.ativos.application.contract.*;
import io.github.jaimenafer.lastro.instituicoes.application.contract.*;
import io.github.jaimenafer.lastro.operacoes.application.dto.*;
import io.github.jaimenafer.lastro.operacoes.application.mapper.*;
import io.github.jaimenafer.lastro.operacoes.application.usecase.IVisaoGeralUseCase;
import io.github.jaimenafer.lastro.operacoes.domain.model.*;
import io.github.jaimenafer.lastro.operacoes.domain.repository.*;
import io.github.jaimenafer.lastro.operacoes.domain.service.ConsolidadorVisaoGeral;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.List;
import java.util.Map;
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
    Map<Long, AtivoResumo> ativosPorId =
        ativos.buscarPorIds(LivroMapper.ativosDoHistorico(historico)).stream()
            .collect(Collectors.toMap(AtivoResumo::id, Function.identity()));
    var instituicaoIds =
        historico.stream().map(Operacao::getInstituicaoId).collect(Collectors.toSet());
    Map<Long, InstituicaoResumo> instituicoesPorId =
        instituicoes.buscarPorIds(instituicaoIds).stream()
            .collect(Collectors.toMap(InstituicaoResumo::id, Function.identity()));
    ResultadoLivro livro = LivroMapper.calcular(historico, List.copyOf(ativosPorId.values()));
    ResumoVisaoGeral resumo =
        ConsolidadorVisaoGeral.resumir(
            historico.stream().map(LivroMapper::fato).toList(), livro, periodo);
    return VisaoGeralMapper.de(
        ponto.map(PontoPartida::getRevisao).orElse(0L),
        referencia,
        periodo,
        !historico.isEmpty(),
        resumo,
        livro,
        ativosPorId,
        instituicoesPorId);
  }
}
