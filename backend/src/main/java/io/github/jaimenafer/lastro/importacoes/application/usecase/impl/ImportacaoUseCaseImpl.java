package io.github.jaimenafer.lastro.importacoes.application.usecase.impl;

import io.github.jaimenafer.lastro.acesso.application.contract.IUsuarioAtual;
import io.github.jaimenafer.lastro.ativos.application.contract.IConsultaAtivos;
import io.github.jaimenafer.lastro.importacoes.application.dto.*;
import io.github.jaimenafer.lastro.importacoes.application.mapper.IdentidadeImportacao;
import io.github.jaimenafer.lastro.importacoes.application.mapper.ImportacaoMapper;
import io.github.jaimenafer.lastro.importacoes.application.port.ILeitorRelatorio;
import io.github.jaimenafer.lastro.importacoes.application.usecase.IImportacaoUseCase;
import io.github.jaimenafer.lastro.importacoes.domain.exception.*;
import io.github.jaimenafer.lastro.importacoes.domain.model.*;
import io.github.jaimenafer.lastro.importacoes.domain.repository.IImportacaoRepository;
import io.github.jaimenafer.lastro.instituicoes.application.contract.IConsultaInstituicoes;
import io.github.jaimenafer.lastro.operacoes.application.contract.*;
import java.nio.charset.StandardCharsets;
import java.util.*;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class ImportacaoUseCaseImpl implements IImportacaoUseCase {
  private final IUsuarioAtual usuario;
  private final ILeitorRelatorio leitor;
  private final IImportacaoRepository repositorio;
  private final IImportacaoOperacoes livro;
  private final IConsultaAtivos ativos;
  private final IConsultaInstituicoes instituicoes;

  @Value("${lastro.importacoes.max-bytes:10485760}")
  private int maxBytes;

  @Value("${lastro.importacoes.max-eventos:5000}")
  private int maxEventos;

  @Override
  @Transactional(readOnly = true, isolation = Isolation.REPEATABLE_READ)
  public PreviaImportacaoResponse prever(byte[] conteudo, DecisoesImportacao decisoes) {
    ArquivoInterpretado arquivo = interpretar(conteudo);
    List<ErroImportacaoResponse> erros = diagnosticos(arquivo, decisoes);
    if (!erros.isEmpty())
      return new PreviaImportacaoResponse(arquivo.hash(), "0", "", 0, 0, erros, List.of());
    PreviaLoteOperacoes previa =
        livro.prever(movimentos(arquivo), decisoes.posicaoAnteriorZerada());
    int reconhecidos =
        (int) previa.eventos().stream().filter(ReconhecimentoOperacao::reconhecida).count();
    return new PreviaImportacaoResponse(
        arquivo.hash(),
        previa.revisao(),
        token(arquivo.hash(), decisoes, previa.revisao()),
        arquivo.eventos().size() - reconhecidos,
        reconhecidos,
        List.of(),
        ImportacaoMapper.eventos(arquivo.eventos(), previa.eventos()));
  }

  @Override
  @Transactional(isolation = Isolation.READ_COMMITTED)
  public ReciboImportacaoResponse confirmar(
      byte[] conteudo,
      String nome,
      DecisoesImportacao decisoes,
      String hash,
      String revisao,
      String previaId,
      String chave) {
    validarChave(chave);
    ArquivoInterpretado arquivo = interpretar(conteudo);
    validarHash(arquivo.hash(), hash);
    Optional<Importacao> repetida = repositorio.buscarPorChave(usuario.id(), chave);
    if (repetida.isPresent()) return repetir(repetida.get(), hash, decisoes);
    Optional<Importacao> mesmoArquivo = repositorio.buscarPorHash(usuario.id(), hash);
    if (mesmoArquivo.isPresent())
      throw new ImportacaoConflitanteException(
          "Este arquivo já foi confirmado. Consulte o recibo "
              + mesmoArquivo.get().getId()
              + " no histórico de importações.");
    validarDiagnosticos(arquivo, decisoes);
    validarPrevia(hash, decisoes, revisao, previaId);
    List<MovimentoImportado> movimentos = movimentos(arquivo);
    livro.bloquearHistorico(movimentos, decisoes.posicaoAnteriorZerada());
    repetida = repositorio.buscarPorChave(usuario.id(), chave);
    if (repetida.isPresent()) return repetir(repetida.get(), hash, decisoes);
    if (repositorio.buscarPorHash(usuario.id(), hash).isPresent())
      throw new ImportacaoConflitanteException(
          "Este arquivo já foi confirmado. Consulte seu recibo no histórico de importações.");
    ResultadoLoteOperacoes resultado =
        livro.confirmar(movimentos, decisoes.posicaoAnteriorZerada(), revisar(revisao));
    int reconhecidos =
        (int) resultado.eventos().stream().filter(ReconhecimentoOperacao::reconhecida).count();
    Importacao importacao =
        Importacao.registrar(
            usuario.id(),
            chave,
            hash,
            decisoes(decisoes),
            nomeSeguro(nome),
            conteudo,
            arquivo.eventos().size() - reconhecidos,
            reconhecidos);
    Map<String, ReconhecimentoOperacao> porChave = new HashMap<>();
    resultado.eventos().forEach(evento -> porChave.put(evento.chave(), evento));
    for (EventoImportacao evento : arquivo.eventos()) {
      ReconhecimentoOperacao reconhecido =
          porChave.get(IdentidadeImportacao.evento(evento.referencia()));
      importacao.vincular(evento.linha(), evento.referencia(), reconhecido.operacaoId());
    }
    return ImportacaoMapper.recibo(repositorio.salvar(importacao));
  }

  @Override
  @Transactional(readOnly = true, isolation = Isolation.REPEATABLE_READ)
  public PaginaImportacoesResponse listar(int pagina, int tamanho) {
    return new PaginaImportacoesResponse(
        repositorio.listar(usuario.id(), pagina, tamanho).stream()
            .map(ImportacaoMapper::resumo)
            .toList(),
        repositorio.contar(usuario.id()),
        pagina,
        tamanho);
  }

  @Override
  @Transactional(readOnly = true)
  public ReciboImportacaoResponse consultar(long id) {
    return ImportacaoMapper.recibo(repositorio.buscarObrigatoria(usuario.id(), id));
  }

  private ArquivoInterpretado interpretar(byte[] conteudo) {
    if (conteudo.length == 0 || conteudo.length > maxBytes)
      throw new ImportacaoInvalidaException("Selecione um arquivo CSV de até 10 MiB.");
    ArquivoInterpretado arquivo = leitor.ler(conteudo);
    if (arquivo.eventos().size() > maxEventos)
      throw new ImportacaoInvalidaException("O arquivo excede o limite de 5.000 eventos.");
    return arquivo;
  }

  private List<ErroImportacaoResponse> diagnosticos(
      ArquivoInterpretado arquivo, DecisoesImportacao decisoes) {
    List<ErroImportacaoResponse> erros =
        new ArrayList<>(
            arquivo.erros().stream()
                .map(erro -> new ErroImportacaoResponse(erro.linha(), erro.mensagem()))
                .toList());
    if (!decisoes.aportesProprios())
      erros.add(
          new ErroImportacaoResponse(
              0, "Confirme que os depósitos são aportes de recursos próprios."));
    if (!decisoes.taxasIncluidas())
      erros.add(
          new ErroImportacaoResponse(
              0, "Confirme que o valor total inclui as taxas do relatório."));
    if (arquivo.eventos().isEmpty() && erros.isEmpty())
      erros.add(new ErroImportacaoResponse(0, "O relatório não contém eventos para importar."));
    return List.copyOf(erros);
  }

  private void validarDiagnosticos(ArquivoInterpretado arquivo, DecisoesImportacao decisoes) {
    List<ErroImportacaoResponse> erros = diagnosticos(arquivo, decisoes);
    if (!erros.isEmpty()) throw new ImportacaoInvalidaException(erros.getFirst().mensagem());
  }

  private List<MovimentoImportado> movimentos(ArquivoInterpretado arquivo) {
    long instituicaoId =
        instituicoes
            .buscarPorCodigo("binance")
            .orElseThrow(() ->
                new ImportacaoInvalidaException(
                    "A instituição Binance não está disponível no catálogo."))
            .id();
    Map<String, Long> origens =
        repositorio.origensComprovadas(
            usuario.id(), arquivo.eventos().stream().map(EventoImportacao::referencia).toList());
    Map<String, Long> porCodigo = new HashMap<>();
    for (String ticker :
        arquivo.eventos().stream().map(EventoImportacao::codigoAtivo).distinct().toList()) {
      String codigo = "BRL".equals(ticker) ? "fiat:BRL" : "cripto:" + ticker;
      long ativoId =
          ativos
              .buscarPorCodigo(codigo)
              .orElseThrow(() ->
                  new ImportacaoInvalidaException(
                      "Ativo do relatório não encontrado no catálogo: " + ticker + "."))
              .id();
      porCodigo.put(ticker, ativoId);
    }
    return arquivo.eventos().stream()
        .map(evento ->
            new MovimentoImportado(
                IdentidadeImportacao.evento(evento.referencia()),
                IdentidadeImportacao.legada(arquivo.hash(), evento.referencia()),
                evento.tipo(),
                instituicaoId,
                porCodigo.get(evento.codigoAtivo()),
                evento.ocorridaEm(),
                evento.quantidade(),
                evento.principal(),
                evento.taxa(),
                evento.referencia(),
                origens.get(evento.referencia())))
        .toList();
  }

  private ReciboImportacaoResponse repetir(
      Importacao importacao, String hash, DecisoesImportacao decisoes) {
    if (!importacao.getHash().equals(hash) || !importacao.getDecisoes().equals(decisoes(decisoes)))
      throw new ImportacaoConflitanteException(
          "A chave ou o arquivo já foi confirmado com dados diferentes. Confira o recibo"
              + " existente.");
    return ImportacaoMapper.recibo(importacao);
  }

  private void validarHash(String recebido, String esperado) {
    if (!recebido.equals(esperado))
      throw new ImportacaoConflitanteException(
          "O arquivo mudou após a prévia. Gere uma nova prévia.");
  }

  private void validarPrevia(
      String hash, DecisoesImportacao decisoes, String revisao, String previaId) {
    if (!token(hash, decisoes, revisao).equals(previaId))
      throw new ImportacaoConflitanteException(
          "A prévia não corresponde ao arquivo e às decisões. Gere uma nova prévia.");
  }

  private String token(String hash, DecisoesImportacao decisoes, String revisao) {
    return IdentidadeImportacao.resumo(
        ("binance-brl-v1|" + usuario.id() + "|" + hash + "|" + decisoes(decisoes) + "|" + revisao)
            .getBytes(StandardCharsets.UTF_8));
  }

  private String decisoes(DecisoesImportacao decisoes) {
    return decisoes.aportesProprios()
        + ":"
        + decisoes.taxasIncluidas()
        + ":"
        + decisoes.posicaoAnteriorZerada();
  }

  private long revisar(String revisao) {
    try {
      return Long.parseLong(revisao);
    } catch (NumberFormatException erro) {
      throw new ImportacaoInvalidaException("A revisão da prévia é inválida.");
    }
  }

  private void validarChave(String chave) {
    try {
      if (!UUID.fromString(chave).toString().equals(chave)) throw new IllegalArgumentException();
    } catch (IllegalArgumentException erro) {
      throw new ImportacaoInvalidaException("Informe uma chave de confirmação UUID válida.");
    }
  }

  private String nomeSeguro(String nome) {
    String limpo = nome == null ? "relatorio.csv" : nome.replace('\\', '/');
    limpo = limpo.substring(limpo.lastIndexOf('/') + 1).replaceAll("[\\p{Cntrl}]", "").strip();
    if (limpo.isBlank()) limpo = "relatorio.csv";
    return limpo.substring(0, Math.min(limpo.length(), 255));
  }
}
