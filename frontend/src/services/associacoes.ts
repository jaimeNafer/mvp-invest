import { requisitarJson } from "./http";

export type PapelAssociacao = "RECEBIMENTO" | "PAGAMENTO" | "TAXA" | "NAO_DEFINIDO";
export type LinhaAssociacaoResponse = {
  id: string;
  versao: string;
  data: string | null;
  instituicaoId: string;
  contaId: string | null;
  ativoTicker: string;
  quantidade: string | null;
  operacao: string;
  referencia: string;
  papel: PapelAssociacao;
  grupoNumero: string | null;
  arquivo: string | null;
  linha: number | null;
  aba: string | null;
};
export type ParcelaAssociacao = { ativoTicker: string; quantidade: string };
export type GrupoAssociacaoResponse = {
  revisao: string;
  previaId: string;
  numero: string | null;
  versao: string | null;
  linhas: LinhaAssociacaoResponse[];
  natureza: "COMPRA" | "VENDA" | "CONVERSAO" | null;
  recebimentos: ParcelaAssociacao[];
  pagamentos: ParcelaAssociacao[];
  taxas: ParcelaAssociacao[];
  principalBrl: string | null;
  custoFinalBrl: string | null;
  pendencias: string[];
  resolvidas: string[];
  completo: boolean;
  desfazer: boolean;
  taxaZeroConfirmada?: boolean;
  cotacaoBrl?: string | null;
};
export type CandidatosAssociacaoResponse = {
  revisao: string;
  linhas: LinhaAssociacaoResponse[];
  grupo: GrupoAssociacaoResponse | null;
  avisos?: string[];
  janelaMilissegundos?: number;
};
export type LinhaAssociacaoRequest = { id: string; versao: string; papel: PapelAssociacao };
export type RequestAssociacao = {
  revisao: string;
  numero: string | null;
  versao: string | null;
  linhas: LinhaAssociacaoRequest[];
  taxaZeroConfirmada: boolean;
  cotacaoBrl: string | null;
  desfazer: boolean;
};
export type RequestAssociacaoPrevia = RequestAssociacao & { previaId?: string };

export function listarCandidatosAssociacao(registroId: string, signal?: AbortSignal) {
  const parametros = new URLSearchParams({ registroId });
  return requisitarJson<CandidatosAssociacaoResponse>(
    `/conciliacao/associacoes/candidatos?${parametros}`,
    { signal },
  );
}

export function obterGrupoAssociacao(numero: string, signal?: AbortSignal) {
  return requisitarJson<GrupoAssociacaoResponse>(
    `/conciliacao/associacoes/${encodeURIComponent(numero)}`,
    { signal },
  );
}

export function obterPreviaAssociacao(dados: RequestAssociacao, signal?: AbortSignal) {
  return requisitarJson<GrupoAssociacaoResponse>("/conciliacao/associacoes/previa", {
    method: "POST",
    body: JSON.stringify(dados),
    signal,
  });
}
