function nanossegundos(valor: string): bigint | null {
  const partes = /^(.*:\d{2})(?:\.(\d{1,9}))?Z$/.exec(valor);
  if (!partes) return null;
  const inicio = Date.parse(`${partes[1]}Z`);
  if (!Number.isFinite(inicio)) return null;
  return BigInt(inicio) * 1000000n + BigInt((partes[2] ?? "").padEnd(9, "0"));
}

export function diferencaSegundos(valor: string, referencia: string): string | null {
  const instante = nanossegundos(valor);
  const origem = nanossegundos(referencia);
  if (instante === null || origem === null) return null;
  const delta = instante - origem;
  const absoluto = delta < 0n ? -delta : delta;
  const inteiro = absoluto / 1000000000n;
  const fracao = (absoluto % 1000000000n).toString().padStart(9, "0").replace(/0+$/, "");
  return `${delta > 0n ? "+" : delta < 0n ? "-" : ""}${inteiro}${fracao ? `.${fracao}` : ""}`;
}
