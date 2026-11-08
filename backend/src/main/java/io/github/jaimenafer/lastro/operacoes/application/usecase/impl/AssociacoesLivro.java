package io.github.jaimenafer.lastro.operacoes.application.usecase.impl;

import static io.github.jaimenafer.lastro.operacoes.application.mapper.ConferenciaMapper.*;

import io.github.jaimenafer.lastro.acesso.application.contract.IUsuarioAtual;
import io.github.jaimenafer.lastro.ativos.application.contract.*;
import io.github.jaimenafer.lastro.operacoes.application.config.IConfiguracaoAssociacao;
import io.github.jaimenafer.lastro.operacoes.application.dto.*;
import io.github.jaimenafer.lastro.operacoes.application.mapper.AssociacaoMapper;
import io.github.jaimenafer.lastro.operacoes.application.mapper.ConferenciaMapper;
import io.github.jaimenafer.lastro.operacoes.application.port.IAssociacoesLivro;
import io.github.jaimenafer.lastro.operacoes.domain.model.*;
import io.github.jaimenafer.lastro.operacoes.domain.repository.*;
import io.github.jaimenafer.lastro.operacoes.domain.service.AgrupamentoNegociacoes;
import io.github.jaimenafer.lastro.operacoes.domain.service.ProjecaoGrupo;
import io.github.jaimenafer.lastro.operacoes.domain.service.ResolvedorCotacao;
import io.github.jaimenafer.lastro.operacoes.domain.service.IdentidadeValoracao;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.time.Duration;
import java.util.*;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class AssociacoesLivro implements IAssociacoesLivro {
  private final IUsuarioAtual usuario;
  private final IGrupoAssociacaoRepository repository;
  private final IHistoricoCotacaoRepository cotacoes;
  private final IValoracaoManualRepository valoracoesManuais;
  private final IRegistroConferenciaRepository registros;
  private final IConsultaAtivos ativos;
  private final IConfiguracaoAssociacao propriedades;

  public List<LinhaAssociacao> linhas() {
    List<RegistroConferencia> fontes = registros.listar(usuario.id());
    Set<Long> ids =
        fontes.stream()
            .map(r -> proposta(r, true).ativoId())
            .filter(Objects::nonNull)
            .map(Long::valueOf)
            .collect(Collectors.toSet());
    Map<Long, AtivoResumo> catalogo =
        ativos.buscarPorIds(ids).stream().collect(Collectors.toMap(AtivoResumo::id, a -> a));
    List<LinhaAssociacao> resultado = new ArrayList<>();
    for (var r : fontes) {
      PropostaRegistro p = proposta(r, true);
      OrigemRegistro origem = origem(r);
      if (!"IMPORTADA".equals(origem.origem())
          || (origem.campos().containsKey("sent_amount")
              && !origem.campos().containsKey("componente")))
        continue;
      AtivoResumo ativo = p.ativoId() == null ? null : catalogo.get(id(p.ativoId()));
      BigDecimal quantidade = numero(p.quantidade());
      PapelLinha papel = papel(origem.operacao(), quantidade);
      if (origemDeConversao(origem) && origem.erros() != null && !origem.erros().isEmpty())
        papel = PapelLinha.NAO_DEFINIDO;
      resultado.add(
          new LinhaAssociacao(
              r.getId(),
              r.getVersao(),
              r.getInstituicaoId(),
              id(p.contaId()),
              ativo == null ? null : ativo.id(),
              ativo == null ? "" : ativo.codigo(),
              ativo == null ? origem.moeda() : ativo.ticker(),
              instante(p.data()),
              quantidade,
              origem.operacao(),
              referencia(origem.campos()),
              papel,
              taxaZeroDeclarada(origem.campos()),
              origemFisica(origem)));
    }
    return List.copyOf(resultado);
  }

  private boolean origemDeConversao(OrigemRegistro origem) {
    String operacao = Objects.toString(origem.operacao(), "").strip().toLowerCase(Locale.ROOT);
    String tipo = origem.campos() == null ? ""
        : origem.campos().getOrDefault("type", "").strip().toLowerCase(Locale.ROOT);
    return Set.of("convert", "binance convert").contains(operacao)
        || Set.of("convert", "binance convert").contains(tipo);
  }

  private String origemFisica(OrigemRegistro origem) {
    if (origem.campos() == null || !origem.campos().containsKey("componente")
        || !Set.of("convert", "binance convert").contains(
            origem.campos().getOrDefault("type", "").strip().toLowerCase(Locale.ROOT))
        || origem.arquivoRef() == null || origem.linha() == null) return null;
    return origem.arquivoRef() + "|" + Objects.toString(origem.aba(), "") + "|" + origem.linha();
  }

  private PapelLinha papel(String tipo, BigDecimal quantidade) {
    if (quantidade == null || !AgrupamentoNegociacoes.tipoNegociacao(tipo))
      return PapelLinha.NAO_DEFINIDO;
    if (tipo.strip().toLowerCase(Locale.ROOT).contains("fee")) return PapelLinha.TAXA;
    return quantidade.signum() > 0 ? PapelLinha.RECEBIMENTO : PapelLinha.PAGAMENTO;
  }

  private String referencia(Map<String, String> campos) {
    for (var e : new TreeMap<>(campos).entrySet()) {
      String chave =
          java.text.Normalizer.normalize(e.getKey(), java.text.Normalizer.Form.NFD)
              .replaceAll("\\p{M}", "")
              .strip()
              .toLowerCase(Locale.ROOT);
      String valor = e.getValue().strip();
      boolean identificador =
          chave.contains("reference") || chave.contains("referencia") || chave.equals("order id");
      boolean observacao = chave.contains("remark") || chave.contains("observ");
      if (AgrupamentoNegociacoes.referenciaForte(valor)
          && (identificador || (observacao && referenciaEstruturada(valor)))) return valor;
    }
    return "";
  }

  private boolean referenciaEstruturada(String valor) {
    return valor.matches("(?is).*(?:wallet|order(?:\\s+id)?|trade|transaction)\\s*[/#:][^\\s]+.*")
        || valor.matches("(?i)[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}");
  }

  private boolean taxaZeroDeclarada(Map<String, String> campos) {
    String taxa = campos.get("fee_amount");
    return taxa != null && taxa.matches("0+(?:\\.0+)?");
  }

  private PropostaGrupo snapshot(String valor) {
    return valor == null ? null : ler(valor, PropostaGrupo.class);
  }

  private boolean atual(
      PropostaGrupo proposta, Map<Long, LinhaAssociacao> atuais, Map<Long, RegistroConferencia> fontes,
      Map<Long, String> impedimentos) {
    if (proposta.desfazer()) return true;
    return proposta.linhas().stream()
        .allMatch(l ->
            atuais.containsKey(l.registroId())
                && versaoCompativel(l, fontes.get(l.registroId()))
                && !impedimentos.containsKey(l.registroId()));
  }

  private boolean versaoCompativel(LinhaAssociacao linha, RegistroConferencia registro) {
    if (registro == null) return false;
    if (registro.getVersao() == linha.versao()) return true;
    return registro.versoesAnteriores().stream()
        .filter(h -> h.getVersao() == linha.versao())
        .map(h -> semMotivo(ler(h.getPropostaJson(), PropostaRegistro.class)))
        .anyMatch(anterior -> anterior.equals(semMotivo(proposta(registro, true))));
  }

  private PropostaRegistro semMotivo(PropostaRegistro p) {
    return new PropostaRegistro(p.data(), p.contaId(), p.ativoId(), p.quantidade(),
        p.custoTotalBrl(), p.natureza(), null, p.moedaCotacaoId(), p.totalBruto(),
        p.cotacaoBrl(), p.taxaAtivoId(), p.taxaQuantidade(), p.taxaInformada(),
        p.transferenciaRef(), p.finalidadeCripto());
  }

  private PropostaGrupo atualizarVersoes(PropostaGrupo proposta, Map<Long, LinhaAssociacao> atuais) {
    if (proposta.desfazer()) return proposta;
    List<LinhaAssociacao> linhas = proposta.linhas().stream().map(l -> {
      LinhaAssociacao atual = atuais.get(l.registroId());
      return new LinhaAssociacao(l.registroId(), atual.versao(), l.instituicaoId(), l.contaId(),
          l.ativoId(), l.codigo(), l.ticker(), l.data(), l.quantidade(), l.operacao(),
          l.referencia(), l.papel(), l.taxaZeroDeclarada(), l.origemFisica());
    }).toList();
    return new PropostaGrupo(linhas, proposta.taxaZeroConfirmada(), proposta.cotacaoBrl(), false);
  }

  public List<GrupoEmConferencia> grupos(boolean propostas, Map<Long, String> impedimentos) {
    return grupos(propostas, impedimentos, false);
  }

  public List<GrupoEmConferencia> gruposParaTimeline(Map<Long, String> impedimentos) {
    return valorarGrupos(grupos(true, impedimentos, true));
  }

  private List<GrupoEmConferencia> grupos(
      boolean propostas, Map<Long, String> impedimentos, boolean exibirInvalidos) {
    Map<Long, RegistroConferencia> fontes = registros.listar(usuario.id()).stream()
        .collect(Collectors.toMap(RegistroConferencia::getId, r -> r));
    Map<Long, LinhaAssociacao> atuais =
        linhas().stream().collect(Collectors.toMap(LinhaAssociacao::registroId, l -> l));
    List<GrupoAssociacao> existentes = repository.listar(usuario.id());
    Set<Long> idsCatalogo =
        atuais.values().stream()
            .map(LinhaAssociacao::ativoId)
            .filter(Objects::nonNull)
            .collect(Collectors.toSet());
    for (var g : existentes)
      for (String json : Arrays.asList(g.getPropostaJson(), g.getAplicadaJson())) {
        PropostaGrupo p = snapshot(json);
        if (p != null)
          p.linhas().stream()
              .map(LinhaAssociacao::ativoId)
              .filter(Objects::nonNull)
              .forEach(idsCatalogo::add);
      }
    Map<Long, AtivoResumo> catalogo =
        ativos.buscarPorIds(idsCatalogo).stream()
            .collect(Collectors.toMap(AtivoResumo::id, a -> a));
    Set<Long> reservados = new HashSet<>();
    List<GrupoEmConferencia> selecionados = new ArrayList<>();
    for (var g : existentes) {
      PropostaGrupo proposta = snapshot(g.getPropostaJson()),
          aplicada = snapshot(g.getAplicadaJson());
      for (var v : Arrays.asList(proposta, aplicada))
        if (v != null && !v.desfazer()) v.linhas().forEach(l -> reservados.add(l.registroId()));
      boolean valida = atual(proposta, atuais, fontes, impedimentos);
      PropostaGrupo escolhida = propostas && valida ? atualizarVersoes(proposta, atuais) : aplicada;
      if (escolhida == null && exibirInvalidos) escolhida = proposta;
      if (escolhida != null)
        selecionados.add(
            emConferencia(
                g.getId(), escolhida, propostas && valida && g.pendente(), false, catalogo));
    }
    if (propostas) {
      List<LinhaAssociacao> livres =
          atuais.values().stream()
              .filter(l ->
                  !reservados.contains(l.registroId())
                      && !impedimentos.containsKey(l.registroId())
                      && (l.ativoId() != null || l.papel() == PapelLinha.TAXA
                          || (l.papel() == PapelLinha.NAO_DEFINIDO
                              && l.operacao().toLowerCase(Locale.ROOT).contains("fee")))
                      && l.contaId() != null
                      && l.quantidade() != null)
              .toList();
      Map<Long, OrigemRegistro> origens = registros.listar(usuario.id()).stream()
          .collect(Collectors.toMap(RegistroConferencia::getId, ConferenciaMapper::origem));
      for (var linhas :
          AgrupamentoNegociacoes.automaticos(livres, propriedades.janelaAssociacao()))
        selecionados.add(
            emConferencia(
                null,
                new PropostaGrupo(
                    linhas,
                    linhas.stream().allMatch(LinhaAssociacao::taxaZeroDeclarada),
                    cotacaoOriginal(linhas, origens),
                    false),
                true,
                true,
                catalogo));
    }
    return List.copyOf(selecionados);
  }

  @Override
  public BigDecimal cotacaoEfetiva(List<LinhaAssociacao> linhas) {
    Set<Long> ids = linhas.stream().map(LinhaAssociacao::registroId).collect(Collectors.toSet());
    Map<Long, OrigemRegistro> origens = registros.listar(usuario.id()).stream()
        .filter(r -> ids.contains(r.getId()))
        .collect(Collectors.toMap(RegistroConferencia::getId, ConferenciaMapper::origem));
    return cotacaoOriginal(linhas, origens);
  }

  private BigDecimal cotacaoOriginal(
      List<LinhaAssociacao> linhas, Map<Long, OrigemRegistro> origens) {
    if (linhas.size() != 2
        || linhas.stream().anyMatch(l -> l.papel() == PapelLinha.TAXA)
        || linhas.stream().anyMatch(l -> !Set.of("convert", "binance convert")
            .contains(l.operacao().strip().toLowerCase(Locale.ROOT)))) return null;
    List<String> proveniencias = linhas.stream().map(l -> origens.get(l.registroId()))
        .filter(Objects::nonNull).map(this::origemFisica).filter(Objects::nonNull).toList();
    if (proveniencias.size() != 2 || !proveniencias.get(0).equals(proveniencias.get(1))) return null;
    LinhaAssociacao pagamento = linhas.stream().filter(l -> l.papel() == PapelLinha.PAGAMENTO)
        .findFirst().orElse(null);
    LinhaAssociacao recebimento = linhas.stream().filter(l -> l.papel() == PapelLinha.RECEBIMENTO)
        .findFirst().orElse(null);
    if (pagamento == null || recebimento == null || pagamento.codigo().equals("fiat:BRL")) return null;
    OrigemRegistro origem = origens.get(pagamento.registroId());
    if (origem == null || origem.campos() == null
        || origem.erros() != null && !origem.erros().isEmpty()) return null;
    Map<String, String> campos = origem.campos();
    String taxa = campos.getOrDefault("fee_amount", "");
    if (!taxa.isBlank() && !taxa.matches("0+(\\.0+)?")) return null;
    BigDecimal enviado = valorOriginal(campos.get("sent_amount"));
    BigDecimal recebido = valorOriginal(campos.get("received_amount"));
    BigDecimal valorEnviado = valorOriginal(campos.get("sent_value_BRL"));
    BigDecimal valorRecebido = valorOriginal(campos.get("received_value_BRL"));
    if (enviado == null || recebido == null || enviado.signum() <= 0 || recebido.signum() <= 0
        || !Objects.equals(campos.get("sent_currency"), pagamento.ticker())
        || !Objects.equals(campos.get("received_currency"), recebimento.ticker())
        || enviado.compareTo(pagamento.quantidade().abs()) != 0
        || recebido.compareTo(recebimento.quantidade().abs()) != 0
        || valorEnviado == null && valorRecebido == null
        || valorEnviado != null && valorRecebido != null && valorEnviado.compareTo(valorRecebido) != 0)
      return null;
    BigDecimal valor = valorEnviado == null ? valorRecebido : valorEnviado;
    if (valor.signum() <= 0 || Math.max(0, valor.stripTrailingZeros().scale()) > 8) return null;
    NaturezaRegistro natureza = AgrupamentoNegociacoes.resumir(
        linhas, false, null, propriedades.janelaAssociacao()).natureza();
    BigDecimal quantidadeCotacao = natureza == NaturezaRegistro.VENDA ? recebido : enviado;
    BigDecimal cotacao = valor.divide(quantidadeCotacao, 18, RoundingMode.HALF_EVEN);
    return cotacao.signum() > 0
            && cotacao.multiply(quantidadeCotacao).setScale(8, RoundingMode.HALF_EVEN)
                .compareTo(valor.setScale(8, RoundingMode.HALF_EVEN)) == 0
        ? cotacao : null;
  }

  private BigDecimal valorOriginal(String valor) {
    if (valor == null || valor.isBlank() || valor.length() > 160
        || !valor.matches("[0-9]+(\\.[0-9]+)?")) return null;
    BigDecimal numero = new BigDecimal(valor);
    return numero.signum() == 0 ? null : numero;
  }

  private GrupoEmConferencia emConferencia(
      Long id,
      PropostaGrupo proposta,
      boolean pendente,
      boolean automatico,
      Map<Long, AtivoResumo> catalogo) {
    String identificador =
        "grupo:registro:"
            + proposta.linhas().stream()
                .mapToLong(LinhaAssociacao::registroId)
                .min()
                .orElse(id == null ? 0 : id);
    ResumoGrupo resumo =
        proposta.desfazer()
            ? new ResumoGrupo(
                null,
                List.of(),
                List.of(),
                List.of(),
                null,
                false,
                List.of(
                    "Associação será desfeita e as linhas serão reprocessadas individualmente."))
            : ValidacaoGrupo.resumir(proposta, catalogo, propriedades.janelaAssociacao());
    return new GrupoEmConferencia(id, identificador, proposta, resumo, pendente, automatico);
  }

  public List<ItemConferenciaLivro> projetar(
      List<ItemConferenciaLivro> itens, boolean propostas, Map<Long, String> impedimentos) {
    List<ItemConferenciaLivro> resultado = new ArrayList<>(itens);
    for (var grupo : valorarGrupos(grupos(propostas, impedimentos))) {
      if (grupo.proposta().desfazer()) continue;
      Set<String> ids =
          grupo.proposta().linhas().stream()
              .map(l -> "registro:" + l.registroId())
              .collect(Collectors.toSet());
      resultado.removeIf(i -> ids.contains(i.id()));
      resultado.addAll(ProjecaoGrupo.projetar(grupo.identificador(), grupo.proposta(), grupo.resumo())
          .stream().map(i -> aplicarValoracao(i, grupo.valoracao(), grupo.taxaExternaBrl())).toList());
    }
    return List.copyOf(resultado);
  }

  private ItemConferenciaLivro aplicarValoracao(ItemConferenciaLivro item, Valoracao valoracao,
      BigDecimal taxaExternaBrl) {
    if (valoracao == null && taxaExternaBrl == null) return item;
    FatoLivro f = item.operacao();
    if (f == null) return item;
    FatoLivro fato = new FatoLivro(f.id(), f.tipo(), f.instituicaoId(), f.ativoId(), f.ocorridaEm(),
        f.ordemLivro(), f.quantidade(), f.custoTotalBrl(), f.moedaCotacaoId(), f.totalBruto(),
        f.cotacaoBrl(), f.taxaAtivoId(), f.taxaQuantidade(),
        valoracao == null ? null : valoracao.valorTotalBrl(), taxaExternaBrl);
    return new ItemConferenciaLivro(item.id(), item.contaId(), item.instituicaoId(), item.ativoId(),
        item.ocorridaEm(), item.ordem(), item.quantidade(), item.custoEntradaBrl(), fato,
        item.ajuste(), item.ancoraId(), item.transferenciaRef(), item.creditoAutomatico(), valoracao);
  }

  private List<GrupoEmConferencia> valorarGrupos(List<GrupoEmConferencia> grupos) {
    Map<Long, ValoracaoManual> valoresManuais = valoracoesManuais.listar(usuario.id()).stream()
        .filter(v -> v.grupoId() != null)
        .collect(Collectors.toMap(ValoracaoManual::grupoId, v -> v));
    List<GrupoEmConferencia> candidatos = grupos.stream()
        .filter(g -> !g.proposta().desfazer() && moedaCotacao(g) != null
            && (taxaEmTerceiroAtivo(g) != null || (!"fiat:BRL".equals(moedaCotacao(g).codigo())
                && (g.proposta().cotacaoBrl() == null || valoresManuais.containsKey(g.id())))))
        .toList();
    if (candidatos.isEmpty()) return grupos;
    List<Instant> datas = candidatos.stream()
        .map(g -> g.proposta().linhas().stream().map(LinhaAssociacao::data)
            .min(Comparator.naturalOrder()).orElseThrow()).toList();
    Instant inicio = datas.stream().min(Comparator.naturalOrder()).orElseThrow().minus(Duration.ofHours(24));
    Instant fim = datas.stream().max(Comparator.naturalOrder()).orElseThrow().plusMillis(1);
    List<CotacaoHistorica> precos = cotacoes.buscar(inicio, fim, "HORA");
    Long brl = ativos.buscarPorCodigo("fiat:BRL").orElseThrow().id();
    Set<Long> ids = grupos.stream().flatMap(g -> g.proposta().linhas().stream())
        .map(LinhaAssociacao::ativoId).filter(Objects::nonNull).collect(Collectors.toSet());
    Map<Long, AtivoResumo> catalogo = ativos.buscarPorIds(ids).stream()
        .collect(Collectors.toMap(AtivoResumo::id, a -> a));
    Map<Long, OrigemRegistro> origens = valoresManuais.isEmpty() ? Map.of()
        : registros.listar(usuario.id()).stream()
            .collect(Collectors.toMap(RegistroConferencia::getId, ConferenciaMapper::origem));
    return grupos.stream().map(g -> valorarGrupo(g, precos, brl, catalogo, valoresManuais, origens)).toList();
  }

  private GrupoEmConferencia valorarGrupo(GrupoEmConferencia grupo, List<CotacaoHistorica> precos,
      Long brl, Map<Long, AtivoResumo> catalogo, Map<Long, ValoracaoManual> valoresManuais,
      Map<Long, OrigemRegistro> origens) {
    PropostaGrupo p = grupo.proposta();
    ParcelaGrupo moeda = moedaCotacao(grupo);
    if (p.desfazer() || moeda == null) return grupo;
    if ("fiat:BRL".equals(moeda.codigo())) return valorarTaxaExterna(grupo, precos, brl, catalogo);
    ValoracaoManual manual = grupo.id() == null ? null : valoresManuais.get(grupo.id());
    if (manual != null && Objects.equals(manual.identidade(), IdentidadeValoracao.grupo(p))) {
      BigDecimal cotacaoEfetiva = cotacaoOriginal(p.linhas(), origens);
      if (cotacaoEfetiva != null) {
        PropostaGrupo efetiva = new PropostaGrupo(p.linhas(), p.taxaZeroConfirmada(), cotacaoEfetiva, false);
        GrupoEmConferencia confirmado = emConferencia(grupo.id(), efetiva, grupo.pendente(), grupo.automatico(), catalogo);
        return valorarTaxaExterna(confirmado, precos, brl, catalogo);
      }
      BigDecimal quantidade = moeda.quantidade();
      if (quantidade == null || quantidade.signum() <= 0) return grupo;
      PropostaGrupo atual = new PropostaGrupo(p.linhas(), p.taxaZeroConfirmada(),
          manual.valorBrl().divide(quantidade, 18, RoundingMode.HALF_EVEN), false);
      GrupoEmConferencia recalculado = emConferencia(grupo.id(), atual, grupo.pendente(), grupo.automatico(), catalogo);
      return valorarTaxaExterna(comValoracao(recalculado,
          new Valoracao(EstadoValoracao.MANUAL, manual.valorBrl(), List.of())), precos, brl, catalogo);
    }
    if (p.cotacaoBrl() != null) return valorarTaxaExterna(grupo, precos, brl, catalogo);
    Long base = moeda.ativoId();
    if (base == null) return grupo;
    Instant data = p.linhas().stream().map(LinhaAssociacao::data).min(Comparator.naturalOrder()).orElseThrow();
    GrupoEmConferencia valorado = ResolvedorCotacao.resolver(base, brl, data, precos)
        .map(r -> {
          PropostaGrupo atual = new PropostaGrupo(p.linhas(), p.taxaZeroConfirmada(),
              r.preco().setScale(18, RoundingMode.HALF_EVEN), false);
          GrupoEmConferencia recalculado = emConferencia(grupo.id(), atual, grupo.pendente(), grupo.automatico(), catalogo);
          BigDecimal total = moeda.quantidade().multiply(r.preco()).setScale(8, RoundingMode.HALF_EVEN);
          return comValoracao(recalculado, new Valoracao(r.aproximado() ? EstadoValoracao.ESTIMADO
              : EstadoValoracao.HISTORICO, total, r.caminho()));
        })
        .orElse(grupo);
    return valorarTaxaExterna(valorado, precos, brl, catalogo);
  }

  private ParcelaGrupo moedaCotacao(GrupoEmConferencia grupo) {
    NaturezaRegistro natureza = grupo.resumo().natureza();
    if (natureza == null) return null;
    List<ParcelaGrupo> moedas = switch (natureza) {
      case COMPRA, CONVERSAO -> grupo.resumo().pagamentos();
      case VENDA -> grupo.resumo().recebimentos();
      default -> List.of();
    };
    return moedas.size() == 1 ? moedas.getFirst() : null;
  }

  private GrupoEmConferencia comValoracao(GrupoEmConferencia grupo, Valoracao valoracao) {
    return new GrupoEmConferencia(grupo.id(), grupo.identificador(), grupo.proposta(), grupo.resumo(),
        grupo.pendente(), grupo.automatico(), valoracao, grupo.taxaExternaBrl());
  }

  private ParcelaGrupo taxaEmTerceiroAtivo(GrupoEmConferencia grupo) {
    ResumoGrupo resumo = grupo.resumo();
    if (resumo.taxas().size() != 1 || resumo.pagamentos().size() != 1
        || resumo.recebimentos().size() != 1) return null;
    ParcelaGrupo taxa = resumo.taxas().getFirst();
    return taxa.ativoId() != null && !taxa.ativoId().equals(resumo.pagamentos().getFirst().ativoId())
        && !taxa.ativoId().equals(resumo.recebimentos().getFirst().ativoId()) ? taxa : null;
  }

  private GrupoEmConferencia valorarTaxaExterna(GrupoEmConferencia grupo,
      List<CotacaoHistorica> precos, Long brl, Map<Long, AtivoResumo> catalogo) {
    ParcelaGrupo taxa = taxaEmTerceiroAtivo(grupo);
    if (taxa == null) return grupo;
    Instant data = grupo.proposta().linhas().stream().map(LinhaAssociacao::data)
        .min(Comparator.naturalOrder()).orElseThrow();
    var cotacao = ResolvedorCotacao.resolver(taxa.ativoId(), brl, data, precos);
    if (cotacao.isEmpty()) return grupo;
    PropostaGrupo proposta = grupo.proposta();
    PropostaGrupo semTaxa = new PropostaGrupo(proposta.linhas().stream()
        .filter(l -> l.papel() != PapelLinha.TAXA).toList(), true, proposta.cotacaoBrl(), false);
    ResumoGrupo validado = ValidacaoGrupo.resumir(semTaxa, catalogo, propriedades.janelaAssociacao());
    if (!validado.completo()) return grupo;
    ResumoGrupo resumo = new ResumoGrupo(validado.natureza(), validado.recebimentos(),
        validado.pagamentos(), grupo.resumo().taxas(), validado.principalBrl(), true, List.of());
    BigDecimal taxaBrl = taxa.quantidade().multiply(cotacao.orElseThrow().preco())
        .setScale(8, RoundingMode.HALF_EVEN);
    return new GrupoEmConferencia(grupo.id(), grupo.identificador(), proposta, resumo,
        grupo.pendente(), grupo.automatico(), grupo.valoracao(), taxaBrl);
  }
  public boolean aplicar(Map<Long, String> impedimentos) {
    boolean mudou = false;
    for (var selecionado : grupos(true, impedimentos))
      if (selecionado.pendente()) {
        GrupoAssociacao grupo =
            selecionado.id() == null
                ? repository.salvar(
                    GrupoAssociacao.criar(
                        usuario.id(), escrever(selecionado.proposta()), Instant.now()))
                : repository.buscar(usuario.id(), selecionado.id());
        if (selecionado.id() != null && !selecionado.proposta().equals(snapshot(grupo.getPropostaJson())))
          grupo.propor(escrever(selecionado.proposta()), usuario.id(), Instant.now());
        grupo.aplicar();
        repository.flush();
        reservar(grupo);
        mudou = true;
      }
    return mudou;
  }

  public void reservar(GrupoAssociacao grupo) {
    Set<Long> ids = new TreeSet<>();
    for (String json : Arrays.asList(grupo.getPropostaJson(), grupo.getAplicadaJson())) {
      PropostaGrupo p = snapshot(json);
      if (p != null && !p.desfazer()) p.linhas().forEach(l -> ids.add(l.registroId()));
    }
    repository.sincronizarVinculos(grupo.getId(), List.copyOf(ids));
  }

  public List<LinhaAssociacaoResponse> linhasResponse(
      List<LinhaAssociacao> linhas, Map<Long, String> numeros) {
    Map<Long, OrigemRegistro> origens =
        registros.listar(usuario.id()).stream()
            .collect(Collectors.toMap(RegistroConferencia::getId, r -> origem(r)));
    return linhas.stream()
        .map(l ->
            AssociacaoMapper.linha(l, numeros.get(l.registroId()), origens.get(l.registroId())))
        .toList();
  }

  public GrupoAssociacaoResponse response(GrupoEmConferencia grupo, String revisao) {
    String versao =
        grupo.id() == null
            ? null
            : Long.toString(repository.buscar(usuario.id(), grupo.id()).getVersao());
    return AssociacaoMapper.grupo(grupo, revisao, versao, origens());
  }

  public List<GrupoAssociacaoResponse> respostas(List<GrupoEmConferencia> grupos, String revisao) {
    Map<Long, OrigemRegistro> origens = origens();
    Map<Long, String> versoes =
        repository.listar(usuario.id()).stream()
            .collect(Collectors.toMap(GrupoAssociacao::getId, g -> Long.toString(g.getVersao())));
    return grupos.stream()
        .map(g ->
            AssociacaoMapper.grupo(
                g, revisao, g.id() == null ? null : versoes.get(g.id()), origens))
        .toList();
  }

  private Map<Long, OrigemRegistro> origens() {
    return registros.listar(usuario.id()).stream()
        .collect(Collectors.toMap(RegistroConferencia::getId, r -> origem(r)));
  }
}
