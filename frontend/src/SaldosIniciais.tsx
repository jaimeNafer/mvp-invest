import PosicoesIniciais from "./PosicoesIniciais";
import FiltrosSaldos from "./FiltrosSaldos";
import { useCallback, useEffect, useRef, useState } from "react";
import Modal from "./Modal";
import Icone from "./Icone";
import SaldoInicialForm, { dataPtBr } from "./SaldoInicialForm";
import { listarCarteiras } from "./services/carteiras";
import type { Carteira } from "./services/carteiras";
import { listarInstituicoes } from "./services/instituicoes";
import type { Instituicao } from "./services/instituicoes";
import { decimalPtBr, moeda } from "./services/decimais";
import { ErroHttp } from "./services/http";
import {
  cancelarSaldo,
  gravarSaldo,
  listarAtivos,
  listarPosicoesIniciais,
  listarSaldos,
  obterSaldo,
} from "./services/saldosIniciais";
import type {
  Ativo,
  CadastroSaldo,
  FiltrosSaldo,
  ListaSaldos,
  PosicaoInicial,
  Saldo,
} from "./services/saldosIniciais";

type Dados = {
  ativos: Ativo[];
  instituicoes: Instituicao[];
  carteiras: Carteira[];
  saldos: ListaSaldos;
  posicoes: PosicaoInicial[];
};
type Janela =
  | { tipo: "form"; saldo: Saldo | null }
  | { tipo: "cancelar" | "detalhe"; saldo: Saldo };
type Tentativa =
  | { tipo: "gravar"; saldo: Saldo | null; entrada: CadastroSaldo; chave: string; incerta: boolean }
  | { tipo: "cancelar"; saldo: Saldo; motivo: string; incerta: boolean };
const inicial: FiltrosSaldo = {
  pagina: 0,
  instituicaoId: "",
  ativoId: "",
  carteiraIds: [],
  situacao: "CONFIRMADA",
};
const situacao = { CONFIRMADA: "Confirmado", SUBSTITUIDA: "Substituído", CANCELADA: "Cancelado" };

