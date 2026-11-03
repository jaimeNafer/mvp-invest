import { requisitarJson } from "./http";

export type Ativo = {
  id: string;
  codigo: string;
  ticker: string;
  nome: string;
  tipo: "FIAT" | "CRIPTO";
  casasDecimais: number;
  ativo: boolean;
};
export type CadastroSaldo = {
  instituicaoId: string;
  ativoId: string;
  dataReferencia: string;
  quantidade: string;
  custoTotalBrl: string;
  custoZeroConfirmado: boolean;
  justificativaCustoZero: string | null;
  motivo?: string;
};
export type Saldo = {
  id: string;
  instituicaoId: string;
  instituicaoNome: string;
  ativoId: string;
  ativoTicker: string;
  dataReferencia: string;
  quantidade: string;
  custoTotalBrl: string;
  situacao: "CONFIRMADA" | "SUBSTITUIDA" | "CANCELADA";
  substituiId: string | null;
  justificativaCustoZero: string | null;
  motivoAlteracao: string | null;
  criadoEm: string;
  alteradoEm: string;
};
export type ListaSaldos = {
  itens: Saldo[];
  pagina: number;
  tamanho: number;
  total: number;
  dataReferencia: string | null;
  possuiSaldosConfirmados: boolean;
};
export type PosicaoInicial = {
  ativoId: string;
  ativoTicker: string;
  quantidade: string;
  custoTotalBrl: string;
  custoMedioBrl: string;
  instituicoes: {
    instituicaoId: string;
    instituicaoNome: string;
    quantidade: string;
    custoInicialInformadoBrl: string;
  }[];
};
export type FiltrosSaldo = {
  pagina: number;
  instituicaoId: string;
  ativoId: string;
  carteiraIds: string[];
  situacao: string;
};

export const listarAtivos = (signal?: AbortSignal) =>
  requisitarJson<Ativo[]>("/ativos", { signal });
export const listarPosicoesIniciais = (signal?: AbortSignal) =>
  requisitarJson<PosicaoInicial[]>("/posicoes-iniciais", { signal });
export const obterSaldo = (id: string) => requisitarJson<Saldo>(`/saldos-iniciais/${id}`);

export function listarSaldos(filtro: FiltrosSaldo, signal?: AbortSignal) {
  const params = new URLSearchParams({
    pagina: String(filtro.pagina),
    tamanho: "50",
    situacao: filtro.situacao,
  });
  if (filtro.instituicaoId) params.set("instituicaoId", filtro.instituicaoId);
  if (filtro.ativoId) params.set("ativoId", filtro.ativoId);
  for (const id of filtro.carteiraIds) params.append("carteiraIds", id);
  return requisitarJson<ListaSaldos>(`/saldos-iniciais?${params}`, { signal });
}

export function gravarSaldo(entrada: CadastroSaldo, chave: string, anterior?: string) {
  const caminho = anterior ? `/saldos-iniciais/${anterior}/substituicoes` : "/saldos-iniciais";
  return requisitarJson<Saldo>(caminho, {
    method: "POST",
    headers: { "Idempotency-Key": chave },
    body: JSON.stringify(entrada),
  });
}

export const cancelarSaldo = (id: string, motivo: string) =>
  requisitarJson<Saldo>(`/saldos-iniciais/${id}/cancelamento`, {
    method: "POST",
    body: JSON.stringify({ motivo }),
  });
