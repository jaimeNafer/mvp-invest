import { requisitarJson } from "./http";
export type DecisoesImportacao = {
  aportesProprios: boolean;
  taxasIncluidas: boolean;
  posicaoAnteriorZerada: boolean;
};
export type EventoImportacao = {
  linha: number;
  referencia: string;
  tipo: string;
  ocorridaEm: string;
  ativo: string;
  quantidade: string;
  principal: string | null;
  taxa: string;
  situacao: "NOVO" | "RECONHECIDO" | "CONFLITO";
};
export type PreviaImportacao = {
  hash: string;
  revisao: string;
  previaId: string;
  novos: number;
  reconhecidos: number;
  erros: { linha: number; mensagem: string }[];
  eventos: EventoImportacao[];
};
export type ResumoImportacao = {
  id: string;
  hash: string;
  nomeArquivo: string;
  criadoEm: string;
  novos: number;
  reconhecidos: number;
};
export type ReciboImportacao = ResumoImportacao & {
  eventos: { linha: number; referencia: string; operacaoId: string }[];
};
export type ListaImportacoes = {
  itens: ResumoImportacao[];
  total: number;
  pagina: number;
  tamanho: number;
};
export type ComandoImportacao = { corpo: FormData; chave: string };
export function arquivoImportacao(arquivo: File, decisoes: DecisoesImportacao) {
  const corpo = new FormData();
  corpo.set("arquivo", arquivo);
  for (const [nome, valor] of Object.entries(decisoes)) corpo.set(nome, String(valor));
  return corpo;
}
export const analisarImportacao = (corpo: FormData, signal: AbortSignal) =>
  requisitarJson<PreviaImportacao>("/importacoes/previa", { method: "POST", body: corpo, signal });
export function prepararImportacao(
  arquivo: File,
  decisoes: DecisoesImportacao,
  previa: PreviaImportacao,
): ComandoImportacao {
  const corpo = arquivoImportacao(arquivo, decisoes);
  corpo.set("hash", previa.hash);
  corpo.set("revisao", previa.revisao);
  corpo.set("previaId", previa.previaId);
  return { corpo, chave: crypto.randomUUID() };
}
export const confirmarImportacao = (comando: ComandoImportacao) =>
  requisitarJson<ReciboImportacao>("/importacoes", {
    method: "POST",
    body: comando.corpo,
    headers: { "Idempotency-Key": comando.chave },
    signal: AbortSignal.timeout(120000),
  });
export const listarImportacoes = (pagina: number, signal?: AbortSignal) =>
  requisitarJson<ListaImportacoes>(`/importacoes?pagina=${pagina}&tamanho=20`, { signal });
export const obterImportacao = (id: string, signal: AbortSignal) =>
  requisitarJson<ReciboImportacao>(`/importacoes/${encodeURIComponent(id)}`, { signal });

export const listarArquivosRecebidos = (pagina: number, signal?: AbortSignal) =>
  requisitarJson<{
    itens: { id: string; nomeArquivo: string; criadoEm: string; registros: number }[];
    pagina: number;
    tamanho: number;
    total: number;
  }>(`/importacoes/recebimentos?pagina=${pagina}&tamanho=20`, { signal });

export const excluirRecebimento = (id: string) =>
  requisitarJson<void>(`/importacoes/recebimentos/${encodeURIComponent(id)}`, { method: "DELETE" });
