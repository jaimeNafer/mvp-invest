package io.github.jaimenafer.lastro.operacoes.application.dto;

import io.github.jaimenafer.lastro.operacoes.domain.model.*;
import jakarta.validation.constraints.*;
import java.util.*;

public record RegistroTimelineResponse(
    String id,
    String data,
    String criadoEm,
    String tipo,
    String descricao,
    String instituicaoId,
    String instituicaoNome,
    String contaId,
    String contaNome,
    String ativoId,
    String ativoTicker,
    String quantidade,
    String custoTotalBrl,
    EstadoConferencia estado,
    String origem,
    String revisadoPor,
    String revisadoEm,
    String versao,
    List<PendenciaConferenciaResponse> pendencias,
    String ancoraId,
    Map<String, String> camposOriginais,
    boolean aplicado,
    NaturezaRegistro natureza,
    String moedaCotacaoId,
    String totalBruto,
    String cotacaoBrl,
    String taxaAtivoId,
    String taxaQuantidade,
    boolean taxaInformada,
    String aceitoPor,
    String aceitoEm,
    String grupoNumero,
    String principalConhecidoBrl,
    String grupoAutomatico,
    boolean aguardaConfirmacao,
    FinalidadeCripto finalidadeCripto,
    boolean taxaDetalhamentoPendente,
    boolean saldoAnteriorPendente,
    SubtipoCredito subtipoCredito,
    List<ParcelaGrupoResponse> pagamentosGrupo,
    List<ParcelaGrupoResponse> recebimentosGrupo) {
  public String chaveGrupo() {
    return grupoNumero == null ? grupoAutomatico : grupoNumero;
  }
}
