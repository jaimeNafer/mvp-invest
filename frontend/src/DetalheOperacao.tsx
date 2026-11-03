import CalculoOperacao from "./CalculoOperacao";
import { decimalPtBr } from "./services/decimais";
import { dataHoraFortaleza, nomesSituacoes, nomesTipos } from "./services/operacoes";
import type { Operacao } from "./services/operacoes";

export default function DetalheOperacao({
  operacao: o,
  onAnterior,
  onSaldos,
}: {
  operacao: Operacao;
  onAnterior: (id: string) => void;
  onSaldos: () => void;
}) {
  return (
    <div className="saldo-form">
      <dl className="saldo-resumo">
        <dt>Registro</dt>
        <dd>#{o.id}</dd>
        <dt>Tipo</dt>
        <dd>{nomesTipos[o.tipo]}</dd>
        <dt>Situação</dt>
        <dd>{nomesSituacoes[o.situacao]}</dd>
        <dt>Instituição</dt>
        <dd>{o.instituicaoNome}</dd>
        <dt>Ativo</dt>
        <dd>{o.ativoTicker}</dd>
        <dt>Data e hora (Fortaleza)</dt>
        <dd>{dataHoraFortaleza(o.ocorridaEm)}</dd>
        <dt>Quantidade</dt>
        <dd>{decimalPtBr(o.quantidade)}</dd>
        {o.custoTotalBrl !== null && (
          <>
            <dt>Custo histórico total</dt>
            <dd>R$ {decimalPtBr(o.custoTotalBrl)}</dd>
          </>
        )}
        {o.moedaCotacaoTicker && (
          <>
            <dt>Moeda de cotação</dt>
            <dd>{o.moedaCotacaoTicker}</dd>
          </>
        )}
        {o.totalBruto !== null && (
          <>
            <dt>Total bruto</dt>
            <dd>
              {decimalPtBr(o.totalBruto)} {o.moedaCotacaoTicker}
            </dd>
          </>
        )}
        {o.precoUnitario && (
          <>
            <dt>Preço unitário</dt>
            <dd>
              {decimalPtBr(o.precoUnitario)} {o.moedaCotacaoTicker}
            </dd>
          </>
        )}
        {o.cotacaoBrl !== null && (
          <>
            <dt>Cotação histórica em reais</dt>
            <dd>R$ {decimalPtBr(o.cotacaoBrl)}</dd>
          </>
        )}
        <dt>Taxa</dt>
        <dd>
          {decimalPtBr(o.taxaQuantidade)} {o.taxaAtivoTicker ?? ""}
        </dd>
        {o.referencia && (
          <>
            <dt>Referência</dt>
            <dd>{o.referencia}</dd>
          </>
        )}
        {o.motivoAlteracao && (
          <>
            <dt>Motivo da alteração</dt>
            <dd>{o.motivoAlteracao}</dd>
          </>
        )}
        <dt>Registrado em (Fortaleza)</dt>
        <dd>{dataHoraFortaleza(o.criadoEm)}</dd>
        <dt>Última alteração (Fortaleza)</dt>
        <dd>{dataHoraFortaleza(o.alteradoEm)}</dd>
      </dl>
      {o.situacao === "CONFIRMADA" && o.calculo && (
        <>
          <h3>Resultados vigentes</h3>
          <CalculoOperacao calculo={o.calculo} />
        </>
      )}
      {o.situacao !== "CONFIRMADA" && (
        <p className="field-hint">
          Este registro está no histórico e não compõe a posição vigente.
        </p>
      )}
      <div className="form-actions">
        {o.substituiId && (
          <button
            type="button"
            className="secondary-button"
            onClick={() => onAnterior(o.substituiId ?? "")}
          >
            Consultar versão anterior
          </button>
        )}
        {o.tipo === "SALDO_INICIAL" && (
          <button type="button" className="secondary-button" onClick={onSaldos}>
            Gerenciar saldos iniciais
          </button>
        )}
      </div>
    </div>
  );
}
