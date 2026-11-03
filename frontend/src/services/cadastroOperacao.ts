import { decimalCanonico } from "./decimais";
import type { CadastroOperacao, Operacao, PedidoPrevia, TipoNegociacao } from "./operacoes";
import type { Ativo } from "./saldosIniciais";

export type CamposOperacao = {
  tipo: TipoNegociacao;
  instituicaoId: string;
  ativoId: string;
  data: string;
  quantidade: string;
  moedaCotacaoId: string;
  totalBruto: string;
  cotacaoBrl: string;
  taxaAtivoId: string;
  taxaQuantidade: string;
  referencia: string;
  motivo: string;
};
function positivo(texto: string) {
  const decimal = decimalCanonico(texto);
  if (!/[1-9]/.test(decimal)) throw new Error("Informe um valor maior que zero.");
  return decimal;
}
export function prepararPedidoPrevia(
  campos: CamposOperacao,
  ativos: Ativo[],
  operacao: Operacao | null,
  cancelar: boolean,
): PedidoPrevia | null {
  if (cancelar)
    return operacao?.id && campos.motivo.trim()
      ? { acao: "CANCELAR", operacaoId: operacao.id, motivo: campos.motivo.trim() }
      : null;
  const brl = ativos.find((a) => a.codigo === "fiat:BRL");
  const ativoId = campos.tipo === "APORTE" ? brl?.id : campos.ativoId;
  if (
    !campos.instituicaoId ||
    !ativoId ||
    !/^\d{4}-\d{2}-\d{2}T\d{2}:\d{2}(?::\d{2})?$/.test(campos.data) ||
    (operacao && !campos.motivo.trim())
  )
    return null;
  try {
    const entrada: CadastroOperacao = {
      tipo: campos.tipo,
      instituicaoId: campos.instituicaoId,
      ativoId,
      ocorridaEm: `${campos.data.length === 16 ? `${campos.data}:00` : campos.data}-03:00`,
      quantidade: positivo(campos.quantidade),
      ...(campos.referencia.trim() ? { referencia: campos.referencia.trim() } : {}),
      ...(operacao ? { motivo: campos.motivo.trim() } : {}),
    };
    if (campos.tipo !== "APORTE") {
      if (!campos.moedaCotacaoId || campos.moedaCotacaoId === ativoId) return null;
      entrada.moedaCotacaoId = campos.moedaCotacaoId;
      entrada.totalBruto = positivo(campos.totalBruto);
      entrada.cotacaoBrl = campos.moedaCotacaoId === brl?.id ? "1" : positivo(campos.cotacaoBrl);
      if (campos.taxaAtivoId) {
        if (![ativoId, campos.moedaCotacaoId].includes(campos.taxaAtivoId)) return null;
        entrada.taxaAtivoId = campos.taxaAtivoId;
        entrada.taxaQuantidade = positivo(campos.taxaQuantidade);
      }
    }
    return operacao?.id
      ? { acao: "SUBSTITUIR", operacaoId: operacao.id, operacao: entrada }
      : { acao: "CRIAR", operacao: entrada };
  } catch {
    return null;
  }
}
