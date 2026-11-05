import type { ResultadoAtivo } from "./services/visaoGeral";
import ResultadoMonetario from "./ResultadoMonetario";

export default function ResultadosPorAtivo({ resultados }: { resultados: ResultadoAtivo[] }) {
  return (
    <section
      className="overview-panel overview-asset-results"
      aria-labelledby="ativos-resultado-titulo"
    >
      <h2 id="ativos-resultado-titulo">Resultado por ativo</h2>
      <p className="overview-note">Realizado líquido no período, do maior ao menor.</p>
      {resultados.length === 0 ? (
        <p className="overview-note">Sem realizações no período.</p>
      ) : (
        <table aria-labelledby="ativos-resultado-titulo">
          <thead>
            <tr>
              <th>Ativo</th>
              <th>Resultado líquido</th>
            </tr>
          </thead>
          <tbody>
            {resultados.map((r) => (
              <tr key={r.ativoId}>
                <td>
                  <strong>{r.ativoTicker}</strong>
                  <small>{r.ativoNome}</small>
                </td>
                <td>
                  <ResultadoMonetario valor={r.resultadoTotalBrl} />
                  <details>
                    <summary>Parcelas</summary>
                    <dl className="asset-result-parcels">
                      <dt>Negociação</dt>
                      <dd>
                        <ResultadoMonetario valor={r.negociacaoBrl} />
                      </dd>
                      <dt>Moeda</dt>
                      <dd>
                        <ResultadoMonetario valor={r.moedaBrl} />
                      </dd>
                      <dt>Efeito das taxas</dt>
                      <dd>
                        <ResultadoMonetario valor={r.taxaBrl} />
                      </dd>
                    </dl>
                  </details>
                </td>
              </tr>
            ))}
          </tbody>
        </table>
      )}
    </section>
  );
}
