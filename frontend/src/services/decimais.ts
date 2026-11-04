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

function arredondarDuasCasas(unidades: bigint, escala: bigint): bigint {
  const centesimos = (unidades * 100n) / escala;
  const resto = (unidades * 100n) % escala;
  return (
    centesimos +
    (resto * 2n > escala || (resto * 2n === escala && centesimos % 2n === 1n) ? 1n : 0n)
  );
}

export function percentual(valor: string): string {
  const negativo = valor.startsWith("-");
  const [inteira, fracao = ""] = (negativo ? valor.slice(1) : valor).split(".");
  const unidades = BigInt(inteira + fracao);
  const escala = 10n ** BigInt(fracao.length);
  if (unidades > 0n && unidades * 100n < escala) {
    return negativo ? "> -0,01%" : "< 0,01%";
  }
  const arredondado = arredondarDuasCasas(unidades, escala);
  const texto = arredondado.toString().padStart(3, "0");
  const sinal = negativo && arredondado > 0n ? "-" : "";
  return `${sinal}${texto.slice(0, -2)},${texto.slice(-2)}%`;
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
  const arredondado = arredondarDuasCasas(unidades, escala);
  const texto = arredondado.toString().padStart(3, "0");
  const sinal = negativo && arredondado > 0n ? "-" : "";
  return `R$ ${sinal}${texto.slice(0, -2).replace(/\B(?=(\d{3})+(?!\d))/g, ".")},${texto.slice(-2)}`;
}
