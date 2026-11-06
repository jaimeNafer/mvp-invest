package io.github.jaimenafer.lastro.importacoes.application.mapper;

import io.github.jaimenafer.lastro.importacoes.application.dto.*;
import io.github.jaimenafer.lastro.importacoes.domain.model.*;
import io.github.jaimenafer.lastro.operacoes.application.contract.*;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class ImportacaoMapper {
  public static List<EventoPreviaImportacao> eventos(
      List<EventoImportacao> eventos, List<ReconhecimentoOperacao> resultados) {
    Map<String, ReconhecimentoOperacao> porChave =
        resultados.stream()
            .collect(Collectors.toMap(ReconhecimentoOperacao::chave, Function.identity()));
    return eventos.stream()
        .map(evento -> {
          ReconhecimentoOperacao resultado =
              porChave.get(IdentidadeImportacao.evento(evento.referencia()));
          return new EventoPreviaImportacao(
              evento.linha(),
              evento.referencia(),
              evento.tipo(),
              evento.ocorridaEm(),
              evento.codigoAtivo(),
              evento.quantidade().toPlainString(),
              evento.principal() == null ? null : evento.principal().toPlainString(),
              evento.taxa().toPlainString(),
              resultado != null && resultado.reconhecida() ? "RECONHECIDO" : "NOVO");
        })
        .toList();
  }

  public static ResumoImportacaoResponse resumo(ResumoImportacao importacao) {
    return new ResumoImportacaoResponse(
        importacao.id().toString(),
        importacao.hash(),
        importacao.nomeArquivo(),
        importacao.criadoEm(),
        importacao.novos(),
        importacao.reconhecidos());
  }

  public static ReciboImportacaoResponse recibo(Importacao importacao) {
    return new ReciboImportacaoResponse(
        importacao.getId().toString(),
        importacao.getHash(),
        importacao.getNomeArquivo(),
        importacao.getCriadoEm(),
        importacao.getNovos(),
        importacao.getReconhecidos(),
        importacao.eventosRegistrados().stream()
            .map(evento ->
                new EventoReciboResponse(
                    evento.getLinha(), evento.getReferencia(), evento.getOperacaoId().toString()))
            .toList());
  }
}
