import { percentual } from "./services/decimais";
import type { ResumoConferencia } from "./services/conciliacao";
import "./conciliacao.css";

export default function ResumoConciliacao({
  resumo,
  onAbrirTimeline,
  compact = false,
}: {
  resumo: ResumoConferencia;
  onAbrirTimeline?: () => void;
  compact?: boolean;
}) {
  if (!compact) {
    const atencao = resumo.pendentes + resumo.inconsistentes;
    return (
      <section
        className="resumo-conciliacao resumo-importacao"
        aria-label="Conferência do histórico"
      >
        <div className="resumo-importacao-topo">
          <div>
            <h2>Resumo dos registros</h2>
            <p>{resumo.mensagem}</p>
          </div>
          <div className="resumo-percentual">
            <strong>{percentual(resumo.percentualCoerencia)}</strong>
            <span>de coerência</span>
          </div>
        </div>
        <progress
          className="progresso-coerencia"
          max={resumo.total || 1}
          value={resumo.coerentes}
          aria-label="Registros coerentes"
          aria-valuetext={`${resumo.coerentes} de ${resumo.total} registros coerentes`}
        />
        <dl className="resumo-indicadores">
          <div>
            <dt>Registros analisados</dt>
            <dd>{resumo.total}</dd>
          </div>
          {resumo.operacoesCoerentes !== undefined && (
            <>
              <div>
                <dt>Operações coerentes</dt>
                <dd>{resumo.operacoesCoerentes}</dd>
              </div>
              <div>
                <dt>Operações com pendência</dt>
                <dd>{resumo.operacoesPendentes}</dd>
              </div>
              <div>
                <dt>Créditos automáticos</dt>
                <dd>{resumo.creditosAutomaticos}</dd>
              </div>
              <div>
                <dt>Não classificados</dt>
                <dd>{resumo.naoClassificados}</dd>
              </div>
              <div>
                <dt>Operações agrupadas</dt>
                <dd>{resumo.operacoesAgrupadas}</dd>
              </div>
            </>
          )}
          <div className="indicador-coerente">
            <dt>Coerentes</dt>
            <dd>{resumo.coerentes}</dd>
          </div>
          <div className="indicador-atencao">
            <dt>Precisam de atenção</dt>
            <dd>{atencao}</dd>
          </div>
        </dl>
        {resumo.ajustesAtivos > 0 && (
          <p className="resumo-ajustes">{resumo.ajustesAtivos} ajustes provisórios ativos</p>
        )}
      </section>
    );
  }
  return (
    <section
      className={`resumo-conciliacao ${compact ? "resumo-compacto" : ""}`}
      aria-label="Conferência do histórico"
    >
      <div>
        <strong>{resumo.mensagem}</strong>
        <p>
          {resumo.registrosAfetados} registros afetados · {resumo.ajustesAtivos} ajustes provisórios
          ativos
        </p>
      </div>
      <p className="coerencia-secundaria">
        {resumo.coerentes} de {resumo.total} registros coerentes (
        {percentual(resumo.percentualCoerencia)})
      </p>
      {onAbrirTimeline && (
        <button type="button" className="secondary-button" onClick={onAbrirTimeline}>
          Conferir histórico
        </button>
      )}
    </section>
  );
}
