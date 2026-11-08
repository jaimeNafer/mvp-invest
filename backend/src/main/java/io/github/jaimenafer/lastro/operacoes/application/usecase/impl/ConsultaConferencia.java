package io.github.jaimenafer.lastro.operacoes.application.usecase.impl;

import static io.github.jaimenafer.lastro.operacoes.application.mapper.ConferenciaMapper.*;

import io.github.jaimenafer.lastro.acesso.application.contract.IUsuarioAtual;
import io.github.jaimenafer.lastro.ativos.application.contract.*;
import io.github.jaimenafer.lastro.instituicoes.application.contract.*;
import io.github.jaimenafer.lastro.operacoes.application.config.IConfiguracaoAssociacao;
import io.github.jaimenafer.lastro.operacoes.application.dto.*;
import io.github.jaimenafer.lastro.operacoes.application.mapper.*;
import io.github.jaimenafer.lastro.operacoes.application.port.*;
import io.github.jaimenafer.lastro.operacoes.domain.model.*;
import io.github.jaimenafer.lastro.operacoes.domain.repository.*;
import io.github.jaimenafer.lastro.operacoes.domain.service.MotorLivro;
import io.github.jaimenafer.lastro.operacoes.domain.service.ProjecaoGrupo;
import io.github.jaimenafer.lastro.operacoes.domain.service.ClassificacaoMovimentacao;
import java.math.*;
import java.time.*;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.*;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true, isolation = Isolation.REPEATABLE_READ)
public class ConsultaConferencia implements IConsultaConferencia, IConsultaLivroConferencia {
  private final IUsuarioAtual usuario;
  private final IOperacaoRepository operacoes;
  private final IRegistroConferenciaRepository registros;
  private final IContaCustodiaRepository custodias;
  private final IAjusteConciliatorioRepository ajustes;
  private final IConsultaAtivos ativos;
  private final IConsultaInstituicoes instituicoes;
  private final IAssociacoesLivro associacoes;
  private final IConfiguracaoAssociacao configuracao;

  public List<ItemConferenciaLivro> projetar(
      boolean propostas, boolean incluirAjustes, Instant ate) {
    List<ItemConferenciaLivro> itens = new ArrayList<>();
    Map<Long, Long> desconhecidas = new HashMap<>();
    custodias.listar(usuario.id()).stream()
        .filter(c -> c.getCodigo().equals("nao_especificada"))
        .forEach(c -> desconhecidas.put(c.getInstituicaoId(), c.getId()));
    for (Operacao o : operacoes.historico(usuario.id()))
      if (o.confirmada() && (ate == null || !o.getOcorridaEm().isAfter(ate)))
        itens.add(
            new ItemConferenciaLivro(
                "operacao:" + o.getId(),
                desconhecidas.getOrDefault(o.getInstituicaoId(), -o.getInstituicaoId()),
                o.getInstituicaoId(),
                o.getAtivoId(),
                o.getOcorridaEm(),
                o.getOrdemLivro(),
                o.getQuantidade(),
                o.getCustoTotalBrl(),
                LivroMapper.fato(o),
                false,
                null,
                null));
    Map<Long, String> impedimentos = impedimentosOrigem();
    Map<String, Optional<AtivoResumo>> moedasOriginais = new HashMap<>();
    List<RegistroConferencia> fontes = registros.listar(usuario.id());
    Map<Long, PropostaRegistro> classificacoes = classificarMovimentacoes(fontes);
    for (RegistroConferencia r : fontes) {
      PropostaRegistro p =
          propostas
              ? propostaEfetiva(r, impedimentos.containsKey(r.getId()), moedasOriginais, classificacoes)
              : ConferenciaMapper.proposta(r, false);
      for (ItemConferenciaLivro item : itensDaProposta(r, p, moedasOriginais))
        if (ate == null || !item.ocorridaEm().isAfter(ate)) itens.add(item);
    }
    itens =
        new ArrayList<>(
            associacoes.projetar(itens, propostas, impedimentos).stream()
                .filter(i -> ate == null || !i.ocorridaEm().isAfter(ate))
                .toList());
    if (incluirAjustes)
      for (AjusteConciliatorio a : ajustes.listarAtivos(usuario.id())) {
        boolean ancoraPresente = itens.stream().anyMatch(i -> i.id().equals(a.getAncoraRef()));
        if (ancoraPresente)
          itens.add(
              new ItemConferenciaLivro(
                  "ajuste:" + a.getId(),
                  a.getContaCustodiaId(),
                  a.getInstituicaoId(),
                  a.getAtivoId(),
                  null,
                  0,
                  a.getQuantidade(),
                  a.getCustoTotalBrl(),
                  null,
                  true,
                  a.getAncoraRef(),
                  null));
      }
    return List.copyOf(itens);
  }

