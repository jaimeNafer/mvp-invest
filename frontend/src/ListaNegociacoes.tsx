import { Fragment, useState } from "react";
import Icone from "./Icone";
import type { Carteira } from "./services/carteiras";
import { nomesEstados } from "./services/conciliacao";
import { decimalPtBr } from "./services/decimais";
import { dataHoraFortaleza, nomesSituacoes, nomesTipos } from "./services/operacoes";
import type { ItemHistoricoOperacao, Operacao } from "./services/operacoes";
import type { RegistroTimeline } from "./services/conciliacao";

export default function ListaNegociacoes({
  itens,
  carteiras,
  onConferencia,
  onDetalhe,
  onCorrigir,
  onCancelar,
  onExcluirSaldoAnterior,
}: {
  itens: ItemHistoricoOperacao[];
  carteiras: Carteira[];
  onConferencia: (id: string) => void;
  onDetalhe: (o: Operacao) => void;
  onCorrigir: (o: Operacao) => void;
  onCancelar: (o: Operacao) => void;
  onExcluirSaldoAnterior: (registro: RegistroTimeline) => void;
}) {
  const [aberta, setAberta] = useState<string | null>(null);
  if (!itens.length) return <p className="empty-state">Nenhuma operação encontrada.</p>;
  const nomesCarteiras = (id: string) =>
    carteiras
      .filter((c) => c.ativo && c.contas.some((conta) => conta.instituicaoId === id))
      .map((c) => c.nome)
      .join(", ") || "—";
  return (
    <div className="table-scroll operacoes-tabela">
      <table>
        <thead>
          <tr>
            <th>Data (Fortaleza)</th>
            <th>Operação</th>
            <th>Instituição</th>
            <th>Carteira</th>
            <th className="operacoes-numero">Movimentação</th>
            <th className="operacoes-numero">Valor em BRL</th>
            <th>Situação</th>
            <th>Ações</th>
          </tr>
        </thead>
        <tbody>
          {itens.map(({ operacao: o, importada: r, linhasOriginais }) => {
            if (o)
              return (
                <tr
                  key={o.id}
                  className={o.situacao !== "CONFIRMADA" ? "carteira-arquivada" : undefined}
                >
                  <td>{dataEmLinhas(o.ocorridaEm)}</td>
                  <td>{nomesTipos[o.tipo]}</td>
                  <td>{o.instituicaoNome}</td>
                  <td>{nomesCarteiras(o.instituicaoId)}</td>
                  <td className="operacoes-numero">
                    {decimalPtBr(o.quantidade)} {o.ativoTicker}
                  </td>
                  <td className="operacoes-numero">
                    {valorBrl(o.calculo?.valorBrutoBrl ?? o.custoTotalBrl)}
                  </td>
                  <td>
                    <span
                      className={`badge ${o.situacao === "CONFIRMADA" ? "" : "badge-archived"}`}
                    >
                      {nomesSituacoes[o.situacao]}
                    </span>
                  </td>
                  <td>
                    <div className="row-actions operacao-acoes">
                      <button
                        type="button"
                        className="secondary-button"
                        aria-label={`Detalhes da operação ${o.id}`}
                        onClick={() => onDetalhe(o)}
                      >
                        Detalhes
                      </button>
                      {o.situacao === "CONFIRMADA" && o.tipo !== "SALDO_INICIAL" && (
                        <>
                          <button
                            type="button"
                            className="icon-button"
                            title="Corrigir operação"
                            aria-label={`Corrigir operação ${o.id}`}
                            onClick={() => onCorrigir(o)}
                          >
                            <Icone nome="editar" />
                          </button>
                          <button
                            type="button"
                            className="icon-button"
                            title="Cancelar operação"
                            aria-label={`Cancelar operação ${o.id}`}
                            onClick={() => onCancelar(o)}
                          >
                            <Icone nome="fechar" />
                          </button>
                        </>
                      )}
                    </div>
                  </td>
                </tr>
              );
            const expandida = aberta === r.id;
            const painelId = `detalhes-${r.id}`;
            const tipo = r.natureza === "CONVERSAO" ? "Conversão" : r.descricao;
            const valor = r.valoracao?.valorTotalBrl ?? r.principalConhecidoBrl ?? r.custoTotalBrl;
            const movimentos = linhasOriginais.map((linha) => ({
              ...linha,
              texto:
                linha.quantidade == null
                  ? `— ${linha.ativoTicker}`
                  : `${decimalPtBr(linha.quantidade)} ${linha.ativoTicker}`,
            }));
            return (
              <Fragment key={r.grupoNumero ?? r.id}>
                <tr className={expandida ? "operacao-aberta" : undefined}>
                  <td>{dataEmLinhas(r.data)}</td>
                  <td>
                    {tipo}
                    <small className="field-hint">
                      {r.origem === "MANUAL"
                        ? "Informado manualmente"
                        : `Importado da ${r.instituicaoNome}`}
                    </small>
                  </td>
                  <td>{r.instituicaoNome}</td>
                  <td>{nomesCarteiras(r.instituicaoId)}</td>
                  <td className="operacoes-numero">
                    {movimentos.map((m) => (
                      <span className="operacao-parcela" key={m.id}>
                        {m.texto}
                      </span>
                    ))}
                  </td>
                  <td className="operacoes-numero">{valorBrl(valor)}</td>
                  <td>
                    <span className="badge">{nomesEstados[r.estado]}</span>
                  </td>
                  <td>
                    <div className="row-actions operacao-acoes">
                      <button
                        type="button"
                        className="secondary-button"
                        aria-expanded={expandida}
                        aria-controls={expandida ? painelId : undefined}
                        aria-label={`${expandida ? "Recolher" : "Detalhes de"} ${tipo} ${r.grupoNumero ?? r.id}`}
                        onClick={() => setAberta(expandida ? null : r.id)}
                      >
                        {expandida ? "▾ Recolher" : "▸ Detalhes"}
                      </button>
                      {r.origem === "MANUAL" &&
                        r.natureza === "SALDO_ANTERIOR" &&
                        /^registro:\d+$/.test(r.id) && (
                          <button
                            type="button"
                            className="icon-button operacao-excluir"
                            title="Excluir saldo anterior"
                            aria-label="Excluir saldo anterior"
                            onClick={() => onExcluirSaldoAnterior(r)}
                          >
                            <Icone nome="fechar" />
                          </button>
                        )}
                    </div>
                  </td>
                </tr>
                {expandida && (
                  <tr className="operacao-detalhe">
                    <td colSpan={8}>
                      <section
                        id={painelId}
                        aria-label="Detalhes da operação"
                        className="operacao-detalhe-painel"
                      >
                        <div>
                          <h3>Movimentação</h3>
                          {r.pagamentosGrupo?.length || r.recebimentosGrupo?.length ? (
                            <dl>
                              {r.pagamentosGrupo?.length ? (
                                <>
                                  <dt>Enviado</dt>
                                  <dd>{parcelas(r.pagamentosGrupo)}</dd>
                                </>
                              ) : null}
                              {r.recebimentosGrupo?.length ? (
                                <>
                                  <dt>Recebido</dt>
                                  <dd>{parcelas(r.recebimentosGrupo)}</dd>
                                </>
                              ) : null}
                            </dl>
                          ) : (
                            <p>{movimentos.map((m) => m.texto).join(" · ")}</p>
                          )}
                          <p>Valor em BRL · {valor == null ? "Indisponível" : valorBrl(valor)}</p>
                          <button
                            type="button"
                            className="secondary-button"
                            onClick={() => onConferencia(r.id)}
                          >
                            {r.estado !== "COERENTE" ||
                            r.saldoAnteriorPendente ||
                            r.taxaDetalhamentoPendente
                              ? "Conferir operação"
                              : r.valoracao?.permiteCorrecaoManual
                                ? "Revisar valor em BRL"
                                : "Ver registro"}
                          </button>
                        </div>
                        <details>
                          <summary>Registros de origem ({movimentos.length})</summary>
                          <ul>
                            {movimentos.map((m) => (
                              <li key={m.id}>
                                <strong>{m.id}</strong> · {m.texto}
                                {m.camposOriginais["Arquivo recebido"] &&
                                  ` · Arquivo ${m.camposOriginais["Arquivo recebido"]}`}
                                {m.camposOriginais.Linha && ` · Linha ${m.camposOriginais.Linha}`}
                              </li>
                            ))}
                          </ul>
                        </details>
                      </section>
                    </td>
                  </tr>
                )}
              </Fragment>
            );
          })}
        </tbody>
      </table>
    </div>
  );
}

function dataEmLinhas(instante: string | null) {
  if (!instante) return "—";
  const [data, hora] = dataHoraFortaleza(instante).split(/,?\s+(?=\d{2}:)/);
  return (
    <>
      <span className="operacao-parcela">{data}</span>
      {hora && <small className="field-hint">{hora}</small>}
    </>
  );
}

function parcelas(valores: { quantidade: string; ativoTicker: string }[]) {
  return valores.map((p) => `${decimalPtBr(p.quantidade)} ${p.ativoTicker}`).join(" · ");
}

function valorBrl(valor?: string | null) {
  return valor == null ? "—" : `R$ ${decimalPtBr(valor)}`;
}
