package io.github.jaimenafer.lastro.operacoes.application.usecase.impl;

import static io.github.jaimenafer.lastro.operacoes.application.mapper.ConferenciaMapper.*;

import io.github.jaimenafer.lastro.acesso.application.contract.IUsuarioAtual;
import io.github.jaimenafer.lastro.ativos.application.contract.*;
import io.github.jaimenafer.lastro.operacoes.application.contract.*;
import io.github.jaimenafer.lastro.operacoes.application.dto.*;
import io.github.jaimenafer.lastro.operacoes.application.mapper.ConferenciaMapper;
import io.github.jaimenafer.lastro.operacoes.application.port.IAssociacoesLivro;
import io.github.jaimenafer.lastro.operacoes.application.port.IConsultaConferencia;
import io.github.jaimenafer.lastro.operacoes.application.port.IPreparacaoConciliacao;
import io.github.jaimenafer.lastro.operacoes.domain.exception.*;
import io.github.jaimenafer.lastro.operacoes.domain.model.*;
import io.github.jaimenafer.lastro.operacoes.domain.repository.*;
import io.github.jaimenafer.lastro.operacoes.domain.service.MotorLivro;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.*;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.*;

@Service
@RequiredArgsConstructor
public class PreparacaoConciliacao implements IPreparacaoConciliacao {
  private final IUsuarioAtual usuario;
  private final IRegistroConferenciaRepository registros;
  private final IAjusteConciliatorioRepository ajustes;
  private final IConsultaConferencia consulta;
  private final IConsultaAtivos ativos;
  private final IAssociacoesLivro associacoes;

  public PreviaConferenciaResponse preparar(String revisao) {
    List<ItemConferenciaLivro> itens = consulta.projetar(true, false, null);
    List<CorrespondenciaConferenciaResponse> correspondencias = correspondencias();
    itens = associarItens(itens, correspondencias);
    Set<Long> ids = itens.stream().map(ItemConferenciaLivro::ativoId).collect(Collectors.toSet());
    for (ItemConferenciaLivro i : itens)
      if (i.operacao() != null) {
        if (i.operacao().moedaCotacaoId() != null) ids.add(i.operacao().moedaCotacaoId());
        if (i.operacao().taxaAtivoId() != null) ids.add(i.operacao().taxaAtivoId());
      }
    Map<Long, AtivoResumo> catalogo =
        ativos.buscarPorIds(ids).stream()
            .collect(Collectors.toMap(AtivoResumo::id, Function.identity()));
    List<LacunaLivro> lacunas =
        MotorLivro.conferir(
                itens,
                catalogo.values().stream()
                    .collect(Collectors.toMap(AtivoResumo::id, AtivoResumo::codigo)))
            .lacunas();
    Map<String, LacunaLivro> necessarias =
        lacunas.stream()
            .collect(
                Collectors.toMap(
                    LacunaLivro::id,
                    Function.identity(),
                    (a, b) ->
                        new LacunaLivro(
                            a.id(),
                            a.ancoraId(),
                            a.contaId(),
                            a.instituicaoId(),
                            a.ativoId(),
                            a.quantidade().add(b.quantidade())),
                    LinkedHashMap::new));
    Map<String, AjusteConciliatorio> aceitos =
        ajustes.listarAtivos(usuario.id()).stream()
            .collect(Collectors.toMap(AjusteConciliatorio::getLacunaRef, Function.identity()));
    Map<String, ContaConferenciaResponse> contas =
        consulta.contas().stream()
            .collect(Collectors.toMap(ContaConferenciaResponse::id, Function.identity()));
    List<AjusteConferenciaResponse> propostas = new ArrayList<>();
    for (LacunaLivro lacuna : necessarias.values()) {
      AjusteConciliatorio anterior = aceitos.remove(lacuna.id());
      BigDecimal antes = anterior == null ? null : anterior.getQuantidade();
      String acao =
          antes == null
              ? "CRIAR"
              : lacuna.quantidade().compareTo(antes) == 0
                  ? "MANTER"
                  : lacuna.quantidade().compareTo(antes) < 0 ? "REDUZIR" : "ALTERAR";
      ContaConferenciaResponse c = contas.get(lacuna.contaId().toString());
      AtivoResumo ativo = catalogo.get(lacuna.ativoId());
      propostas.add(
          new AjusteConferenciaResponse(
              lacuna.id(),
              lacuna.ancoraId(),
              c.id(),
              c.nome(),
              c.instituicaoNome(),
              ativo.id().toString(),
              ativo.ticker(),
              decimal(lacuna.quantidade()),
              ativo.codigo().equals("fiat:BRL")
                  ? decimal(lacuna.quantidade().setScale(8, RoundingMode.HALF_EVEN))
                  : null,
              acao,
              decimal(antes)));
    }
    for (AjusteConciliatorio anterior : aceitos.values()) {
      ContaConferenciaResponse c = contas.get(anterior.getContaCustodiaId().toString());
      String ticker = ativos.buscar(anterior.getAtivoId()).orElseThrow().ticker();
      propostas.add(
          new AjusteConferenciaResponse(
              anterior.getLacunaRef(),
              anterior.getAncoraRef(),
              c.id(),
              c.nome(),
              c.instituicaoNome(),
              anterior.getAtivoId().toString(),
              ticker,
              "0",
              null,
              "ENCERRAR",
              decimal(anterior.getQuantidade())));
    }
    propostas.sort(Comparator.comparing(AjusteConferenciaResponse::lacunaId));
    List<GrupoEmConferencia> grupos = associacoes.grupos(true, consulta.impedimentosOrigem());
    List<GrupoAssociacaoResponse> gruposResponse =
        associacoes.respostas(
            grupos.stream().filter(GrupoEmConferencia::pendente).toList(), revisao);
    int novos = consulta.propostasParaAplicar().size();
    String token =
        hash(
            usuario.id()
                + "|"
                + revisao
                + "|"
                + ConferenciaMapper.escrever(propostas)
                + "|"
                + ConferenciaMapper.escrever(correspondencias)
                + "|"
                + ConferenciaMapper.escrever(gruposResponse));
    return new PreviaConferenciaResponse(
        revisao,
        token,
        novos,
        List.copyOf(propostas),
        correspondencias,
        consulta.resumir(consulta.registros()),
        gruposResponse);
  }