  public Map<Long, String> impedimentosOrigem() {
    List<RegistroConferencia> fontes = registros.listar(usuario.id());
    Map<Long, PropostaRegistro> classificacoes = classificarMovimentacoes(fontes);
    List<Operacao> historicoLegado = operacoes.historico(usuario.id());
    List<Operacao> aberturas =
        operacoes.historico(usuario.id()).stream()
            .filter(Operacao::confirmada)
            .filter(o -> o.getTipo() == TipoOperacao.SALDO_INICIAL)
            .toList();
    Map<Long, String> resultado = new HashMap<>();
    Map<String, Map<String, List<RegistroConferencia>>> porOriginal = new HashMap<>();
    Map<String, List<RegistroConferencia>> referencias = new HashMap<>();
    Map<String, Optional<AtivoResumo>> moedasOriginais = new HashMap<>();
    for (RegistroConferencia r : fontes) {
      Map<String, String> campos = ConferenciaMapper.origem(r).campos();
      String referencia = campos.get("id");
      if (referencia != null && !referencia.isBlank()
          && !(campos.containsKey("componente") && referencia.matches("[0-9]+")))
        referencias
            .computeIfAbsent(r.getInstituicaoId() + ":" + referencia, k -> new ArrayList<>())
            .add(r);
      if (referencia != null
          && !referencia.isBlank()
          && historicoLegado.stream()
              .anyMatch(o ->
                  referencia.equals(o.getReferencia())
                      && o.getInstituicaoId().equals(r.getInstituicaoId())))
        resultado.put(
            r.getId(),
            "A referência já existe no histórico financeiro, inclusive suas correções ou"
                + " cancelamento. Confira o vínculo antes de aplicar novamente.");
      PropostaRegistro p = propostaEfetiva(r, false, moedasOriginais, classificacoes);
      List<ItemConferenciaLivro> movimentos = itensDaProposta(r, p, moedasOriginais);
      if (movimentos.stream()
          .anyMatch(i ->
              aberturas.stream()
                  .anyMatch(o ->
                      o.getAtivoId().equals(i.ativoId())
                          && o.getInstituicaoId().equals(i.instituicaoId())
                          && !i.ocorridaEm().isAfter(o.getOcorridaEm()))))
        resultado.put(
            r.getId(),
            "Um movimento do registro pode estar incluído no saldo de abertura. Confira sua relação"
                + " com a posição inicial antes de aplicá-lo.");
      if (r.getArquivoRef() != null) {
        String assinatura =
            ConferenciaMapper.hash(new TreeMap<>(ConferenciaMapper.origem(r).campos()).toString());
        porOriginal
            .computeIfAbsent(assinatura, k -> new HashMap<>())
            .computeIfAbsent(r.getArquivoRef(), k -> new ArrayList<>())
            .add(r);
      }
    }
    for (List<RegistroConferencia> ocorrencias : referencias.values())
      if (ocorrencias.stream().map(this::linhaFisica).distinct().count() > 1)
        for (RegistroConferencia r : ocorrencias)
          if (r.getAplicadaJson() == null)
            resultado.put(
                r.getId(),
                "A referência do evento aparece em mais de um registro recebido. As ocorrências"
                    + " foram preservadas; confirme sua relação antes de aplicar novamente.");
    for (Map<String, List<RegistroConferencia>> arquivos : porOriginal.values())
      if (arquivos.size() > 1) {
        for (List<RegistroConferencia> ocorrencias : arquivos.values())
          for (RegistroConferencia r : ocorrencias)
            if (r.getAplicadaJson() == null)
              resultado.put(
                  r.getId(),
                  "Há registros originais iguais em arquivos diferentes. Confirme a sobreposição"
                      + " antes de aplicar; a multiplicidade foi preservada.");
      }
    return Map.copyOf(resultado);
  }

  private String linhaFisica(RegistroConferencia registro) {
    OrigemRegistro origem = ConferenciaMapper.origem(registro);
    return origem.campos().containsKey("componente")
        ? origem.arquivoRef() + "|" + origem.aba() + "|" + origem.linha()
        : "registro:" + registro.getId();
  }

  public Map<Long, String> impedimentos() {
    Map<Long, String> resultado = new HashMap<>(impedimentosOrigem());
    for (RegistroConferencia r : registros.listar(usuario.id()))
      if (manterObservacoes(r, ConferenciaMapper.proposta(r, true)))
        resultado.putIfAbsent(
            r.getId(),
            "As quantidades originais são preservadas. Esta proposta ainda não determina uma"
                + " negociação completa com suas contrapartidas.");
    return Map.copyOf(resultado);
  }

  private boolean manterObservacoes(RegistroConferencia r, PropostaRegistro p) {
    boolean negociacao =
        p.natureza() == NaturezaRegistro.COMPRA || p.natureza() == NaturezaRegistro.VENDA;
    if (!negociacao) return false;
    boolean csv = ConferenciaMapper.origem(r).campos().containsKey("sent_amount");
    return csv
        ? negociacaoIncompleta(r, p)
        : "IMPORTADA".equals(ConferenciaMapper.origem(r).origem());
  }

  private PropostaRegistro propostaEfetiva(
      RegistroConferencia r,
      boolean origemImpedida,
      Map<String, Optional<AtivoResumo>> moedasOriginais,
      Map<Long, PropostaRegistro> classificacoes) {
    PropostaRegistro atual = ConferenciaMapper.proposta(r, true),
        aplicada = ConferenciaMapper.proposta(r, false);
    atual = propostaClassificada(r, atual, aplicada, classificacoes);
    if (origemImpedida || creditoOriginalInconsistente(r, atual)
        || movimentacaoEarnInconsistente(r, atual)
        || !aproveitavel(r, atual, moedasOriginais)) return aplicada;
    if (!manterObservacoes(r, atual)) return atual;
    if (aplicada != null) return aplicada;
    PropostaRegistro original =
        r.versoesAnteriores().isEmpty()
            ? atual
            : ConferenciaMapper.ler(
                r.versoesAnteriores().getFirst().getPropostaJson(), PropostaRegistro.class);
    return aproveitavel(r, original, moedasOriginais) ? original : null;
  }

  private boolean creditoOriginalInconsistente(RegistroConferencia r, PropostaRegistro proposta) {
    OrigemRegistro origem = ConferenciaMapper.origem(r);
    if (proposta == null || r.getVersao() != 1
        || proposta.natureza() != NaturezaRegistro.OBSERVADA
        || ClassificacaoMovimentacao.subtipoCredito(origem.operacao()) == null) return false;
    return !legivel(proposta) || numero(proposta.quantidade()).signum() <= 0
        || origem.erros() != null && !origem.erros().isEmpty();
  }

  private boolean movimentacaoEarnInconsistente(RegistroConferencia r, PropostaRegistro proposta) {
    OrigemRegistro origem = ConferenciaMapper.origem(r);
    if (proposta == null || r.getVersao() != 1
        || proposta.natureza() != NaturezaRegistro.OBSERVADA
        || !ClassificacaoMovimentacao.movimentacaoEarn(origem.operacao())) return false;
    return !legivel(proposta)
        || ClassificacaoMovimentacao.classificarMovimentacaoEarn(
            origem.operacao(), numero(proposta.quantidade())) == NaturezaRegistro.OBSERVADA
        || origem.erros() != null && !origem.erros().isEmpty();
  }

