import { decimalPtBr, moeda } from "./services/decimais";

export default function ValorMonetario({ valor }: { valor: string }) {
  const completo = `Valor completo: R$ ${decimalPtBr(valor)}`;
  return <span title={completo}>{moeda(valor)}</span>;
}
