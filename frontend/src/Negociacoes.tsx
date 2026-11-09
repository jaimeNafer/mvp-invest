import { useCallback, useEffect, useRef, useState } from "react";

import DetalheOperacao from "./DetalheOperacao";

import "./operacoes-historico.css";

import Icone from "./Icone";

import ListaNegociacoes from "./ListaNegociacoes";

import Modal from "./Modal";

import OperacaoForm from "./OperacaoForm";

import { listarCarteiras } from "./services/carteiras";

import type { Carteira } from "./services/carteiras";

import { listarInstituicoes } from "./services/instituicoes";

import type { Instituicao } from "./services/instituicoes";

import {
  listarHistoricoOperacoes,
  mensagemOperacao,
  nomesTipos,
  obterOperacao,
} from "./services/operacoes";

import type { FiltrosOperacoes, HistoricoOperacoes, Operacao } from "./services/operacoes";

import { listarAtivos } from "./services/saldosIniciais";

import type { Ativo } from "./services/saldosIniciais";

import {
  comandoExclusaoSaldoAnterior,
  executarConferencia,
  listarTimeline,
  mensagemConferencia,
} from "./services/conciliacao";

import type { RegistroTimeline } from "./services/conciliacao";

import { ErroHttp } from "./services/http";

import { decimalPtBr } from "./services/decimais";

import { listarArquivosRecebidos } from "./services/importacoes";

type Dados = {
  ativos: Ativo[];

  instituicoes: Instituicao[];

  carteiras: Carteira[];

  operacoes: HistoricoOperacoes;
};

type Janela =
  | { tipo: "form"; operacao: Operacao | null }
  | { tipo: "cancelar" | "detalhe"; operacao: Operacao };

const inicial: FiltrosOperacoes = {
  tipo: "",

  situacao: "CONFIRMADA",

  instituicaoId: "",

  ativoId: "",

  carteiraIds: [],

  pagina: 0,
};