  public Map<Long, PropostaRegistro> propostasParaAplicar() {
    Map<Long, String> impedimentos = impedimentosOrigem();
    Map<Long, Long> versoesAgrupadas = new HashMap<>();
    associacoes.grupos(true, impedimentos).stream()
        .filter(g -> !g.proposta().desfazer())
        .flatMap(g -> g.proposta().linhas().stream())
        .forEach(l -> versoesAgrupadas.put(l.registroId(), l.versao()));
    Map<Long, PropostaRegistro> propostas = new LinkedHashMap<>();
    Map<String, Optional<AtivoResumo>> moedasOriginais = new HashMap<>();
    List<RegistroConferencia> fontes = registros.listar(usuario.id());
    Map<Long, PropostaRegistro> classificacoes = classificarMovimentacoes(fontes);
    for (RegistroConferencia r : fontes) {
      PropostaRegistro efetiva =
          propostaEfetiva(r, impedimentos.containsKey(r.getId()), moedasOriginais, classificacoes);
      if ((!versoesAgrupadas.containsKey(r.getId())
              || versoesAgrupadas.get(r.getId()) == r.getVersao())
          && aproveitavel(r, efetiva, moedasOriginais)
          && !efetiva.equals(ConferenciaMapper.proposta(r, false)))
        propostas.put(r.getId(), efetiva);
    }
    return Map.copyOf(propostas);
  }

  private PropostaRegistro propostaClassificada(
      RegistroConferencia registro,
      PropostaRegistro atual,
      PropostaRegistro aplicada,
      Map<Long, PropostaRegistro> classificacoes) {
    if (atual == null || registro.getVersao() != 1 || atual.natureza() != NaturezaRegistro.OBSERVADA)
      return atual;
    if (aplicada != null && aplicada.natureza() != NaturezaRegistro.OBSERVADA) return aplicada;
    return classificacoes.getOrDefault(registro.getId(), atual);
  }

  private PropostaRegistro comNatureza(PropostaRegistro p, NaturezaRegistro natureza) {
    return new PropostaRegistro(
        p.data(), p.contaId(), p.ativoId(), p.quantidade(), p.custoTotalBrl(),
        natureza, p.motivo(), p.moedaCotacaoId(), p.totalBruto(), p.cotacaoBrl(),
        p.taxaAtivoId(), p.taxaQuantidade(), p.taxaInformada(), p.transferenciaRef());
  }

  private PropostaRegistro recuperarAtivoOriginal(
      RegistroConferencia registro, PropostaRegistro proposta, OrigemRegistro origem,
      Map<String, Optional<AtivoResumo>> cache) {
    if (proposta == null || proposta.ativoId() != null || registro.getVersao() != 1
        || proposta.natureza() != NaturezaRegistro.OBSERVADA
        || (ClassificacaoMovimentacao.subtipoCredito(origem.operacao()) == null
            && !ClassificacaoMovimentacao.movimentacaoEarn(origem.operacao()))
        || origem.moeda() == null) return proposta;
    Optional<AtivoResumo> ativo = cache.computeIfAbsent(origem.moeda(),
        moeda -> ativos.buscarPorCodigo("cripto:" + moeda));
    if (ativo.isEmpty()) return proposta;
    return new PropostaRegistro(
        proposta.data(), proposta.contaId(), ativo.get().id().toString(), proposta.quantidade(),
        proposta.custoTotalBrl(), proposta.natureza(), proposta.motivo(), proposta.moedaCotacaoId(),
        proposta.totalBruto(), proposta.cotacaoBrl(), proposta.taxaAtivoId(),
        proposta.taxaQuantidade(), proposta.taxaInformada(), proposta.transferenciaRef());
  }

  private Map<Long, PropostaRegistro> classificarMovimentacoes(List<RegistroConferencia> fontes) {
    Map<Long, PropostaRegistro> resultado = new HashMap<>();
    Map<Long, PropostaRegistro> propostas = new HashMap<>();
    Map<Long, OrigemRegistro> origens = new HashMap<>();
    Map<String, Optional<AtivoResumo>> ativosOriginais = new HashMap<>();
    Map<String, List<RegistroConferencia>> porAtivoEValor = new HashMap<>();
    for (RegistroConferencia registro : fontes) {
      OrigemRegistro origem = ConferenciaMapper.origem(registro);
      PropostaRegistro proposta = recuperarAtivoOriginal(
          registro, ConferenciaMapper.proposta(registro, true), origem, ativosOriginais);
      propostas.put(registro.getId(), proposta);
      origens.put(registro.getId(), origem);
      if (legivel(proposta)) {
        String chave = proposta.ativoId() + "|" + decimal(numero(proposta.quantidade()).abs());
        porAtivoEValor.computeIfAbsent(chave, ignorado -> new ArrayList<>()).add(registro);
      }
    }
    Set<Long> idsAtivos =
        propostas.values().stream().filter(ConferenciaMapper::legivel)
            .map(p -> id(p.ativoId())).collect(Collectors.toSet());
    Set<Long> criptoativos =
        ativos.buscarPorIds(idsAtivos).stream().filter(AtivoResumo::cripto)
            .map(AtivoResumo::id).collect(Collectors.toSet());
    for (RegistroConferencia registro : fontes) {
      PropostaRegistro proposta = propostas.get(registro.getId());
      OrigemRegistro origem = origens.get(registro.getId());
      if (!podeClassificarAutomaticamente(registro, proposta, origem)) continue;
      BigDecimal quantidade = numero(proposta.quantidade());
      NaturezaRegistro movimentoEarn =
          ClassificacaoMovimentacao.classificarMovimentacaoEarn(origem.operacao(), quantidade);
      if (movimentoEarn == NaturezaRegistro.MOVIMENTACAO_INTERNA) {
        resultado.put(registro.getId(), comNatureza(proposta, movimentoEarn));
        continue;
      }
      if (quantidade.signum() > 0
          && criptoativos.contains(id(proposta.ativoId()))
          && ClassificacaoMovimentacao.subtipoCredito(origem.operacao()) != null) {
        resultado.put(registro.getId(), comNatureza(proposta, NaturezaRegistro.CREDITO_AUTOMATICO));
        continue;
      }
      if (ClassificacaoMovimentacao.classificarSaidaCripto(
              origem.operacao(), quantidade, criptoativos.contains(id(proposta.ativoId())))
          == NaturezaRegistro.SAIDA_CRIPTO) {
        resultado.put(registro.getId(), comNatureza(proposta, NaturezaRegistro.SAIDA_CRIPTO));
        continue;
      }
      NaturezaRegistro natureza = ClassificacaoMovimentacao.classificar(
          origem.operacao(), origem.moeda(), quantidade, origem.campos(), false);
      if (natureza == NaturezaRegistro.OBSERVADA) continue;
      String chave = proposta.ativoId() + "|" + decimal(quantidade.abs());
      boolean possivelTransferencia =
          porAtivoEValor.getOrDefault(chave, List.of()).stream()
              .filter(outra -> !outra.getId().equals(registro.getId()))
              .anyMatch(outra ->
                  indicaContrapartida(proposta, propostas.get(outra.getId()), origens.get(outra.getId())));
      if (!possivelTransferencia) resultado.put(registro.getId(), comNatureza(proposta, natureza));
    }
    return resultado;
  }

