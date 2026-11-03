import { Fragment } from "react";
import ValorMonetario from "./ValorMonetario";
import type { Calculo } from "./services/operacoes";

const parcelas: [keyof Calculo, string][] = [
  ["valorBrutoBrl", "Valor bruto em reais"],
  ["taxaBrl", "Taxa em reais"],
  ["custoBaixadoBrl", "Custo baixado"],
  ["resultadoNegociacaoBrl", "Resultado da negociação"],
  ["resultadoMoedaBrl", "Resultado da moeda de cotação"],
  ["resultadoTaxaBrl", "Resultado da moeda da taxa"],
  ["resultadoTotalBrl", "Resultado total"],
];
export default function CalculoOperacao({ calculo }: { calculo: Calculo }) {
  return (
    <dl className="saldo-resumo">
      {parcelas.map(([campo, titulo]) => (
        <Fragment key={campo}>
          <dt>{titulo}</dt>
          <dd>
            <ValorMonetario valor={calculo[campo]} />
          </dd>
        </Fragment>
      ))}
    </dl>
  );
}
