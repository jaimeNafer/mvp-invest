package io.github.jaimenafer.lastro.operacoes.application.usecase.impl;

import static io.github.jaimenafer.lastro.operacoes.application.mapper.ConferenciaMapper.*;

import io.github.jaimenafer.lastro.acesso.application.contract.IUsuarioAtual;
import io.github.jaimenafer.lastro.operacoes.application.config.IConfiguracaoAssociacao;
import io.github.jaimenafer.lastro.operacoes.application.dto.RevisaoConferenciaResponse;
import io.github.jaimenafer.lastro.operacoes.application.dto.ValoracaoManualRequest;
import io.github.jaimenafer.lastro.operacoes.application.dto.PropostaRegistro;
import io.github.jaimenafer.lastro.operacoes.application.port.IAssociacoesLivro;
import io.github.jaimenafer.lastro.operacoes.application.usecase.IValoracaoManualUseCase;
import io.github.jaimenafer.lastro.operacoes.domain.exception.OperacaoConflitanteException;
import io.github.jaimenafer.lastro.operacoes.domain.exception.OperacaoInvalidaException;
import io.github.jaimenafer.lastro.operacoes.domain.model.ComandoConciliacao;
import io.github.jaimenafer.lastro.operacoes.domain.model.GrupoAssociacao;
import io.github.jaimenafer.lastro.operacoes.domain.model.PontoPartida;
import io.github.jaimenafer.lastro.operacoes.domain.model.PropostaGrupo;
import io.github.jaimenafer.lastro.operacoes.domain.model.RegistroConferencia;
import io.github.jaimenafer.lastro.operacoes.domain.repository.IComandoConciliacaoRepository;
import io.github.jaimenafer.lastro.operacoes.domain.repository.IGrupoAssociacaoRepository;
import io.github.jaimenafer.lastro.operacoes.domain.repository.IPontoPartidaRepository;
import io.github.jaimenafer.lastro.operacoes.domain.repository.IRegistroConferenciaRepository;
import io.github.jaimenafer.lastro.operacoes.domain.repository.IValoracaoManualRepository;
import io.github.jaimenafer.lastro.operacoes.domain.service.IdentidadeValoracao;
import io.github.jaimenafer.lastro.operacoes.domain.service.AgrupamentoNegociacoes;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.Objects;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class ValoracaoManualUseCaseImpl implements IValoracaoManualUseCase {
  private final IUsuarioAtual usuario;
  private final IPontoPartidaRepository pontos;
  private final IRegistroConferenciaRepository registros;
  private final IGrupoAssociacaoRepository grupos;
  private final IValoracaoManualRepository valoracoes;
  private final IComandoConciliacaoRepository comandos;
  private final IConfiguracaoAssociacao configuracao;
  private final IAssociacoesLivro livro;

  @Override
  @Transactional(isolation = Isolation.READ_COMMITTED)
  public RevisaoConferenciaResponse registrarGrupo(String numero, ValoracaoManualRequest request, String chave) {
    if (numero == null || !numero.startsWith("OP-"))
      throw new OperacaoInvalidaException("Informe uma operação agrupada válida.");
    Long grupoId = id(numero.substring(3));
    return registrar(null, grupoId, request, chave);
  }

  @Override
  @Transactional(isolation = Isolation.READ_COMMITTED)
  public RevisaoConferenciaResponse registrarLinha(String id, ValoracaoManualRequest request, String chave) {
    return registrar(registroId(id), null, request, chave);
  }

  private RevisaoConferenciaResponse registrar(Long registroId, Long grupoId,
      ValoracaoManualRequest request, String chave) {
    UUID comando = chave(chave);
    PontoPartida ponto = pontos.bloquear(usuario.id())
        .orElseThrow(() -> new OperacaoInvalidaException("Receba os registros antes de informar o valor."));
    String fingerprint = hash("valoracao|" + registroId + "|" + grupoId + "|" + escrever(request));
    var anterior = comandos.buscar(usuario.id(), comando);
    if (anterior.isPresent()) {
      if (!anterior.get().getFingerprint().equals(fingerprint))
        throw new OperacaoConflitanteException("A chave de idempotência já foi usada com outro conteúdo.");
      return ler(anterior.get().getRespostaJson(), RevisaoConferenciaResponse.class);
    }
    if (!Long.toString(ponto.getRevisao()).equals(request.revisao()))
      throw new OperacaoConflitanteException("O histórico mudou. Atualize antes de informar o valor.");
    BigDecimal valor = numero(textoDecimal(request.valorTotalBrl(), "valor em reais"));
    if (valor == null || valor.signum() <= 0 || Math.max(0, valor.stripTrailingZeros().scale()) > 8)
      throw new OperacaoInvalidaException("Informe valor positivo em reais com até 8 casas decimais.");
    String identidade = validarReferencia(registroId, grupoId, request.versao(), valor);
    boolean mudou = identidade != null && valoracoes.listar(usuario.id()).stream()
        .filter(v -> Objects.equals(v.registroId(), registroId) && Objects.equals(v.grupoId(), grupoId))
        .noneMatch(v -> v.valorBrl().compareTo(valor) == 0 && Objects.equals(v.identidade(), identidade));
    Instant agora = Instant.now();
    if (mudou) {
      valoracoes.salvar(usuario.id(), registroId, grupoId, valor, identidade, agora);
      ponto.avancarRevisao(usuario.id(), agora);
    }
    RevisaoConferenciaResponse resposta = new RevisaoConferenciaResponse(Long.toString(ponto.getRevisao()));
    comandos.salvar(ComandoConciliacao.registrar(usuario.id(), comando, fingerprint, escrever(resposta), agora));
    return resposta;
  }

  private String validarReferencia(Long registroId, Long grupoId, String versao, BigDecimal valor) {
    if (grupoId != null) {
      GrupoAssociacao grupo = grupos.buscar(usuario.id(), grupoId);
      if (!Long.toString(grupo.getVersao()).equals(versao))
        throw new OperacaoConflitanteException("A operação mudou. Atualize antes de informar o valor.");
      PropostaGrupo proposta = ler(grupo.getPropostaJson(), PropostaGrupo.class);
      if (proposta.desfazer())
        throw new OperacaoInvalidaException("A associação está marcada para desfazer.");
      BigDecimal efetivo = AgrupamentoNegociacoes.resumir(proposta.linhas(),
          proposta.taxaZeroConfirmada(), livro.cotacaoEfetiva(proposta.linhas()),
          configuracao.janelaAssociacao()).principalBrl();
      if (valorJaConfirmado(efetivo, valor)) return null;
      return IdentidadeValoracao.grupo(proposta);
    }
    RegistroConferencia registro = registros.buscar(usuario.id(), registroId);
    if (!Long.toString(registro.getVersao()).equals(versao))
      throw new OperacaoConflitanteException("O registro mudou. Atualize antes de informar o valor.");
    boolean agrupado = grupos.listar(usuario.id()).stream().anyMatch(g ->
        contemRegistro(g.getPropostaJson(), registroId) || contemRegistro(g.getAplicadaJson(), registroId));
    if (agrupado)
      throw new OperacaoInvalidaException("Informe o valor na operação agrupada.");
    PropostaRegistro proposta = proposta(registro, true);
    if (valorJaConfirmado(numero(proposta.custoTotalBrl()), valor)) return null;
    return IdentidadeValoracao.registro(registroId, registro.getInstituicaoId(), id(proposta.contaId()),
        id(proposta.ativoId()), instante(proposta.data()), numero(proposta.quantidade()));
  }

  private boolean valorJaConfirmado(BigDecimal efetivo, BigDecimal informado) {
    if (efetivo == null) return false;
    if (efetivo.compareTo(informado) != 0)
      throw new OperacaoInvalidaException("O valor informado diverge do valor efetivo em reais. Corrija os dados da operação.");
    return true;
  }

  private boolean contemRegistro(String json, Long registroId) {
    if (json == null) return false;
    PropostaGrupo grupo = ler(json, PropostaGrupo.class);
    return !grupo.desfazer() && grupo.linhas().stream().anyMatch(l -> l.registroId() == registroId);
  }

  private UUID chave(String valor) {
    try {
      return UUID.fromString(valor);
    } catch (IllegalArgumentException | NullPointerException erro) {
      throw new OperacaoInvalidaException("Informe chave de idempotência UUID válida.");
    }
  }
}
