import Icone from "./Icone";
import { decimalPtBr } from "./services/decimais";
import { dataHoraFortaleza, nomesSituacoes, nomesTipos } from "./services/operacoes";
import type { Operacao } from "./services/operacoes";

export default function ListaNegociacoes({
  operacoes,
  onDetalhe,
  onCorrigir,
  onCancelar,
}: {
  operacoes: Operacao[];
  onDetalhe: (o: Operacao) => void;
  onCorrigir: (o: Operacao) => void;
  onCancelar: (o: Operacao) => void;
}) {
  if (operacoes.length === 0) return <p className="empty-state">Nenhuma operação encontrada.</p>;
  return (
    <div className="table-scroll">
      <table>
        <thead>
          <tr>
            <th>Data (Fortaleza)</th>
            <th>Tipo</th>
            <th>Instituição</th>
            <th>Ativo</th>
            <th>Quantidade</th>
            <th>Total bruto</th>
            <th>Situação</th>
            <th>Ações</th>
          </tr>
        </thead>
        <tbody>
          {operacoes.map((o) => (
            <tr
              key={o.id}
              className={o.situacao !== "CONFIRMADA" ? "carteira-arquivada" : undefined}
            >
              <td>{dataHoraFortaleza(o.ocorridaEm)}</td>
              <td>{nomesTipos[o.tipo]}</td>
              <td>{o.instituicaoNome}</td>
              <td>{o.ativoTicker}</td>
              <td>{decimalPtBr(o.quantidade)}</td>
              <td>
                {o.totalBruto !== null
                  ? `${decimalPtBr(o.totalBruto)} ${o.moedaCotacaoTicker}`
                  : "—"}
              </td>
              <td>
                <span className={`badge ${o.situacao === "CONFIRMADA" ? "" : "badge-archived"}`}>
                  {nomesSituacoes[o.situacao]}
                </span>
              </td>
              <td>
                <div className="row-actions">
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
          ))}
        </tbody>
      </table>
    </div>
  );
}
