package io.github.jaimenafer.lastro.operacoes.application.usecase.impl;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

import io.github.jaimenafer.lastro.acesso.application.contract.IUsuarioAtual;
import io.github.jaimenafer.lastro.ativos.application.contract.IConsultaAtivos;
import io.github.jaimenafer.lastro.operacoes.application.dto.*;
import io.github.jaimenafer.lastro.operacoes.application.mapper.ConferenciaMapper;
import io.github.jaimenafer.lastro.operacoes.application.port.*;
import io.github.jaimenafer.lastro.operacoes.domain.repository.*;
import java.util.*;
import org.junit.jupiter.api.Test;

class PrioridadeConferenciaTests {
  @Test
  void filtroNaoRemoveDependentesDaContagemDeImpacto() {
    IConsultaConferencia consulta = mock(IConsultaConferencia.class);
    IUsuarioAtual usuario = mock(IUsuarioAtual.class);
    when(usuario.id()).thenReturn("teste");
    var anterior = linha("registro:1", "1", "2024-01-01T12:00:00Z", "custo:registro:1");
    var maior = linha("registro:2", "1", "2024-01-02T12:00:00Z", "custo:registro:2");
    when(consulta.registros())
        .thenReturn(
            List.of(
                anterior,
                maior,
                linha("operacao:3", "2", "2024-01-03T12:00:00Z", "custo:registro:2"),
                linha("operacao:4", "2", "2024-01-04T12:00:00Z", "custo:registro:2")));
    var usecase =
        new ConferenciaUseCaseImpl(
            usuario,
            mock(IPontoPartidaRepository.class),
            mock(IRegistroConferenciaRepository.class),
            mock(IContaCustodiaRepository.class),
            mock(IAjusteConciliatorioRepository.class),
            mock(IComandoConciliacaoRepository.class),
            consulta,
            mock(IPreparacaoConciliacao.class),
            mock(IConsultaAtivos.class),
            mock(io.github.jaimenafer.lastro.operacoes.application.port.IAssociacoesLivro.class));
    var resposta = usecase.listar(0, 50, null, 1L, null);
    assertThat(resposta.itens()).hasSize(2);
    assertThat(resposta.proximaPendenciaId()).isEqualTo("registro:2");
  }

  private RegistroTimelineResponse linha(String id, String ativo, String data, String causa) {
    Map<String, Object> dados = new HashMap<>(Map.of(
                "id",
                id,
                "ativoId",
                ativo,
                "data",
                data,
                "origem",
                "IMPORTADA",
                "estado",
                "PENDENTE",
                "aplicado",
                true,
                "taxaInformada",
                false,
                "pendencias",
                List.of(
                    Map.of(
                        "id",
                        causa,
                        "mensagem",
                        "Custo pendente",
                        "impacto",
                        "Resultado incompleto"))));
    dados.put("aguardaConfirmacao", false);
    dados.put("taxaDetalhamentoPendente", false);
    dados.put("saldoAnteriorPendente", false);
    return ConferenciaMapper.ler(ConferenciaMapper.escrever(dados), RegistroTimelineResponse.class);
  }
}
