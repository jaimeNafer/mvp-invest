import type { GrupoAssociacaoResponse } from "./associacoes";
import { requisitarJson } from "./http";

export type EstadoConferencia = "COERENTE" | "PENDENTE" | "CORRECAO_NECESSARIA";
export type MarcadorAtualizacaoCotacoes = { atualizadaEm: string | null };
export type NaturezaRegistro =
  | "OBSERVADA"
  | "SALDO_ANTERIOR"
  | "APORTE"
  | "RETIRADA"
  | "COMPRA"
  | "VENDA"
  | "CONVERSAO"
  | "SAIDA_CRIPTO"
  | "CREDITO_AUTOMATICO"
  | "MOVIMENTACAO_INTERNA";
export type FinalidadeCripto = "DESCONHECIDA" | "CARTEIRA_PROPRIA" | "TERCEIROS" | "OUTRA";
export type StatusValoracao = "CONFIRMADO" | "HISTORICO" | "ESTIMADO" | "MANUAL" | "INDISPONIVEL";
export type ValoracaoTimeline = {
  statusValoracao: StatusValoracao;
  valorTotalBrl: string | null;
  cotacaoUtilizada: string | null;
  dataHoraCotacao: string | null;
  fonteCotacao: string | null;
  metodoConversao: string | null;
  aproximado: boolean;
  motivoIndisponibilidade: string | null;
  permiteCorrecaoManual: boolean;
};

