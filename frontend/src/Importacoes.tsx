import { useCallback, useEffect, useRef, useState } from "react";
import ImportacaoForm from "./ImportacaoForm";
import DetalheOperacao from "./DetalheOperacao";
import Modal from "./Modal";
import ReciboArquivo from "./ReciboArquivo";
import { listarImportacoes, obterImportacao } from "./services/importacoes";
import type { ListaImportacoes, ReciboImportacao } from "./services/importacoes";
import { dataHoraFortaleza, mensagemOperacao, obterOperacao } from "./services/operacoes";
import type { Operacao } from "./services/operacoes";
export default function Importacoes({
  onBloqueio,
  reciboId,
  onFecharRecibo,
}: {
  onBloqueio: (b: boolean) => void;
  reciboId?: string;
  onFecharRecibo?: () => void;
}) {
  const somenteRecibos = Boolean(reciboId);
  const [pagina, setPagina] = useState({ numero: 0 });
  const [lista, setLista] = useState<ListaImportacoes | null>(null);
  const [erro, setErro] = useState("");
  const [carregando, setCarregando] = useState(false);
  const [bloqueado, setBloqueado] = useState(false);
  const [detalhe, setDetalhe] = useState<ReciboImportacao | null>(null);
  const [operacaoAberta, setOperacaoAberta] = useState(false);
  const [operacao, setOperacao] = useState<Operacao | null>(null);
  const [erroOperacao, setErroOperacao] = useState("");
  const [carregandoOperacao, setCarregandoOperacao] = useState(false);
  const consultaDetalhe = useRef<AbortController | null>(null);
  const consultaOperacao = useRef<AbortController | null>(null);
  const bloquear = useCallback(
    (valor: boolean) => {
      setBloqueado(valor);
      onBloqueio(valor);
    },
    [onBloqueio],
  );
  useEffect(() => {
    if (reciboId) {
      const controller = new AbortController();
      void obterImportacao(reciboId, controller.signal)
        .then((resposta) => {
          if (!controller.signal.aborted) setDetalhe(resposta);
        })
        .catch((falha) => {
          if (!controller.signal.aborted) setErro(mensagemOperacao(falha));
        });
      return () => controller.abort();
    }
    const controller = new AbortController();
    setCarregando(true);
    setErro("");
    void listarImportacoes(pagina.numero, controller.signal)
      .then((resposta) => {
        if (!controller.signal.aborted) setLista(resposta);
      })
      .catch((falha) => {
        if (!controller.signal.aborted) setErro(mensagemOperacao(falha));
      })
      .finally(() => {
        if (!controller.signal.aborted) setCarregando(false);
      });
    return () => controller.abort();
  }, [pagina, reciboId]);
  useEffect(() => () => consultaDetalhe.current?.abort(), []);
  useEffect(() => () => consultaOperacao.current?.abort(), []);
  async function abrirOperacao(id: string) {
    consultaOperacao.current?.abort();
    const controller = new AbortController();
    consultaOperacao.current = controller;
    setOperacaoAberta(true);
    setCarregandoOperacao(true);
    setErroOperacao("");
    setOperacao(null);
    try {
      const resposta = await obterOperacao(id, controller.signal);
      if (!controller.signal.aborted) setOperacao(resposta);
    } catch (falha) {
      if (!controller.signal.aborted) setErroOperacao(mensagemOperacao(falha));
    } finally {
      if (!controller.signal.aborted) setCarregandoOperacao(false);
    }
  }
  function voltarAoRecibo() {
    consultaOperacao.current?.abort();
    setOperacaoAberta(false);
    setOperacao(null);
    setErroOperacao("");
    setCarregandoOperacao(false);
  }
  function fecharDetalhe() {
    if (operacaoAberta) voltarAoRecibo();
    else if (onFecharRecibo) onFecharRecibo();
    else setDetalhe(null);
  }
  async function abrir(id: string) {
    consultaDetalhe.current?.abort();
    const controller = new AbortController();
    consultaDetalhe.current = controller;
    setErro("");
    try {
      const resposta = await obterImportacao(id, controller.signal);
      if (!controller.signal.aborted) setDetalhe(resposta);
    } catch (falha) {
      if (!controller.signal.aborted) setErro(mensagemOperacao(falha));
    }
  }
  return (
    <section
      aria-label={somenteRecibos ? "Recibos dos relatórios anteriores" : undefined}
      aria-labelledby={somenteRecibos ? undefined : "page-title"}
    >
      {!somenteRecibos && (
        <div className="intro">
          <p className="eyebrow">SEU HISTÓRICO</p>
          <h1 id="page-title">Importar operações</h1>
          <p className="intro-text">
            Confira e registre as operações do seu arquivo em uma única confirmação.
          </p>
        </div>
      )}
      {!somenteRecibos && (
        <ImportacaoForm
          onBloqueio={bloquear}
          onOperacao={(id) => void abrirOperacao(id)}
          onConcluido={() => {
            setPagina({ numero: 0 });
          }}
        />
      )}
      {!somenteRecibos && (
        <section className="operations-panel import-panel" aria-label="Importações anteriores">
          {!somenteRecibos && <h2>Importações anteriores</h2>}
          {erro && (
            <p role="alert" className="message-error">
              {erro}
            </p>
          )}
          {carregando && !lista ? (
            <p role="status">Carregando importações…</p>
          ) : erro ? (
            <button
              type="button"
              className="secondary-button"
              disabled={bloqueado}
              onClick={() => setPagina((p) => ({ ...p }))}
            >
              Tentar novamente
            </button>
          ) : (
            lista && (
              <>
                {lista.itens.length === 0 ? (
                  <p className="empty-state">Nenhuma importação encontrada.</p>
                ) : (
                  <div className="table-scroll">
                    <table>
                      <thead>
                        <tr>
                          <th>Arquivo</th>
                          <th>Recebido em</th>
                          <th>Novos</th>
                          <th>Já registrados</th>
                          <th>Ações</th>
                        </tr>
                      </thead>
                      <tbody>
                        {lista.itens.map((item) => (
                          <tr key={item.id}>
                            <td>{item.nomeArquivo}</td>
                            <td>{dataHoraFortaleza(item.criadoEm)}</td>
                            <td>{item.novos}</td>
                            <td>{item.reconhecidos}</td>
                            <td>
                              <button
                                type="button"
                                className="secondary-button"
                                aria-label={`Detalhes de ${item.nomeArquivo}`}
                                disabled={bloqueado}
                                onClick={() => void abrir(item.id)}
                              >
                                Detalhes
                              </button>
                            </td>
                          </tr>
                        ))}
                      </tbody>
                    </table>
                  </div>
                )}
                {lista.total > lista.tamanho && (
                  <div className="pagination">
                    <span>{lista.total} importação(ões)</span>
                    <button
                      type="button"
                      className="secondary-button"
                      disabled={pagina.numero === 0 || bloqueado}
                      onClick={() => setPagina((p) => ({ numero: p.numero - 1 }))}
                    >
                      Página anterior
                    </button>
                    <span>Página {pagina.numero + 1}</span>
                    <button
                      type="button"
                      className="secondary-button"
                      disabled={(pagina.numero + 1) * lista.tamanho >= lista.total || bloqueado}
                      onClick={() => setPagina((p) => ({ numero: p.numero + 1 }))}
                    >
                      Próxima página
                    </button>
                  </div>
                )}
              </>
            )
          )}
        </section>
      )}
      {(reciboId || detalhe || operacaoAberta) && (
        <Modal
          titulo={operacaoAberta ? "Detalhes da operação" : "Detalhes da importação"}
          bloqueado={false}
          onFechar={fecharDetalhe}
        >
          {reciboId &&
            !detalhe &&
            (erro ? <p role="alert">{erro}</p> : <p role="status">Carregando recibo…</p>)}
          {detalhe && (
            <div hidden={operacaoAberta}>
              <ReciboArquivo recibo={detalhe} onOperacao={(id) => void abrirOperacao(id)} />
            </div>
          )}
          {operacaoAberta && (
            <>
              {carregandoOperacao && <p role="status">Carregando operação…</p>}
              {erroOperacao && (
                <p role="alert" className="message-error">
                  {erroOperacao}
                </p>
              )}
              {operacao && (
                <DetalheOperacao operacao={operacao} onAnterior={(id) => void abrirOperacao(id)} />
              )}
              <div className="form-actions">
                <button type="button" className="secondary-button" onClick={voltarAoRecibo}>
                  Voltar ao recibo
                </button>
              </div>
            </>
          )}
        </Modal>
      )}
    </section>
  );
}
