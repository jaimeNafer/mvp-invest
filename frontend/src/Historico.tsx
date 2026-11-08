import { useState } from "react";
import AssociacaoRegistros from "./AssociacaoRegistros";
import { classesEstados, comandoConferencia, nomesEstados } from "./services/conciliacao";
import { decimalPtBr } from "./services/decimais";
import PreviaConciliacao from "./PreviaConciliacao";
import RecebimentoHistorico from "./RecebimentoHistorico";
import RegistroHistorico from "./RegistroHistorico";
import ValoracaoHistorico from "./ValoracaoHistorico";
import ResumoConciliacao from "./ResumoConciliacao";
import type { FiltroTimeline, RegistroTimeline } from "./services/conciliacao";
import useHistorico from "./useHistorico";
import "./conciliacao.css";

export default function Historico({ onBloqueio }: { onBloqueio?: (b: boolean) => void }) {
  const [linhaGrupoAberta, setLinhaGrupoAberta] = useState<string | null>(null);
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
  } = useHistorico(onBloqueio);
  const desabilitado = bloqueado || carregando || falhaAtualizacao;
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
      />
    );
  }
  let diaAnterior = "";
  return (
    <section className="historico-conciliacao" aria-labelledby="historico-titulo">
      <header className="historico-cabecalho">
        <div>
          <h1 id="historico-titulo">Importação de operações</h1>
          <p>Importe arquivos da Binance e acompanhe a conferência das suas operações.</p>
        </div>
        <div className="conciliacao-actions">
          <button
            type="button"
            className="primary-button"
            disabled={desabilitado || analisando}
            onClick={analisar}
          >
            {analisando ? "Conferindo…" : "Conferir histórico"}
          </button>
          <button
            type="button"
            className="secondary-button"
            disabled={desabilitado || solicitandoCotacoes}
            onClick={() => void solicitarCotacoes()}
          >
            {solicitandoCotacoes ? "Solicitando…" : "Atualizar cotações"}
          </button>
          <button
            type="button"
            className="secondary-button"
            disabled={desabilitado || !pagina?.proximaPendenciaId}
            onClick={proximaPendencia}
          >
            Próxima pendência
          </button>
        </div>
      </header>
      <RecebimentoHistorico
        disabled={desabilitado || analisando}
        onBloqueio={bloquear}
        onRecebido={recebido}
      />
      {pagina && <ResumoConciliacao resumo={pagina.resumo} />}
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
      <fieldset className="filtros-conciliacao" disabled={desabilitado}>
        <legend>Filtrar histórico</legend>
        <label>
          Estado
          <select
            value={filtro.estado}
            onChange={(evento) =>
              filtrar({ estado: evento.target.value as FiltroTimeline["estado"], pagina: 0 })
            }
          >
            <option value="">Todos</option>
            <option value="COERENTE">Coerentes</option>
            <option value="PENDENTE">Com pendências</option>
            <option value="CORRECAO_NECESSARIA">Precisam de correção</option>
          </select>
        </label>
        <label>
          Ativo
          <select
            value={filtro.ativoId}
            onChange={(evento) => filtrar({ ativoId: evento.target.value, pagina: 0 })}
          >
            <option value="">Todos os ativos</option>
            {ativos.map((ativo) => (
              <option key={ativo.id} value={ativo.id}>
                {ativo.ticker}
              </option>
            ))}
          </select>
        </label>
        <label>
          Instituição / conta
          <select
            value={filtro.contaId}
            onChange={(evento) => filtrar({ contaId: evento.target.value, pagina: 0 })}
          >
            <option value="">Todas as contas</option>
            {contas.map((conta) => (
              <option key={conta.id} value={conta.id}>
                {conta.instituicaoNome} · {conta.nome}
              </option>
            ))}
          </select>
        </label>
      </fieldset>
      {carregando && <p role="status">Carregando histórico…</p>}
      {pagina && (
        <>
          <section className="timeline-registros" aria-label="Registros do histórico">
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
          {pagina.total === 0 && <p>Nenhum registro encontrado para estes filtros.</p>}
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