  private List<CorrespondenciaConferenciaResponse> correspondencias() {
    Set<Long> impedidos = consulta.impedimentos().keySet();
    Map<String, List<RegistroConferencia>> grupos = new TreeMap<>();
    for (RegistroConferencia r : registros.listar(usuario.id())) {
      PropostaRegistro p = ConferenciaMapper.proposta(r, true);
      String op = ConferenciaMapper.origem(r).operacao().toLowerCase(Locale.ROOT);
      if (impedidos.contains(r.getId())
          || !legivel(p)
          || p.transferenciaRef() != null
          || p.natureza() != NaturezaRegistro.OBSERVADA
          || !op.contains("transfer")
          || !op.contains("spot")
          || !op.contains("funding")) continue;
      String chave =
          r.getInstituicaoId()
              + "|"
              + p.ativoId()
              + "|"
              + decimal(numero(p.quantidade()).abs())
              + "|"
              + op;
      grupos.computeIfAbsent(chave, k -> new ArrayList<>()).add(r);
    }
    Map<String, ContaConferenciaResponse> contas =
        consulta.contas().stream()
            .collect(Collectors.toMap(ContaConferenciaResponse::id, Function.identity()));
    List<CorrespondenciaConferenciaResponse> pares = new ArrayList<>();
    for (List<RegistroConferencia> grupo : grupos.values()) {
      if (grupo.size() != 2) continue;
      grupo.sort(Comparator.comparing(RegistroConferencia::getId));
      RegistroConferencia a = grupo.getFirst(), b = grupo.getLast();
      PropostaRegistro pa = ConferenciaMapper.proposta(a, true),
          pb = ConferenciaMapper.proposta(b, true);
      if (pa.contaId().equals(pb.contaId())
          || numero(pa.quantidade()).add(numero(pb.quantidade())).signum() != 0) continue;
      boolean saida = numero(pa.quantidade()).signum() < 0;
      pares.add(
          new CorrespondenciaConferenciaResponse(
              "transferencia:" + a.getId() + ":" + b.getId(),
              List.of("registro:" + a.getId(), "registro:" + b.getId()),
              "Transferência interna indicada no arquivo",
              contas.get(saida ? pa.contaId() : pb.contaId()).nome(),
              contas.get(saida ? pb.contaId() : pa.contaId()).nome(),
              decimal(numero(pa.quantidade()).abs()),
              ConferenciaMapper.origem(a).moeda()));
    }
    return List.copyOf(pares);
  }

  private List<ItemConferenciaLivro> associarItens(
      List<ItemConferenciaLivro> itens, List<CorrespondenciaConferenciaResponse> pares) {
    Map<String, String> refs = new HashMap<>();
    pares.forEach(p -> p.registroIds().forEach(id -> refs.put(id, p.id())));
    return itens.stream()
        .map(i ->
            refs.containsKey(i.id())
                ? new ItemConferenciaLivro(
                    i.id(),
                    i.contaId(),
                    i.instituicaoId(),
                    i.ativoId(),
                    i.ocorridaEm(),
                    i.ordem(),
                    i.quantidade(),
                    i.custoEntradaBrl(),
                    i.operacao(),
                    i.ajuste(),
                    i.ancoraId(),
                    refs.get(i.id()))
                : i)
        .toList();
  }
}
