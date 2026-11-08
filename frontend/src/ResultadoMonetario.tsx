import ValorMonetario from "./ValorMonetario";

export default function ResultadoMonetario({ valor }: { valor: string | null }) {
  if (valor === null) return <ValorMonetario valor={null} />;
  const sinal = valor.startsWith("-") ? -1 : /[1-9]/.test(valor) ? 1 : 0;
  return (
    <span className={sinal < 0 ? "result-negative" : sinal > 0 ? "result-positive" : undefined}>
      {sinal > 0 && "+"}
      <ValorMonetario valor={valor} />
    </span>
  );
}