export default function Negociacoes({
  onSaldos,

  onBloqueio,

  carteiraId,

  arquivoId,

  onConferencia,

  onSelecionarArquivo,
}: {
  onSaldos: () => void;

  onBloqueio: (b: boolean) => void;

  carteiraId?: string;

  arquivoId?: string;

  onConferencia: (id: string) => void;

  onSelecionarArquivo: (id?: string) => void;
}) {
  const [arquivos, setArquivos] = useState<{ id: string; nomeArquivo: string }[]>([]);
  const [erroArquivos, setErroArquivos] = useState(false);

  useEffect(() => {
    const controller = new AbortController();
    async function carregarArquivos() {
      const arquivos = [];
      for (let pagina = 0; ; pagina++) {
        const lote = await listarArquivosRecebidos(pagina, controller.signal);
        arquivos.push(...lote.itens);
        if (lote.itens.length === 0 || (pagina + 1) * lote.tamanho >= lote.total) break;
      }
      if (!controller.signal.aborted) setArquivos(arquivos);
    }
    void carregarArquivos().catch(() => {
      if (!controller.signal.aborted) setErroArquivos(true);
    });
    return () => controller.abort();
  }, []);

  const [dados, setDados] = useState<Dados | null>(null);

  const [filtros, setFiltros] = useState({
    ...inicial,

    carteiraIds: carteiraId ? [carteiraId] : [],
  });

  const [carregando, setCarregando] = useState(true);

  const [erro, setErro] = useState("");

  const [aviso, setAviso] = useState("");

  const [janela, setJanela] = useState<Janela | null>(null);

  const [bloqueado, setBloqueado] = useState(false);

  const [saldoParaExcluir, setSaldoParaExcluir] = useState<RegistroTimeline | null>(null);

  const [erroExclusao, setErroExclusao] = useState("");

  const consultaAtual = useRef(0);

  const detalheAtual = useRef(0);

  const exclusaoEmExecucao = useRef(false);

  const bloquear = useCallback(
    (b: boolean) => {
      setBloqueado(b);

      onBloqueio(b);
    },

    [onBloqueio],
  );

  const carregar = useCallback(
    async (signal?: AbortSignal) => {
      const consulta = ++consultaAtual.current;

      setCarregando(true);

      setErro("");

      try {
        const [ativos, instituicoes, carteiras, operacoes] = await Promise.all([
          listarAtivos(signal),

          listarInstituicoes(signal),

          listarCarteiras(signal),

          listarHistoricoOperacoes(filtros, arquivoId, signal),
        ]);

        if (signal?.aborted || consulta !== consultaAtual.current) return;

        setDados({
          ativos,

          instituicoes,

          carteiras,

          operacoes,
        });
      } catch (falha) {
        if (!signal?.aborted && consulta === consultaAtual.current)
          setErro(mensagemOperacao(falha));
      } finally {
        if (!signal?.aborted && consulta === consultaAtual.current) setCarregando(false);
      }
    },

    [filtros, arquivoId],
  );

  useEffect(() => {
    const controller = new AbortController();

    void carregar(controller.signal);

    return () => controller.abort();
  }, [carregar]);

  function abrir(j: Janela) {
    detalheAtual.current++;

    setJanela(j);

    setAviso("");

    setErro("");
  }

  function fechar() {
    if (bloqueado) return;

    detalheAtual.current++;

    setJanela(null);
  }

  async function detalhe(id: string) {
    const consulta = ++detalheAtual.current;

    setErro("");

    try {
      const operacao = await obterOperacao(id);

      if (consulta === detalheAtual.current) setJanela({ tipo: "detalhe", operacao });
    } catch (falha) {
      if (consulta === detalheAtual.current) setErro(mensagemOperacao(falha));
    }
  }

  function filtrar(alteracao: Partial<FiltrosOperacoes>) {
    setFiltros((f) => ({ ...f, ...alteracao, pagina: alteracao.pagina ?? 0 }));
  }

  function concluido() {
    setAviso(
      janela?.tipo === "cancelar"
        ? "Operação cancelada. O registro foi preservado no histórico."
        : janela?.operacao
          ? "Operação corrigida. A versão anterior foi preservada."
          : "Operação confirmada.",
    );

    setJanela(null);

    void carregar();
  }

  async function confirmarExclusaoSaldoAnterior() {
    if (!saldoParaExcluir || bloqueado || exclusaoEmExecucao.current) return;
    exclusaoEmExecucao.current = true;
    setErroExclusao("");
    bloquear(true);
    try {
      const timeline = await listarTimeline({
        pagina: 0,
        estado: "",
        ativoId: "",
        contaId: "",
        registroId: saldoParaExcluir.id,
      });
      const registroAtual = timeline.itens.find((item) => item.id === saldoParaExcluir.id);
      if (registroAtual?.origem !== "MANUAL" || registroAtual.natureza !== "SALDO_ANTERIOR")
        throw new Error("O saldo anterior não está mais disponível para exclusão.");
      const comando = comandoExclusaoSaldoAnterior(registroAtual, timeline.revisao);
      await executarConferencia(comando);
      setSaldoParaExcluir(null);
      setAviso("Saldo anterior excluído permanentemente.");
      await carregar();
    } catch (falha) {
      const incerta = !(falha instanceof ErroHttp) || falha.status >= 500;
      setErroExclusao(
        `${mensagemConferencia(falha)}${incerta ? " O resultado não foi confirmado. Atualize o histórico para verificar se o saldo foi excluído." : ""}`,
      );
    } finally {
      exclusaoEmExecucao.current = false;
      bloquear(false);
    }
  }

  return (
    <section className="operacoes-historico" aria-labelledby="page-title">
      <div className="intro">
        <p className="eyebrow">SEU HISTÓRICO</p>
        <h1 id="page-title">Operações</h1>
        <p className="intro-text">Consulte e registre suas operações de investimento.</p>
      </div>
      {aviso && (
        <p role="status" className="message-success">
          {aviso}
        </p>
      )}
      {erro && (
        <p role="alert" className="message-error">
          {erro}
        </p>
      )}
      {carregando && !dados ? (
        <p role="status">Carregando operações…</p>
      ) : erro && !dados ? (
        <button type="button" className="secondary-button" onClick={() => void carregar()}>
          Tentar novamente
        </button>
      ) : (
        dados && (
          <section
            className="operations-panel"
            aria-label="Histórico de operações"
            aria-busy={carregando}
          >
            <div className="list-heading">
              <h2>Histórico de operações</h2>
              <button
                type="button"
                className="primary-button add-button"
                onClick={() => abrir({ tipo: "form", operacao: null })}
              >
                <Icone nome="adicionar" /> Adicionar operação
              </button>
            </div>
            <div className="saldo-filtros operacoes-filtros">
              <label>
                Carteira
                <select
                  value={filtros.carteiraIds[0] ?? ""}
                  onChange={(e) => filtrar({ carteiraIds: e.target.value ? [e.target.value] : [] })}
                >
                  <option value="">Todas as carteiras</option>
                  {dados.carteiras.map((c) => (
                    <option key={c.id} value={c.id}>
                      {c.nome}
                      {c.ativo ? "" : " (arquivada)"}
                    </option>
                  ))}
                </select>
              </label>
              <label>
                Instituição
                <select
                  aria-label="Filtrar instituição"
                  value={filtros.instituicaoId}
                  onChange={(e) => filtrar({ instituicaoId: e.target.value })}
                >
                  <option value="">Todas</option>
                  {dados.instituicoes.map((i) => (
                    <option key={i.id} value={i.id}>
                      {i.nome}
                    </option>
                  ))}
                </select>
              </label>
              <label>
                Tipo
                <select
                  aria-label="Filtrar tipo"
                  value={filtros.tipo}
                  onChange={(e) => filtrar({ tipo: e.target.value as FiltrosOperacoes["tipo"] })}
                >
                  <option value="">Todos</option>
                  {Object.entries({
                    ...nomesTipos,

                    CONVERSAO: "Conversão",

                    RETIRADA: "Retirada",

                    CREDITO_AUTOMATICO: "Rendimento e recompensa",

                    SAIDA_CRIPTO: "Saída de criptoativo",

                    MOVIMENTACAO_INTERNA: "Movimentação interna",
                  }).map(([tipo, nome]) => (
                    <option key={tipo} value={tipo}>
                      {nome}
                    </option>
                  ))}
                </select>
              </label>
              <label>
                Ativo
                <select
                  aria-label="Filtrar ativo"
                  value={filtros.ativoId}
                  onChange={(e) => filtrar({ ativoId: e.target.value })}
                >
                  <option value="">Todos</option>
                  {dados.ativos.map((a) => (
                    <option key={a.id} value={a.id}>
                      {a.ticker}
                    </option>
                  ))}
                </select>
              </label>
              <label>
                Situação
                <select
                  aria-label="Filtrar situação"
                  value={filtros.situacao}
                  onChange={(e) =>
                    filtrar({ situacao: e.target.value as FiltrosOperacoes["situacao"] })
                  }
                >
                  <option value="CONFIRMADA">Confirmados</option>
                  <option value="TODAS">Todo o histórico</option>
                  <option value="SUBSTITUIDA">Substituídos</option>
                  <option value="CANCELADA">Cancelados</option>
                </select>
              </label>
              <label>
                Arquivo de origem
                <select
                  value={arquivoId ?? ""}
                  onChange={(e) => {
                    filtrar({ pagina: 0 });
                    onSelecionarArquivo(e.target.value || undefined);
                  }}
                >
                  <option value="">Todos os arquivos</option>
                  {arquivoId && !arquivos.some((arquivo) => arquivo.id === arquivoId) && (
                    <option value={arquivoId}>Arquivo {arquivoId}</option>
                  )}
                  {arquivos.map((arquivo) => (
                    <option key={arquivo.id} value={arquivo.id}>
                      {arquivo.nomeArquivo}
                    </option>
                  ))}
                </select>
              </label>
            </div>
            {erroArquivos && (
              <p className="field-hint">Não foi possível carregar a lista de arquivos.</p>
            )}
            <button
              className="secondary-button operacoes-limpar"
              type="button"
              disabled={
                !arquivoId &&
                !filtros.carteiraIds.length &&
                !filtros.instituicaoId &&
                !filtros.tipo &&
                !filtros.ativoId &&
                filtros.situacao === inicial.situacao
              }
              onClick={() => {
                filtrar(inicial);
                if (arquivoId) onSelecionarArquivo(undefined);
              }}
            >
              Limpar filtros
            </button>
            <ListaNegociacoes
              itens={dados.operacoes.itens}
              carteiras={dados.carteiras}
              onConferencia={onConferencia}
              onDetalhe={(o) => {
                if (o.id) void detalhe(o.id);
              }}
              onCorrigir={(operacao) => abrir({ tipo: "form", operacao })}
              onCancelar={(operacao) => abrir({ tipo: "cancelar", operacao })}
              onExcluirSaldoAnterior={(registro) => {
                setErroExclusao("");
                setSaldoParaExcluir(registro);
              }}
            />
            <div className="pagination">
              <span>{dados.operacoes.total} registro(s)</span>
              <button
                type="button"
                className="secondary-button"
                disabled={carregando || !!erro || dados.operacoes.pagina === 0}
                onClick={() => filtrar({ pagina: filtros.pagina - 1 })}
              >
                Página anterior
              </button>
              <span>Página {dados.operacoes.pagina + 1}</span>
              <button
                type="button"
                className="secondary-button"
                disabled={
                  carregando ||
                  !!erro ||
                  (dados.operacoes.pagina + 1) * dados.operacoes.tamanho >= dados.operacoes.total
                }
                onClick={() => filtrar({ pagina: filtros.pagina + 1 })}
              >
                Próxima página
              </button>
            </div>
          </section>
        )
      )}
      {janela && dados && (
        <Modal
          titulo={
            janela.tipo === "detalhe"
              ? "Detalhes da operação"
              : janela.tipo === "cancelar"
                ? "Cancelar operação"
                : janela.operacao
                  ? "Corrigir operação"
                  : "Adicionar operação"
          }
          bloqueado={bloqueado}
          onFechar={fechar}
        >
          {janela.tipo === "detalhe" ? (
            <>
              <DetalheOperacao
                operacao={janela.operacao}
                onAnterior={(id) => void detalhe(id)}
                onSaldos={onSaldos}
              />
              {erro && (
                <p role="alert" className="message-error">
                  {erro}
                </p>
              )}
              <div className="form-actions">
                <button type="button" className="secondary-button" onClick={fechar}>
                  Fechar
                </button>
              </div>
            </>
          ) : (
            <OperacaoForm
              ativos={dados.ativos}
              instituicoes={
                janela.operacao || !filtros.carteiraIds.length
                  ? dados.instituicoes
                  : dados.instituicoes.filter((instituicao) =>
                      dados.carteiras.some(
                        (carteira) =>
                          filtros.carteiraIds.includes(carteira.id) &&
                          carteira.contas.some((conta) => conta.instituicaoId === instituicao.id),
                      ),
                    )
              }
              operacao={janela.operacao}
              cancelar={janela.tipo === "cancelar"}
              onFechar={fechar}
              onConcluido={concluido}
              onBloqueio={bloquear}
            />
          )}
        </Modal>
      )}
      {saldoParaExcluir && (
        <Modal
          titulo="Excluir saldo anterior"
          bloqueado={bloqueado}
          onFechar={() => {
            if (bloqueado) return;
            setSaldoParaExcluir(null);
            setErroExclusao("");
          }}
        >
          <div className="saldo-form">
            <p>
              Excluir permanentemente o saldo anterior declarado de {saldoParaExcluir.ativoTicker}?
            </p>
            <p>
              {saldoParaExcluir.quantidade == null
                ? "Quantidade desconhecida"
                : `${decimalPtBr(saldoParaExcluir.quantidade)} ${saldoParaExcluir.ativoTicker}`}
            </p>
            {erroExclusao && (
              <p role="alert" className="message-error">
                {erroExclusao}
              </p>
            )}
            <div className="form-actions">
              {erroExclusao && (
                <button
                  type="button"
                  className="secondary-button"
                  disabled={bloqueado}
                  onClick={() => {
                    setSaldoParaExcluir(null);
                    setErroExclusao("");
                    void carregar();
                  }}
                >
                  Atualizar histórico
                </button>
              )}
              <button
                type="button"
                className="primary-button"
                disabled={bloqueado}
                onClick={() => void confirmarExclusaoSaldoAnterior()}
              >
                {bloqueado ? "Excluindo…" : "Excluir permanentemente"}
              </button>
              <button
                type="button"
                className="secondary-button"
                disabled={bloqueado}
                onClick={() => {
                  setSaldoParaExcluir(null);
                  setErroExclusao("");
                }}
              >
                Voltar
              </button>
            </div>
          </div>
        </Modal>
      )}
    </section>
  );
}
