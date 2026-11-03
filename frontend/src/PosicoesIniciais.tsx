import { decimalPtBr, moeda } from "./services/decimais";
import type { PosicaoInicial } from "./services/saldosIniciais";
export default function PosicoesIniciais({ posicoes }: { posicoes: PosicaoInicial[] }) {
  return (
    <section className="operations-panel" aria-label="Posição inicial global">
      <h2>Posição inicial global</h2>
      <p className="field-hint">
        Todos os seus saldos iniciais confirmados, em todas as instituições. Os filtros da listagem
        não alteram esta base.
      </p>
      {posicoes.length === 0 ? (
        <p className="empty-state">Nenhuma posição inicial confirmada.</p>
      ) : (
        <div className="table-scroll">
          <table>
            <thead>
              <tr>
                <th>Ativo</th>
                <th>Quantidade total</th>
                <th>Custo total</th>
                <th>Custo médio por unidade</th>
              </tr>
            </thead>
            <tbody>
              {posicoes.map((p) => (
                <tr key={p.ativoId}>
                  <td>{p.ativoTicker}</td>
                  <td>{decimalPtBr(p.quantidade)}</td>
                  <td title={`Valor completo: R$ ${decimalPtBr(p.custoTotalBrl)}`}>
                    {moeda(p.custoTotalBrl)}
                  </td>
                  <td title={`Valor completo: R$ ${decimalPtBr(p.custoMedioBrl)}`}>
                    {moeda(p.custoMedioBrl)}
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