  private boolean podeClassificarAutomaticamente(
      RegistroConferencia registro, PropostaRegistro proposta, OrigemRegistro origem) {
    return registro.getVersao() == 1
        && legivel(proposta)
        && proposta.natureza() == NaturezaRegistro.OBSERVADA
        && proposta.transferenciaRef() == null
        && "IMPORTADA".equals(origem.origem())
        && origem.campos() != null
        && (origem.erros() == null || origem.erros().isEmpty());
  }

  private boolean indicaContrapartida(
      PropostaRegistro proposta, PropostaRegistro contraparte, OrigemRegistro origem) {
    if (!legivel(contraparte)
        || Objects.equals(proposta.contaId(), contraparte.contaId())
        || !Objects.equals(proposta.ativoId(), contraparte.ativoId())
        || numero(contraparte.quantidade()).add(numero(proposta.quantidade())).signum() != 0)
      return false;
    String operacao = Objects.toString(origem.operacao(), "").toLowerCase(Locale.ROOT);
    if (!(operacao.equals("deposit") || operacao.equals("withdraw") || operacao.contains("transfer")))
      return false;
    return Duration.between(instante(proposta.data()), instante(contraparte.data())).abs()
            .compareTo(configuracao.janelaAssociacao())
        <= 0;
  }

  private List<ItemConferenciaLivro> itensDaProposta(
      RegistroConferencia r, PropostaRegistro p, Map<String, Optional<AtivoResumo>> moedas) {
    if (p == null || p.data() == null || p.contaId() == null
        || p.natureza() == NaturezaRegistro.MOVIMENTACAO_INTERNA) return List.of();
    ItemConferenciaLivro principal = ConferenciaMapper.item(r, p);
    ItemConferenciaLivro referencia =
        principal != null
            ? principal
            : new ItemConferenciaLivro(
                "registro:" + r.getId(),
                id(p.contaId()),
                r.getInstituicaoId(),
                null,
                instante(p.data()),
                r.getId(),
                null,
                null,
                null,
                false,
                null,
                null);
    ItemConferenciaLivro saida = contrapartida(r, p, referencia, moedas);
    List<ItemConferenciaLivro> itens = new ArrayList<>();
    if (principal != null) itens.add(principal);
    if (saida != null) itens.add(saida);
    return List.copyOf(itens);
  }

  private boolean aproveitavel(
      RegistroConferencia r,
      PropostaRegistro p,
      Map<String, Optional<AtivoResumo>> moedasOriginais) {
    return legivel(p) || !itensDaProposta(r, p, moedasOriginais).isEmpty();
  }

  private ItemConferenciaLivro contrapartida(
      RegistroConferencia r,
      PropostaRegistro p,
      ItemConferenciaLivro principal,
      Map<String, Optional<AtivoResumo>> moedas) {
    Map<String, String> campos = ConferenciaMapper.origem(r).campos();
    if (campos.containsKey("componente")) return null;
    String enviada = campos.get("sent_amount");
    String recebida = campos.get("received_amount");
    String moeda = campos.get("sent_currency");
    if (principal.operacao() != null
        || enviada == null
        || enviada.isBlank()
        || recebida == null
        || recebida.isBlank()
        || moeda == null
        || !enviada.matches("[0-9]+(\\.[0-9]+)?")) return null;
    BigDecimal quantidade = new BigDecimal(enviada);
    if (quantidade.signum() == 0) return null;
    if (Math.max(0, quantidade.stripTrailingZeros().scale()) > 18
        || quantidade.precision() - quantidade.scale() > 20) return null;
    Optional<AtivoResumo> ativo =
        moedas.computeIfAbsent(
            moeda,
            c ->
                ativos.buscarPorCodigo(
                    (c.equals("BRL") || c.equals("USD") ? "fiat:" : "cripto:") + c));
    if (ativo.isEmpty()) return null;
    return new ItemConferenciaLivro(
        principal.id() + ":saida",
        principal.contaId(),
        principal.instituicaoId(),
        ativo.get().id(),
        principal.ocorridaEm(),
        principal.ordem(),
        quantidade.negate(),
        null,
        null,
        false,
        null,
        null);
  }

  public RetratoConferencia carregar(Instant ate) {
    List<ItemConferenciaLivro> itens = projetar(false, true, ate);
    Map<Long, AtivoResumo> catalogo = catalogo(itens);
    Map<Long, InstituicaoResumo> locais =
        instituicoes
            .buscarPorIds(
                itens.stream().map(ItemConferenciaLivro::instituicaoId).collect(Collectors.toSet()))
            .stream()
            .collect(Collectors.toMap(InstituicaoResumo::id, Function.identity()));
    ResultadoConferenciaLivro livro = MotorLivro.conferir(itens, codigos(catalogo));
    List<RegistroTimelineResponse> linhas = registros();
    CoberturaQuantidades cobertura = cobertura(itens, livro, ate);
    return new RetratoConferencia(
        itens,
        livro,
        resumir(linhas),
        catalogo,
        locais,
        cobertura.completa(),
        cobertura.semData(),
        cobertura.primeiraDataPendente());
  }

