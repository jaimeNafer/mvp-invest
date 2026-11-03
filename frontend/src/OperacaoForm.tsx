import { useEffect, useMemo, useRef, useState } from "react";
import CalculoOperacao from "./CalculoOperacao";
import CamposOperacao from "./CamposOperacao";
import { prepararPedidoPrevia } from "./services/cadastroOperacao";
import type { CamposOperacao as Campos } from "./services/cadastroOperacao";
import PosicoesAtuais from "./PosicoesAtuais";
import { decimalPtBr } from "./services/decimais";
import { ErroHttp } from "./services/http";
import type { Instituicao } from "./services/instituicoes";
import {
  confirmarOperacao,
  consultarPrevia,
  dataHoraFormulario,
  mensagemOperacao,
} from "./services/operacoes";
import type { ComandoOperacao, Operacao, PedidoPrevia, Previa } from "./services/operacoes";
import type { Ativo } from "./services/saldosIniciais";

type Props = {
  ativos: Ativo[];
  instituicoes: Instituicao[];
  operacao: Operacao | null;
  cancelar: boolean;
  onFechar: () => void;
  onConcluido: () => void;
  onBloqueio: (bloqueado: boolean) => void;
};

export default function OperacaoForm({
  ativos,
  instituicoes,
  operacao,
  cancelar,
  onFechar,
  onConcluido,
  onBloqueio,
}: Props) {
  const [campos, setCampos] = useState<Campos>({
    tipo: operacao?.tipo === "VENDA" || operacao?.tipo === "APORTE" ? operacao.tipo : "COMPRA",
    instituicaoId: operacao?.instituicaoId ?? "",
    ativoId: operacao?.ativoId ?? "",
    data: operacao ? dataHoraFormulario(operacao.ocorridaEm) : "",
    quantidade: decimalPtBr(operacao?.quantidade ?? ""),
    moedaCotacaoId: operacao?.moedaCotacaoId ?? "",
    totalBruto: decimalPtBr(operacao?.totalBruto ?? ""),
    cotacaoBrl: decimalPtBr(operacao?.cotacaoBrl ?? ""),
    taxaAtivoId: operacao?.taxaAtivoId ?? "",
    taxaQuantidade: operacao?.taxaAtivoId ? decimalPtBr(operacao.taxaQuantidade) : "",
    referencia: operacao?.referencia ?? "",
    motivo: "",
  });
  const [previa, setPrevia] = useState<{ resposta: Previa; pedido: PedidoPrevia } | null>(null);
  const [consultando, setConsultando] = useState(false);
  const [salvando, setSalvando] = useState(false);
  const [erro, setErro] = useState("");

  const [incerta, setIncerta] = useState<ComandoOperacao | null>(null);
  const sequencia = useRef(0);
  const envio = useRef(false);
  const pedido = useMemo(
    () => prepararPedidoPrevia(campos, ativos, operacao, cancelar),
    [campos, ativos, operacao, cancelar],
  );
  const bloqueado = salvando || !!incerta;

  useEffect(() => {
    onBloqueio(bloqueado);
  }, [bloqueado, onBloqueio]);
  useEffect(() => {
    if (!bloqueado) return;
    function proteger(event: BeforeUnloadEvent) {
      event.preventDefault();
    }
    window.addEventListener("beforeunload", proteger);
    return () => window.removeEventListener("beforeunload", proteger);
  }, [bloqueado]);
  useEffect(() => {
    if (!pedido || incerta) return;
    const controller = new AbortController();
    const consulta = ++sequencia.current;
    const timer = window.setTimeout(async () => {
      setConsultando(true);
      try {
        const resposta = await consultarPrevia(
          pedido,
          AbortSignal.any([controller.signal, AbortSignal.timeout(10000)]),
        );
        if (!controller.signal.aborted && consulta === sequencia.current)
          setPrevia({ resposta, pedido });
      } catch (falha) {
        if (!controller.signal.aborted && consulta === sequencia.current)
          setErro(mensagemOperacao(falha));
      } finally {
        if (!controller.signal.aborted && consulta === sequencia.current) setConsultando(false);
      }
    }, 350);
    return () => {
      window.clearTimeout(timer);
      controller.abort();
    };
  }, [pedido, incerta]);

  function alterar(alteracao: Partial<Campos>) {
    sequencia.current++;
    setPrevia(null);
    setConsultando(false);
    setErro("");
    setCampos((c) => ({ ...c, ...alteracao }));
  }
  async function confirmar() {
    if (envio.current || (!incerta && (!previa || consultando))) return;
    const p = previa?.pedido;
    const comando: ComandoOperacao | null =
      incerta ??
      (p && previa
        ? {
            caminho:
              p.acao === "CRIAR"
                ? "/operacoes"
                : `/operacoes/${encodeURIComponent(p.operacaoId)}/${p.acao === "CANCELAR" ? "cancelamento" : "substituicoes"}`,
            entrada:
              p.acao === "CANCELAR"
                ? { revisao: previa.resposta.revisao, motivo: p.motivo }
                : { revisao: previa.resposta.revisao, operacao: p.operacao },
            chave: crypto.randomUUID(),
          }
        : null);
    if (!comando) return;
    envio.current = true;
    setSalvando(true);
    setErro("");
    onBloqueio(true);
    try {
      await confirmarOperacao(comando);
      setIncerta(null);
      onBloqueio(false);
      onConcluido();
    } catch (falha) {
      setErro(mensagemOperacao(falha));
      if (!(falha instanceof ErroHttp) || falha.status >= 500) setIncerta(comando);
      else {
        setIncerta(null);
        sequencia.current++;
        setPrevia(null);
        setConsultando(false);
        setCampos((c) => ({ ...c }));
        onBloqueio(false);
      }
    } finally {
      envio.current = false;
      setSalvando(false);
    }
  }

  return (
    <form
      className="saldo-form"
      onSubmit={(e) => {
        e.preventDefault();
        void confirmar();
      }}
      aria-label={
        cancelar ? "Cancelar operação" : operacao ? "Corrigir operação" : "Adicionar operação"
      }
    >
      {cancelar ? (
        <p>
          O registro será preservado no histórico. Confira a posição resultante antes de confirmar.
        </p>
      ) : (
        <p className="field-hint">
          Informe os valores brutos do extrato. Use vírgula para decimais. Os resultados são
          atualizados antes da confirmação.
        </p>
      )}
      <CamposOperacao
        campos={campos}
        ativos={ativos}
        instituicoes={instituicoes}
        cancelar={cancelar}
        corrigindo={!!operacao}
        bloqueado={bloqueado}
        alterar={alterar}
      />
      {erro && (
        <p role="alert" className="message-error">
          {erro}
        </p>
      )}
      {incerta && (
        <p role="status" className="pending-request">
          O resultado do envio ainda não foi confirmado. Verifique a mesma solicitação antes de sair
          ou registrar outra alteração.
        </p>
      )}
      {consultando ? (
        <p role="status">Atualizando prévia…</p>
      ) : (
        !previa &&
        !incerta && (
          <p className="field-hint">
            Preencha os campos obrigatórios com valores válidos para consultar a prévia.
          </p>
        )
      )}
      {previa && (
        <section aria-label="Prévia da operação" className="operation-preview">
          <h3>Confira a alteração</h3>
          <p className="field-hint">
            A prévia ainda não foi registrada. Confira os resultados e a posição após a alteração.
          </p>
          {previa.resposta.operacao?.precoUnitario && (
            <dl className="saldo-resumo">
              <dt>Preço unitário</dt>
              <dd>
                {decimalPtBr(previa.resposta.operacao.precoUnitario)}{" "}
                {previa.resposta.operacao.moedaCotacaoTicker}
              </dd>
            </dl>
          )}
          {previa.resposta.calculo && <CalculoOperacao calculo={previa.resposta.calculo} />}
          <details>
            <summary>Ver posição após a alteração</summary>
            <PosicoesAtuais posicoes={previa.resposta.posicoes} previa />
          </details>
        </section>
      )}
      {!previa && pedido && !consultando && !incerta && erro && (
        <button
          type="button"
          className="secondary-button"
          onClick={() => {
            setErro("");
            setCampos((c) => ({ ...c }));
          }}
        >
          Atualizar prévia
        </button>
      )}
      <div className="form-actions">
        <button
          type="submit"
          className="primary-button"
          disabled={salvando || (!incerta && (!previa || consultando))}
        >
          {salvando
            ? "Confirmando…"
            : incerta
              ? "Verificar solicitação"
              : cancelar
                ? "Confirmar cancelamento"
                : operacao
                  ? "Confirmar correção"
                  : "Confirmar operação"}
        </button>
        <button type="button" className="secondary-button" disabled={bloqueado} onClick={onFechar}>
          Voltar
        </button>
      </div>
    </form>
  );
}
