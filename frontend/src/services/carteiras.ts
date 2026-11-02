import { requisitarJson } from "./http";

export type Conta = { id: string; instituicaoId: string; instituicaoNome: string };
export type Carteira = { id: string; nome: string; ativo: boolean; contas: Conta[] };
export type CadastroCarteira = { nome: string; instituicaoIds: string[] };

export function listarCarteiras(signal?: AbortSignal): Promise<Carteira[]> {
  return requisitarJson("/carteiras", { signal });
}

export function cadastrarCarteira(cadastro: CadastroCarteira): Promise<Carteira> {
  return requisitarJson("/carteiras", { method: "POST", body: JSON.stringify(cadastro) });
}

export function alterarCarteira(id: string, cadastro: CadastroCarteira): Promise<Carteira> {
  return requisitarJson(`/carteiras/${encodeURIComponent(id)}`, {
    method: "PUT",
    body: JSON.stringify(cadastro),
  });
}

export function alterarSituacaoCarteira(id: string, ativo: boolean): Promise<Carteira> {
  return requisitarJson(`/carteiras/${encodeURIComponent(id)}/situacao`, {
    method: "PATCH",
    body: JSON.stringify({ ativo }),
  });
}
