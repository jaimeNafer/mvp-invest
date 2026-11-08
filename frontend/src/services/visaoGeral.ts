import type { ResumoConferencia } from "./conciliacao";
import { requisitarJson } from "./http";
import type { Posicao } from "./operacoes";

export type PeriodoVisaoGeral = { dataInicio: string; dataFim: string };
export type PontoPatrimonioDiario = {
  data: string;
  valorTotalBrl: string | null;
  valorConhecidoBrl: string;
  ativosComPreco: number;
  ativosSemPreco: number;
  quantidadesCompletas: boolean;
};
export type PosicaoVisaoGeral = Posicao & {
  ativoNome: string;
  participacaoPercentual: string | null;
};
export type ResultadoMensal = {
  dataInicio: string;
  dataFim: string;
  resultadoBrl: string | null;
  acumuladoBrl: string | null;
  operacoes: number;
};
export type ResultadoAtivo = {
  ativoId: string;
  ativoTicker: string;
  ativoNome: string;
  negociacaoBrl: string | null;
  moedaBrl: string | null;
  taxaBrl: string | null;
  resultadoTotalBrl: string | null;
  subtotalResultadoBrl?: string;
};
export type SaldoMoeda = Pick<
  PosicaoVisaoGeral,
  | "ativoId"
  | "ativoTicker"
  | "ativoNome"
  | "quantidade"
  | "instituicoes"
  | "quantidadeDisponivel"
  | "comAjustes"
>;
export type TipoAviso = "CUSTO_ZERO_DECLARADO" | "REFERENCIA_REPETIDA";
export type Conferencia = { custoZeroDeclarado: number; referenciasRepetidas: number };
export type AvisosConferencia = {
  revisao: string;
  pagina: number;
  tamanho: number;
  total: number;
  itens: {
    operacaoId: string;
    ocorridaEm: string;
    instituicaoNome: string;
    ativoTicker: string;
    referencia: string | null;
    justificativa: string | null;
  }[];
};
export type VisaoGeral = PeriodoVisaoGeral & {
  revisao: string;
  dataReferencia: string | null;
  possuiRegistrosAteFim: boolean;
  resumo: {
    custoSaldosBrl: string | null;
    resultadoRealizadoBrl: string | null;
    taxasBrl: string | null;
    aportesBrl: string | null;
    operacoesNoPeriodo: number;
  };
  resultado: { negociacaoBrl: string | null; moedaBrl: string | null; taxaBrl: string | null };
  posicoes: PosicaoVisaoGeral[];
  dataSaldos: string;
  saldosMoedas: SaldoMoeda[];
  evolucaoResultado: { acumuladoInicialBrl: string; meses: ResultadoMensal[] };
  resultadosPorAtivo: ResultadoAtivo[];
  conferencia: Conferencia;
  conciliacao?: ResumoConferencia;
  disponibilidade?: {
    quantidadesCompletas?: boolean;
    custoCompleto: boolean;
    resultadoCompleto: boolean;
    taxasCompletas: boolean;
    comAjustes: boolean;
    subtotalCustoBrl: string;
    subtotalResultadoBrl: string;
    subtotalTaxasBrl: string;
  };
};

export function consultarVisaoGeral(periodo?: PeriodoVisaoGeral, signal?: AbortSignal) {
  const parametros = periodo ? `?${new URLSearchParams(periodo)}` : "";
  return requisitarJson<VisaoGeral>(`/visao-geral${parametros}`, { signal });
}

export function consultarPatrimonioDiario(periodo?: PeriodoVisaoGeral, signal?: AbortSignal) {
  const parametros = periodo ? `?${new URLSearchParams(periodo)}` : "";
  return requisitarJson<PontoPatrimonioDiario[]>(`/visao-geral/patrimonio-diario${parametros}`, {
    signal,
  });
}

export function consultarAvisos(
  dataFim: string,
  tipo: TipoAviso,
  pagina: number,
  signal: AbortSignal,
) {
  const parametros = new URLSearchParams({ dataFim, tipo, pagina: String(pagina), tamanho: "10" });
  return requisitarJson<AvisosConferencia>(`/visao-geral/avisos?${parametros}`, { signal });
}
