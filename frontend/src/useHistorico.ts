import { useCallback, useEffect, useRef, useState } from "react";
import {
  comandoConferencia,
  conferirHistorico,
  executarConferencia,
  listarContasCustodia,
  listarTimeline,
  mensagemConferencia,
} from "./services/conciliacao";
import type {
  ComandoConferencia,
  ContaCustodia,
  DadosConferencia,
  FiltroTimeline,
  PaginaTimeline,
  PreviaConciliacao as Previa,
  RegistroTimeline,
} from "./services/conciliacao";
import { ErroHttp } from "./services/http";
import { listarAtivos } from "./services/saldosIniciais";
import type { Ativo } from "./services/saldosIniciais";

const filtroInicial: FiltroTimeline = { pagina: 0, estado: "", ativoId: "", contaId: "" };
export default function useHistorico(onBloqueio?: (b: boolean) => void) {
  const [filtro, setFiltro] = useState(filtroInicial);
  const [pagina, setPagina] = useState<PaginaTimeline | null>(null);
  const [contas, setContas] = useState<ContaCustodia[]>([]);
  const [ativos, setAtivos] = useState<Ativo[]>([]);
  const [associando, setAssociando] = useState<string | null>(null);
  const [aberto, setAberto] = useState<string | null>(null);
  const [previa, setPrevia] = useState<Previa | null>(null);
  const [carregando, setCarregando] = useState(true);
  const [falhaAtualizacao, setFalhaAtualizacao] = useState(false);
  const [analisando, setAnalisando] = useState(false);
  const [bloqueado, setBloqueado] = useState(false);
  const [executando, setExecutando] = useState(false);
  const [comando, setComando] = useState<ComandoConferencia | null>(null);
  const [erro, setErro] = useState("");
  const [aviso, setAviso] = useState("");
  const consultaAtual = useRef(0);
  const previaAtual = useRef(0);
  const emExecucao = useRef(false);
  const focoPendente = useRef<string | null>(null);
  const bloquear = useCallback(
    (valor: boolean) => {
      setBloqueado(valor);
      onBloqueio?.(valor);
    },
    [onBloqueio],
  );
  const carregar = useCallback(
    async (signal?: AbortSignal) => {
      const consulta = ++consultaAtual.current;
      setCarregando(true);
      setErro("");
      try {
        const [dados, contasAtuais, ativosAtuais] = await Promise.all([
          listarTimeline(filtro, signal),
          listarContasCustodia(signal),
          listarAtivos(signal),
        ]);
        if (signal?.aborted || consulta !== consultaAtual.current) return;
        setPagina(dados);
        setFalhaAtualizacao(false);
        setContas(contasAtuais);
        setAtivos(ativosAtuais);
      } catch (falha) {
        if (!signal?.aborted && consulta === consultaAtual.current) {
          setFalhaAtualizacao(true);
          setErro(mensagemConferencia(falha));
        }
      } finally {
        if (!signal?.aborted && consulta === consultaAtual.current) setCarregando(false);
      }
    },
    [filtro],
  );
  useEffect(() => {
    const abortar = new AbortController();
    void carregar(abortar.signal);
    return () => abortar.abort();
  }, [carregar]);
  useEffect(() => {
    if (!bloqueado) return;
    const impedirSaida = (evento: BeforeUnloadEvent) => evento.preventDefault();
    window.addEventListener("beforeunload", impedirSaida);
    return () => window.removeEventListener("beforeunload", impedirSaida);
  }, [bloqueado]);
  useEffect(() => {
    const id = focoPendente.current;
    if (carregando || !id || !pagina?.itens.some((item) => item.id === id)) return;
    focoPendente.current = null;
    setAberto(id);
    document.getElementById(`timeline-${id}`)?.focus();
  }, [pagina, carregando]);
  async function executar(proximo: ComandoConferencia) {
    if (emExecucao.current) return;
    emExecucao.current = true;
    setComando(proximo);
    setExecutando(true);
    bloquear(true);
    setErro("");
    setAviso("");
    ++consultaAtual.current;
    ++previaAtual.current;
    try {
      await executarConferencia(proximo);
      setComando(null);
      bloquear(false);
      setPrevia(null);
      setAberto(null);
      setAssociando(null);
      setAviso(proximo.mensagem);
      await carregar();
    } catch (falha) {
      const incerta = !(falha instanceof ErroHttp) || falha.status >= 500;
      if (!incerta) {
        setComando(null);
        bloquear(false);
        if (falha.status === 409) {
          setPrevia(null);
          await carregar();
        }
      }
      setErro(
        `${mensagemConferencia(falha)}${incerta ? " O resultado ainda não foi confirmado. Repita a confirmação para conferir o mesmo comando." : ""}`,
      );
    } finally {
      emExecucao.current = false;
      setExecutando(false);
    }
  }
  async function analisar() {
    if (!pagina || bloqueado || analisando) return;
    const consulta = ++previaAtual.current;
    setAnalisando(true);
    setErro("");
    setAviso("");
    setPrevia(null);
    try {
      const dados = await conferirHistorico(pagina.revisao);
      if (consulta === previaAtual.current) setPrevia(dados);
    } catch (falha) {
      if (consulta === previaAtual.current) setErro(mensagemConferencia(falha));
    } finally {
      if (consulta === previaAtual.current) setAnalisando(false);
    }
  }
  function filtrar(alteracao: Partial<FiltroTimeline>) {
    ++previaAtual.current;
    setAnalisando(false);
    setPrevia(null);
    setAberto(null);
    setFiltro((atual) => ({ ...atual, ...alteracao }));
  }
  function salvar(registro: RegistroTimeline, dados: DadosConferencia, saldoAnterior: boolean) {
    if (!pagina) return;
    const entrada = saldoAnterior
      ? { ...dados, revisao: pagina.revisao, ancoraId: registro.id }
      : { ...dados, revisao: pagina.revisao, versao: registro.versao };
    void executar(
      comandoConferencia(
        saldoAnterior
          ? "/conciliacao/saldos-anteriores"
          : `/conciliacao/registros/${encodeURIComponent(registro.id)}`,
        entrada,
        saldoAnterior
          ? "Saldo anterior registrado. Confira as mudanças propostas para os ajustes."
          : "Dados salvos. Confira as pendências atualizadas.",
        saldoAnterior ? "POST" : "PUT",
      ),
    );
  }
  function proximaPendencia() {
    if (!pagina?.proximaPendenciaId) return;
    const id = pagina.proximaPendenciaId;
    const paginaDestino = pagina.proximaPendenciaPagina ?? pagina.pagina;
    setAviso("Prioridade: mais registros afetados, depois gravidade e registro mais antigo.");
    if (paginaDestino === pagina.pagina && pagina.itens.some((item) => item.id === id)) {
      setAberto(id);
      document.getElementById(`timeline-${id}`)?.focus();
    } else {
      focoPendente.current = id;
      filtrar({ pagina: paginaDestino });
    }
  }

  async function recebido() {
    ++previaAtual.current;
    setPrevia(null);
    setAnalisando(false);
    await carregar();
  }
  function revisar(registro: RegistroTimeline) {
    if (!pagina) return;
    void executar(
      comandoConferencia(
        `/conciliacao/registros/${encodeURIComponent(registro.id)}/revisao`,
        { revisao: pagina.revisao, versao: registro.versao },
        "Revisão anotada. O estado automático foi preservado.",
      ),
    );
  }
  function confirmar(lacunas: string[], correspondencias: string[]) {
    if (!previa) return;
    void executar(
      comandoConferencia(
        "/conciliacao/confirmacoes",
        {
          revisao: previa.revisao,
          previaId: previa.previaId,
          aceitarLacunaIds: lacunas,
          aceitarCorrespondenciaIds: correspondencias,
        },
        "Conciliação confirmada.",
      ),
    );
  }
  return {
    filtro,
    pagina,
    contas,
    ativos,
    aberto,
    setAberto,
    associando,
    setAssociando,
    previa,
    setPrevia,
    carregando,
    falhaAtualizacao,
    analisando,
    bloqueado,
    executando,
    comando,
    erro,
    aviso,
    bloquear,
    carregar,
    executar,
    analisar,
    filtrar,
    salvar,
    proximaPendencia,
    recebido,
    revisar,
    confirmar,
  };
}
