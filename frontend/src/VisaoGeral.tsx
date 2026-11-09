import ResumoConciliacao from "./ResumoConciliacao";
import { useEffect, useRef, useState } from "react";
import ComposicaoCustos from "./ComposicaoCustos";
import ConferenciaVisaoGeral from "./ConferenciaVisaoGeral";
import EvolucaoResultado from "./EvolucaoResultado";
import PatrimonioDiario from "./PatrimonioDiario";
import ResultadoMonetario from "./ResultadoMonetario";
import ResultadosPorAtivo from "./ResultadosPorAtivo";
import SaldosMoedas from "./SaldosMoedas";
import SaldosVisaoGeral from "./SaldosVisaoGeral";
import { consultarPatrimonioDiario, consultarVisaoGeral } from "./services/visaoGeral";
import { consultarMarcadorAtualizacaoCotacoes } from "./services/conciliacao";
import { listarCarteiras } from "./services/carteiras";
import type { Carteira } from "./services/carteiras";
import type {
  IndicadorDisponibilidade,
  PeriodoVisaoGeral,
  PontoPatrimonioDiario,
  VisaoGeral as Consolidacao,
} from "./services/visaoGeral";
import ValorMonetario from "./ValorMonetario";
import "./visao-geral.css";

function dataLegivel(data: string) {
  return data.split("-").reverse().join("/");
}

function indicadorLegado(
  dados: Consolidacao,
  campo: "custo" | "resultado" | "taxas" | "aportesLiquidos",
): IndicadorDisponibilidade {
  const disponibilidade = dados.disponibilidade;
  if (disponibilidade?.[campo]) return disponibilidade[campo];
  const legado = {
    custo: [
      disponibilidade?.custoCompleto,
      disponibilidade?.subtotalCustoBrl,
      dados.resumo.custoSaldosBrl,
    ],
    resultado: [
      disponibilidade?.resultadoCompleto,
      disponibilidade?.subtotalResultadoBrl,
      dados.resumo.resultadoRealizadoBrl,
    ],
    taxas: [
      disponibilidade?.taxasCompletas,
      disponibilidade?.subtotalTaxasBrl,
      dados.resumo.taxasBrl,
    ],
    aportesLiquidos: [true, dados.resumo.aportesBrl, dados.resumo.aportesBrl],
  }[campo] as [boolean | undefined, string | null | undefined, string | null];
  const completo = legado[0] !== false;
  return {
    valorConhecido: legado[1] ?? legado[2] ?? null,
    status: completo ? "COMPLETO" : legado[1] == null ? "INDISPONIVEL" : "PARCIAL",
    componentesConhecidos: 0,
    componentesDesconhecidos: completo ? 0 : 1,
    motivo: null,
  };
}

function IndicadorResumo({
  titulo,
  indicador,
  resultado = false,
}: {
  titulo: string;
  indicador: IndicadorDisponibilidade;
  resultado?: boolean;
}) {
  const parcial = indicador.status === "PARCIAL";
  const indisponivel = indicador.status === "INDISPONIVEL";
  const valor = indisponivel ? null : indicador.valorConhecido;
  const quantidade = indicador.componentesDesconhecidos;
  const ressalva = parcial
    ? [
        quantidade > 0
          ? `${quantidade} ${quantidade === 1 ? "componente desconhecido" : "componentes desconhecidos"}`
          : "subtotal conhecido",
        indicador.motivo,
      ]
        .filter(Boolean)
        .join(" · ")
    : indisponivel
      ? (indicador.motivo ?? "Sem dados suficientes para calcular.")
      : null;
  return (
    <div>
      <dt>{titulo}</dt>
      <dd>{resultado ? <ResultadoMonetario valor={valor} /> : <ValorMonetario valor={valor} />}</dd>
      {ressalva && <small className="overview-indicator-note">{ressalva}</small>}
    </div>
  );
}

