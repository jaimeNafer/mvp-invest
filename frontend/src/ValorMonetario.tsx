import { decimalPtBr, moeda } from "./services/decimais";

export default function ValorMonetario({ valor }: { valor: string | null }) {
  if (valor === null)
    return <span title="Faltam informações para calcular este valor.">Indisponível</span>;
  const completo = `Valor completo: R$ ${decimalPtBr(valor)}`;
  return <span title={completo}>{moeda(valor)}</span>;
}