export function consultarMarcadorAtualizacaoCotacoes(signal?: AbortSignal) {
  return requisitarJson<MarcadorAtualizacaoCotacoes>("/conciliacao/cotacoes/atualizacao", {
    signal,
  });
}
export type ResumoConferencia = {
  total: number;
  operacoesCoerentes?: number;
  operacoesPendentes?: number;
  creditosAutomaticos?: number;
  naoClassificados?: number;
  operacoesAgrupadas?: number;
  coerentes: number;
  pendentes: number;
  inconsistentes: number;
  ajustesAtivos: number;
  percentualCoerencia: string;
  registrosAfetados: number;
  vendasAfetadas: number;
  ativosAfetados: string[];
  mensagem: string;
};
export type ContaCustodia = {
  id: string;
  nome: string;
  instituicaoId: string;
  instituicaoNome: string;
};
export type RegistroTimeline = {
  grupoAutomatico?: string | null;
  grupoNumero?: string | null;
  principalConhecidoBrl?: string | null;
  id: string;
  data: string | null;
  criadoEm: string;
  tipo: string;
  descricao: string;
  instituicaoId: string;
  instituicaoNome: string;
  contaId: string;
  contaNome: string;
  ativoId: string | null;
  ativoTicker: string;
  quantidade: string | null;
  custoTotalBrl: string | null;
  estado: EstadoConferencia;
  origem: "IMPORTADA" | "MANUAL" | "SINTETICA";
  revisadoPor: string | null;
  revisadoEm: string | null;
  versao: string;
  aceitoPor?: string | null;
  aceitoEm?: string | null;
  pendencias: { id: string; mensagem: string; impacto: string }[];
  ancoraId: string | null;
  camposOriginais: Record<string, string>;
  aplicado?: boolean;
  aguardaConfirmacao?: boolean;
  taxaDetalhamentoPendente?: boolean;
  saldoAnteriorPendente?: boolean;
  subtipoCredito?: "BONIFICACAO" | "AIRDROP" | "RENDIMENTO" | null;
  pagamentosGrupo?: { ativoTicker: string; quantidade: string }[];
  recebimentosGrupo?: { ativoTicker: string; quantidade: string }[];
  natureza?: NaturezaRegistro;
  finalidadeCripto?: FinalidadeCripto;
  moedaCotacaoId?: string | null;
  totalBruto?: string | null;
  cotacaoBrl?: string | null;
  taxaAtivoId?: string | null;
  taxaQuantidade?: string | null;
  taxaInformada?: boolean;
  valoracao?: ValoracaoTimeline;
};
export type FiltroTimeline = {
  pagina: number;
  estado: EstadoConferencia | "PENDENCIAS" | "";
  ativoId: string;
  contaId: string;
  registroId?: string;
};
export type PaginaTimeline = {
  itens: RegistroTimeline[];
  linhasPorGrupo?: Record<string, RegistroTimeline[]>;
  total: number;
  pagina: number;
  tamanho: number;
  revisao: string;
  resumo: ResumoConferencia;
  proximaPendenciaId: string | null;
  proximaPendenciaPagina?: number | null;
};
export type Recebimento = {
  arquivos: {
    id: string | null;
    nomeArquivo: string;
    hash: string;
    registros: number;
    reconhecido: boolean;
    erro: string | null;
  }[];
  recebidos: number;
  reconhecidos: number;
  problemas: number;
  revisao: string;
};
export type PreviaConciliacao = {
  grupos?: GrupoAssociacaoResponse[];
  revisao: string;
  previaId: string;
  registrosParaAplicar?: number;
  ajustes: {
    lacunaId: string;
    ancoraId: string;
    contaId: string;
    contaNome: string;
    instituicaoNome: string;
    ativoId: string;
    ativoTicker: string;
    quantidadeSugerida: string;
    custoTotalBrl: string | null;
    acao: "CRIAR" | "MANTER" | "REDUZIR" | "ENCERRAR" | "ALTERAR";
    quantidadeAnterior: string | null;
  }[];
  correspondencias: {
    id: string;
    registroIds: string[];
    descricao: string;
    contaOrigem: string;
    contaDestino: string;
    quantidade: string;
    ativoTicker: string;
  }[];
  resumo: ResumoConferencia;
};
export type DadosConferencia = {
  data: string | null;
  contaId: string | null;
  ativoId: string | null;
  quantidade: string | null;
  custoTotalBrl: string | null;
  natureza: NaturezaRegistro;
  motivo: string;
  finalidadeCripto?: FinalidadeCripto;
  moedaCotacaoId?: string | null;
  totalBruto?: string | null;
  cotacaoBrl?: string | null;
  taxaAtivoId?: string | null;
  taxaQuantidade?: string | null;
  taxaInformada?: boolean;
};
export type ComandoConferencia = {
  caminho: string;
  metodo: "DELETE" | "PUT" | "POST";
  corpo: string;
  chave?: string;
  mensagem: string;
};
export function comandoConferencia(
  caminho: string,
  entrada: unknown,
  mensagem: string,
  metodo: "DELETE" | "POST" | "PUT" = "POST",
): ComandoConferencia {
  return { caminho, corpo: JSON.stringify(entrada), metodo, chave: crypto.randomUUID(), mensagem };
}
export function comandoExclusaoSaldoAnterior(registro: RegistroTimeline, revisao: string) {
  if (
    registro.origem !== "MANUAL" ||
    registro.natureza !== "SALDO_ANTERIOR" ||
    !/^registro:\d+$/.test(registro.id)
  )
    throw new Error("Somente saldos anteriores declarados podem ser excluídos.");
  return {
    caminho: `/conciliacao/registros/${encodeURIComponent(registro.id)}?revisao=${encodeURIComponent(revisao)}`,
    corpo: "",
    metodo: "DELETE" as const,
    mensagem: "Saldo anterior excluído permanentemente.",
  };
}
export function comandoValoracao(
  registro: RegistroTimeline,
  revisao: string,
  valorTotalBrl: string,
  versao = registro.versao,
): ComandoConferencia {
  const caminho = registro.grupoNumero
    ? `/conciliacao/associacoes/${encodeURIComponent(registro.grupoNumero)}/valoracao`
    : `/conciliacao/registros/${encodeURIComponent(registro.id)}/valoracao`;
  return comandoConferencia(
    caminho,
    { revisao, versao, valorTotalBrl },
    "Valor informado. O histórico será recalculado automaticamente.",
    "PUT",
  );
}
export function comandoAtualizacaoCotacoes(): ComandoConferencia {
  return {
    caminho: "/conciliacao/cotacoes/atualizacoes",
    metodo: "POST",
    corpo: "",
    chave: crypto.randomUUID(),
    mensagem: "Solicitação recebida. O histórico será recalculado automaticamente.",
  };
}
export function executarConferencia(comando: ComandoConferencia) {
  return requisitarJson(comando.caminho, {
    method: comando.metodo,
    ...(comando.corpo ? { body: comando.corpo } : {}),
    ...(comando.chave ? { headers: { "Idempotency-Key": comando.chave } } : {}),
    signal: AbortSignal.timeout(120000),
  });
}
export function listarTimeline(filtro: FiltroTimeline, signal?: AbortSignal) {
  const params = new URLSearchParams({ pagina: String(filtro.pagina), tamanho: "50" });
  if (filtro.estado) params.set("estado", filtro.estado);
  if (filtro.ativoId) params.set("ativoId", filtro.ativoId);
  if (filtro.contaId) params.set("contaId", filtro.contaId);
  if (filtro.registroId) params.set("registroId", filtro.registroId);
  return requisitarJson<PaginaTimeline>(`/conciliacao/timeline?${params}`, { signal });
}
export const listarContasCustodia = (signal?: AbortSignal) =>
  requisitarJson<ContaCustodia[]>("/conciliacao/contas", { signal });
export const conferirHistorico = (revisao: string, signal?: AbortSignal) =>
  requisitarJson<PreviaConciliacao>("/conciliacao/previa", {
    method: "POST",
    body: JSON.stringify({ revisao }),
    signal,
  });
export function receberArquivos(arquivos: File[]) {
  const corpo = new FormData();
  for (const arquivo of arquivos) corpo.append("arquivos", arquivo);
  return requisitarJson<Recebimento>("/importacoes/recebimentos", {
    method: "POST",
    body: corpo,
    signal: AbortSignal.timeout(120000),
  });
}
export const nomesEstados: Record<EstadoConferencia, string> = {
  COERENTE: "Coerente",
  PENDENTE: "Com pendência",
  CORRECAO_NECESSARIA: "Precisa de correção",
};
export const classesEstados: Record<EstadoConferencia, string> = {
  COERENTE: "estado-coerente",
  PENDENTE: "estado-pendente",
  CORRECAO_NECESSARIA: "estado-correcao",
};
export const mensagemConferencia = (erro: unknown) =>
  erro instanceof Error
    ? erro.message
    : "Não foi possível concluir a conferência. Tente novamente.";
