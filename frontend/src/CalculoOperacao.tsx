import { Fragment } from "react";
import AjudaCampo from "./AjudaCampo";
import ValorMonetario from "./ValorMonetario";
import type { Calculo } from "./services/operacoes";

const parcelas: [keyof Calculo, string][] = [
  ["valorBrutoBrl", "Valor bruto em reais"],
  ["taxaBrl", "Taxa em reais"],
  ["custoBaixadoBrl", "Custo de aquisição utilizado"],
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
          <dt>
            {campo === "custoBaixadoBrl" ? (
              <AjudaCampo
                rotulo={titulo}
                descricao="Custo histórico dos ativos ou moedas utilizados na operação, pelo custo médio anterior. Na venda: quantidade vendida × custo médio anterior. Se houver saída adicional de ativos para pagar taxas, o custo dessas unidades também é incluído."
              />
            ) : (
              titulo
            )}
          </dt>
          <dd>
            <ValorMonetario valor={calculo[campo]} />
          </dd>
        </Fragment>
      ))}
    </dl>
  );
}
