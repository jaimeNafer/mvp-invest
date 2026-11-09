import { useEffect, useState } from "react";
import Modal from "./Modal";
import RecebimentoHistorico from "./RecebimentoHistorico";
import Historico from "./Historico";
import Importacoes from "./Importacoes";
import { listarCarteiras } from "./services/carteiras";
import type { Carteira } from "./services/carteiras";
import { mensagemConferencia } from "./services/conciliacao";
import {
  excluirRecebimento,
  listarArquivosRecebidos,
  listarImportacoes,
} from "./services/importacoes";
import "./importacoes-progressivas.css";

type ReciboHistorico = {
  itens: {
    id: string;
    nomeArquivo: string;
    criadoEm: string;
    registros: number;
    legado?: boolean;
  }[];
  pagina: number;
  tamanho: number;
  total: number;
};

export default function ImportacoesProgressivas({
  carteiraId,
  onBloqueio,
  onOperacoes,
  onConferencia,
  onEditarCarteiras,
  onCriarCarteira,
}: {
  carteiraId?: string;
  onBloqueio: (b: boolean) => void;
  onOperacoes: (arquivoId: string, carteiraId?: string) => void;
  onConferencia: () => void;
  onEditarCarteiras?: () => void;
  onCriarCarteira?: () => void;
}) {
  const [carteiras, setCarteiras] = useState<Carteira[]>([]);
  const [carregandoCarteiras, setCarregandoCarteiras] = useState(true);
  const [erroCarteiras, setErroCarteiras] = useState("");
  const [instituicaoId, setInstituicaoId] = useState("");
  const [selecaoCarteira, setSelecaoCarteira] = useState(carteiraId ?? "");
  const [bloqueado, setBloqueado] = useState(false);
  const [reciboLegado, setReciboLegado] = useState<string>();
  const [recibo, setRecibo] = useState<{ id: string; nomeArquivo: string }[]>([]);
  const [pagina, setPagina] = useState(0);
  const [historico, setHistorico] = useState<ReciboHistorico | null>(null);
  const [erroHistorico, setErroHistorico] = useState("");
  const [atualizacaoHistorico, setAtualizacaoHistorico] = useState(0);
  const [recebimentoParaExcluir, setRecebimentoParaExcluir] =
    useState<ReciboHistorico["itens"][number]>();
  const [excluindo, setExcluindo] = useState(false);
  const [erroExclusao, setErroExclusao] = useState("");

  async function excluir() {
    if (!recebimentoParaExcluir) return;
    setExcluindo(true);
    setErroExclusao("");
    try {
      const id = recebimentoParaExcluir.legado
        ? `importacao:${recebimentoParaExcluir.id}`
        : recebimentoParaExcluir.id;
      await excluirRecebimento(id);
      setRecebimentoParaExcluir(undefined);
      setAtualizacaoHistorico((valor) => valor + 1);
    } catch (falha) {
      setErroExclusao(mensagemConferencia(falha));
    } finally {
      setExcluindo(false);
    }
  }

  useEffect(() => {
    const controller = new AbortController();
    void listarCarteiras(controller.signal)
      .then((resposta) => {
        if (controller.signal.aborted) return;
        setCarteiras(resposta);
        const carteira = carteiraId
          ? resposta.find((item) => item.id === carteiraId && item.ativo)
          : resposta.filter((item) => item.ativo).length === 1
            ? resposta.find((item) => item.ativo)
            : undefined;
        setSelecaoCarteira(carteira?.id ?? "");
        setInstituicaoId((atual) => selecionarInstituicao(carteira, atual));
      })
      .catch((falha) => {
        if (!controller.signal.aborted) setErroCarteiras(mensagemConferencia(falha));
      })
      .finally(() => {
        if (!controller.signal.aborted) setCarregandoCarteiras(false);
      });
    return () => controller.abort();
  }, [carteiraId]);

  useEffect(() => {
    const controller = new AbortController();
    if (atualizacaoHistorico > 0) setErroHistorico("");
    void Promise.allSettled([
      listarArquivosRecebidos(pagina, controller.signal),
      listarImportacoes(pagina, controller.signal),
    ]).then(([progressivos, anteriores]) => {
      if (controller.signal.aborted) return;
      const recentes = progressivos.status === "fulfilled" ? progressivos.value : null;
      const antigos = anteriores.status === "fulfilled" ? anteriores.value : null;
      const itens = [
        ...(recentes?.itens ?? []),
        ...(antigos?.itens ?? []).map((item) => ({
          ...item,
          legado: true,
          registros: item.novos + item.reconhecidos,
        })),
      ].sort((a, b) => b.criadoEm.localeCompare(a.criadoEm));
      setHistorico({
        itens,
        pagina,
        tamanho: 20,
        total: Math.max(recentes?.total ?? 0, antigos?.total ?? 0),
      });
      setErroHistorico(
        recentes && antigos ? "" : "Parte do histórico está temporariamente indisponível.",
      );
    });
    return () => controller.abort();
  }, [pagina, atualizacaoHistorico]);

  const ativas = carteiras.filter((item) => item.ativo);
  const carteira = carteiras.find((item) => item.id === selecaoCarteira && item.ativo);
  const temBinance = !!carteira?.contas.some(
    (conta) =>
      conta.instituicaoId === instituicaoId && conta.instituicaoNome.toLowerCase() === "binance",
  );
  function bloquear(valor: boolean) {
    setBloqueado(valor);
    onBloqueio(valor);
  }

  return (
    <section className="importacoes-progressivas" aria-labelledby="importacoes-progressivas-titulo">
      <header className="intro">
        <p className="eyebrow">SEU HISTÓRICO</p>
        <h1 id="importacoes-progressivas-titulo">Importação de operações</h1>
        <p className="intro-text">
          Receba arquivos da Binance e acompanhe a conferência do histórico.
        </p>
      </header>
      {erroCarteiras && <p role="alert">{erroCarteiras}</p>}
      {carregandoCarteiras ? (
        <p role="status">Carregando carteiras…</p>
      ) : ativas.length ? (
        <section
          className="operations-panel importacao-contexto"
          aria-labelledby="destino-importacao"
        >
          <h2 id="destino-importacao">Destino da importação</h2>
          <div className="saldo-filtros importacao-campos">
            <label>
              Carteira
              <select
                value={selecaoCarteira}
                disabled={bloqueado}
                onChange={(evento) => {
                  const id = evento.target.value;
                  setSelecaoCarteira(id);
                  setInstituicaoId((atual) =>
                    selecionarInstituicao(
                      ativas.find((item) => item.id === id),
                      atual,
                    ),
                  );
                }}
              >
                <option value="">Selecione uma carteira</option>
                {ativas.map((item) => (
                  <option key={item.id} value={item.id}>
                    {item.nome}
                  </option>
                ))}
              </select>
            </label>
            <label>
              Instituição
              <select
                value={instituicaoId}
                disabled={bloqueado || !carteira?.contas.length}
                onChange={(evento) => setInstituicaoId(evento.target.value)}
              >
                <option value="">Selecione uma instituição</option>
                {carteira?.contas.map((conta) => (
                  <option
                    key={conta.instituicaoId}
                    value={conta.instituicaoId}
                    disabled={!aceitaArquivos(conta.instituicaoNome)}
                  >
                    {conta.instituicaoNome}
                  </option>
                ))}
              </select>
            </label>
          </div>
          {!temBinance && carteira && (
            <div className="importacao-acoes">
              <p>Vincule a Binance à carteira para importar arquivos.</p>
              {onEditarCarteiras && (
                <button type="button" className="secondary-button" onClick={onEditarCarteiras}>
                  Gerenciar carteiras
                </button>
              )}
            </div>
          )}
          <RecebimentoHistorico
            disabled={bloqueado || !temBinance}
            onBloqueio={bloquear}
            onRecebido={async () => {}}
            onArquivoRecebido={(arquivos) => {
              setRecibo(arquivos);
              setAtualizacaoHistorico((valor) => valor + 1);
            }}
          />
        </section>
      ) : (
        <section className="operations-panel importacao-requisito">
          <div>
            <h2>Prepare sua carteira</h2>
            <p>Para importar arquivos, vincule a Binance a uma carteira ativa.</p>
          </div>
          {onCriarCarteira && (
            <button type="button" className="primary-button" onClick={onCriarCarteira}>
              Criar carteira
            </button>
          )}
        </section>
      )}
      {recibo.length > 0 && (
        <section className="operations-panel" aria-label="Ações após o recebimento">
          <h2>Próximas ações</h2>
          <p>Revise o resumo e aplique o lote. Pendências individuais ficam na Conferência.</p>
          <Historico modoAplicacao bloqueioExterno={bloqueado} onBloqueio={bloquear} />
          <div className="importacao-acoes">
            <button
              className="secondary-button"
              type="button"
              disabled={bloqueado}
              onClick={onConferencia}
            >
              Conferir histórico
            </button>
            {recibo.map((arquivo) => (
              <button
                className="secondary-button"
                type="button"
                disabled={bloqueado}
                key={arquivo.id}
                onClick={() => onOperacoes(arquivo.id)}
              >
                Ver operações de {arquivo.nomeArquivo}
              </button>
            ))}
          </div>
        </section>
      )}
      <section className="operations-panel import-panel" aria-label="Recebimentos anteriores">
        <h2>Recebimentos anteriores</h2>
        {erroHistorico && <p role="alert">{erroHistorico}</p>}
        {!historico ? (
          <p role="status">Carregando recebimentos…</p>
        ) : historico.itens.length === 0 ? (
          <p className="empty-state">Nenhum recebimento encontrado.</p>
        ) : (
          <>
            <ul className="importacao-arquivos">
              {historico.itens.map((item) => (
                <li key={`${item.legado ? "recibo" : "arquivo"}:${item.id}`}>
                  <div>
                    <strong>{item.nomeArquivo}</strong>
                    <p>
                      Binance · {item.registros} {item.registros === 1 ? "registro" : "registros"}
                      {item.legado ? " · Concluído · " : " · Recebido · "}
                      {new Date(item.criadoEm).toLocaleDateString("pt-BR", {
                        timeZone: "America/Fortaleza",
                      })}
                    </p>
                  </div>
                  <div className="importacao-acoes">
                    <button
                      className="secondary-button"
                      type="button"
                      disabled={bloqueado}
                      onClick={() =>
                        item.legado ? setReciboLegado(item.id) : onOperacoes(item.id)
                      }
                    >
                      {item.legado ? "Ver recibo" : "Ver operações"}
                    </button>
                    <button
                      className="secondary-button"
                      type="button"
                      aria-label={`Excluir ${item.nomeArquivo}`}
                      disabled={bloqueado || excluindo}
                      onClick={() => {
                        setErroExclusao("");
                        setRecebimentoParaExcluir(item);
                      }}
                    >
                      Excluir
                    </button>
                  </div>
                </li>
              ))}
            </ul>
            {historico.total > historico.tamanho && (
              <nav className="pagination" aria-label="Páginas de recebimentos">
                <button
                  className="secondary-button"
                  type="button"
                  disabled={pagina === 0}
                  onClick={() => setPagina(pagina - 1)}
                >
                  Página anterior
                </button>
                <span>Página {pagina + 1}</span>
                <button
                  className="secondary-button"
                  type="button"
                  disabled={(pagina + 1) * historico.tamanho >= historico.total}
                  onClick={() => setPagina(pagina + 1)}
                >
                  Próxima página
                </button>
              </nav>
            )}
          </>
        )}
      </section>
      {reciboLegado && (
        <Importacoes
          key={reciboLegado}
          onBloqueio={bloquear}
          reciboId={reciboLegado}
          onFecharRecibo={() => setReciboLegado(undefined)}
        />
      )}
      {recebimentoParaExcluir && (
        <Modal
          titulo="Excluir recebimento"
          bloqueado={excluindo}
          onFechar={() => setRecebimentoParaExcluir(undefined)}
        >
          <p>
            Arquivo: <strong>{recebimentoParaExcluir.nomeArquivo}</strong>
          </p>
          <p>
            A exclusão é permanente. O arquivo e os dados associados a este recebimento serão
            removidos.
          </p>
          {erroExclusao && (
            <p role="alert" className="message-error">
              {erroExclusao}
            </p>
          )}
          <div className="form-actions">
            <button
              type="button"
              className="secondary-button"
              disabled={excluindo}
              onClick={() => setRecebimentoParaExcluir(undefined)}
            >
              Cancelar
            </button>
            <button
              type="button"
              className="primary-button"
              disabled={excluindo}
              onClick={() => void excluir()}
            >
              {excluindo ? "Excluindo…" : "Excluir recebimento"}
            </button>
          </div>
        </Modal>
      )}
    </section>
  );
}

function aceitaArquivos(nome: string) {
  return nome.toLowerCase() === "binance";
}

function selecionarInstituicao(carteira: Carteira | undefined, atual: string) {
  const elegiveis = carteira?.contas.filter((conta) => aceitaArquivos(conta.instituicaoNome)) ?? [];
  if (elegiveis.some((conta) => conta.instituicaoId === atual)) return atual;
  return elegiveis.length === 1 ? elegiveis[0].instituicaoId : "";
}
