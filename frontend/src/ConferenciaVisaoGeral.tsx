import { useEffect, useState } from "react";
import DetalheOperacao from "./DetalheOperacao";
import Modal from "./Modal";
import { dataHoraFortaleza, obterOperacao } from "./services/operacoes";
import type { Operacao } from "./services/operacoes";
import { consultarAvisos } from "./services/visaoGeral";
import type { AvisosConferencia, Conferencia, TipoAviso } from "./services/visaoGeral";

export default function ConferenciaVisaoGeral({
  conferencia,
  dataFim,
  revisao,
  onAtualizar,
  onOperacoes,
  carteiraId,
}: {
  conferencia: Conferencia;
  dataFim: string;
  revisao: string;
  onAtualizar: () => void;
  onOperacoes: () => void;
  carteiraId?: string;
}) {
  const [consulta, setConsulta] = useState<{ tipo: TipoAviso; pagina: number } | null>(null);
  const tipo = consulta?.tipo;
  const pagina = consulta?.pagina ?? 0;
  const [lista, setLista] = useState<AvisosConferencia | null>(null);
  const [pedidoDetalhe, setPedidoDetalhe] = useState<{ id: string } | null>(null);
  const operacaoId = pedidoDetalhe?.id;
  const setOperacaoId = (id: string | null) => setPedidoDetalhe(id ? { id } : null);
  const [operacao, setOperacao] = useState<Operacao | null>(null);
  const [carregandoLista, setCarregandoLista] = useState(false);
  const [carregandoDetalhe, setCarregandoDetalhe] = useState(false);
  const carregando = operacaoId ? carregandoDetalhe : carregandoLista;
  const [erro, setErro] = useState("");

  useEffect(() => {
    if (!consulta) return;
    const controller = new AbortController();
    setLista(null);
    setErro("");
    setCarregandoLista(true);
    void consultarAvisos(dataFim, consulta.tipo, consulta.pagina, controller.signal, carteiraId)
      .then((resposta) => {
        if (!controller.signal.aborted) setLista(resposta);
      })
      .catch((falha: unknown) => {
        if (!controller.signal.aborted)
          setErro(falha instanceof Error ? falha.message : "Não foi possível consultar os avisos.");
      })
      .finally(() => {
        if (!controller.signal.aborted) setCarregandoLista(false);
      });
    return () => controller.abort();
  }, [dataFim, consulta, carteiraId]);

  useEffect(() => {
    setOperacao(null);
    if (!pedidoDetalhe) return;
    const controller = new AbortController();
    setErro("");
    setCarregandoDetalhe(true);
    void obterOperacao(pedidoDetalhe.id, controller.signal)
      .then((resposta) => {
        if (!controller.signal.aborted) setOperacao(resposta);
      })
      .catch((falha: unknown) => {
        if (!controller.signal.aborted)
          setErro(
            falha instanceof Error ? falha.message : "Não foi possível consultar o registro.",
          );
      })
      .finally(() => {
        if (!controller.signal.aborted) setCarregandoDetalhe(false);
      });
    return () => controller.abort();
  }, [pedidoDetalhe]);

  function abrir(aviso: TipoAviso) {
    setLista(null);
    setOperacaoId(null);
    setConsulta({ tipo: aviso, pagina: 0 });
  }

  const desatualizado = lista !== null && lista.revisao !== revisao;
  return (
    <section className="overview-panel overview-review" aria-labelledby="conferencia-titulo">
      <h2 id="conferencia-titulo">Conferência de registros</h2>
      <div className="overview-review-actions">
        <button
          type="button"
          className="secondary-button"
          disabled={conferencia.custoZeroDeclarado === 0}
          onClick={() => abrir("CUSTO_ZERO_DECLARADO")}
        >
          {conferencia.custoZeroDeclarado}{" "}
          {conferencia.custoZeroDeclarado === 1
            ? "saldo inicial com custo zero declarado"
            : "saldos iniciais com custo zero declarado"}
        </button>
        <button
          type="button"
          className="secondary-button"
          disabled={conferencia.referenciasRepetidas === 0}
          onClick={() => abrir("REFERENCIA_REPETIDA")}
        >
          {conferencia.referenciasRepetidas}{" "}
          {conferencia.referenciasRepetidas === 1
            ? "registro com referência repetida"
            : "registros com referência repetida"}
        </button>
      </div>
      <p className="overview-note">
        Avisos para revisão até a data dos saldos; não comprovam erros nem conferência com extratos.
      </p>
      {tipo && (
        <Modal
          titulo={
            operacaoId
              ? "Detalhes do registro"
              : tipo === "CUSTO_ZERO_DECLARADO"
                ? "Custo zero declarado"
                : "Referências repetidas"
          }
          bloqueado={false}
          onFechar={() => {
            setConsulta(null);
            setOperacaoId(null);
          }}
        >
          {carregando && <p role="status">Consultando registros…</p>}
          {erro && (
            <div>
              <p role="alert">{erro}</p>
              <button
                type="button"
                className="secondary-button"
                onClick={() =>
                  pedidoDetalhe
                    ? setPedidoDetalhe({ ...pedidoDetalhe })
                    : consulta && setConsulta({ ...consulta })
                }
              >
                Tentar novamente
              </button>
            </div>
          )}
          {desatualizado ? (
            <div>
              <p role="status">O histórico mudou. Atualize o painel antes de conferir os avisos.</p>
              <button className="primary-button" type="button" onClick={onAtualizar}>
                Atualizar painel
              </button>
            </div>
          ) : operacaoId ? (
            <>
              {operacao && (
                <DetalheOperacao
                  operacao={operacao}
                  onAnterior={setOperacaoId}
                  onSaldos={onOperacoes}
                />
              )}
              <button
                type="button"
                className="secondary-button"
                onClick={() => {
                  setOperacaoId(null);
                  setErro("");
                }}
              >
                Voltar aos avisos
              </button>
            </>
          ) : (
            lista && (
              <>
                <p className="overview-note">
                  {tipo === "CUSTO_ZERO_DECLARADO"
                    ? "O custo foi declarado como zero com justificativa. Revise a documentação de origem se necessário."
                    : "A mesma referência aparece em vários registros da instituição. Execuções distintas podem compartilhar uma referência."}
                </p>
                {lista.itens.length === 0 ? (
                  <p>Nenhum aviso nesta página.</p>
                ) : (
                  <ul className="review-items">
                    {lista.itens.map((item) => (
                      <li className="review-item" key={item.operacaoId}>
                        <strong>
                          {item.ativoTicker} · {item.instituicaoNome}
                        </strong>
                        <span>{dataHoraFortaleza(item.ocorridaEm)}</span>
                        <span>
                          {tipo === "CUSTO_ZERO_DECLARADO" ? item.justificativa : item.referencia}
                        </span>
                        <button
                          type="button"
                          className="secondary-button"
                          onClick={() => setOperacaoId(item.operacaoId)}
                        >
                          Ver registro #{item.operacaoId}
                        </button>
                      </li>
                    ))}
                  </ul>
                )}
                <div className="review-pagination">
                  <button
                    type="button"
                    className="secondary-button"
                    disabled={pagina === 0 || carregando}
                    onClick={() => setConsulta({ tipo, pagina: pagina - 1 })}
                  >
                    Anterior
                  </button>
                  <span>
                    Página {lista.pagina + 1} · {lista.total} registros
                  </span>
                  <button
                    type="button"
                    className="secondary-button"
                    disabled={(pagina + 1) * lista.tamanho >= lista.total || carregando}
                    onClick={() => setConsulta({ tipo, pagina: pagina + 1 })}
                  >
                    Próxima
                  </button>
                </div>
              </>
            )
          )}
        </Modal>
      )}
    </section>
  );
}