export default function VisaoGeral({
  onOperacoes,
  carteiraId,
  onCarteira,
  onCriarCarteira,
  onImportacoes,
  onConferencia,
}: {
  onOperacoes: () => void;
  carteiraId?: string;
  onCarteira?: (id?: string) => void;
  onCriarCarteira?: () => void;
  onImportacoes?: () => void;
  onConferencia?: () => void;
}) {
  const [carteiras, setCarteiras] = useState<Carteira[] | null>(null);
  const selecionarCarteira = Boolean(onCarteira);
  const [dados, setDados] = useState<Consolidacao | null>(null);
  const [patrimonio, setPatrimonio] = useState<PontoPatrimonioDiario[]>([]);
  const [patrimonioCarregado, setPatrimonioCarregado] = useState(false);
  const [versaoPatrimonio, setVersaoPatrimonio] = useState(0);
  const [carregandoPatrimonio, setCarregandoPatrimonio] = useState(false);
  const [erroPatrimonio, setErroPatrimonio] = useState("");
  const [periodo, setPeriodo] = useState<PeriodoVisaoGeral>({ dataInicio: "", dataFim: "" });
  const [consulta, setConsulta] = useState<{
    periodo?: PeriodoVisaoGeral;
    atualizacao?: number;
  }>({});
  const [carregando, setCarregando] = useState(true);
  const [erro, setErro] = useState("");
  const [avisoCotacoesAtualizadas, setAvisoCotacoesAtualizadas] = useState(false);
  const atualizacaoAutomatica = useRef(false);
  const controladorPatrimonio = useRef<AbortController | null>(null);

  const carregarPatrimonio = (forcar = false) => {
    if (!dados || carregando || carregandoPatrimonio || (patrimonioCarregado && !forcar)) return;
    const fim = new Date(`${dados.dataFim}T00:00:00Z`);
    const inicioSolicitado = new Date(`${dados.dataInicio}T00:00:00Z`);
    const inicioLimite = new Date(fim);
    inicioLimite.setUTCDate(inicioLimite.getUTCDate() - 365);
    const periodoPatrimonio = {
      dataInicio:
        inicioSolicitado < inicioLimite
          ? inicioLimite.toISOString().slice(0, 10)
          : dados.dataInicio,
      dataFim: dados.dataFim,
    };
    const controller = new AbortController();
    controladorPatrimonio.current?.abort();
    controladorPatrimonio.current = controller;
    setCarregandoPatrimonio(true);
    setErroPatrimonio("");
    void consultarPatrimonioDiario(periodoPatrimonio, controller.signal, carteiraId)
      .then((pontos) => {
        if (!controller.signal.aborted && Array.isArray(pontos)) {
          setPatrimonio(pontos);
          setPatrimonioCarregado(true);
        }
      })
      .catch((falha: unknown) => {
        if (!controller.signal.aborted)
          setErroPatrimonio(
            falha instanceof Error
              ? falha.message
              : "Não foi possível consultar a evolução do patrimônio.",
          );
      })
      .finally(() => {
        if (!controller.signal.aborted) {
          controladorPatrimonio.current = null;
          setCarregandoPatrimonio(false);
        }
      });
  };

  useEffect(() => () => controladorPatrimonio.current?.abort(), []);

  useEffect(() => {
    if (!selecionarCarteira) return;
    const controller = new AbortController();
    void listarCarteiras(controller.signal)
      .then((lista) => {
        if (!controller.signal.aborted) setCarteiras(lista);
      })
      .catch(() => {});
    return () => controller.abort();
  }, [selecionarCarteira]);

  useEffect(() => {
    const controller = new AbortController();
    let ativo = true;
    let consultando = false;
    let marcadorInicializado = false;
    let marcadorAtual: string | null = null;
    const verificarMarcador = async () => {
      if (!ativo || consultando) return;
      consultando = true;
      try {
        const resposta = await consultarMarcadorAtualizacaoCotacoes(controller.signal);
        if (!ativo) return;
        if (!marcadorInicializado) {
          marcadorInicializado = true;
          marcadorAtual = resposta.atualizadaEm;
          return;
        }
        if (resposta.atualizadaEm !== marcadorAtual) {
          marcadorAtual = resposta.atualizadaEm;
          atualizacaoAutomatica.current = true;
          setConsulta((atual) => ({
            ...atual,
            atualizacao: (atual.atualizacao ?? 0) + 1,
          }));
        }
      } catch {
        return;
      } finally {
        consultando = false;
      }
    };
    void verificarMarcador();
    const intervalo = window.setInterval(() => void verificarMarcador(), 30_000);
    return () => {
      ativo = false;
      controller.abort();
      window.clearInterval(intervalo);
    };
  }, []);

  useEffect(() => {
    const recargaAutomatica = atualizacaoAutomatica.current;
    atualizacaoAutomatica.current = false;
    const controller = new AbortController();
    if (!recargaAutomatica) {
      controladorPatrimonio.current?.abort();
      controladorPatrimonio.current = null;
      setCarregandoPatrimonio(false);
      setCarregando(true);
      setErro("");
    }
    void consultarVisaoGeral(consulta.periodo, controller.signal, carteiraId)
      .then((resposta) => {
        if (controller.signal.aborted) return;
        if (!recargaAutomatica) {
          setVersaoPatrimonio((versao) => versao + 1);
          setPatrimonio([]);
          setPatrimonioCarregado(false);
          setErroPatrimonio("");
        }
        setDados(resposta);
        if (recargaAutomatica) setAvisoCotacoesAtualizadas(true);
        if (!consulta.periodo)
          setPeriodo({ dataInicio: resposta.dataInicio, dataFim: resposta.dataFim });
      })
      .catch((falha: unknown) => {
        if (!controller.signal.aborted && !recargaAutomatica)
          setErro(
            falha instanceof Error ? falha.message : "Não foi possível consultar a visão geral.",
          );
      })
      .finally(() => {
        if (!controller.signal.aborted) setCarregando(false);
      });
    return () => controller.abort();
  }, [consulta, carteiraId]);

  return (
    <div className="overview-page">
      <header className="overview-heading">
        <h1>Visão geral</h1>
        <form
          className="overview-filters"
          onSubmit={(e) => {
            e.preventDefault();
            atualizacaoAutomatica.current = false;
            setConsulta({ periodo: { ...periodo }, atualizacao: consulta.atualizacao });
          }}
        >
          {onCarteira && carteiras && (carteiras.length > 0 || carteiraId) && (
            <label className="overview-wallet-filter">
              Carteira
              <select
                value={carteiraId ?? ""}
                onChange={(e) => onCarteira(e.target.value || undefined)}
              >
                <option value="">Consolidado</option>
                {carteiras
                  .filter((c) => c.ativo || c.id === carteiraId)
                  .map((c) => (
                    <option key={c.id} value={c.id}>
                      {c.nome}
                    </option>
                  ))}
              </select>
            </label>
          )}
          <label htmlFor="visao-de">De</label>
          <input
            id="visao-de"
            type="date"
            required
            value={periodo.dataInicio}
            onChange={(e) => setPeriodo({ ...periodo, dataInicio: e.target.value })}
          />
          <label htmlFor="visao-ate">Até</label>
          <input
            id="visao-ate"
            type="date"
            required
            value={periodo.dataFim}
            onChange={(e) => setPeriodo({ ...periodo, dataFim: e.target.value })}
          />
          <button className="primary-button" type="submit" disabled={carregando}>
            Aplicar
          </button>
        </form>
      </header>
      {carregando && !dados && (
        <p role="status" className="overview-note">
          Consultando seus investimentos…
        </p>
      )}
      {erro && (
        <div className="overview-error">
          <p role="alert">{erro}</p>
          <button
            className="secondary-button"
            type="button"
            onClick={() => setConsulta({ ...consulta })}
          >
            Tentar novamente
          </button>
        </div>
      )}
      {dados && (
        <>
          <p className="overview-caption">
            Saldos a custo até {dataLegivel(dados.dataSaldos)} · Resultados de{" "}
            {dataLegivel(dados.dataInicio)} a {dataLegivel(dados.dataFim)}
          </p>
          {avisoCotacoesAtualizadas && (
            <p role="status" className="overview-note">
              Novas cotações carregadas; os cálculos foram atualizados.
            </p>
          )}
          {dados.dataReferencia && dados.dataInicio < dados.dataReferencia && (
            <p className="overview-note">
              O histórico cadastrado começa em {dataLegivel(dados.dataReferencia)}.
            </p>
          )}
          {dados.disponibilidade?.comAjustes && (
            <p className="overview-note">
              Os saldos incluem ajustes provisórios. Ajustes não são aportes ou lucro.
            </p>
          )}
          {dados.possuiRegistrosAteFim && (
            <dl className="overview-indicators">
              <IndicadorResumo
                titulo="Custo dos saldos"
                indicador={indicadorLegado(dados, "custo")}
              />
              <IndicadorResumo
                titulo="Resultado realizado líquido"
                indicador={indicadorLegado(dados, "resultado")}
                resultado
              />
              <IndicadorResumo titulo="Taxas" indicador={indicadorLegado(dados, "taxas")} />
              <IndicadorResumo
                titulo="Aportes líquidos"
                indicador={indicadorLegado(dados, "aportesLiquidos")}
              />
              <div>
                <dt>Operações no período</dt>
                <dd>{dados.resumo.operacoesNoPeriodo}</dd>
              </div>
            </dl>
          )}
          {dados.conciliacao && dados.conciliacao.total > 0 && (
            <ResumoConciliacao
              resumo={dados.conciliacao}
              onAbrirTimeline={onConferencia ?? onOperacoes}
              compact
            />
          )}
          {!dados.possuiRegistrosAteFim ? (
            <div className="overview-empty">
              <p>
                {dados.conciliacao && dados.conciliacao.total > 0
                  ? "Há registros recebidos aguardando conferência e aplicação."
                  : "Nenhum registro até a data final. Consulte outro período ou registre suas operações."}
              </p>
              {(dados.conciliacao?.total ?? 0) > 0 && onConferencia ? (
                <button className="primary-button" type="button" onClick={onConferencia}>
                  Conferir histórico
                </button>
              ) : carteiras?.length === 0 && onCriarCarteira ? (
                <>
                  <h2>Comece organizando seus investimentos</h2>
                  <p>Crie uma carteira para reunir suas operações e acompanhar seus resultados.</p>
                  <button className="primary-button" type="button" onClick={onCriarCarteira}>
                    Criar carteira
                  </button>
                </>
              ) : (
                <>
                  <button
                    className="primary-button"
                    type="button"
                    onClick={onImportacoes ?? onOperacoes}
                  >
                    {onImportacoes ? "Importar operações" : "Ir para operações"}
                  </button>
                  {onImportacoes && (
                    <button className="secondary-button" type="button" onClick={onOperacoes}>
                      Adicionar operação
                    </button>
                  )}
                </>
              )}
            </div>
          ) : (
            <>
              {dados.resumo.operacoesNoPeriodo === 0 && (
                <p className="overview-note">
                  {dados.disponibilidade?.resultadoCompleto === false
                    ? "Há movimentações em conferência. A contagem considera somente operações com dados financeiros completos."
                    : "Nenhuma operação no período. Os saldos incluem os registros anteriores."}
                </p>
              )}
              <SaldosMoedas
                saldos={dados.saldosMoedas}
                completo={dados.disponibilidade?.quantidadesCompletas}
              />
              <PatrimonioDiario
                key={versaoPatrimonio}
                pontos={patrimonio}
                carregando={carregandoPatrimonio}
                erro={erroPatrimonio}
                carregado={patrimonioCarregado}
                onAbrir={carregarPatrimonio}
                onTentarNovamente={() => carregarPatrimonio(true)}
              />
              <div className="overview-analysis">
                <EvolucaoResultado evolucao={dados.evolucaoResultado} />
                <ResultadosPorAtivo
                  resultados={dados.resultadosPorAtivo}
                  completo={dados.disponibilidade?.resultadoCompleto}
                />
              </div>
              <div className="overview-main">
                <ComposicaoCustos
                  posicoes={dados.posicoes}
                  completo={indicadorLegado(dados, "custo").status === "COMPLETO"}
                  custosDesconhecidos={indicadorLegado(dados, "custo").componentesDesconhecidos}
                />
                <SaldosVisaoGeral
                  posicoes={dados.posicoes}
                  completo={dados.disponibilidade?.quantidadesCompletas}
                />
              </div>
            </>
          )}
          {dados.possuiRegistrosAteFim && (
            <ConferenciaVisaoGeral
              conferencia={dados.conferencia}
              dataFim={dados.dataSaldos}
              revisao={dados.revisao}
              onAtualizar={() => setConsulta({ ...consulta })}
              onOperacoes={onOperacoes}
              carteiraId={carteiraId}
            />
          )}
        </>
      )}
    </div>
  );
}
