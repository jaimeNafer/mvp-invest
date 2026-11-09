import { requisitarJson } from "./http";
import type { RegistroTimeline } from "./conciliacao";

export type TipoNegociacao = "APORTE" | "COMPRA" | "VENDA";
export type TipoOperacao = "SALDO_INICIAL" | TipoNegociacao;
export type SituacaoOperacao = "CONFIRMADA" | "SUBSTITUIDA" | "CANCELADA";
export type CadastroOperacao = {
  tipo: TipoNegociacao;
  instituicaoId: string;
  ativoId: string;
  ocorridaEm: string;
  quantidade: string;
  moedaCotacaoId?: string;
  totalBruto?: string;
  cotacaoBrl?: string;
  taxaAtivoId?: string;
  taxaQuantidade?: string;
  referencia?: string;
  motivo?: string;
};
export type Calculo = {
  valorBrutoBrl: string | null;
  taxaBrl: string | null;
  custoBaixadoBrl: string | null;
  resultadoNegociacaoBrl: string | null;
  resultadoMoedaBrl: string | null;
  resultadoTaxaBrl: string | null;
  resultadoTotalBrl: string | null;
};
export type Operacao = {
  id: string | null;
  tipo: TipoOperacao;
  instituicaoId: string;
  instituicaoNome: string;
  ativoId: string;
  ativoTicker: string;
  ocorridaEm: string;
  quantidade: string;
  custoTotalBrl: string | null;
  moedaCotacaoId: string | null;
  moedaCotacaoTicker: string | null;
  totalBruto: string | null;
  precoUnitario?: string | null;
  cotacaoBrl: string | null;
  taxaAtivoId: string | null;
  taxaAtivoTicker: string | null;
  taxaQuantidade: string;
  referencia: string | null;
  situacao: SituacaoOperacao;
  substituiId: string | null;
  motivoAlteracao: string | null;
  criadoEm: string;
  alteradoEm: string;
  calculo: Calculo | null;
};
export type Posicao = {
  ativoId: string;
  ativoTicker: string;
  quantidade: string;
  custoTotalBrl: string | null;
  custoMedioBrl: string | null;
  quantidadeDisponivel?: boolean;
  comAjustes?: boolean;
  causas?: string[];
  instituicoes: { instituicaoId: string; instituicaoNome: string; quantidade: string }[];
};
export type FiltrosOperacoes = {
  tipo:
    | TipoOperacao
    | "RETIRADA"
    | "CONVERSAO"
    | "CREDITO_AUTOMATICO"
    | "SAIDA_CRIPTO"
    | "MOVIMENTACAO_INTERNA"
    | "";
  situacao: SituacaoOperacao | "TODAS";
  instituicaoId: string;
  ativoId: string;
  carteiraIds: string[];
  pagina: number;
};
export type ListaOperacoes = {
  itens: Operacao[];
  total: number;
  pagina: number;
  tamanho: number;
  revisao: string;
  dataReferencia: string | null;
};
export type ItemHistoricoOperacao =
  | { operacao: Operacao; importada: null; linhasOriginais: [] }
  | { operacao: null; importada: RegistroTimeline; linhasOriginais: RegistroTimeline[] };
export type HistoricoOperacoes = {
  itens: ItemHistoricoOperacao[];
  total: number;
  pagina: number;
  tamanho: number;
};
export type PedidoPrevia =
  | { acao: "CRIAR"; operacao: CadastroOperacao }
  | { acao: "SUBSTITUIR"; operacaoId: string; operacao: CadastroOperacao }
  | { acao: "CANCELAR"; operacaoId: string; motivo: string };
export type Previa = {
  revisao: string;
  operacao: Operacao | null;
  calculo: Calculo | null;
  posicoes: Posicao[];
};
export type Confirmacao =
  | { revisao: string; operacao: CadastroOperacao }
  | { revisao: string; motivo: string };
export type ComandoOperacao = { caminho: string; entrada: Confirmacao; chave: string };

function parametrosOperacoes(filtros: FiltrosOperacoes) {
  const params = new URLSearchParams({
    pagina: String(filtros.pagina),
    tamanho: "50",
    situacao: filtros.situacao,
  });
  if (filtros.tipo) params.set("tipo", filtros.tipo);
  if (filtros.instituicaoId) params.set("instituicaoId", filtros.instituicaoId);
  if (filtros.ativoId) params.set("ativoId", filtros.ativoId);
  for (const id of filtros.carteiraIds) params.append("carteiraIds", id);
  return params;
}
export function listarOperacoes(filtros: FiltrosOperacoes, signal?: AbortSignal) {
  return requisitarJson<ListaOperacoes>(`/operacoes?${parametrosOperacoes(filtros)}`, { signal });
}
export function listarHistoricoOperacoes(
  filtros: FiltrosOperacoes,
  arquivoId?: string,
  signal?: AbortSignal,
) {
  const params = parametrosOperacoes(filtros);
  if (arquivoId) params.set("arquivoId", arquivoId);
  return requisitarJson<HistoricoOperacoes>(`/operacoes/historico?${params}`, { signal });
}
export const listarPosicoes = (signal?: AbortSignal) =>
  requisitarJson<{ revisao: string; itens: Posicao[]; quantidadesCompletas?: boolean }>(
    "/posicoes",
    { signal },
  );
export const obterOperacao = (id: string, signal?: AbortSignal) =>
  requisitarJson<Operacao>(`/operacoes/${encodeURIComponent(id)}`, { signal });
export const consultarPrevia = (pedido: PedidoPrevia, signal: AbortSignal) =>
  requisitarJson<Previa>("/operacoes/previa", {
    method: "POST",
    body: JSON.stringify(pedido),
    signal,
  });
export const confirmarOperacao = (comando: ComandoOperacao) =>
  requisitarJson<Operacao>(comando.caminho, {
    method: "POST",
    headers: { "Idempotency-Key": comando.chave },
    body: JSON.stringify(comando.entrada),
  });

export const nomesTipos: Record<TipoOperacao, string> = {
  SALDO_INICIAL: "Saldo inicial",
  APORTE: "Aporte",
  COMPRA: "Compra",
  VENDA: "Venda",
};
export const nomesSituacoes: Record<SituacaoOperacao, string> = {
  CONFIRMADA: "Confirmada",
  SUBSTITUIDA: "Substituída",
  CANCELADA: "Cancelada",
};
export const mensagemOperacao = (falha: unknown) =>
  falha instanceof Error ? falha.message : "Não foi possível concluir a solicitação.";
export const dataHoraFortaleza = (instante: string) =>
  new Date(instante).toLocaleString("pt-BR", { timeZone: "America/Fortaleza" });
export function dataHoraFormulario(instante: string) {
  const partes = new Intl.DateTimeFormat("sv-SE", {
    timeZone: "America/Fortaleza",
    year: "numeric",
    month: "2-digit",
    day: "2-digit",
    hour: "2-digit",
    minute: "2-digit",
    second: "2-digit",
    hourCycle: "h23",
  }).format(new Date(instante));
  return partes.replace(" ", "T");
}
