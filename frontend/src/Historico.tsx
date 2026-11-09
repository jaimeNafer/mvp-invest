import { useState } from "react";
import Select from "./Select";
import AssociacaoRegistros from "./AssociacaoRegistros";
import { classesEstados, comandoConferencia, nomesEstados } from "./services/conciliacao";
import { decimalPtBr } from "./services/decimais";
import PreviaConciliacao from "./PreviaConciliacao";
import RecebimentoHistorico from "./RecebimentoHistorico";
import RegistroHistorico from "./RegistroHistorico";
import ValoracaoHistorico from "./ValoracaoHistorico";
import Modal from "./Modal";
import ResumoConciliacao from "./ResumoConciliacao";
import type { FiltroTimeline, RegistroTimeline } from "./services/conciliacao";
import useHistorico from "./useHistorico";
import "./conciliacao.css";

export default function Historico({
  onBloqueio,
  modoConferencia,
  modoAplicacao,
  bloqueioExterno,
  registroId,
  onOperacoes,
}: {
  onBloqueio?: (b: boolean) => void;
  modoConferencia?: boolean;
  modoAplicacao?: boolean;
  bloqueioExterno?: boolean;
  registroId?: string;
  onOperacoes?: () => void;
}) {
  const [linhaGrupoAberta, setLinhaGrupoAberta] = useState<string | null>(null);
  const [saldoParaExcluir, setSaldoParaExcluir] = useState<RegistroTimeline | null>(null);
  const {
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
    salvarValoracao,
    solicitarCotacoes,
    atualizacaoCotacoesSolicitada,
    solicitandoCotacoes,
    recarregarHistoricoCotacoes,
    confirmar,
    excluirSaldoAnterior,
    exclusaoErro,
    excluindo,
    limparErroExclusao,
  } = useHistorico(
    onBloqueio,
    modoConferencia ? { estado: registroId ? "" : "PENDENCIAS", registroId } : undefined,
  );
  const desabilitado = bloqueioExterno || bloqueado || carregando || falhaAtualizacao;
  function renderizarRegistro(registro: RegistroTimeline, emGrupo = false) {
    return (
      <RegistroHistorico
        key={registro.id}
        emGrupo={emGrupo}
        registro={registro}
        aberto={(emGrupo ? linhaGrupoAberta : aberto) === registro.id}
        disabled={desabilitado || analisando}
        contas={contas}
        ativos={ativos}
        onAbrir={(valor) => (emGrupo ? setLinhaGrupoAberta : setAberto)(valor ? registro.id : null)}
        onAssociar={() => {
          setPrevia(null);
          setAssociando(registro.id);
        }}
        associacao={
          associando === registro.id && pagina ? (
            <AssociacaoRegistros
              key={associando}
              registroId={associando}
              revisao={pagina.revisao}
              disabled={desabilitado}
              onCancelar={() => setAssociando(null)}
              onSalvar={async (dados) => {
                await executar(
                  comandoConferencia(
                    "/conciliacao/associacoes/propostas",
                    dados,
                    "Associação proposta. Confira o histórico para aplicar os efeitos e revisar os ajustes.",
                  ),
                );
              }}
            />
          ) : null
        }
        onSalvar={(dados, saldoAnterior) => salvar(registro, dados, saldoAnterior)}
        onSalvarValoracao={(valorTotalBrl) => salvarValoracao(registro, valorTotalBrl)}
        onRevisar={() => revisar(registro)}
        podeExcluirSaldoAnterior={
          modoConferencia === true &&
          !emGrupo &&
          registro.origem === "MANUAL" &&
          registro.natureza === "SALDO_ANTERIOR" &&
          /^registro:\d+$/.test(registro.id)
        }
        onExcluirSaldoAnterior={() => {
          limparErroExclusao();
          setSaldoParaExcluir(registro);
        }}
      />
    );
  }
  let diaAnterior = "";
  return (
    <section className="historico-conciliacao" aria-labelledby="historico-titulo">
      <header className="historico-cabecalho">
        <div>
          {modoAplicacao ? (
            <h3 id="historico-titulo">Aplicação dos registros</h3>
          ) : (
            <>
              <h1 id="historico-titulo">
                {modoConferencia ? "Conferência" : "Importação de operações"}
              </h1>
              <p>
                {modoConferencia
                  ? "Resolva as pendências e aplique as alterações do histórico."
                  : "Importe arquivos da Binance e acompanhe a conferência das suas operações."}
              </p>
            </>
          )}
        </div>
        <div className="conciliacao-actions">
          <button
            type="button"
            className="primary-button"
            disabled={desabilitado || analisando}
            onClick={analisar}
          >
            {analisando ? "Conferindo…" : modoAplicacao ? "Revisar resumo" : "Conferir histórico"}
          </button>
          {!modoAplicacao && (
            <button
              type="button"
              className="secondary-button"
              disabled={desabilitado || solicitandoCotacoes}
              onClick={() => void solicitarCotacoes()}
            >
              {solicitandoCotacoes ? "Solicitando…" : "Atualizar cotações"}
            </button>
          )}
          {!modoAplicacao && (
            <button
              type="button"
              className="secondary-button"
              disabled={desabilitado || !pagina?.proximaPendenciaId}
              onClick={proximaPendencia}
            >
              Próxima pendência
            </button>
          )}
        </div>
      </header>
      {!modoConferencia && !modoAplicacao && (
        <RecebimentoHistorico
          disabled={desabilitado || analisando}
          onBloqueio={bloquear}
          onRecebido={recebido}
        />
      )}
      {pagina && !modoConferencia && !modoAplicacao && <ResumoConciliacao resumo={pagina.resumo} />}
      {erro && (
        <p className="error-message" role="alert">
          {erro}
        </p>
      )}
      {aviso && <p role="status">{aviso}</p>}
      {atualizacaoCotacoesSolicitada && (
        <button
          className="secondary-button"
          type="button"
          disabled={desabilitado}
          onClick={() => void recarregarHistoricoCotacoes()}
        >
          Recarregar histórico
        </button>
      )}
      {falhaAtualizacao && (
        <p role="status">
          Não foi possível atualizar. Os dados anteriores e o preenchimento foram preservados; tente
          novamente antes de continuar.
        </p>
      )}
      {comando && (
        <div className="confirmacao-pendente" role="status">
          <p>
            {executando
              ? "Aguardando confirmação. Mantenha esta página aberta."
              : "Mantenha os dados e repita a confirmação para verificar o resultado."}
          </p>
          <button
            className="primary-button"
            type="button"
            disabled={executando}
            onClick={() => void executar(comando)}
          >
            Repetir confirmação
          </button>
        </div>
      )}
      {previa && (
        <PreviaConciliacao
          key={previa.previaId}
          previa={previa}
          disabled={bloqueado}
          onFechar={() => setPrevia(null)}
          onConfirmar={confirmar}
        />
      )}
      {saldoParaExcluir && (
        <Modal
          titulo="Excluir saldo anterior"
          bloqueado={excluindo}
          onFechar={() => {
            if (excluindo) return;
            setSaldoParaExcluir(null);
            limparErroExclusao();
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
              {saldoParaExcluir.data &&
                ` · ${new Date(saldoParaExcluir.data).toLocaleDateString("pt-BR")}`}
            </p>
            {exclusaoErro && (
              <p role="alert" className="message-error">
                {exclusaoErro}
              </p>
            )}
            <div className="form-actions">
              {exclusaoErro && (
                <button
                  className="secondary-button"
                  type="button"
                  disabled={excluindo}
                  onClick={() => {
                    setSaldoParaExcluir(null);
                    limparErroExclusao();
                    void carregar();
                  }}
                >
                  Atualizar histórico
                </button>
              )}
              <button
                className="primary-button"
                type="button"
                disabled={excluindo}
                onClick={async () => {
                  if (await excluirSaldoAnterior(saldoParaExcluir)) setSaldoParaExcluir(null);
                }}
              >
                {excluindo ? "Excluindo…" : "Excluir permanentemente"}
              </button>
              <button
                className="secondary-button"
                type="button"
                disabled={excluindo}
                onClick={() => setSaldoParaExcluir(null)}
              >
                Voltar
              </button>
            </div>
          </div>
        </Modal>
      )}
      {!modoAplicacao && (
        <fieldset className="filtros-conciliacao" disabled={desabilitado}>
          <legend>Filtrar histórico</legend>
          <label htmlFor="filtro-estado">
            Estado
            <Select
              id="filtro-estado"
              value={filtro.estado}
              onChange={(evento) =>
                filtrar({ estado: evento.target.value as FiltroTimeline["estado"], pagina: 0 })
              }
            >
              {modoConferencia && <option value="PENDENCIAS">Pendências a resolver</option>}
              <option value="">Todos</option>
              <option value="COERENTE">Coerentes</option>
              <option value="PENDENTE">Com pendências</option>
              <option value="CORRECAO_NECESSARIA">Precisam de correção</option>
            </Select>
          </label>
          <label htmlFor="filtro-ativo">
            Ativo
            <Select
              id="filtro-ativo"
              value={filtro.ativoId}
              onChange={(evento) => filtrar({ ativoId: evento.target.value, pagina: 0 })}
            >
              <option value="">Todos os ativos</option>
              {ativos.map((ativo) => (
                <option key={ativo.id} value={ativo.id}>
                  {ativo.ticker}
                </option>
              ))}
            </Select>
          </label>
          <label htmlFor="filtro-conta">
            Instituição / conta
            <Select
              id="filtro-conta"
              value={filtro.contaId}
              onChange={(evento) => filtrar({ contaId: evento.target.value, pagina: 0 })}
            >
              <option value="">Todas as contas</option>
              {contas.map((conta) => (
                <option key={conta.id} value={conta.id}>
                  {conta.instituicaoNome} · {conta.nome}
                </option>
              ))}
            </Select>
          </label>
        </fieldset>
      )}
      {filtro.registroId && (
        <button
          type="button"
          className="secondary-button"
          disabled={desabilitado}
          onClick={() => filtrar({ registroId: undefined, estado: "PENDENCIAS", pagina: 0 })}
        >
          Ver todas as pendências
        </button>
      )}
      {onOperacoes && (
        <button
          type="button"
          className="secondary-button"
          disabled={desabilitado}
          onClick={onOperacoes}
        >
          Ver operações
        </button>
      )}
      {carregando && !pagina && <p role="status">Carregando histórico…</p>}
      {pagina && !modoAplicacao && (
        <>
          <section
            className="timeline-registros"
            aria-label="Registros do histórico"
            aria-busy={carregando}
          >
            {pagina.itens.map((registro) => {
              const dia = registro.data
                ? new Date(registro.data).toLocaleDateString("pt-BR")
                : registro.origem === "SINTETICA"
                  ? "Ajustes conciliatórios"
                  : "Sem data resolvida";
              const novoDia = dia !== diaAnterior;
              diaAnterior = dia;
              const chaveGrupo = registro.grupoNumero ?? registro.grupoAutomatico;
              const grupo = chaveGrupo ? pagina.linhasPorGrupo?.[chaveGrupo] : undefined;
              const conversao = (grupo ?? [registro]).some((linha) =>
                ["binance convert", "convert"].includes((linha.tipo ?? "").toLowerCase()),
              );
              const parcelas = (
                valores: { ativoTicker: string; quantidade: string }[] | undefined,
              ) =>
                valores?.map((p) => `${decimalPtBr(p.quantidade)} ${p.ativoTicker}`).join(" + ") ??
                "";
              const origem = parcelas(registro.pagamentosGrupo);
              const destino = parcelas(registro.recebimentosGrupo);
              return (
                <div key={registro.id}>
                  {novoDia && <h2 className="timeline-dia">{dia}</h2>}
                  {grupo?.length ? (
                    <article
                      className="registro-historico"
                      id={`timeline-${registro.id}`}
                      tabIndex={-1}
                    >
                      <div className="registro-linha">
                        <div>
                          <button
                            type="button"
                            className="registro-abrir"
                            aria-expanded={aberto === registro.id}
                            onClick={() => setAberto(aberto === registro.id ? null : registro.id)}
                          >
                            {conversao
                              ? "Binance Convert"
                              : registro.grupoNumero
                                ? `Operação ${registro.grupoNumero}`
                                : "Operação identificada"}
                          </button>
                          <small>
                            {registro.instituicaoNome} · {registro.contaNome}
                          </small>
                          {conversao && registro.grupoNumero && (
                            <small>Operação {registro.grupoNumero}</small>
                          )}
                          {(registro.aguardaConfirmacao ?? !registro.aplicado) && (
                            <small>Aguardando confirmação global</small>
                          )}
                        </div>
                        <div>
                          {conversao && origem && destino
                            ? `${origem} → ${destino}`
                            : [
                                ...new Set(grupo.map((linha) => linha.ativoTicker).filter(Boolean)),
                              ].join(" · ")}
                          {conversao && registro.data && (
                            <small>
                              {new Date(registro.data).toLocaleTimeString("pt-BR", {
                                hour: "2-digit",
                                minute: "2-digit",
                              })}
                            </small>
                          )}
                        </div>
                        <span className={`estado-conferencia ${classesEstados[registro.estado]}`}>
                          {nomesEstados[registro.estado]}
                        </span>
                      </div>
                      {aberto === registro.id && (
                        <section
                          className="registro-detalhes"
                          aria-label="Linhas originais da operação"
                        >
                          <ValoracaoHistorico
                            valoracao={registro.valoracao}
                            disabled={desabilitado || analisando}
                            onSalvar={(valorTotalBrl) => salvarValoracao(registro, valorTotalBrl)}
                          />
                          <div className="resumo-operacao">
                            {grupo.map((linha) => (
                              <span key={linha.id}>
                                {linha.ativoTicker} ·{" "}
                                {linha.quantidade == null ? "—" : decimalPtBr(linha.quantidade)}
                              </span>
                            ))}
                          </div>
                          {conversao && registro.principalConhecidoBrl == null && (
                            <p>
                              Valor em reais indisponível; custo e resultado podem estar
                              incompletos.
                            </p>
                          )}
                          {registro.principalConhecidoBrl != null && (
                            <p>
                              Valor da operação: R$ {decimalPtBr(registro.principalConhecidoBrl)}
                            </p>
                          )}
                          <details>
                            <summary>Ver {grupo.length} registros originais</summary>
                            {grupo.map((linha) => renderizarRegistro(linha, true))}
                          </details>
                        </section>
                      )}
                    </article>
                  ) : (
                    renderizarRegistro(registro)
                  )}
                </div>
              );
            })}
          </section>
          {pagina.total === 0 && (
            <p>
              {modoConferencia &&
              filtro.estado === "PENDENCIAS" &&
              !filtro.ativoId &&
              !filtro.contaId
                ? "Sem pendências individuais."
                : "Nenhum registro encontrado para estes filtros."}
            </p>
          )}
          {(!modoConferencia || pagina.total > 0) && (
            <nav className="paginacao-conciliacao" aria-label="Páginas do histórico">
              <button
                className="secondary-button"
                type="button"
                disabled={desabilitado || pagina.pagina === 0}
                onClick={() => filtrar({ pagina: pagina.pagina - 1 })}
              >
                Página anterior
              </button>
              <span>
                Página {pagina.pagina + 1} · {pagina.total} registros
              </span>
              <button
                className="secondary-button"
                type="button"
                disabled={desabilitado || (pagina.pagina + 1) * pagina.tamanho >= pagina.total}
                onClick={() => filtrar({ pagina: pagina.pagina + 1 })}
              >
                Próxima página
              </button>
            </nav>
          )}
        </>
      )}
      {(!pagina || falhaAtualizacao) && !carregando && (
        <button className="secondary-button" type="button" onClick={() => void carregar()}>
          Tentar novamente
        </button>
      )}
    </section>
  );
}
