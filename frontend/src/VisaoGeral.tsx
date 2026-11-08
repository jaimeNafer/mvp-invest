import ResumoConciliacao from "./ResumoConciliacao";
import { useEffect, useState } from "react";
import ComposicaoCustos from "./ComposicaoCustos";
import ConferenciaVisaoGeral from "./ConferenciaVisaoGeral";
import EvolucaoResultado from "./EvolucaoResultado";
import ResultadoMonetario from "./ResultadoMonetario";
import ResultadosPorAtivo from "./ResultadosPorAtivo";
import SaldosMoedas from "./SaldosMoedas";
import SaldosVisaoGeral from "./SaldosVisaoGeral";
import { consultarVisaoGeral } from "./services/visaoGeral";
import type { PeriodoVisaoGeral, VisaoGeral as Consolidacao } from "./services/visaoGeral";
import ValorMonetario from "./ValorMonetario";
import "./visao-geral.css";

function dataLegivel(data: string) {
  return data.split("-").reverse().join("/");
}

export default function VisaoGeral({ onOperacoes }: { onOperacoes: () => void }) {
  const [dados, setDados] = useState<Consolidacao | null>(null);
  const [periodo, setPeriodo] = useState<PeriodoVisaoGeral>({ dataInicio: "", dataFim: "" });
  const [consulta, setConsulta] = useState<{ periodo?: PeriodoVisaoGeral }>({});
  const [carregando, setCarregando] = useState(true);
  const [erro, setErro] = useState("");

  useEffect(() => {
    const controller = new AbortController();
    setCarregando(true);
    setErro("");
    setDados(null);
    void consultarVisaoGeral(consulta.periodo, controller.signal)
      .then((resposta) => {
        if (controller.signal.aborted) return;
        setDados(resposta);
        if (!consulta.periodo)
          setPeriodo({ dataInicio: resposta.dataInicio, dataFim: resposta.dataFim });
      })
      .catch((falha: unknown) => {
        if (!controller.signal.aborted)
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
            setConsulta({ periodo: { ...periodo } });
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
