package io.github.jaimenafer.lastro.importacoes.application.usecase.impl;

import io.github.jaimenafer.lastro.acesso.application.contract.IUsuarioAtual;
import io.github.jaimenafer.lastro.importacoes.application.dto.*;
import io.github.jaimenafer.lastro.importacoes.application.port.ILeitorMovimentacoes;
import io.github.jaimenafer.lastro.importacoes.application.usecase.IRecebimentoUseCase;
import io.github.jaimenafer.lastro.importacoes.domain.exception.ImportacaoInvalidaException;
import io.github.jaimenafer.lastro.importacoes.domain.model.ArquivoRecebido;
import io.github.jaimenafer.lastro.importacoes.domain.repository.IArquivoRecebidoRepository;
import io.github.jaimenafer.lastro.importacoes.domain.repository.IImportacaoRepository;
import io.github.jaimenafer.lastro.operacoes.application.contract.IRecebimentoConferencia;
import io.github.jaimenafer.lastro.operacoes.application.contract.LinhaConferenciaRecebida;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class RecebimentoUseCaseImpl implements IRecebimentoUseCase {
  private final IUsuarioAtual usuario;
  private final ILeitorMovimentacoes leitor;
  private final IArquivoRecebidoRepository recebidos;
  private final IImportacaoRepository importacoes;
  private final IRecebimentoConferencia conferencia;

  @Value("${lastro.importacoes.recebimento.max-arquivos:20}")
  private int maxArquivos;

  @Value("${lastro.importacoes.recebimento.max-bytes:10485760}")
  private long maxBytes;

  @Value("${lastro.importacoes.recebimento.max-registros:10000}")
  private int maxRegistros;

  @Override
  @Transactional(isolation = Isolation.READ_COMMITTED)
  public RecebimentoResponse receber(List<ArquivoParaRecebimento> arquivos) {
    validarLimites(arquivos);
    var documentos = interpretar(arquivos);
    if (documentos.stream().anyMatch(d -> d.erro() == null)) conferencia.bloquearHistorico();
    var respostas = new ArrayList<ArquivoRecebidoResponse>();
    for (var documento : documentos) respostas.add(receberDocumento(documento));
    int novos =
        respostas.stream()
            .filter(r -> r.erro() == null && !r.reconhecido())
            .mapToInt(ArquivoRecebidoResponse::registros)
            .sum();
    int reconhecidos =
        respostas.stream()
            .filter(ArquivoRecebidoResponse::reconhecido)
            .mapToInt(ArquivoRecebidoResponse::registros)
            .sum();
    int problemas = (int) respostas.stream().filter(r -> r.erro() != null).count();
    return new RecebimentoResponse(
        respostas, novos, reconhecidos, problemas, conferencia.revisao());
  }

  private void validarLimites(List<ArquivoParaRecebimento> arquivos) {
    if (arquivos.isEmpty() || arquivos.size() > maxArquivos)
      throw new ImportacaoInvalidaException("Selecione de 1 a " + maxArquivos + " arquivos.");
    long bytes = arquivos.stream().mapToLong(a -> a.conteudo().length).sum();
    if (bytes > maxBytes)
      throw new ImportacaoInvalidaException("Os arquivos excedem o limite total permitido.");
  }

  private List<DocumentoParaRecebimento> interpretar(List<ArquivoParaRecebimento> arquivos) {
    var documentos = new ArrayList<DocumentoParaRecebimento>();
    int total = 0;
    for (var arquivo : arquivos) {
      try {
        var documento = leitor.ler(arquivo.nomeArquivo(), arquivo.conteudo());
        total = Math.addExact(total, documento.registros().size());
        documentos.add(new DocumentoParaRecebimento(arquivo, documento, null));
      } catch (ImportacaoInvalidaException erro) {
        documentos.add(new DocumentoParaRecebimento(arquivo, null, erro.getMessage()));
      }
    }
    if (total > maxRegistros)
      throw new ImportacaoInvalidaException(
          "Os arquivos excedem o limite de " + maxRegistros + " registros.");
    return documentos;
  }

  private ArquivoRecebidoResponse receberDocumento(DocumentoParaRecebimento item) {
    if (item.erro() != null)
      return new ArquivoRecebidoResponse(
          null, item.arquivo().nomeArquivo(), null, 0, false, item.erro());
    var documento = item.documento();
    var existente = recebidos.buscarPorHash(usuario.id(), documento.hash());
    if (existente.isPresent()) {
      var arquivo = existente.get();
      return new ArquivoRecebidoResponse(
          arquivo.getId().toString(),
          documento.nomeArquivo(),
          documento.hash(),
          arquivo.getRegistros(),
          true,
          null);
    }
    var legado = importacoes.buscarPorHash(usuario.id(), documento.hash());
    if (legado.isPresent())
      return new ArquivoRecebidoResponse(
          "importacao:" + legado.get().getId(),
          documento.nomeArquivo(),
          documento.hash(),
          documento.registros().size(),
          true,
          null);
    var arquivo =
        recebidos.salvar(
            ArquivoRecebido.receber(
                usuario.id(), documento, item.arquivo().conteudo(), Instant.now()));
    var linhas =
        documento.registros().stream()
            .map(l ->
                new LinhaConferenciaRecebida(
                    l.linha(),
                    l.aba(),
                    l.conta(),
                    l.operacao(),
                    l.moeda(),
                    l.instanteOriginal(),
                    l.ocorridaEm(),
                    l.quantidade(),
                    l.quantidadeOriginal(),
                    l.camposOriginais(),
                    l.erros()))
            .toList();
    conferencia.registrar(arquivo.getId().toString(), documento.hash(), linhas);
    return new ArquivoRecebidoResponse(
        arquivo.getId().toString(),
        documento.nomeArquivo(),
        documento.hash(),
        documento.registros().size(),
        false,
        null);
  }
}
