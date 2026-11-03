import { useState } from "react";
import type { FormEvent } from "react";
import { decimalCanonico, decimalPtBr, moeda } from "./services/decimais";
import type { Instituicao } from "./services/instituicoes";
import type { Ativo, CadastroSaldo, Saldo } from "./services/saldosIniciais";

type Props = {
  ativos: Ativo[];
  instituicoes: Instituicao[];
  saldo: Saldo | null;
  dataReferencia: string | null;
  possuiConfirmados: boolean;
  salvando: boolean;
  erro: string;
  pendente: CadastroSaldo | null;
  onSalvar: (entrada: CadastroSaldo) => void;
  onCancelar: () => void;
};

export default function SaldoInicialForm(props: Props) {
  const { saldo, pendente } = props;
  const [instituicaoId, setInstituicao] = useState(
    pendente?.instituicaoId ?? saldo?.instituicaoId ?? "",
  );
  const [ativoId, setAtivo] = useState(pendente?.ativoId ?? saldo?.ativoId ?? "");
  const [data, setData] = useState(
    pendente?.dataReferencia ?? saldo?.dataReferencia ?? props.dataReferencia ?? "",
  );
  const [quantidade, setQuantidade] = useState(
    decimalPtBr(pendente?.quantidade ?? saldo?.quantidade ?? ""),
  );
  const [custo, setCusto] = useState(
    decimalPtBr(pendente?.custoTotalBrl ?? saldo?.custoTotalBrl ?? ""),
  );
  const [zero, setZero] = useState(
    pendente?.custoZeroConfirmado ?? !!saldo?.justificativaCustoZero,
  );
  const [justificativa, setJustificativa] = useState(
    pendente?.justificativaCustoZero ?? saldo?.justificativaCustoZero ?? "",
  );
  const [motivo, setMotivo] = useState(pendente?.motivo ?? "");
  const [erro, setErro] = useState("");
  const [revisao, setRevisao] = useState<CadastroSaldo | null>(pendente);
  const ativo = props.ativos.find((a) => a.id === ativoId);
  const custoBrl = ativo?.codigo === "fiat:BRL" ? quantidade : custo;

  function revisar(event: FormEvent<HTMLFormElement>) {
    event.preventDefault();
    try {
      if (!instituicaoId || !ativoId || !data)
        throw new Error("Informe instituição, ativo e data de referência.");
      const q = decimalCanonico(quantidade);
      const c = decimalCanonico(custoBrl);
      if (!/[1-9]/.test(q)) throw new Error("A quantidade deve ser maior que zero.");
      if (!/[1-9]/.test(c) && (!zero || !justificativa.trim()))
        throw new Error("Confirme o custo zero e informe sua justificativa.");
      if (saldo && !motivo.trim()) throw new Error("Informe o motivo da correção.");
      setRevisao({
        instituicaoId,
        ativoId,
        dataReferencia: data,
        quantidade: q,
        custoTotalBrl: c,
        custoZeroConfirmado: !/[1-9]/.test(c) && zero,
        justificativaCustoZero: !/[1-9]/.test(c) ? justificativa.trim() : null,
        ...(saldo ? { motivo: motivo.trim() } : {}),
      });
      setErro("");
    } catch (falha) {
      setErro(falha instanceof Error ? falha.message : "Verifique os campos.");
    }
  }

  if (revisao)
    return (
      <div className="saldo-form">
        <p>
          Confira os dados antes de confirmar. O custo informado será a base inicial do
          acompanhamento.
        </p>
        <dl className="saldo-resumo">
          <dt>Instituição</dt>
          <dd>{props.instituicoes.find((i) => i.id === revisao.instituicaoId)?.nome}</dd>
          <dt>Ativo</dt>
          <dd>{props.ativos.find((a) => a.id === revisao.ativoId)?.ticker}</dd>
          <dt>Data de referência</dt>
          <dd>{dataPtBr(revisao.dataReferencia)}</dd>
          <dt>Quantidade</dt>
          <dd>{decimalPtBr(revisao.quantidade)}</dd>
          <dt>Custo histórico total</dt>
          <dd title={decimalPtBr(revisao.custoTotalBrl)}>{moeda(revisao.custoTotalBrl)}</dd>
          {revisao.justificativaCustoZero && (
            <>
              <dt>Justificativa do custo zero</dt>
              <dd>{revisao.justificativaCustoZero}</dd>
            </>
          )}
          {revisao.motivo && (
            <>
              <dt>Motivo da correção</dt>
              <dd>{revisao.motivo}</dd>
            </>
          )}
        </dl>
        {(erro || props.erro) && (
          <p role="alert" className="message-error">
            {erro || props.erro}
          </p>
        )}
        {pendente && (
          <p className="field-hint">
            O resultado do envio ainda não foi confirmado. Tente novamente para verificar a mesma
            solicitação.
          </p>
        )}
        <div className="form-actions">
          <button
            type="button"
            className="primary-button"
            disabled={props.salvando}
            onClick={() => props.onSalvar(revisao)}
          >
            {props.salvando
              ? "Confirmando…"
              : pendente
                ? "Verificar solicitação"
                : saldo
                  ? "Confirmar correção"
                  : "Confirmar saldo inicial"}
          </button>
          {!pendente && (
            <button
              type="button"
              className="secondary-button"
              disabled={props.salvando}
              onClick={() => setRevisao(null)}
            >
              Voltar
            </button>
          )}
          <button
            type="button"
            className="secondary-button"
            disabled={props.salvando}
            onClick={props.onCancelar}
          >
            {pendente ? "Consultar listagem" : "Cancelar"}
          </button>
        </div>
      </div>
    );

  return (
    <form
      className="saldo-form"
      onSubmit={revisar}
      aria-label={saldo ? "Corrigir saldo inicial" : "Adicionar saldo inicial"}
    >
      <fieldset disabled={props.salvando} className="saldo-campos">
        <label htmlFor="saldo-instituicao">Instituição</label>
        <select
          id="saldo-instituicao"
          required
          value={instituicaoId}
          onChange={(e) => setInstituicao(e.target.value)}
        >
          <option value="">Selecione</option>
          {props.instituicoes.map((i) => (
            <option key={i.id} value={i.id} disabled={!i.ativo}>
              {i.nome}
              {!i.ativo ? " — inativa" : ""}
            </option>
          ))}
        </select>
        <label htmlFor="saldo-ativo">Ativo</label>
        <select
          id="saldo-ativo"
          required
          value={ativoId}
          onChange={(e) => {
            setAtivo(e.target.value);
            setZero(false);
            setJustificativa("");
          }}
        >
          <option value="">Selecione</option>
          {props.ativos.map((a) => (
            <option key={a.id} value={a.id} disabled={!a.ativo}>
              {a.ticker} — {a.nome}
              {!a.ativo ? " — inativo" : ""}
            </option>
          ))}
        </select>
        <label htmlFor="saldo-data">Data de início do acompanhamento</label>
        <input
          id="saldo-data"
          type="date"
          required
          value={data}
          disabled={props.possuiConfirmados}
          onChange={(e) => setData(e.target.value)}
        />
        <p className="field-hint">
          Posição antes das movimentações desse dia. Esta data será comum a todos os seus saldos
          iniciais.
        </p>
        <label htmlFor="saldo-quantidade">Quantidade</label>
        <input
          id="saldo-quantidade"
          inputMode="decimal"
          autoComplete="off"
          required
          value={quantidade}
          onChange={(e) => setQuantidade(e.target.value)}
        />
        <label htmlFor="saldo-custo">Custo histórico total em reais</label>
        <input
          id="saldo-custo"
          inputMode="decimal"
          autoComplete="off"
          required
          value={custoBrl}
          readOnly={ativo?.codigo === "fiat:BRL"}
          onChange={(e) => {
            setCusto(e.target.value);
            setZero(false);
            setJustificativa("");
          }}
        />
        <p className="field-hint">
          Use vírgula para decimais. Informe o custo histórico conhecido; para BRL, ele corresponde
          à quantidade.
        </p>
        {custoBrl.trim() && !/[1-9]/.test(custoBrl) && (
          <>
            <label className="check-label">
              <input type="checkbox" checked={zero} onChange={(e) => setZero(e.target.checked)} />{" "}
              Confirmo que o custo histórico é zero
            </label>
            <label htmlFor="saldo-justificativa">Justificativa do custo zero</label>
            <textarea
              id="saldo-justificativa"
              maxLength={300}
              required
              value={justificativa}
              onChange={(e) => setJustificativa(e.target.value)}
            />
          </>
        )}
        {saldo && (
          <>
            <label htmlFor="saldo-motivo">Motivo da correção</label>
            <textarea
              id="saldo-motivo"
              required
              maxLength={300}
              value={motivo}
              onChange={(e) => setMotivo(e.target.value)}
            />
          </>
        )}
      </fieldset>
      {(erro || props.erro) && (
        <p className="message-error" role="alert">
          {erro || props.erro}
        </p>
      )}
      <div className="form-actions">
        <button className="primary-button" type="submit" disabled={props.salvando}>
          Revisar dados
        </button>
        <button
          className="secondary-button"
          type="button"
          disabled={props.salvando}
          onClick={props.onCancelar}
        >
          Cancelar
        </button>
      </div>
    </form>
  );
}

export function dataPtBr(data: string): string {
  return data.split("-").reverse().join("/");
}
