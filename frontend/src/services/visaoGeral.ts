import { requisitarJson } from "./http";
import type { Posicao } from "./operacoes";

export type PeriodoVisaoGeral = { dataInicio: string; dataFim: string };
export type PosicaoVisaoGeral = Posicao & {
  ativoNome: string;
  participacaoPercentual: string | null;
};
export type ResultadoMensal = {
  dataInicio: string;
  dataFim: string;
  resultadoBrl: string;
  acumuladoBrl: string;
  operacoes: number;
};
export type ResultadoAtivo = {
  ativoId: string;
  ativoTicker: string;
  ativoNome: string;
  negociacaoBrl: string;
  moedaBrl: string;
  taxaBrl: string;
  resultadoTotalBrl: string;
};
export type SaldoMoeda = Pick<
  PosicaoVisaoGeral,
  "ativoId" | "ativoTicker" | "ativoNome" | "quantidade" | "instituicoes"
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
    custoSaldosBrl: string;
    resultadoRealizadoBrl: string;
    taxasBrl: string;
    aportesBrl: string;
    operacoesNoPeriodo: number;
  };
  resultado: { negociacaoBrl: string; moedaBrl: string; taxaBrl: string };
  posicoes: PosicaoVisaoGeral[];
  dataSaldos: string;
  saldosMoedas: SaldoMoeda[];
  evolucaoResultado: { acumuladoInicialBrl: string; meses: ResultadoMensal[] };
  resultadosPorAtivo: ResultadoAtivo[];
  conferencia: Conferencia;
};

export function consultarVisaoGeral(periodo?: PeriodoVisaoGeral, signal?: AbortSignal) {
  const parametros = periodo ? `?${new URLSearchParams(periodo)}` : "";
  return requisitarJson<VisaoGeral>(`/visao-geral${parametros}`, { signal });
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
