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
import type {
  PeriodoVisaoGeral,
  PontoPatrimonioDiario,
  VisaoGeral as Consolidacao,
} from "./services/visaoGeral";
import ValorMonetario from "./ValorMonetario";
import "./visao-geral.css";

function dataLegivel(data: string) {
  return data.split("-").reverse().join("/");
}

export default function VisaoGeral({ onOperacoes }: { onOperacoes: () => void }) {
  const [dados, setDados] = useState<Consolidacao | null>(null);
  const [patrimonio, setPatrimonio] = useState<PontoPatrimonioDiario[]>([]);
  const [patrimonioCarregado, setPatrimonioCarregado] = useState(false);
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
    if (!dados || carregandoPatrimonio || (patrimonioCarregado && !forcar)) return;
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
    void consultarPatrimonioDiario(periodoPatrimonio, controller.signal)
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
      setDados(null);
      setPatrimonio([]);
      setPatrimonioCarregado(false);
      setErroPatrimonio("");
    }
    void consultarVisaoGeral(consulta.periodo, controller.signal)
      .then((resposta) => {
        if (controller.signal.aborted) return;
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
  }, [consulta]);

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
      {carregando && (
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
          {dados.disponibilidade &&
            (!dados.disponibilidade.custoCompleto ||
              !dados.disponibilidade.resultadoCompleto ||
              !dados.disponibilidade.taxasCompletas) && (
              <p className="overview-note">
                Totais incompletos. Parcelas conhecidas: custo{" "}
                <ValorMonetario valor={dados.disponibilidade.subtotalCustoBrl} /> · resultado{" "}
                <ValorMonetario valor={dados.disponibilidade.subtotalResultadoBrl} /> · taxas{" "}
                <ValorMonetario valor={dados.disponibilidade.subtotalTaxasBrl} />. Valores
                desconhecidos permanecem indisponíveis.
              </p>
            )}
          {dados.disponibilidade?.comAjustes && (
            <p className="overview-note">
              Os saldos incluem ajustes provisórios. Ajustes não são aportes ou lucro.
            </p>
          )}
          {dados.possuiRegistrosAteFim && (
            <dl className="overview-indicators">
              <div>
                <dt>Custo dos saldos</dt>
                <dd>
                  <ValorMonetario valor={dados.resumo.custoSaldosBrl} />
                </dd>
              </div>
              <div>
                <dt>Resultado realizado líquido</dt>
                <dd>
                  <ResultadoMonetario valor={dados.resumo.resultadoRealizadoBrl} />
                </dd>
              </div>
              <div>
                <dt>Taxas</dt>
                <dd>
                  <ValorMonetario valor={dados.resumo.taxasBrl} />
                </dd>
              </div>
              <div>
                <dt>Operações no período</dt>
                <dd>{dados.resumo.operacoesNoPeriodo}</dd>
              </div>
            </dl>
          )}
          {dados.conciliacao && dados.conciliacao.total > 0 && (
            <ResumoConciliacao resumo={dados.conciliacao} onAbrirTimeline={onOperacoes} compact />
          )}
          {!dados.possuiRegistrosAteFim ? (
            <div className="overview-empty">
              <p>
                {dados.conciliacao && dados.conciliacao.total > 0
                  ? "Há registros recebidos aguardando conferência e aplicação."
                  : "Nenhum registro até a data final. Consulte outro período ou registre suas operações."}
              </p>
              <button className="primary-button" type="button" onClick={onOperacoes}>
                Ir para operações
              </button>
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
                key={JSON.stringify(consulta.periodo ?? {})}
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
                  completo={dados.disponibilidade?.custoCompleto}
                />
                <SaldosVisaoGeral
                  posicoes={dados.posicoes}
                  completo={dados.disponibilidade?.quantidadesCompletas}
                />
              </div>
            </>
          )}
          {dados.possuiRegistrosAteFim && (
            <section className="overview-panel overview-results" aria-labelledby="resultado-titulo">
              <div>
                <h2 id="resultado-titulo">Resultado do período</h2>
                <p>As taxas já estão consideradas no cálculo.</p>
              </div>
              <dl>
                <div>
                  <dt>Aportes</dt>
                  <dd>
                    <ValorMonetario valor={dados.resumo.aportesBrl} />
                  </dd>
                </div>
                <div>
                  <dt>Negociação</dt>
                  <dd>
                    <ValorMonetario valor={dados.resultado.negociacaoBrl} />
                  </dd>
                </div>
                <div>
                  <dt>Moeda</dt>
                  <dd>
                    <ValorMonetario valor={dados.resultado.moedaBrl} />
                  </dd>
                </div>
                <div>
                  <dt>Efeito das taxas</dt>
                  <dd>
                    <ValorMonetario valor={dados.resultado.taxaBrl} />
                  </dd>
                </div>
              </dl>
            </section>
          )}
          {dados.possuiRegistrosAteFim && (
            <ConferenciaVisaoGeral
              conferencia={dados.conferencia}
              dataFim={dados.dataSaldos}
              revisao={dados.revisao}
              onAtualizar={() => setConsulta({ ...consulta })}
              onOperacoes={onOperacoes}
            />
          )}
        </>
      )}
    </div>
  );
}