  private CoberturaQuantidades cobertura(
      List<ItemConferenciaLivro> itens, ResultadoConferenciaLivro livro, Instant ate) {
    boolean
        completa =
            livro.posicoes().stream().allMatch(PosicaoConferenciaLivro::quantidadeDisponivel),
        semData = false;
    LocalDate primeira = null;
    Set<String> ids = itens.stream().map(ItemConferenciaLivro::id).collect(Collectors.toSet());
    Set<Long> agrupados =
        associacoes.grupos(false, Map.of()).stream()
            .filter(g -> !g.proposta().desfazer())
            .flatMap(g -> g.proposta().linhas().stream())
            .filter(l -> l.ativoId() != null)
            .map(LinhaAssociacao::registroId)
            .collect(Collectors.toSet());
    for (RegistroConferencia r : registros.listar(usuario.id())) {
      PropostaRegistro aplicada = ConferenciaMapper.proposta(r, false),
          referencia = aplicada == null ? ConferenciaMapper.proposta(r, true) : aplicada;
      Instant data = instante(referencia.data());
      if (ate != null && data != null && data.isAfter(ate)) continue;
      boolean coberta =
          agrupados.contains(r.getId())
              || (legivel(aplicada) && contrapartidasCobertas(r, aplicada, ids));
      if (!coberta) {
        completa = false;
        if (data == null) semData = true;
        else {
          LocalDate dia = data.atZone(ZoneId.of("America/Fortaleza")).toLocalDate();
          if (primeira == null || dia.isBefore(primeira)) primeira = dia;
        }
      }
    }
    return new CoberturaQuantidades(completa, semData, primeira);
  }

  private boolean contrapartidasCobertas(
      RegistroConferencia r, PropostaRegistro p, Set<String> ids) {
    if (p.natureza() == NaturezaRegistro.MOVIMENTACAO_INTERNA) return true;
    Map<String, String> campos = ConferenciaMapper.origem(r).campos();
    if (campos.containsKey("componente")) return ids.contains("registro:" + r.getId());
    if (!campos.containsKey("sent_amount")) return true;
    if (ConferenciaMapper.item(r, p).operacao() != null) return true;
    String enviada = campos.getOrDefault("sent_amount", ""),
        recebida = campos.getOrDefault("received_amount", ""),
        tipo = campos.getOrDefault("type", "");
    boolean negociacao = Set.of("Buy", "Sell", "Convert", "Trade").contains(tipo);
    if (negociacao && (enviada.isBlank() || recebida.isBlank())) return false;
    String taxa = campos.getOrDefault("fee_amount", ""),
        moedaTaxa = campos.getOrDefault("fee_currency", "");
    boolean semTaxa = taxa.matches("0+(\\.0+)?");
    if (!semTaxa
        && (!taxa.isBlank() || !moedaTaxa.isBlank())
        && !moedaTaxa.equals(campos.get("sent_currency"))
        && !moedaTaxa.equals(campos.get("received_currency"))) return false;
    if (enviada.isBlank() || recebida.isBlank() || enviada.matches("0+(\\.0+)?")) return true;
    return ids.contains("registro:" + r.getId() + ":saida");
  }

  public ResultadoLivro validarCandidato(List<Operacao> candidato) {
    RetratoConferencia atual = carregar(null);
    boolean comConferencia = atual.itens().stream().anyMatch(i -> !i.id().startsWith("operacao:"));
    if (!comConferencia)
      return LivroMapper.calcular(
          candidato, ativos.buscarPorIds(LivroMapper.ativosDoHistorico(candidato)));
    List<ItemConferenciaLivro> itens =
        new ArrayList<>(
            atual.itens().stream().filter(i -> !i.id().startsWith("operacao:")).toList());
    Map<Long, Long> contas = new HashMap<>();
    custodias.listar(usuario.id()).stream()
        .filter(c -> c.getCodigo().equals("nao_especificada"))
        .forEach(c -> contas.put(c.getInstituicaoId(), c.getId()));
    int indice = 0;
    for (Operacao o : candidato)
      if (o.confirmada())
        itens.add(
            new ItemConferenciaLivro(
                o.getId() == null ? "operacao:candidato:" + indice++ : "operacao:" + o.getId(),
                contas.getOrDefault(o.getInstituicaoId(), -o.getInstituicaoId()),
                o.getInstituicaoId(),
                o.getAtivoId(),
                o.getOcorridaEm(),
                o.getOrdemLivro(),
                o.getQuantidade(),
                o.getCustoTotalBrl(),
                LivroMapper.fato(o),
                false,
                null,
                null));
    Map<Long, AtivoResumo> catalogo = catalogo(itens);
    ResultadoConferenciaLivro livro = MotorLivro.conferir(itens, codigos(catalogo));
    Set<String> conflitosAnteriores =
        atual.livro().efeitos().stream()
            .filter(e -> e.estado() == EstadoConferencia.CORRECAO_NECESSARIA)
            .map(EfeitoConferenciaLivro::id)
            .collect(Collectors.toSet());
    if (livro.efeitos().stream()
        .anyMatch(e ->
            e.estado() == EstadoConferencia.CORRECAO_NECESSARIA
                && !conflitosAnteriores.contains(e.id())))
      throw new io.github.jaimenafer.lastro.operacoes.domain.exception.OperacaoInvalidaException(
          "A alteração deixa saldo insuficiente na conta de custódia. Confira a origem dos recursos"
              + " e os registros posteriores.");
    return LivroConferenciaMapper.livro(
        new RetratoConferencia(
            itens,
            livro,
            atual.resumo(),
            catalogo,
            atual.instituicoes(),
            atual.quantidadesCompletas(),
            atual.incertezaSemData(),
            atual.primeiraDataPendente()));
  }

  private Map<Long, AtivoResumo> catalogo(List<ItemConferenciaLivro> itens) {
    Set<Long> ids = new HashSet<>();
    for (ItemConferenciaLivro i : itens) {
      ids.add(i.ativoId());
      if (i.operacao() != null) {
        if (i.operacao().moedaCotacaoId() != null) ids.add(i.operacao().moedaCotacaoId());
        if (i.operacao().taxaAtivoId() != null) ids.add(i.operacao().taxaAtivoId());
      }
    }
    return ativos.buscarPorIds(ids).stream()
        .collect(Collectors.toMap(AtivoResumo::id, Function.identity()));
  }

  private Map<Long, String> codigos(Map<Long, AtivoResumo> catalogo) {
    return catalogo.values().stream()
        .collect(Collectors.toMap(AtivoResumo::id, AtivoResumo::codigo));
  }

  public List<ContaConferenciaResponse> contas() {
    List<ContaCustodia> todas = custodias.listar(usuario.id());
    Map<Long, InstituicaoResumo> locais =
        instituicoes
            .buscarPorIds(
                todas.stream().map(ContaCustodia::getInstituicaoId).collect(Collectors.toSet()))
            .stream()
            .collect(Collectors.toMap(InstituicaoResumo::id, Function.identity()));
    return todas.stream()
        .map(c ->
            new ContaConferenciaResponse(
                c.getId().toString(),
                c.getNome(),
                c.getInstituicaoId().toString(),
                locais.get(c.getInstituicaoId()).nome()))
        .toList();
  }

