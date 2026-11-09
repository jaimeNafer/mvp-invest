import { decimalPtBr, percentual } from "./services/decimais";
import type { PosicaoVisaoGeral } from "./services/visaoGeral";
import ValorMonetario from "./ValorMonetario";

export default function ComposicaoCustos({
  posicoes,
  completo = true,
  custosDesconhecidos = 0,
}: {
  posicoes: PosicaoVisaoGeral[];
  completo?: boolean;
  custosDesconhecidos?: number;
}) {
  const possuiComposicao = posicoes.some(
    (p) => p.custoTotalBrl !== null && p.participacaoPercentual !== null,
  );
  return (
    <section className="overview-panel overview-composition" aria-labelledby="composicao-titulo">
      <h2 id="composicao-titulo">Composição a custo</h2>
      <p className="overview-note composition-caption">
        {completo
          ? "Distribuição pelo custo de aquisição, sem cotação de mercado."
          : `Distribuição da parcela com custo identificado${custosDesconhecidos ? `; ${custosDesconhecidos} ${custosDesconhecidos === 1 ? "ativo sem custo identificado" : "ativos sem custo identificado"}` : ""}.`}
      </p>
      {possuiComposicao ? (
        <ul>
          {posicoes
            .filter((p) => p.participacaoPercentual !== null)
            .map((p) => (
              <li key={p.ativoId}>
                <div className="composition-label">
                  <strong>{p.ativoTicker}</strong>
                  <span
                    title={`Percentual completo: ${decimalPtBr(p.participacaoPercentual ?? "0")}%`}
                  >
                    {percentual(p.participacaoPercentual ?? "0")}
                  </span>
                </div>
                <div className="composition-track" aria-hidden="true">
                  <div style={{ width: `${p.participacaoPercentual ?? "0"}%` }} />
                </div>
                <small>
                  <ValorMonetario valor={p.custoTotalBrl} />
                </small>
              </li>
            ))}
        </ul>
      ) : (
        <p className="overview-note">
          {posicoes.some((p) => p.custoTotalBrl === null)
            ? "Os custos conhecidos estão disponíveis na tabela de saldos."
            : "Não há custo positivo para distribuir entre os ativos."}
        </p>
      )}
    </section>
  );
}
