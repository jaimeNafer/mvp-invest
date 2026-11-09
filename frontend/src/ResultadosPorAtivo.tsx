import type { ResultadoAtivo } from "./services/visaoGeral";
import ResultadoMonetario from "./ResultadoMonetario";

export default function ResultadosPorAtivo({
  resultados,
  completo = true,
}: {
  resultados: ResultadoAtivo[];
  completo?: boolean;
}) {
  return (
    <section
      className="overview-panel overview-asset-results"
      aria-labelledby="ativos-resultado-titulo"
    >
      <h2 id="ativos-resultado-titulo">Resultado por ativo</h2>
      <p className="overview-note">
        {completo
          ? "Realizado líquido no período, do maior ao menor."
          : "Parcelas classificadas e calculáveis; não representam o resultado completo."}
      </p>
      {resultados.length === 0 ? (
        <p className="overview-note">
          {completo
            ? "Sem realizações no período."
            : "Há movimentações em conferência; os resultados ainda não estão completos."}
        </p>
      ) : (
        <table aria-labelledby="ativos-resultado-titulo">
          <thead>
            <tr>
              <th>Ativo</th>
              <th>{completo ? "Resultado líquido" : "Subtotal conhecido"}</th>
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
                  <ResultadoMonetario
                    valor={
                      completo
                        ? r.resultadoTotalBrl
                        : r.subtotalResultadoBrl === undefined
                          ? r.resultadoTotalBrl
                          : r.subtotalResultadoBrl
                    }
                  />
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
