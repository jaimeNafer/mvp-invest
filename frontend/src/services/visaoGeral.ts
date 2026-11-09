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
  aportesLiquidosBrl?: string | null;
  aportesLiquidosConhecidosBrl?: string | null;
};
export type IndicadorDisponibilidade = {
  valorConhecido: string | null;
  status: "COMPLETO" | "PARCIAL" | "INDISPONIVEL";
  componentesConhecidos: number;
  componentesDesconhecidos: number;
  motivo: string | null;
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
  subtotalResultadoBrl?: string | null;
  subtotalAcumuladoBrl?: string | null;
};
export type ResultadoAtivo = {
  ativoId: string;
  ativoTicker: string;
  ativoNome: string;
  negociacaoBrl: string | null;
  moedaBrl: string | null;
  taxaBrl: string | null;
  resultadoTotalBrl: string | null;
  subtotalResultadoBrl?: string | null;
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
    custo?: IndicadorDisponibilidade;
    resultado?: IndicadorDisponibilidade;
    taxas?: IndicadorDisponibilidade;
    aportesLiquidos?: IndicadorDisponibilidade;
  };
};

function parametrosVisao(periodo?: PeriodoVisaoGeral, carteiraId?: string) {
  const parametros = new URLSearchParams(periodo);
  if (carteiraId) parametros.set("carteiraId", carteiraId);
  return parametros.size ? `?${parametros}` : "";
}

export function consultarVisaoGeral(
  periodo?: PeriodoVisaoGeral,
  signal?: AbortSignal,
  carteiraId?: string,
) {
  const parametros = parametrosVisao(periodo, carteiraId);
  return requisitarJson<VisaoGeral>(`/visao-geral${parametros}`, { signal });
}

export function consultarPatrimonioDiario(
  periodo?: PeriodoVisaoGeral,
  signal?: AbortSignal,
  carteiraId?: string,
) {
  const parametros = parametrosVisao(periodo, carteiraId);
  return requisitarJson<PontoPatrimonioDiario[]>(`/visao-geral/patrimonio-diario${parametros}`, {
    signal,
  });
}

export function consultarAvisos(
  dataFim: string,
  tipo: TipoAviso,
  pagina: number,
  signal: AbortSignal,
  carteiraId?: string,
) {
  const parametros = new URLSearchParams({ dataFim, tipo, pagina: String(pagina), tamanho: "10" });
  if (carteiraId) parametros.set("carteiraId", carteiraId);
  return requisitarJson<AvisosConferencia>(`/visao-geral/avisos?${parametros}`, { signal });
}
