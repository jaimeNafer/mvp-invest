import { decimalPtBr } from "./services/decimais";
import ValorMonetario from "./ValorMonetario";
import type { Posicao } from "./services/operacoes";

export default function PosicoesAtuais({
  posicoes,
  previa = false,
}: {
  posicoes: Posicao[];
  previa?: boolean;
}) {
  return (
    <section
      className="operations-panel"
      aria-label={previa ? "Posição após a alteração" : "Posição atual global"}
    >
      <h2>{previa ? "Posição após a alteração" : "Posição atual global"}</h2>
      {!previa && (
        <p className="field-hint">
          Posição de todos os seus ativos e instituições. Os filtros do histórico não alteram esta
          posição.
        </p>
      )}
      {posicoes.length === 0 ? (
        <p className="empty-state">Nenhuma posição encontrada.</p>
      ) : (
        <div className="table-scroll">
          <table>
            <thead>
              <tr>
                <th>Ativo</th>
                <th>Quantidade</th>
                <th>Custo total</th>
                <th>Custo médio por unidade</th>
                <th>Custódia</th>
              </tr>
            </thead>
            <tbody>
              {posicoes.map((p) => (
                <tr key={p.ativoId}>
                  <td>{p.ativoTicker}</td>
                  <td>{decimalPtBr(p.quantidade)}</td>
                  <td>
                    <ValorMonetario valor={p.custoTotalBrl} />
                  </td>
                  <td>
                    <ValorMonetario valor={p.custoMedioBrl} />
                  </td>
                  <td>
                    {p.instituicoes.map((i) => (
                      <div key={i.instituicaoId}>
                        {i.instituicaoNome}: {decimalPtBr(i.quantidade)} {p.ativoTicker}
                      </div>
                    ))}
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
