import { useEffect, useId, useState } from "react";
import { decimalCanonico, decimalPtBr, moeda } from "./services/decimais";
import type { StatusValoracao, ValoracaoTimeline } from "./services/conciliacao";

const nomesStatus: Record<StatusValoracao, string> = {
  CONFIRMADO: "Valor confirmado",
  HISTORICO: "Cotação histórica",
  ESTIMADO: "Valor estimado",
  MANUAL: "Valor informado manualmente",
  INDISPONIVEL: "Valor indisponível",
};

export default function ValoracaoHistorico({
  valoracao,
  disabled,
  onSalvar,
  permitirAcao = true,
}: {
  valoracao?: ValoracaoTimeline;
  disabled: boolean;
  onSalvar: (valorTotalBrl: string) => void | Promise<void>;
  permitirAcao?: boolean;
}) {
  const prefixo = useId();
  const [editando, setEditando] = useState(false);
  const [salvando, setSalvando] = useState(false);
  const [valorDigitado, setValorDigitado] = useState(
    valoracao?.valorTotalBrl == null ? "" : decimalPtBr(valoracao.valorTotalBrl),
  );
  const [erro, setErro] = useState("");

  useEffect(() => {
    setValorDigitado(valoracao?.valorTotalBrl == null ? "" : decimalPtBr(valoracao.valorTotalBrl));
  }, [valoracao?.valorTotalBrl]);

  if (!valoracao) return null;

  async function salvar(evento: React.FormEvent<HTMLFormElement>) {
    evento.preventDefault();
    try {
      const valorTotalBrl = decimalCanonico(valorDigitado);
      setSalvando(true);
      await onSalvar(valorTotalBrl);
      setErro("");
      setEditando(false);
    } catch (falha) {
      setErro(falha instanceof Error ? falha.message : "Informe um valor em reais válido.");
    } finally {
      setSalvando(false);
    }
  }

  return (
    <section className="valoracao-historico" aria-label="Valoração em reais">
      <div className="valoracao-resumo">
        <span className={`valoracao-status valoracao-${valoracao.statusValoracao.toLowerCase()}`}>
          {nomesStatus[valoracao.statusValoracao]}
        </span>
        {valoracao.valorTotalBrl !== null && <strong>{moeda(valoracao.valorTotalBrl)}</strong>}
      </div>
      {valoracao.motivoIndisponibilidade && <p>{valoracao.motivoIndisponibilidade}</p>}
      {(valoracao.aproximado ||
        valoracao.fonteCotacao ||
        valoracao.metodoConversao ||
        valoracao.cotacaoUtilizada ||
        valoracao.dataHoraCotacao) && (
        <small className="valoracao-origem">
          {valoracao.aproximado && <span>Estimativa aproximada</span>}
          {valoracao.fonteCotacao && <span>Fonte: {valoracao.fonteCotacao}</span>}
          {valoracao.cotacaoUtilizada && (
            <span>Cotação utilizada: R$ {decimalPtBr(valoracao.cotacaoUtilizada)}</span>
          )}
          {valoracao.metodoConversao && <span>Método: {valoracao.metodoConversao}</span>}
          {valoracao.dataHoraCotacao && (
            <span>Cotação de {new Date(valoracao.dataHoraCotacao).toLocaleString("pt-BR")}</span>
          )}
        </small>
      )}
      {permitirAcao && valoracao.permiteCorrecaoManual && !editando && (
        <button
          className="secondary-button"
          type="button"
          disabled={disabled || salvando}
          onClick={() => {
            setErro("");
            setValorDigitado(
              valoracao.valorTotalBrl == null ? "" : decimalPtBr(valoracao.valorTotalBrl),
            );
            setEditando(true);
          }}
        >
          Informar valor em reais
        </button>
      )}
      {editando && (
        <form className="valoracao-form" onSubmit={salvar}>
          <label htmlFor={`${prefixo}-total-brl`}>Valor total em reais</label>
          <input
            id={`${prefixo}-total-brl`}
            inputMode="decimal"
            autoComplete="off"
            value={valorDigitado}
            disabled={disabled || salvando}
            onChange={(evento) => setValorDigitado(evento.target.value)}
          />
          <p>Ao salvar, o histórico será recalculado automaticamente.</p>
          {erro && <p role="alert">{erro}</p>}
          <div className="conciliacao-actions">
            <button className="primary-button" type="submit" disabled={disabled || salvando}>
              {salvando ? "Salvando…" : "Salvar valor"}
            </button>
            <button
              className="secondary-button"
              type="button"
              disabled={disabled || salvando}
              onClick={() => setEditando(false)}
            >
              Cancelar
            </button>
          </div>
        </form>
      )}
    </section>
  );
}
