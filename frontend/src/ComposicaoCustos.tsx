import { decimalPtBr, percentual } from "./services/decimais";
import type { PosicaoVisaoGeral } from "./services/visaoGeral";
import ValorMonetario from "./ValorMonetario";

export default function ComposicaoCustos({
  posicoes,
  completo = true,
}: {
  posicoes: PosicaoVisaoGeral[];
  completo?: boolean;
}) {
  const possuiComposicao = completo && posicoes.some((p) => p.participacaoPercentual !== null);
  return (
    <section className="overview-panel overview-composition" aria-labelledby="composicao-titulo">
      <h2 id="composicao-titulo">Composição a custo</h2>
      <p className="overview-note composition-caption">
        Distribuição pelo custo de aquisição, sem cotação de mercado.
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
          {!completo || posicoes.some((p) => p.custoTotalBrl === null)
            ? "Composição indisponível enquanto houver custos desconhecidos."
            : "Não há custo positivo para distribuir entre os ativos."}
        </p>
      )}
    </section>
  );
}
