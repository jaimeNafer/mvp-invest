import { useId } from "react";
import { moeda } from "./services/decimais";
import type { VisaoGeral } from "./services/visaoGeral";
import ResultadoMonetario from "./ResultadoMonetario";

const data = (valor: string) => valor.split("-").reverse().join("/");
const mes = (valor: string) => `${valor.slice(5, 7)}/${valor.slice(0, 4)}`;

export default function EvolucaoResultado({
  evolucao,
}: {
  evolucao: VisaoGeral["evolucaoResultado"];
}) {
  const tituloId = useId();
  const descricaoId = useId();
  const parcial = evolucao.meses.some((m) => m.acumuladoBrl === null);
  const valores = [
    evolucao.acumuladoInicialBrl,
    ...evolucao.meses.map((m) => m.acumuladoBrl ?? "0"),
  ];
  const numeros = valores.map(Number);
  const max = numeros.reduce((a, b) => Math.max(a, b), 0);
  const min = numeros.reduce((a, b) => Math.min(a, b), 0);
  const amplitude = max - min || 1;
  const y = (valor: number) => (max === min ? 80 : 24 + ((max - valor) / amplitude) * 112);
  const x = (indice: number) => 10 + (indice / Math.max(1, numeros.length - 1)) * 620;
  const pontos = numeros.map((v, i) => `${x(i)},${y(v)}`).join(" ");
  const frequenciaRotulos = Math.max(1, Math.ceil(evolucao.meses.length / 3));
  return (
    <section className="overview-panel overview-evolution" aria-labelledby={tituloId}>
      <h2 id={tituloId}>Resultado realizado acumulado</h2>
      <p className="overview-note">Acumulado a partir de zero no período selecionado.</p>
      {parcial && (
        <p className="overview-note">
          Resultado incompleto. Consulte os meses calculáveis na tabela.
        </p>
      )}
      {!parcial && (
        <div className="result-chart">
          <div className="chart-axis" aria-hidden="true">
            <span style={{ top: y(0) - 6 }}>R$ 0,00</span>
            {max > 0 && (
              <span style={{ top: y(max) - 18 }}>{moeda(valores[numeros.indexOf(max)])}</span>
            )}
            {min < 0 && (
              <span style={{ top: y(min) + 2 }}>{moeda(valores[numeros.indexOf(min)])}</span>
            )}
          </div>
          <div className="chart-plot">
            <svg
              viewBox="0 0 640 164"
              preserveAspectRatio="none"
              role="img"
              aria-label="Evolução mensal do resultado realizado"
              aria-describedby={descricaoId}
            >
              <title>Evolução mensal do resultado realizado</title>
              <desc id={descricaoId}>
                Valores em reais. Meses sem operações mantêm o acumulado. Consulte os valores na
                tabela mensal abaixo.
              </desc>
              <line
                x1="10"
                x2="630"
                y1={y(0)}
                y2={y(0)}
                className="chart-zero"
                vectorEffect="non-scaling-stroke"
              />
              <polyline
                points={pontos}
                className="chart-result"
                vectorEffect="non-scaling-stroke"
              />
              {evolucao.meses.map((m, i) => (
                <circle
                  key={m.dataInicio}
                  cx={x(i + 1)}
                  cy={y(numeros[i + 1])}
                  r="3"
                  className="chart-point"
                >
                  <title>{`${data(m.dataInicio)} a ${data(m.dataFim)}: ${moeda(m.acumuladoBrl ?? "0")} acumulado; ${moeda(m.resultadoBrl ?? "0")} no mês; ${m.operacoes} operações`}</title>
                </circle>
              ))}
            </svg>
            <div className="chart-months" aria-hidden="true">
              {evolucao.meses.map(
                (m, i) =>
                  ((i % frequenciaRotulos === 0 &&
                    i <= evolucao.meses.length - 1 - frequenciaRotulos) ||
                    i === evolucao.meses.length - 1) && (
                    <span
                      className="chart-month-label"
                      key={m.dataInicio}
                      style={
                        i === evolucao.meses.length - 1
                          ? { right: 0 }
                          : { left: `${(x(i + 1) / 640) * 100}%`, transform: "translateX(-50%)" }
                      }
                    >
                      {mes(m.dataInicio)}
                    </span>
                  ),
              )}
            </div>
          </div>
        </div>
      )}
      <details className="monthly-values">
        <summary>Valores por mês</summary>
        <div className="table-scroll">
          <table aria-label="Resultado mensal">
            <thead>
              <tr>
                <th>Período</th>
                <th>Operações</th>
                <th>Resultado líquido</th>
                <th>Acumulado</th>
              </tr>
            </thead>
            <tbody>
              {evolucao.meses.map((m) => (
                <tr key={m.dataInicio}>
                  <td>
                    {data(m.dataInicio)} a {data(m.dataFim)}
                  </td>
                  <td>{m.operacoes}</td>
                  <td>
                    <ResultadoMonetario valor={m.resultadoBrl} />
                  </td>
                  <td>
                    <ResultadoMonetario valor={m.acumuladoBrl} />
                  </td>
                </tr>
              ))}
            </tbody>
          </table>
        </div>
      </details>
    </section>
  );
}
