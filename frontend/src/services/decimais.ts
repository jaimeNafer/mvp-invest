export function decimalCanonico(texto: string): string {
  const valor = texto.trim();
  if (!/^(?:0|[1-9]\d*|[1-9]\d{0,2}(?:\.\d{3})+)(?:,\d+)?$/.test(valor)) {
    throw new Error("Informe valores válidos, usando vírgula para as casas decimais.");
  }
  return valor.replaceAll(".", "").replace(",", ".");
}

export function decimalPtBr(valor: string): string {
  const [inteira, fracao = ""] = valor.split(".");
  const casas = fracao.replace(/0+$/, "");
  return `${inteira.replace(/\B(?=(\d{3})+(?!\d))/g, ".")}${casas ? `,${casas}` : ""}`;
}

export function moeda(valor: string): string {
  const negativo = valor.startsWith("-");
  const absoluto = negativo ? valor.slice(1) : valor;
  const [inteira, fracao = ""] = absoluto.split(".");
  const unidades = BigInt(inteira + fracao);
  const escala = 10n ** BigInt(fracao.length);
  if (unidades > 0n && unidades * 100n < escala) {
    return negativo ? "entre R$ -0,01 e R$ 0,00" : "menor que R$ 0,01";
  }
  const centavos = (unidades * 100n) / escala;
  const resto = (unidades * 100n) % escala;
  const arredondado =
    centavos + (resto * 2n > escala || (resto * 2n === escala && centavos % 2n === 1n) ? 1n : 0n);
  const texto = arredondado.toString().padStart(3, "0");
  const sinal = negativo && arredondado > 0n ? "-" : "";
  return `R$ ${sinal}${texto.slice(0, -2).replace(/\B(?=(\d{3})+(?!\d))/g, ".")},${texto.slice(-2)}`;
}
