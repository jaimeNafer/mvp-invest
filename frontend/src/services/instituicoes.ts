import { requisitarJson } from "./http";

export type Instituicao = { id: string; nome: string; ativo: boolean };

export function listarInstituicoes(signal?: AbortSignal): Promise<Instituicao[]> {
  return requisitarJson("/instituicoes", { signal });
}
