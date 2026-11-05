import { decimalPtBr } from "./services/decimais";
import type { SaldoMoeda } from "./services/visaoGeral";

export default function SaldosMoedas({ saldos }: { saldos: SaldoMoeda[] }) {
  return (
    <section className="overview-currencies overview-panel" aria-label="Saldos em moeda">
      <h2>Saldos em moeda</h2>
      {saldos.length === 0 ? (
        <p className="overview-note">Sem saldo registrado em moedas de pagamento.</p>
      ) : (
        <dl>
          {saldos.map((s) => (
            <div key={s.ativoId}>
              <dt title={s.ativoNome}>{s.ativoTicker}</dt>
              <dd>{decimalPtBr(s.quantidade)}</dd>
            </div>
          ))}
        </dl>
      )}
      <p className="overview-note">
        Quantidades registradas; bloqueios da instituição não são informados.
      </p>
    </section>
  );
}