  public List<RegistroTimelineResponse> registros() {
    List<ItemConferenciaLivro> itens = projetar(true, true, null);
    Map<Long, AtivoResumo> catalogo = catalogo(itens);
    Set<Long> propostos =
        registros.listar(usuario.id()).stream()
            .map(r -> ConferenciaMapper.proposta(r, true).ativoId())
            .filter(Objects::nonNull)
            .map(Long::valueOf)
            .collect(Collectors.toSet());
    ativos.buscarPorIds(propostos).forEach(a -> catalogo.put(a.id(), a));
    Map<String, EfeitoConferenciaLivro> efeitos =
        MotorLivro.conferir(itens, codigos(catalogo)).efeitos().stream()
            .collect(
                Collectors.toMap(
                    e -> canonico(e.id()), Function.identity(), this::combinarEfeitos));
    Map<Long, ContaConferenciaResponse> contas =
        contas().stream().collect(Collectors.toMap(c -> Long.valueOf(c.id()), Function.identity()));
    List<RegistroTimelineResponse> linhas = new ArrayList<>();
    Map<String, AjusteConciliatorio> ajustesPorId =
        ajustes.listarAtivos(usuario.id()).stream()
            .collect(Collectors.toMap(a -> "ajuste:" + a.getId(), Function.identity()));
    Map<Long, Operacao> legados =
        operacoes.historico(usuario.id()).stream()
            .collect(Collectors.toMap(Operacao::getId, Function.identity()));
    Map<Long, String> impedimentos = impedimentos();
    Map<Long, GrupoEmConferencia> porGrupo = new HashMap<>();
    associacoes.gruposParaTimeline(impedimentosOrigem()).stream()
        .filter(g -> !g.proposta().desfazer())
        .forEach(g -> g.proposta().linhas().forEach(l -> porGrupo.put(l.registroId(), g)));
    List<RegistroConferencia> fontes = registros.listar(usuario.id());
    Map<Long, PropostaRegistro> classificacoes = classificarMovimentacoes(fontes);
    for (RegistroConferencia r : fontes) {
      GrupoEmConferencia grupo = porGrupo.get(r.getId());
      PropostaRegistro p = propostaClassificada(r, ConferenciaMapper.proposta(r, true),
          ConferenciaMapper.proposta(r, false), classificacoes);
      OrigemRegistro origem = ConferenciaMapper.origem(r);
      AtivoResumo ativo = p.ativoId() == null ? null : catalogo.get(Long.valueOf(p.ativoId()));
      ContaConferenciaResponse conta = contas.get(Long.valueOf(p.contaId()));
      EfeitoConferenciaLivro efeito = efeitos.get(identificadorEfeito(r, grupo));
      List<PendenciaConferenciaResponse> problemas = new ArrayList<>();
      if (grupo == null && negociacaoIncompleta(r, p))
        problemas.add(
            new PendenciaConferenciaResponse(
                "negociacao",
                "Complete moeda de cotação, total bruto, cotação e informação da taxa. Taxa ausente"
                    + " permanece desconhecida.",
                "Resultado e taxas desta negociação permanecem indisponíveis."));
      if (impedimentos.containsKey(r.getId()) && grupo == null)
        problemas.add(
            new PendenciaConferenciaResponse(
                "origem",
                impedimentos.get(r.getId()),
                "Este registro aguarda conciliação e não será aplicado novamente."));
      if (!legivel(p))
        problemas.add(
            new PendenciaConferenciaResponse(
                "campos",
                "Complete data, ativo e quantidade para aplicar este registro.",
                "Quantidade e custos indisponíveis."));
      boolean conversaoIdentificada = grupo != null
          && grupo.resumo().natureza() != null
          && grupo.proposta().linhas().stream()
              .filter(l -> l.papel() != PapelLinha.TAXA)
              .allMatch(l -> Set.of("binance convert", "convert")
                  .contains(l.operacao().strip().toLowerCase(Locale.ROOT)));
      if (efeito != null && p.natureza() != NaturezaRegistro.SAIDA_CRIPTO)
        for (String causa : efeito.causas()) {
          if (conversaoIdentificada && causa.startsWith("custo:")) continue;
          problemas.add(
              new PendenciaConferenciaResponse(
                  causa, mensagem(causa), "Cálculos dependentes permanecem em conferência."));
        }
      if (grupo != null) {
        if (grupo.proposta().linhas().stream()
            .anyMatch(l -> l.registroId() == r.getId() && l.versao() != r.getVersao()))
          problemas.add(
              new PendenciaConferenciaResponse(
                  "grupo_desatualizado",
                  "Uma linha mudou após a associação. Revise o grupo antes de aplicar a alteração.",
                  "A versão financeira anterior permanece preservada."));
        for (String pendencia : grupo.resumo().pendencias())
          problemas.add(
              new PendenciaConferenciaResponse(
                  "grupo",
                  pendencia,
                  "Custos e resultados da operação agrupada permanecem em conferência."));
      }
      if (grupo == null
          && p.natureza() == NaturezaRegistro.OBSERVADA
          && p.transferenciaRef() == null)
        problemas.add(
            new PendenciaConferenciaResponse(
                "natureza",
                "Classifique a movimentação e informe os dados conhecidos.",
                "Resultado e taxas podem estar incompletos."));
      EstadoConferencia estado =
          (p.data() == null || p.quantidade() == null)
              ? EstadoConferencia.CORRECAO_NECESSARIA
              : efeito == null ? EstadoConferencia.PENDENTE : efeito.estado();
      if (conversaoIdentificada && efeito != null
          && efeito.estado() != EstadoConferencia.CORRECAO_NECESSARIA
          && problemas.isEmpty()) estado = EstadoConferencia.COERENTE;
      if (p.natureza() == NaturezaRegistro.MOVIMENTACAO_INTERNA
          && legivel(p) && problemas.isEmpty())
        estado = EstadoConferencia.COERENTE;
      if (p.natureza() == NaturezaRegistro.SAIDA_CRIPTO
          && legivel(p) && efeito != null && problemas.isEmpty())
        estado = EstadoConferencia.COERENTE;
      if (estado == EstadoConferencia.COERENTE && !problemas.isEmpty())
        estado = EstadoConferencia.PENDENTE;
      linhas.add(
          new RegistroTimelineResponse(
              "registro:" + r.getId(),
              p.data(),
              r.getCriadoEm().toString(),
              origem.operacao(),
              p.natureza() == NaturezaRegistro.SAIDA_CRIPTO ? "Saída de criptomoeda"
                  : p.natureza() == NaturezaRegistro.APORTE ? "Aporte em reais"
                  : p.natureza() == NaturezaRegistro.RETIRADA ? "Retirada em reais" : origem.operacao(),
              r.getInstituicaoId().toString(),
              conta.instituicaoNome(),
              p.contaId(),
              conta.nome(),
              p.ativoId(),
              ativo == null ? origem.moeda() : ativo.ticker(),
              p.quantidade(),
              p.custoTotalBrl(),
              estado,
              origem.origem(),
              r.getRevisadoPor(),
              r.getRevisadoEm() == null ? null : r.getRevisadoEm().toString(),
              Long.toString(r.getVersao()),
              List.copyOf(problemas),
              null,
              camposOriginais(origem),
              r.getAplicadaJson() != null,
              p.natureza(),
              p.moedaCotacaoId(),
              p.totalBruto(),
              p.cotacaoBrl(),
              p.taxaAtivoId(),
              p.taxaQuantidade(),
              p.taxaInformada(),
              null,
              null,
              grupo == null || grupo.id() == null ? null : "OP-" + grupo.id(),
              grupo == null ? null : decimal(grupo.resumo().principalBrl()),
              grupo != null && grupo.automatico() ? grupo.identificador() : null,
              !Objects.equals(p, ConferenciaMapper.proposta(r, false)) || (grupo != null && grupo.pendente()),
              p.natureza() == NaturezaRegistro.SAIDA_CRIPTO
                  ? (p.finalidadeCripto() == null ? FinalidadeCripto.DESCONHECIDA : p.finalidadeCripto()) : null,
              p.natureza() == NaturezaRegistro.SAIDA_CRIPTO && !p.taxaInformada() && origem.campos() != null
                  && origem.campos().values().stream().filter(Objects::nonNull)
                      .anyMatch(v -> v.toLowerCase(Locale.ROOT).contains("fee is included")),
              p.natureza() == NaturezaRegistro.SAIDA_CRIPTO && efeito != null
                  && efeito.estado() == EstadoConferencia.CORRECAO_NECESSARIA,
              p.natureza() == NaturezaRegistro.CREDITO_AUTOMATICO
                  ? ClassificacaoMovimentacao.subtipoCredito(origem.operacao()) : null,
              grupo == null ? List.of() : grupo.resumo().pagamentos().stream()
                  .map(v -> new ParcelaGrupoResponse(v.ticker(), decimal(v.quantidade()))).toList(),
              grupo == null ? List.of() : grupo.resumo().recebimentos().stream()
                  .map(v -> new ParcelaGrupoResponse(v.ticker(), decimal(v.quantidade()))).toList()));
    }
    List<ItemConferenciaLivro> exibidos = new ArrayList<>(itens);
    Set<String> idsExibidos =
        itens.stream().map(ItemConferenciaLivro::id).collect(Collectors.toSet());
    for (AjusteConciliatorio ajuste : ajustesPorId.values()) {
      String idAjuste = "ajuste:" + ajuste.getId();
      if (!idsExibidos.contains(idAjuste))
        exibidos.add(
            new ItemConferenciaLivro(
                idAjuste,
                ajuste.getContaCustodiaId(),
                ajuste.getInstituicaoId(),
                ajuste.getAtivoId(),
                null,
                0,
                ajuste.getQuantidade(),
                ajuste.getCustoTotalBrl(),
                null,
                true,
                ajuste.getAncoraRef(),
                null));
    }
    Set<Long> ativosAjustes =
        ajustesPorId.values().stream()
            .map(AjusteConciliatorio::getAtivoId)
            .filter(id -> !catalogo.containsKey(id))
            .collect(Collectors.toSet());
    ativos.buscarPorIds(ativosAjustes).forEach(a -> catalogo.put(a.id(), a));
    for (ItemConferenciaLivro i : exibidos)
      if (i.id().startsWith("operacao:") || i.ajuste()) {
        AtivoResumo a = catalogo.get(i.ativoId());
        ContaConferenciaResponse c = contas.get(i.contaId());
        EfeitoConferenciaLivro e = efeitos.get(i.id());
        String instituicao = c == null ? "Instituição" : c.instituicaoNome();
        String conta = c == null ? "Não especificada" : c.nome();
        EstadoConferencia estado = e == null ? EstadoConferencia.PENDENTE : e.estado();
        List<PendenciaConferenciaResponse> pendencias =
            e == null
                ? List.of()
                : e.causas().stream()
                    .map(v ->
                        new PendenciaConferenciaResponse(
                            v, mensagem(v), "Confira os dados de origem."))
                    .toList();
        linhas.add(
            new RegistroTimelineResponse(
                i.id(),
                i.ocorridaEm() == null ? null : i.ocorridaEm().toString(),
                i.ajuste()
                    ? ajustesPorId.get(i.id()).getCriadoEm().toString()
                    : legados.get(i.operacao().id()).getCriadoEm().toString(),
                i.ajuste() ? "AJUSTE" : i.operacao().tipo().name(),
                i.ajuste() ? "Ajuste conciliatório" : i.operacao().tipo().name(),
                i.instituicaoId().toString(),
                instituicao,
                i.contaId().toString(),
                conta,
                i.ativoId().toString(),
                a.ticker(),
                decimal(i.quantidade()),
                decimal(i.custoEntradaBrl()),
                estado,
                i.ajuste() ? "SINTETICA" : "MANUAL",
                null,
                null,
                i.ajuste() ? Long.toString(ajustesPorId.get(i.id()).getVersao()) : "1",
                pendencias,
                i.ancoraId(),
                Map.of(),
                true,
                null,
                null,
                null,
                null,
                null,
                null,
                false,
                i.ajuste() ? ajustesPorId.get(i.id()).getAceitoPor() : null,
                i.ajuste() ? ajustesPorId.get(i.id()).getCriadoEm().toString() : null,
                null,
                null,
                null,
                false,
                null,
                false,
                false,
                null,
                List.of(),
                List.of()));
      }
    Map<String, String> datas =
        linhas.stream()
            .filter(l -> l.data() != null)
            .collect(
                Collectors.toMap(RegistroTimelineResponse::id, RegistroTimelineResponse::data));
    linhas.sort(
        Comparator.comparing((RegistroTimelineResponse l) ->
                l.data() != null ? instante(l.data()) : instanteAncora(l.ancoraId(), datas))
            .thenComparing(l -> l.origem().equals("SINTETICA") ? 0 : 1)
            .thenComparing(RegistroTimelineResponse::id));
    return List.copyOf(linhas);
  }

