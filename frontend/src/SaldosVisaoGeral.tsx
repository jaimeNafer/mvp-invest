import { decimalPtBr } from "./services/decimais";
import type { PosicaoVisaoGeral } from "./services/visaoGeral";
import ValorMonetario from "./ValorMonetario";

export default function SaldosVisaoGeral({ posicoes }: { posicoes: PosicaoVisaoGeral[] }) {
  return (
    <section className="overview-panel overview-balances" aria-labelledby="saldos-titulo">
      <h2 id="saldos-titulo">Saldos e custos</h2>
      {posicoes.length === 0 ? (
        <p className="overview-note">Sem posições na data final.</p>
      ) : (
        <div className="table-scroll">
          <table aria-labelledby="saldos-titulo">
            <thead>
              <tr>
                <th>Ativo</th>
                <th>Quantidade</th>
                <th>Custo médio</th>
                <th>Custo total</th>
                <th>Custódia</th>
              </tr>
            </thead>
            <tbody>
              {posicoes.map((p) => (
                <tr key={p.ativoId}>
                  <td>
                    <strong>{p.ativoTicker}</strong>
                    <small>{p.ativoNome}</small>
                  </td>
                  <td>{decimalPtBr(p.quantidade)}</td>
                  <td>
                    <ValorMonetario valor={p.custoMedioBrl} />
                  </td>
                  <td>
                    <ValorMonetario valor={p.custoTotalBrl} />
                  </td>
                  <td>
                    {p.instituicoes.length === 1 ? (
                      p.instituicoes[0].instituicaoNome
                    ) : (
                      <details>
                        <summary>{p.instituicoes.length} instituições</summary>
                        <ul>
                          {p.instituicoes.map((i) => (
                            <li key={i.instituicaoId}>
                              <span>{i.instituicaoNome}</span>: {decimalPtBr(i.quantidade)}{" "}
                              {p.ativoTicker}
                            </li>
                          ))}
                        </ul>
                      </details>
                    )}
                  </td>
                </tr>
              ))}
            </tbody>
          </table>
        </div>
      )}
    </section>
  );
}
