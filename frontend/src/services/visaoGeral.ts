import { requisitarJson } from "./http";
import type { Posicao } from "./operacoes";

export type PeriodoVisaoGeral = { dataInicio: string; dataFim: string };
export type PosicaoVisaoGeral = Posicao & {
  ativoNome: string;
  participacaoPercentual: string | null;
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
};

export function consultarVisaoGeral(periodo?: PeriodoVisaoGeral, signal?: AbortSignal) {
  const parametros = periodo ? `?${new URLSearchParams(periodo)}` : "";
  return requisitarJson<VisaoGeral>(`/visao-geral${parametros}`, { signal });
}