  private String identificadorEfeito(RegistroConferencia registro, GrupoEmConferencia grupo) {
    if (grupo == null) return "registro:" + registro.getId();
    if (grupo.resumo().completo()) return grupo.identificador();
    if (!ProjecaoGrupo.projetaParcelasConsolidadas(grupo.proposta(), grupo.resumo()))
      return "registro:" + registro.getId();
    LinhaAssociacao linha = grupo.proposta().linhas().stream()
        .filter(l -> l.registroId() == registro.getId()).findFirst().orElseThrow();
    long representante = grupo.proposta().linhas().stream()
        .filter(l -> Objects.equals(l.ativoId(), linha.ativoId()))
        .mapToLong(LinhaAssociacao::registroId).min().orElseThrow();
    return "registro:" + representante;
  }

  private boolean negociacaoIncompleta(RegistroConferencia r, PropostaRegistro p) {
    return legivel(p)
        && (p.natureza() == NaturezaRegistro.COMPRA || p.natureza() == NaturezaRegistro.VENDA)
        && ConferenciaMapper.item(r, p).operacao() == null;
  }

  private Map<String, String> camposOriginais(OrigemRegistro origem) {
    Map<String, String> campos = new LinkedHashMap<>(origem.campos());
    if (origem.arquivoRef() != null) campos.put("Arquivo recebido", origem.arquivoRef());
    if (origem.linha() != null) campos.put("Linha de origem", origem.linha().toString());
    if (origem.aba() != null && !origem.aba().isBlank()) campos.put("Aba de origem", origem.aba());
    return Map.copyOf(campos);
  }