export default function SaldosIniciais() {
  const [dados, setDados] = useState<Dados | null>(null);
  const [filtros, setFiltros] = useState(inicial);
  const [carregando, setCarregando] = useState(true);
  const [erroLista, setErroLista] = useState("");
  const [erro, setErro] = useState("");
  const [aviso, setAviso] = useState("");
  const [salvando, setSalvando] = useState(false);
  const [janela, setJanela] = useState<Janela | null>(null);
  const [tentativa, setTentativa] = useState<Tentativa | null>(null);
  const [motivo, setMotivo] = useState("");
  const consultaAtual = useRef(0);

  const carregar = useCallback(
    async (signal?: AbortSignal) => {
      const consulta = ++consultaAtual.current;
      setCarregando(true);
      setErroLista("");
      try {
        const [ativos, instituicoes, carteiras, saldos, posicoes] = await Promise.all([
          listarAtivos(signal),
          listarInstituicoes(signal),
          listarCarteiras(signal),
          listarSaldos(filtros, signal),
          listarPosicoesIniciais(signal),
        ]);
        if (!signal?.aborted && consulta === consultaAtual.current)
          setDados({ ativos, instituicoes, carteiras, saldos, posicoes });
      } catch (falha) {
        if (!signal?.aborted && consulta === consultaAtual.current) setErroLista(mensagem(falha));
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

  function abrir(janela: Janela) {
    setErro("");
    setAviso("");
    setMotivo("");
    setJanela(janela);
  }
  function fechar() {
    setJanela(null);
    if (tentativa?.incerta) void carregar();
  }
  function retomar() {
    if (!tentativa) return;
    setErro("");
    if (tentativa.tipo === "cancelar") {
      setMotivo(tentativa.motivo);
      setJanela({ tipo: "cancelar", saldo: tentativa.saldo });
    } else setJanela({ tipo: "form", saldo: tentativa.saldo });
  }

  async function executar(comando: Tentativa) {
    if (salvando) return;
    setSalvando(true);
    setErro("");
    setAviso("");
    setTentativa(comando);
    try {
      if (comando.tipo === "cancelar") await cancelarSaldo(comando.saldo.id, comando.motivo);
      else await gravarSaldo(comando.entrada, comando.chave, comando.saldo?.id);
      setTentativa(null);
      setJanela(null);
      setAviso(
        comando.tipo === "cancelar"
          ? "Saldo inicial cancelado. O registro foi preservado no histórico."
          : comando.saldo
            ? "Saldo inicial corrigido. A versão anterior foi preservada."
            : "Saldo inicial confirmado.",
      );
      await carregar();
    } catch (falha) {
      const incerta = !(falha instanceof ErroHttp) || falha.status >= 500;
      setTentativa(incerta ? { ...comando, incerta: true } : null);
      setErro(mensagem(falha));
    } finally {
      setSalvando(false);
    }
  }

  function salvar(entrada: CadastroSaldo) {
    if (janela?.tipo !== "form") return;
    const comando =
      tentativa?.tipo === "gravar"
        ? tentativa
        : {
            tipo: "gravar" as const,
            saldo: janela.saldo,
            entrada,
            chave: crypto.randomUUID(),
            incerta: false,
          };
    void executar(comando);
  }

  async function anterior(id: string) {
    setErro("");
    try {
      setJanela({ tipo: "detalhe", saldo: await obterSaldo(id) });
    } catch (falha) {
      setErro(mensagem(falha));
    }
  }

  function filtrar(alteracao: Partial<FiltrosSaldo>) {
    setFiltros((f) => ({ ...f, ...alteracao, pagina: alteracao.pagina ?? 0 }));
  }

  return (
    <section aria-labelledby="page-title">
      <div className="intro">
        <p className="eyebrow">SEU HISTÓRICO</p>
        <h1 id="page-title">Operações</h1>
        <p className="intro-text">
          Registre a posição inicial dos seus investimentos e acompanhe sua base de custo.
        </p>
      </div>
      {aviso && (
        <p role="status" className="message-success">
          {aviso}
        </p>
      )}
      {tentativa?.incerta && !janela && (
        <div className="pending-request" role="status">
          <p>
            Há uma solicitação cujo resultado ainda não foi confirmado. Verifique o envio antes de
            registrar outra alteração.
          </p>
          <button className="secondary-button" type="button" onClick={retomar}>
            Verificar solicitação
          </button>
        </div>
      )}
      {erro && !janela && (
        <p className="message-error" role="alert">
          {erro}
        </p>
      )}
      {carregando && !dados ? <p role="status">Carregando operações…</p> : null}
      {erroLista && (
        <div>
          <p role="alert" className="message-error">
            {erroLista}
          </p>
          <button type="button" className="secondary-button" onClick={() => void carregar()}>
            Tentar novamente
          </button>
        </div>
      )}
      {dados && (
        <>
          <section className="operations-panel" aria-label="Saldos iniciais" aria-busy={carregando}>
            <div className="list-heading">
              <div>
                <h2>Saldos iniciais</h2>
                <p className="field-hint">
                  {dados.saldos.dataReferencia
                    ? `Início do acompanhamento: ${dataPtBr(dados.saldos.dataReferencia)}`
                    : "Informe sua posição na data de início do acompanhamento."}
                </p>
              </div>
              <button
                className="primary-button add-button"
                type="button"
                disabled={salvando || !!tentativa}
                onClick={() => abrir({ tipo: "form", saldo: null })}
              >
                <Icone nome="adicionar" /> Adicionar saldo inicial
              </button>
            </div>
            <FiltrosSaldos
              instituicoes={dados.instituicoes}
              ativos={dados.ativos}
              carteiras={dados.carteiras}
              filtros={filtros}
              onFiltrar={filtrar}
            />
            {dados.saldos.itens.length === 0 ? (
              <p className="empty-state">Nenhum saldo inicial encontrado.</p>
            ) : (
              <div className="table-scroll">
                <table>
                  <thead>
                    <tr>
                      <th>Data</th>
                      <th>Instituição</th>
                      <th>Ativo</th>
                      <th>Quantidade</th>
                      <th>Custo inicial informado</th>
                      <th>Situação</th>
                      <th>Ações</th>
                    </tr>
                  </thead>
                  <tbody>
                    {dados.saldos.itens.map((s) => (
                      <tr
                        key={s.id}
                        className={s.situacao !== "CONFIRMADA" ? "carteira-arquivada" : undefined}
                      >
                        <td>{dataPtBr(s.dataReferencia)}</td>
                        <td>{s.instituicaoNome}</td>
                        <td>{s.ativoTicker}</td>
                        <td>{decimalPtBr(s.quantidade)}</td>
                        <td title={`Valor completo: R$ ${decimalPtBr(s.custoTotalBrl)}`}>
                          {moeda(s.custoTotalBrl)}
                        </td>
                        <td>
                          <span
                            className={`badge ${s.situacao === "CONFIRMADA" ? "" : "badge-archived"}`}
                          >
                            {situacao[s.situacao]}
                          </span>
                        </td>
                        <td>
                          <div className="row-actions">
                            <button
                              type="button"
                              className="secondary-button"
                              aria-label={`Detalhes do saldo ${s.id}`}
                              onClick={() => abrir({ tipo: "detalhe", saldo: s })}
                            >
                              Detalhes
                            </button>
                            {s.situacao === "CONFIRMADA" && (
                              <>
                                <button
                                  type="button"
                                  className="icon-button"
                                  title="Corrigir saldo inicial"
                                  aria-label={`Corrigir saldo ${s.id}`}
                                  disabled={salvando || !!tentativa}
                                  onClick={() => abrir({ tipo: "form", saldo: s })}
                                >
                                  <Icone nome="editar" />
                                </button>
                                <button
                                  type="button"
                                  className="icon-button"
                                  title="Cancelar saldo inicial"
                                  aria-label={`Cancelar saldo ${s.id}`}
                                  disabled={salvando || !!tentativa}
                                  onClick={() => abrir({ tipo: "cancelar", saldo: s })}
                                >
                                  <Icone nome="fechar" />
                                </button>
                              </>
                            )}
                          </div>
                        </td>
                      </tr>
                    ))}
                  </tbody>
                </table>
              </div>
            )}
            <div className="pagination">
              <span>{dados.saldos.total} registro(s)</span>
              <button
                type="button"
                className="secondary-button"
                disabled={carregando || !!erroLista || dados.saldos.pagina === 0}
                onClick={() => filtrar({ pagina: filtros.pagina - 1 })}
              >
                Página anterior
              </button>
              <span>Página {dados.saldos.pagina + 1}</span>
              <button
                type="button"
                className="secondary-button"
                disabled={
                  carregando ||
                  !!erroLista ||
                  (dados.saldos.pagina + 1) * dados.saldos.tamanho >= dados.saldos.total
                }
                onClick={() => filtrar({ pagina: filtros.pagina + 1 })}
              >
                Próxima página
              </button>
            </div>
          </section>
          <PosicoesIniciais posicoes={dados.posicoes} />
        </>
      )}
      {janela && dados && (
        <Modal
          titulo={
            janela.tipo === "form"
              ? janela.saldo
                ? "Corrigir saldo inicial"
                : "Adicionar saldo inicial"
              : janela.tipo === "cancelar"
                ? "Cancelar saldo inicial"
                : "Detalhes do saldo inicial"
          }
          bloqueado={salvando}
          onFechar={fechar}
        >
          {janela.tipo === "form" ? (
            <SaldoInicialForm
              ativos={dados.ativos}
              instituicoes={dados.instituicoes}
              saldo={janela.saldo}
              dataReferencia={dados.saldos.dataReferencia}
              possuiConfirmados={dados.saldos.possuiSaldosConfirmados}
              salvando={salvando}
              erro={erro}
              pendente={
                tentativa?.tipo === "gravar" && tentativa.incerta ? tentativa.entrada : null
              }
              onSalvar={salvar}
              onCancelar={fechar}
            />
          ) : janela.tipo === "cancelar" ? (
            <form
              className="saldo-form"
              aria-label="Cancelar saldo inicial"
              onSubmit={(e) => {
                e.preventDefault();
                if (janela.tipo === "cancelar")
                  void executar(
                    tentativa?.tipo === "cancelar"
                      ? tentativa
                      : {
                          tipo: "cancelar",
                          saldo: janela.saldo,
                          motivo: motivo.trim(),
                          incerta: false,
                        },
                  );
              }}
            >
              <p>
                O registro será preservado no histórico e deixará de compor a posição inicial
                vigente.
              </p>
              <label htmlFor="motivo-cancelamento">Motivo do cancelamento</label>
              <textarea
                id="motivo-cancelamento"
                required
                maxLength={300}
                value={motivo}
                disabled={salvando || !!tentativa?.incerta}
                onChange={(e) => setMotivo(e.target.value)}
              />
              {erro && (
                <p role="alert" className="message-error">
                  {erro}
                </p>
              )}
              <div className="form-actions">
                <button className="primary-button" type="submit" disabled={salvando}>
                  {salvando
                    ? "Cancelando…"
                    : tentativa?.incerta
                      ? "Verificar solicitação"
                      : "Confirmar cancelamento"}
                </button>
                <button
                  className="secondary-button"
                  type="button"
                  disabled={salvando}
                  onClick={() => setJanela(null)}
                >
                  Voltar
                </button>
              </div>
            </form>
          ) : (
            <div className="saldo-form">
              <dl className="saldo-resumo">
                <dt>Registro</dt>
                <dd>#{janela.saldo.id}</dd>
                <dt>Situação</dt>
                <dd>{situacao[janela.saldo.situacao]}</dd>
                <dt>Instituição</dt>
                <dd>{janela.saldo.instituicaoNome}</dd>
                <dt>Ativo</dt>
                <dd>{janela.saldo.ativoTicker}</dd>
                <dt>Data de referência</dt>
                <dd>{dataPtBr(janela.saldo.dataReferencia)}</dd>
                <dt>Quantidade</dt>
                <dd>{decimalPtBr(janela.saldo.quantidade)}</dd>
                <dt>Custo histórico total</dt>
                <dd>R$ {decimalPtBr(janela.saldo.custoTotalBrl)}</dd>
                {janela.saldo.justificativaCustoZero && (
                  <>
                    <dt>Justificativa do custo zero</dt>
                    <dd>{janela.saldo.justificativaCustoZero}</dd>
                  </>
                )}
                {janela.saldo.motivoAlteracao && (
                  <>
                    <dt>Motivo da alteração</dt>
                    <dd>{janela.saldo.motivoAlteracao}</dd>
                  </>
                )}
                <dt>Registrado em</dt>
                <dd>
                  {new Date(janela.saldo.criadoEm).toLocaleString("pt-BR", {
                    timeZone: "America/Fortaleza",
                  })}
                </dd>
                <dt>Última alteração</dt>
                <dd>
                  {new Date(janela.saldo.alteradoEm).toLocaleString("pt-BR", {
                    timeZone: "America/Fortaleza",
                  })}
                </dd>
              </dl>
              {erro && (
                <p role="alert" className="message-error">
                  {erro}
                </p>
              )}
              <div className="form-actions">
                {janela.saldo.substituiId && (
                  <button
                    type="button"
                    className="secondary-button"
                    onClick={() => void anterior(janela.saldo.substituiId ?? "")}
                  >
                    Consultar versão anterior
                  </button>
                )}
                <button type="button" className="secondary-button" onClick={() => setJanela(null)}>
                  Fechar
                </button>
              </div>
            </div>
          )}
        </Modal>
      )}
    </section>
  );
}

function mensagem(falha: unknown): string {
  return falha instanceof Error ? falha.message : "Não foi possível concluir a solicitação.";
}
