import PageHeader from "./PageHeader";
import { useCallback, useEffect, useRef, useState } from "react";
import Icone from "./Icone";
import Modal from "./Modal";
import CarteiraForm from "./CarteiraForm";
import {
  alterarSituacaoCarteira,
  cadastrarCarteira,
  listarCarteiras,
  alterarCarteira,
} from "./services/carteiras";
import type { Carteira, CadastroCarteira } from "./services/carteiras";

export default function Carteiras() {
  const ultimaConsulta = useRef(0);
  const [carteiras, setCarteiras] = useState<Carteira[]>([]);
  const [carregando, setCarregando] = useState(true);
  const [salvando, setSalvando] = useState(false);
  const [modal, setModal] = useState<{ carteira: Carteira | null } | null>(null);
  const [erroLista, setErroLista] = useState("");
  const [erroAcao, setErroAcao] = useState("");
  const [aviso, setAviso] = useState("");

  const carregar = useCallback(async (signal?: AbortSignal) => {
    const consulta = ++ultimaConsulta.current;
    setCarregando(true);
    setErroLista("");
    try {
      const dados = await listarCarteiras(signal);
      if (!signal?.aborted && consulta === ultimaConsulta.current) setCarteiras(dados);
    } catch (erro) {
      if (!signal?.aborted && consulta === ultimaConsulta.current) setErroLista(mensagem(erro));
    } finally {
      if (!signal?.aborted && consulta === ultimaConsulta.current) setCarregando(false);
    }
  }, []);

  useEffect(() => {
    const controller = new AbortController();
    void carregar(controller.signal);
    return () => controller.abort();
  }, [carregar]);

  function abrirModal(carteira: Carteira | null) {
    setErroAcao("");
    setAviso("");
    setModal({ carteira });
  }

  async function salvar(cadastro: CadastroCarteira): Promise<boolean> {
    if (!modal || salvando) return false;
    setSalvando(true);
    setErroAcao("");
    setAviso("");
    try {
      if (modal.carteira) await alterarCarteira(modal.carteira.id, cadastro);
      else await cadastrarCarteira(cadastro);
      setAviso(modal.carteira ? "Carteira atualizada." : "Carteira cadastrada.");
      await carregar();
      setModal(null);
      return true;
    } catch (erro) {
      setErroAcao(mensagem(erro));
      return false;
    } finally {
      setSalvando(false);
    }
  }

  async function mudarSituacao(carteira: Carteira) {
    setSalvando(true);
    setErroAcao("");
    setAviso("");
    try {
      await alterarSituacaoCarteira(carteira.id, !carteira.ativo);
      setAviso(carteira.ativo ? "Carteira arquivada." : "Carteira reativada.");
      await carregar();
    } catch (erro) {
      setErroAcao(mensagem(erro));
    } finally {
      setSalvando(false);
    }
  }

  return (
    <section aria-labelledby="page-title">
      <PageHeader
        id="page-title"
        eyebrow="CADASTROS"
        title="Carteiras"
        description="Organize suas contas em carteiras de investimento."
      />
      {aviso && (
        <p className="message-success" role="status">
          {aviso}
        </p>
      )}
      {erroAcao && !modal && (
        <p className="message-error" role="alert">
          {erroAcao}
        </p>
      )}
      <section className="carteiras-list" aria-label="Lista de carteiras">
        <div className="list-heading">
          <h2>Suas carteiras</h2>
          <button
            className="primary-button add-button"
            type="button"
            disabled={salvando}
            onClick={() => abrirModal(null)}
          >
            <Icone nome="adicionar" /> Adicionar carteira
          </button>
        </div>
        <div aria-busy={carregando}>
          {carregando ? (
            <p role="status">Carregando carteiras…</p>
          ) : erroLista ? (
            <div>
              <p className="message-error" role="alert">
                {erroLista}
              </p>
              <button className="secondary-button" type="button" onClick={() => void carregar()}>
                Tentar novamente
              </button>
            </div>
          ) : carteiras.length === 0 ? (
            <p className="empty-state">Nenhuma carteira cadastrada.</p>
          ) : (
            <div className="table-scroll">
              <table>
                <thead>
                  <tr>
                    <th>Nome</th>
                    <th>Contas</th>
                    <th>Situação</th>
                    <th>Ações</th>
                  </tr>
                </thead>
                <tbody>
                  {carteiras.map((carteira) => (
                    <tr
                      key={carteira.id}
                      className={carteira.ativo ? undefined : "carteira-arquivada"}
                    >
                      <td>{carteira.nome}</td>
                      <td>{carteira.contas.map((conta) => conta.instituicaoNome).join(", ")}</td>
                      <td>
                        <span className={`badge ${carteira.ativo ? "" : "badge-archived"}`}>
                          {carteira.ativo ? "Ativa" : "Arquivada"}
                        </span>
                      </td>
                      <td>
                        <div className="row-actions">
                          <button
                            className="icon-button"
                            type="button"
                            aria-label={`Editar ${carteira.nome}`}
                            title="Editar carteira"
                            disabled={salvando || !carteira.ativo}
                            onClick={() => abrirModal(carteira)}
                          >
                            <Icone nome="editar" />
                          </button>
                          <button
                            className="icon-button"
                            type="button"
                            aria-label={`${carteira.ativo ? "Arquivar" : "Reativar"} ${carteira.nome}`}
                            title={carteira.ativo ? "Arquivar carteira" : "Reativar carteira"}
                            disabled={salvando}
                            onClick={() => void mudarSituacao(carteira)}
                          >
                            <Icone nome={carteira.ativo ? "arquivar" : "reativar"} />
                          </button>
                        </div>
                      </td>
                    </tr>
                  ))}
                </tbody>
              </table>
            </div>
          )}
        </div>
      </section>
      {modal && (
        <Modal
          titulo={modal.carteira ? "Editar carteira" : "Adicionar carteira"}
          bloqueado={salvando}
          onFechar={() => setModal(null)}
        >
          <CarteiraForm
            carteira={modal.carteira}
            salvando={salvando}
            erroServidor={erroAcao}
            onSalvar={salvar}
            onCancelar={() => setModal(null)}
          />
        </Modal>
      )}
    </section>
  );
}

function mensagem(erro: unknown): string {
  return erro instanceof Error
    ? erro.message
    : "Não foi possível concluir a ação. Tente novamente.";
}
