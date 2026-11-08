import { useCallback, useEffect, useRef, useState } from "react";
import DetalheOperacao from "./DetalheOperacao";
import FiltrosSaldos from "./FiltrosSaldos";
import Icone from "./Icone";
import ListaNegociacoes from "./ListaNegociacoes";
import Modal from "./Modal";
import OperacaoForm from "./OperacaoForm";
import PosicoesAtuais from "./PosicoesAtuais";
import { listarCarteiras } from "./services/carteiras";
import type { Carteira } from "./services/carteiras";
import { listarInstituicoes } from "./services/instituicoes";
import type { Instituicao } from "./services/instituicoes";
import {
  listarOperacoes,
  listarPosicoes,
  mensagemOperacao,
  nomesTipos,
  obterOperacao,
} from "./services/operacoes";
import type {
  FiltrosOperacoes,
  ListaOperacoes,
  Operacao,
  Posicao,
  TipoOperacao,
} from "./services/operacoes";
import { listarAtivos } from "./services/saldosIniciais";
import type { Ativo } from "./services/saldosIniciais";

type Dados = {
  ativos: Ativo[];
  instituicoes: Instituicao[];
  carteiras: Carteira[];
  operacoes: ListaOperacoes;
  posicoes: Posicao[];
  quantidadesCompletas?: boolean;
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
}: {
  onSaldos: () => void;
  onBloqueio: (b: boolean) => void;
}) {
  const [dados, setDados] = useState<Dados | null>(null);
  const [filtros, setFiltros] = useState(inicial);
  const [carregando, setCarregando] = useState(true);
  const [erro, setErro] = useState("");
  const [aviso, setAviso] = useState("");
  const [janela, setJanela] = useState<Janela | null>(null);
  const [bloqueado, setBloqueado] = useState(false);
  const consultaAtual = useRef(0);
  const detalheAtual = useRef(0);
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
        const [ativos, instituicoes, carteiras, operacoes, posicoes] = await Promise.all([
          listarAtivos(signal),
          listarInstituicoes(signal),
          listarCarteiras(signal),
          listarOperacoes(filtros, signal),
          listarPosicoes(signal),
        ]);
        if (signal?.aborted || consulta !== consultaAtual.current) return;
        if (operacoes.revisao !== posicoes.revisao) {
          setDados(null);
          setErro(
            "Seu histórico foi atualizado durante a consulta. Tente novamente para consultar os dados mais recentes.",
          );
          return;
        }
        setDados({
          ativos,
          instituicoes,
          carteiras,
          operacoes,
          posicoes: posicoes.itens,
          quantidadesCompletas: posicoes.quantidadesCompletas,
        });
      } catch (falha) {
        if (!signal?.aborted && consulta === consultaAtual.current)
          setErro(mensagemOperacao(falha));
      } finally {
        if (!signal?.aborted && consulta === consultaAtual.current) setCarregando(false);
      }
    },
    [filtros],
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
  return (
    <section aria-labelledby="page-title">
      <div className="intro">
        <p className="eyebrow">SEU HISTÓRICO</p>
        <h1 id="page-title">Operações</h1>
        <p className="intro-text">
          Registre aportes, compras e vendas e acompanhe a posição dos seus investimentos.
        </p>
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
      {carregando ? (
        <p role="status">Carregando operações…</p>
      ) : erro && !dados ? (
        <button type="button" className="secondary-button" onClick={() => void carregar()}>
          Tentar novamente
        </button>
      ) : (
        dados && (
          <>
            <section className="operations-panel" aria-label="Histórico de operações">
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
              <div className="saldo-filtros">
                <label>
                  Tipo
                  <select
                    aria-label="Filtrar tipo"
                    value={filtros.tipo}
                    onChange={(e) => filtrar({ tipo: e.target.value as TipoOperacao | "" })}
                  >
                    <option value="">Todos</option>
                    {(Object.entries(nomesTipos) as [TipoOperacao, string][]).map(
                      ([tipo, nome]) => (
                        <option key={tipo} value={tipo}>
                          {nome}
                        </option>
                      ),
                    )}
                  </select>
                </label>
              </div>
              <FiltrosSaldos
                ativos={dados.ativos}
                instituicoes={dados.instituicoes}
                carteiras={dados.carteiras}
                filtros={filtros}
                onFiltrar={(alteracao) => {
                  const { situacao, ...outros } = alteracao;
                  filtrar({
                    ...outros,
                    ...(situacao ? { situacao: situacao as FiltrosOperacoes["situacao"] } : {}),
                  });
                }}
              />
              <ListaNegociacoes
                operacoes={dados.operacoes.itens}
                onDetalhe={(o) => {
                  if (o.id) void detalhe(o.id);
                }}
                onCorrigir={(operacao) => abrir({ tipo: "form", operacao })}
                onCancelar={(operacao) => abrir({ tipo: "cancelar", operacao })}
              />
              <div className="pagination">
                <span>{dados.operacoes.total} registro(s)</span>
                <button
                  type="button"
                  className="secondary-button"
                  disabled={filtros.pagina === 0}
                  onClick={() => filtrar({ pagina: filtros.pagina - 1 })}
                >
                  Página anterior
                </button>
                <span>Página {filtros.pagina + 1}</span>
                <button
                  type="button"
                  className="secondary-button"
                  disabled={(filtros.pagina + 1) * dados.operacoes.tamanho >= dados.operacoes.total}
                  onClick={() => filtrar({ pagina: filtros.pagina + 1 })}
                >
                  Próxima página
                </button>
              </div>
            </section>
            <PosicoesAtuais posicoes={dados.posicoes} completo={dados.quantidadesCompletas} />
          </>
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
              instituicoes={dados.instituicoes}
              operacao={janela.operacao}
              cancelar={janela.tipo === "cancelar"}
              onFechar={fechar}
              onConcluido={concluido}
              onBloqueio={bloquear}
            />
          )}
        </Modal>
      )}
    </section>
  );
}