  private Instant instanteAncora(String ancora, Map<String, String> datas) {
    String data = ancora == null ? null : datas.get(canonico(ancora));
    return data == null ? Instant.MAX : instante(data);
  }

  private String canonico(String id) {
    return id.startsWith("registro:") && id.indexOf(':', 9) > 0
        ? id.substring(0, id.indexOf(':', 9))
        : id;
  }

  private EfeitoConferenciaLivro combinarEfeitos(
      EfeitoConferenciaLivro a, EfeitoConferenciaLivro b) {
    Set<String> causas = new LinkedHashSet<>(a.causas());
    causas.addAll(b.causas());
    return new EfeitoConferenciaLivro(
        canonico(a.id()),
        a.estado().ordinal() > b.estado().ordinal() ? a.estado() : b.estado(),
        List.copyOf(causas),
        a.calculo() != null ? a.calculo() : b.calculo());
  }

  private String mensagem(String causa) {
    if (causa.startsWith("custo:")) return "Informe o custo histórico conhecido desta entrada.";
    if (causa.startsWith("ajuste:")) return "Este cálculo utiliza um ajuste provisório.";
    if (causa.startsWith("transferencia:")) return "Confira as duas pernas da transferência.";
    return "Falta origem de saldo para esta movimentação.";
  }

  public ResumoConferenciaResponse resumir(List<RegistroTimelineResponse> linhas) {
    List<RegistroTimelineResponse> reais =
        linhas.stream().filter(l -> !l.origem().equals("SINTETICA")).toList();
    long verdes = reais.stream().filter(l -> l.estado() == EstadoConferencia.COERENTE).count();
    long vermelhos =
        reais.stream().filter(l -> l.estado() == EstadoConferencia.CORRECAO_NECESSARIA).count();
    long afetados = reais.size() - verdes;
    Map<String, List<RegistroTimelineResponse>> operacoes = reais.stream()
        .filter(l -> l.chaveGrupo() != null || (l.natureza() != NaturezaRegistro.CREDITO_AUTOMATICO
            && l.natureza() != NaturezaRegistro.MOVIMENTACAO_INTERNA
            && l.natureza() != NaturezaRegistro.OBSERVADA))
        .collect(Collectors.groupingBy(l -> l.chaveGrupo() == null ? l.id() : l.chaveGrupo()));
    long operacoesCoerentes = operacoes.values().stream()
        .filter(grupo -> grupo.stream().allMatch(l -> l.estado() == EstadoConferencia.COERENTE))
        .count();
    long creditos = reais.stream()
        .filter(l -> l.natureza() == NaturezaRegistro.CREDITO_AUTOMATICO).count();
    long naoClassificados = reais.stream()
        .filter(l -> l.natureza() == NaturezaRegistro.OBSERVADA && l.chaveGrupo() == null).count();
    long agrupadas = reais.stream().map(RegistroTimelineResponse::chaveGrupo)
        .filter(Objects::nonNull).distinct().count();
    return new ResumoConferenciaResponse(
        reais.size(),
        verdes,
        afetados - vermelhos,
        vermelhos,
        linhas.size() - reais.size(),
        reais.isEmpty()
            ? "100"
            : BigDecimal.valueOf(verdes)
                .multiply(BigDecimal.valueOf(100))
                .divide(BigDecimal.valueOf(reais.size()), 4, RoundingMode.HALF_EVEN)
                .toPlainString(),
        afetados,
        reais.stream()
            .filter(l ->
                l.natureza() == NaturezaRegistro.VENDA && l.estado() != EstadoConferencia.COERENTE)
            .count(),
        reais.stream()
            .filter(l -> l.estado() != EstadoConferencia.COERENTE)
            .map(RegistroTimelineResponse::ativoTicker)
            .distinct()
            .sorted()
            .toList(),
        afetados == 0
            ? "Registros consistentes com os dados cadastrados."
            : "Há informações pendentes que afetam os cálculos.",
        operacoesCoerentes, operacoes.size() - operacoesCoerentes, creditos, naoClassificados,
        agrupadas);
  }
}
